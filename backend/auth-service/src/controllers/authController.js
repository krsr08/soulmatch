const { validationResult } = require('express-validator');
const otpService = require('../services/otpService');
const tokenService = require('../services/tokenService');
const firebaseAuthService = require('../services/firebaseAuthService');
const userRepo = require('../repositories/userRepository');
const logger = require('../utils/logger');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const { getDB } = require('../config/database');
const { recordServerAnalyticsEvent } = require('../../../shared/controlPlane');

function normalizeRequestedUserType(value) {
  if (value === undefined || value === null) return null;
  const text = String(value).trim();
  if (!text) return null;
  return userRepo.normalizeUserType(text);
}

function ensureRequestedUserType(user, requestedUserType) {
  const currentUserType = userRepo.normalizeUserType(user?.user_type);
  if (!requestedUserType) return currentUserType;
  if (currentUserType !== requestedUserType) {
    if (requestedUserType === 'agent' && currentUserType === 'member') {
      throw new AppError(409, ErrorCodes.VALIDATION_ERROR, 'This mobile number is already registered as a user. Please use a different number to register as an agent.');
    }
    throw new AppError(409, ErrorCodes.VALIDATION_ERROR, `This account is already registered as a ${currentUserType}. Please continue with that account type.`);
  }
  return currentUserType;
}

async function ensureAgentUpgradeAllowed(userId) {
  const db = await getDB();
  const [memberProfile, advisorProfile] = await Promise.all([
    db.query('SELECT 1 FROM profiles WHERE user_id=$1 LIMIT 1', [userId]),
    db.query('SELECT 1 FROM advisors WHERE user_id=$1 LIMIT 1', [userId])
  ]);
  if (memberProfile.rows.length || advisorProfile.rows.length) {
    throw new AppError(409, ErrorCodes.VALIDATION_ERROR, 'This mobile number is already registered as a user. Please use a different number to register as an agent.');
  }
}

async function ensureMemberSelectionAllowed(userId) {
  const db = await getDB();
  const [memberProfile, advisorProfile] = await Promise.all([
    db.query('SELECT 1 FROM profiles WHERE user_id=$1 LIMIT 1', [userId]),
    db.query('SELECT 1 FROM advisors WHERE user_id=$1 LIMIT 1', [userId])
  ]);
  if (memberProfile.rows.length || advisorProfile.rows.length) {
    throw new AppError(409, ErrorCodes.VALIDATION_ERROR, 'This account has already started a profile flow. Please continue with the current account type.');
  }
}

async function getStartedFlowState(userId) {
  const db = await getDB();
  const [memberProfile, advisorProfile] = await Promise.all([
    db.query('SELECT 1 FROM profiles WHERE user_id=$1 LIMIT 1', [userId]),
    db.query('SELECT 1 FROM advisors WHERE user_id=$1 LIMIT 1', [userId])
  ]);
  return {
    hasMemberProfile: memberProfile.rows.length > 0,
    hasAdvisorProfile: advisorProfile.rows.length > 0
  };
}

async function shouldRequireRoleSelection(user, requestedUserType) {
  if (requestedUserType || !user?.user_id) return false;
  const currentUserType = userRepo.normalizeUserType(user.user_type);
  if (currentUserType !== 'member' || user.role_selected_at) return false;
  const flowState = await getStartedFlowState(user.user_id);
  return !flowState.hasMemberProfile && !flowState.hasAdvisorProfile;
}

async function buildAuthPayload(user, tokens, options = {}) {
  const requestedUserType = options.requestedUserType || null;
  const resolvedUserType = userRepo.normalizeUserType(options.userType || user?.user_type);
  return {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    userId: user.user_id,
    isNewUser: options.isNewUser === true,
    userType: resolvedUserType,
    requiresRoleSelection: await shouldRequireRoleSelection(user, requestedUserType)
  };
}

async function recordNewUserConsent(req, user, method) {
  await userRepo.recordSignupConsent(user.user_id, {
    method,
    ipAddress: req.ip || req.socket?.remoteAddress || null,
    userAgent: req.get('user-agent') || null
  });
}

exports.sendOTP = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, errors.array()[0].msg));
    const { phone } = req.body;
    if (await otpService.isLocked(phone)) {
      const ttl = await otpService.getLockTTL(phone);
      return next(new AppError(429, ErrorCodes.OTP_LOCKED, 'Too many attempts. Try again in ' + Math.ceil(ttl/60) + ' minutes.'));
    }
    if (!await otpService.canSendOTP(phone)) {
      const ttl = await otpService.getSendTTL(phone);
      return next(new AppError(429, ErrorCodes.RATE_LIMIT_EXCEEDED || ErrorCodes.VALIDATION_ERROR, 'OTP request limit reached. Try again in ' + Math.ceil(Math.max(ttl, 60)/60) + ' minutes.'));
    }
    const otp = otpService.generateOTP();
    await otpService.storeOTP(phone, otp);
    let delivery;
    try {
      delivery = await otpService.sendSMS(phone, 'Your SoulMatch code: ' + otp + '. Valid 5 mins.');
    } catch (error) {
      await otpService.clearOTP(phone);
      throw error;
    }
    if (!delivery?.delivered) {
      await otpService.clearOTP(phone);
      return next(new AppError(503, ErrorCodes.SERVICE_UNAVAILABLE, 'OTP delivery is not configured on the server yet. Configure Twilio or enable mock OTP for local testing.'));
    }
    await otpService.incrementSendCount(phone);
    logger.info('OTP sent to ' + phone);
    res.json({ success: true, message: 'OTP sent', data: { expiresIn: 300 } });
  } catch (err) { next(err); }
};

exports.verifyOTP = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, errors.array()[0].msg));
    const { phone, otp, inviteCode, acquisitionSource, userType } = req.body;
    const requestedUserType = normalizeRequestedUserType(userType);
    const normalizedPhone = userRepo.normalizePhone(phone) || phone;
    if (!await otpService.hasOTP(phone)) return next(new AppError(400, ErrorCodes.OTP_EXPIRED, 'OTP expired. Request new one.'));
    const validOtp = await otpService.verifyStoredOTP(phone, otp);
    if (!validOtp) {
      await otpService.incrementAttempts(phone);
      const attempts = await otpService.getAttempts(phone);
      const remaining = Math.max(0, 3 - attempts);
      return next(new AppError(400, ErrorCodes.OTP_INVALID, 'Invalid OTP. ' + remaining + ' attempt(s) remaining.'));
    }
    await otpService.clearOTP(phone);
    await otpService.clearAttempts(phone);
    let user = await userRepo.findByPhone(normalizedPhone);
    const isNewUser = !user;
    let referral = null;
    if (inviteCode) {
      referral = await userRepo.findReferralCode(String(inviteCode).toUpperCase());
      if (!referral) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Invite code is invalid or expired'));
    }
    if (isNewUser) {
      const newUserType = requestedUserType || 'member';
      user = await userRepo.create({
        phone: normalizedPhone,
        is_verified: true,
        user_type: newUserType,
        role_selected_at: requestedUserType ? new Date() : null,
        referred_by_code: referral?.code || null,
        acquisition_source: acquisitionSource || referral?.channel || null,
        referred_at: referral ? new Date() : null
      });
      await recordNewUserConsent(req, user, 'otp');
      if (referral) {
        await userRepo.recordReferralRedemption({
          referralCodeId: referral.referral_code_id,
          referredUserId: user.user_id,
          referrerUserId: referral.owner_user_id,
          metadata: { phone: normalizedPhone, acquisitionSource: acquisitionSource || null }
        });
      }
    } else {
      ensureRequestedUserType(user, requestedUserType);
      await userRepo.updateLastLogin(user.user_id);
    }
    const resolvedUserType = ensureRequestedUserType(user, requestedUserType);
    const { accessToken, refreshToken } = tokenService.generatePair({ userId: user.user_id, phone: user.phone, userType: resolvedUserType });
    await tokenService.storeRefresh(user.user_id, refreshToken);
    if (isNewUser) {
      const db = await getDB();
      await recordServerAnalyticsEvent(db, {
        eventType: 'sign_up',
        serviceName: 'auth-service',
        userId: user.user_id,
        payload: {
          method: 'otp',
          inviteCode: referral?.code || null,
          acquisitionSource: acquisitionSource || referral?.channel || null
        }
      });
    }
    res.json({
      success: true,
      data: await buildAuthPayload(user, { accessToken, refreshToken }, {
        isNewUser,
        userType: resolvedUserType,
        requestedUserType
      })
    });
  } catch (err) { next(err); }
};

exports.googleLogin = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, errors.array()[0].msg));
    const { googleToken, inviteCode, acquisitionSource, userType } = req.body;
    const requestedUserType = normalizeRequestedUserType(userType);
    if (!googleToken) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'googleToken required'));
    const payload = await tokenService.verifyGoogleToken(googleToken);
    const { sub: googleId, email } = payload;
    if (!payload.email_verified) {
      return next(new AppError(401, ErrorCodes.UNAUTHORIZED, 'Google email must be verified before sign in.'));
    }
    let user = await userRepo.findByGoogleId(googleId);
    if (!user && email) {
      user = await userRepo.findByEmail(email);
      if (user && !user.google_id) {
        user = await userRepo.attachGoogleId(user.user_id, googleId);
      }
    }
    const isNewUser = !user;
    let referral = null;
    if (inviteCode) {
      referral = await userRepo.findReferralCode(String(inviteCode).toUpperCase());
      if (!referral) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Invite code is invalid or expired'));
    }
    if (isNewUser) {
      const newUserType = requestedUserType || 'member';
      user = await userRepo.create({
        google_id: googleId,
        email,
        is_verified: true,
        user_type: newUserType,
        role_selected_at: requestedUserType ? new Date() : null,
        referred_by_code: referral?.code || null,
        acquisition_source: acquisitionSource || referral?.channel || null,
        referred_at: referral ? new Date() : null
      });
      await recordNewUserConsent(req, user, 'google');
      if (referral) {
        await userRepo.recordReferralRedemption({
          referralCodeId: referral.referral_code_id,
          referredUserId: user.user_id,
          referrerUserId: referral.owner_user_id,
          metadata: { email, acquisitionSource: acquisitionSource || null, method: 'google' }
        });
      }
    } else {
      ensureRequestedUserType(user, requestedUserType);
      await userRepo.updateLastLogin(user.user_id);
    }
    const resolvedUserType = ensureRequestedUserType(user, requestedUserType);
    const { accessToken, refreshToken } = tokenService.generatePair({ userId: user.user_id, email: user.email, userType: resolvedUserType });
    await tokenService.storeRefresh(user.user_id, refreshToken);
    if (isNewUser) {
      const db = await getDB();
      await recordServerAnalyticsEvent(db, {
        eventType: 'sign_up',
        serviceName: 'auth-service',
        userId: user.user_id,
        payload: {
          method: 'google',
          inviteCode: referral?.code || null,
          acquisitionSource: acquisitionSource || referral?.channel || null
        }
      });
    }
    res.json({
      success: true,
      data: await buildAuthPayload(user, { accessToken, refreshToken }, {
        isNewUser,
        userType: resolvedUserType,
        requestedUserType
      })
    });
  } catch (err) { next(err); }
};

exports.firebasePhoneLogin = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, errors.array()[0].msg));
    const { firebaseToken, phone, inviteCode, acquisitionSource, userType } = req.body;
    const requestedUserType = normalizeRequestedUserType(userType);
    if (!firebaseToken) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'firebaseToken required'));

    const decoded = await firebaseAuthService.verifyPhoneToken(firebaseToken);
    const verifiedPhone = userRepo.normalizePhone(decoded.phone_number) || String(decoded.phone_number || '').trim();
    if (!verifiedPhone) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Verified phone number not found in Firebase token.'));
    }
    if (phone && (userRepo.normalizePhone(phone) || String(phone).trim()) !== verifiedPhone) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Phone number does not match Firebase verification.'));
    }

    let user = await userRepo.findByPhone(verifiedPhone);
    const isNewUser = !user;
    let referral = null;
    if (inviteCode) {
      referral = await userRepo.findReferralCode(String(inviteCode).toUpperCase());
      if (!referral) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Invite code is invalid or expired'));
    }

    if (isNewUser) {
      const newUserType = requestedUserType || 'member';
      user = await userRepo.create({
        phone: verifiedPhone,
        is_verified: true,
        user_type: newUserType,
        role_selected_at: requestedUserType ? new Date() : null,
        referred_by_code: referral?.code || null,
        acquisition_source: acquisitionSource || referral?.channel || null,
        referred_at: referral ? new Date() : null
      });
      await recordNewUserConsent(req, user, 'firebase_phone');
      if (referral) {
        await userRepo.recordReferralRedemption({
          referralCodeId: referral.referral_code_id,
          referredUserId: user.user_id,
          referrerUserId: referral.owner_user_id,
          metadata: { phone: verifiedPhone, acquisitionSource: acquisitionSource || null, method: 'firebase_phone' }
        });
      }
    } else {
      ensureRequestedUserType(user, requestedUserType);
      await userRepo.updateLastLogin(user.user_id);
    }

    const resolvedUserType = ensureRequestedUserType(user, requestedUserType);
    const { accessToken, refreshToken } = tokenService.generatePair({ userId: user.user_id, phone: user.phone, userType: resolvedUserType });
    await tokenService.storeRefresh(user.user_id, refreshToken);

    if (isNewUser) {
      const db = await getDB();
      await recordServerAnalyticsEvent(db, {
        eventType: 'sign_up',
        serviceName: 'auth-service',
        userId: user.user_id,
        payload: {
          method: 'firebase_phone',
          inviteCode: referral?.code || null,
          acquisitionSource: acquisitionSource || referral?.channel || null
        }
      });
    }

    res.json({
      success: true,
      data: await buildAuthPayload(user, { accessToken, refreshToken }, {
        isNewUser,
        userType: resolvedUserType,
        requestedUserType
      })
    });
  } catch (err) { next(err); }
};

exports.refreshToken = async (req, res, next) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'refreshToken required'));
    const decoded = tokenService.verifyRefresh(refreshToken);
    if (await tokenService.isRefreshRevoked(decoded)) {
      return next(new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid refresh token'));
    }
    const stored = await tokenService.getRefresh(decoded.userId);
    if (stored !== refreshToken) return next(new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid refresh token'));
    const user = await userRepo.findById(decoded.userId);
    const resolvedUserType = userRepo.normalizeUserType(user?.user_type || decoded.userType);
    const tokens = tokenService.generatePair({ userId: decoded.userId, userType: resolvedUserType });
    await tokenService.revokeRefresh(decoded);
    await tokenService.storeRefresh(decoded.userId, tokens.refreshToken);
    res.json({
      success: true,
      data: {
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: user?.user_id || decoded.userId,
        isNewUser: false,
        userType: resolvedUserType
      }
    });
  } catch (err) { next(err); }
};

exports.logout = async (req, res, next) => {
  try {
    await tokenService.clearRefresh(req.user.userId);
    res.json({ success: true, message: 'Logged out' });
  } catch (err) { next(err); }
};

exports.selectUserType = async (req, res, next) => {
  try {
    const requestedUserType = normalizeRequestedUserType(req.body?.userType);
    if (!requestedUserType || !['member', 'agent'].includes(requestedUserType)) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'userType must be member or agent'));
    }
    const user = await userRepo.findById(req.user.userId);
    if (!user) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Account not found'));

    const currentUserType = userRepo.normalizeUserType(user.user_type);
    if (currentUserType === requestedUserType) {
      const selectedUser = user.role_selected_at ? user : await userRepo.markRoleSelected(user.user_id);
      const flowState = await getStartedFlowState(user.user_id);
      const { accessToken, refreshToken } = tokenService.generatePair({
        userId: selectedUser.user_id,
        phone: selectedUser.phone,
        email: selectedUser.email,
        userType: currentUserType
      });
      await tokenService.storeRefresh(selectedUser.user_id, refreshToken);
      return res.json({
        success: true,
        data: {
          accessToken,
          refreshToken,
          userId: selectedUser.user_id,
          isNewUser: requestedUserType === 'member'
            ? !flowState.hasMemberProfile
            : !flowState.hasAdvisorProfile,
          userType: currentUserType,
          requiresRoleSelection: false
        }
      });
    }

    if (currentUserType === 'member' && requestedUserType === 'agent') {
      await ensureAgentUpgradeAllowed(user.user_id);
    } else if (currentUserType === 'agent' && requestedUserType === 'member') {
      await ensureMemberSelectionAllowed(user.user_id);
    } else {
      return next(new AppError(409, ErrorCodes.VALIDATION_ERROR, `This account is already registered as a ${currentUserType}. Please continue with that account type.`));
    }

    const updated = await userRepo.updateUserType(user.user_id, requestedUserType);
    const flowState = await getStartedFlowState(updated.user_id);
    const { accessToken, refreshToken } = tokenService.generatePair({
      userId: updated.user_id,
      phone: updated.phone,
      email: updated.email,
      userType: requestedUserType
    });
    await tokenService.storeRefresh(updated.user_id, refreshToken);

    const db = await getDB();
    await recordServerAnalyticsEvent(db, {
      eventType: 'account_type_selected',
      serviceName: 'auth-service',
      userId: updated.user_id,
      payload: { selectedUserType: requestedUserType }
    });

    res.json({
      success: true,
      data: {
        accessToken,
        refreshToken,
        userId: updated.user_id,
        isNewUser: requestedUserType === 'member'
          ? !flowState.hasMemberProfile
          : !flowState.hasAdvisorProfile,
        userType: requestedUserType,
        requiresRoleSelection: false
      }
    });
  } catch (err) { next(err); }
};
