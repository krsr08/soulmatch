const { validationResult } = require('express-validator');
const otpService = require('../services/otpService');
const tokenService = require('../services/tokenService');
const userRepo = require('../repositories/userRepository');
const logger = require('../utils/logger');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const { getDB } = require('../config/database');
const { recordAnalyticsEvent } = require('../../shared/controlPlane');

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
    const { phone, otp, inviteCode, acquisitionSource } = req.body;
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
    let user = await userRepo.findByPhone(phone);
    const isNewUser = !user;
    let referral = null;
    if (inviteCode) {
      referral = await userRepo.findReferralCode(String(inviteCode).toUpperCase());
      if (!referral) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Invite code is invalid or expired'));
    }
    if (isNewUser) {
      user = await userRepo.create({
        phone,
        is_verified: true,
        referred_by_code: referral?.code || null,
        acquisition_source: acquisitionSource || referral?.channel || null,
        referred_at: referral ? new Date() : null
      });
      if (referral) {
        await userRepo.recordReferralRedemption({
          referralCodeId: referral.referral_code_id,
          referredUserId: user.user_id,
          referrerUserId: referral.owner_user_id,
          metadata: { phone, acquisitionSource: acquisitionSource || null }
        });
      }
    }
    else await userRepo.updateLastLogin(user.user_id);
    const { accessToken, refreshToken } = tokenService.generatePair({ userId: user.user_id, phone: user.phone });
    await tokenService.storeRefresh(user.user_id, refreshToken);
    if (isNewUser) {
      const db = await getDB();
      await recordAnalyticsEvent(db, {
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
    res.json({ success: true, data: { accessToken, refreshToken, userId: user.user_id, isNewUser } });
  } catch (err) { next(err); }
};

exports.googleLogin = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, errors.array()[0].msg));
    const { googleToken, inviteCode, acquisitionSource } = req.body;
    if (!googleToken) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'googleToken required'));
    const payload = await tokenService.verifyGoogleToken(googleToken);
    const { sub: googleId, email } = payload;
    let user = await userRepo.findByGoogleId(googleId);
    const isNewUser = !user;
    let referral = null;
    if (inviteCode) {
      referral = await userRepo.findReferralCode(String(inviteCode).toUpperCase());
      if (!referral) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Invite code is invalid or expired'));
    }
    if (isNewUser) {
      user = await userRepo.create({
        google_id: googleId,
        email,
        is_verified: true,
        referred_by_code: referral?.code || null,
        acquisition_source: acquisitionSource || referral?.channel || null,
        referred_at: referral ? new Date() : null
      });
      if (referral) {
        await userRepo.recordReferralRedemption({
          referralCodeId: referral.referral_code_id,
          referredUserId: user.user_id,
          referrerUserId: referral.owner_user_id,
          metadata: { email, acquisitionSource: acquisitionSource || null, method: 'google' }
        });
      }
    }
    const { accessToken, refreshToken } = tokenService.generatePair({ userId: user.user_id, email: user.email });
    await tokenService.storeRefresh(user.user_id, refreshToken);
    if (isNewUser) {
      const db = await getDB();
      await recordAnalyticsEvent(db, {
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
    res.json({ success: true, data: { accessToken, refreshToken, userId: user.user_id, isNewUser } });
  } catch (err) { next(err); }
};

exports.refreshToken = async (req, res, next) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'refreshToken required'));
    const decoded = tokenService.verifyRefresh(refreshToken);
    const stored = await tokenService.getRefresh(decoded.userId);
    if (stored !== refreshToken) return next(new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid refresh token'));
    const user = await userRepo.findById(decoded.userId);
    const tokens = tokenService.generatePair({ userId: decoded.userId });
    await tokenService.storeRefresh(decoded.userId, tokens.refreshToken);
    res.json({
      success: true,
      data: {
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: user?.user_id || decoded.userId,
        isNewUser: false
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
