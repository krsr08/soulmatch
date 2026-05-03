const repo = require('../repositories/profileRepository');
const media = require('../services/mediaService');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');

const ALLOWED_VERIFICATION_TYPES = new Set(['profile', 'identity', 'photo', 'education', 'income', 'family']);
const ALLOWED_ASSIST_SUPPORT_LEVELS = new Set(['self_service', 'family_assisted', 'advisor_assisted']);
const isBlank = (value) => typeof value !== 'string' || !value.trim();
const hasPositiveNumber = (value) => Number.isFinite(Number(value)) && Number(value) > 0;

async function requireOwnedProfile(profileId, userId) {
  const profile = await repo.findById(profileId);
  if (!profile) throw new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found');
  if (profile.user_id !== userId) throw new AppError(403, ErrorCodes.FORBIDDEN, 'Not authorized');
  return profile;
}

function validateStepData(step, data) {
  switch (step) {
    case 1:
      if (isBlank(data.firstName) || isBlank(data.lastName) || isBlank(data.dob) || isBlank(data.religion) || isBlank(data.motherTongue)) {
        return 'Basic details need first name, last name, date of birth, religion, and mother tongue.';
      }
      return null;
    case 2:
      if (!hasPositiveNumber(data.heightCm) || isBlank(data.complexion)) {
        return 'Physical details need height and complexion before you can save this step.';
      }
      return null;
    case 3:
      if (isBlank(data.educationLevel) || isBlank(data.occupation) || isBlank(data.workingCity)) {
        return 'Education details need education level, occupation, and working city.';
      }
      return null;
    case 4:
      if (isBlank(data.familyType) || isBlank(data.familyCity)) {
        return 'Family details need family type and family city.';
      }
      return null;
    case 5:
      if (isBlank(data.diet) || isBlank(data.aboutMe) || data.aboutMe.trim().length < 30) {
        return 'Lifestyle details need diet and an "About me" section with at least 30 characters.';
      }
      return null;
    case 6:
      return null;
    default:
      return 'Step must be 1-6';
  }
}

function normalizeVerificationType(type) {
  const normalized = String(type || 'profile').trim().toLowerCase().replace(/\s+/g, '_');
  return ALLOWED_VERIFICATION_TYPES.has(normalized) ? normalized : null;
}

function normalizeDocumentUrl(body) {
  const raw = body?.documentUrl || body?.document_url || null;
  if (!raw) return null;
  const value = String(raw).trim();
  if (!value) return null;
  if (/^https?:\/\//i.test(value) || value.startsWith('/uploads/')) return value;
  return false;
}

function normalizeProfileStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['active', 'inactive'].includes(normalized) ? normalized : null;
}

function normalizeAssistSupportLevel(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ALLOWED_ASSIST_SUPPORT_LEVELS.has(normalized) ? normalized : null;
}

function normalizeAssistPayload(body = {}) {
  const supportLevel = normalizeAssistSupportLevel(body.supportLevel || body.support_level || 'self_service');
  const isOptedIn = body.isOptedIn === true || body.is_opted_in === true || body.enabled === true;
  if (!supportLevel) return null;
  return {
    isOptedIn,
    supportLevel,
    preferredContactWindow: String(body.preferredContactWindow || body.preferred_contact_window || '').trim(),
    familyContactName: String(body.familyContactName || body.family_contact_name || '').trim(),
    familyContactPhone: String(body.familyContactPhone || body.family_contact_phone || '').trim(),
    notes: String(body.notes || '').trim()
  };
}

exports.createOrUpdateStep = async (req, res, next) => {
  try {
    const { step, ...data } = req.body;
    const userId = req.user.userId;
    const normalizedStep = Number.parseInt(step, 10);
    let profile = await repo.findByUserId(userId);
    if (!profile && normalizedStep !== 1) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Complete Step 1 first'));
    const validationError = validateStepData(normalizedStep, data);
    if (validationError) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, validationError));
    let result;
    switch (normalizedStep) {
      case 1: result = await repo.upsertBasicInfo(userId, data); break;
      case 2: result = await repo.upsertPhysical(userId, data); break;
      case 3: result = await repo.upsertEducation(userId, data); break;
      case 4: result = await repo.upsertFamily(userId, data); break;
      case 5: result = await repo.upsertLifestyle(userId, data); break;
      case 6: result = await repo.upsertHoroscope(userId, data); break;
      default: return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Step must be 1-6'));
    }
    const score = await repo.calcCompletion(result.profile_id);
    if (normalizedStep === 6 || score >= 60) await repo.setPublished(result.profile_id, true);
    res.json({ success: true, data: { profileId: result.profile_id, completionScore: score, step: normalizedStep } });
  } catch (err) { next(err); }
};
exports.getMyProfile = async (req, res, next) => {
  try {
    const p = await repo.findFullByUserId(req.user.userId);
    if (p?.profile_id) p.completion_score = await repo.calcCompletion(p.profile_id);
    res.json({ success: true, data: p, isNewProfile: !p });
  } catch (err) { next(err); }
};
exports.getAssistStatus = async (req, res, next) => {
  try {
    const status = await repo.getAssistStatusByUserId(req.user.userId);
    if (!status) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const recommendations = await repo.listAdvisorRecommendations(status.profileId, 3);
    res.json({
      success: true,
      data: {
        ...status,
        recommendations
      }
    });
  } catch (err) { next(err); }
};
exports.updateAssistStatus = async (req, res, next) => {
  try {
    const payload = normalizeAssistPayload(req.body);
    if (!payload) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Support level must be self_service, family_assisted, or advisor_assisted.'));
    }
    const updated = await repo.upsertAssistStatus(req.user.userId, payload);
    if (!updated) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const status = await repo.getAssistStatusByUserId(req.user.userId);
    const recommendations = status?.profileId ? await repo.listAdvisorRecommendations(status.profileId, 3) : [];
    res.json({
      success: true,
      data: {
        ...status,
        recommendations
      },
      message: payload.isOptedIn && payload.supportLevel === 'advisor_assisted'
        ? 'SoulMatch Assist has been updated. We assigned the best-fit advisor available for your area.'
        : 'SoulMatch Assist preferences saved.'
    });
  } catch (err) { next(err); }
};
exports.getProfile = async (req, res, next) => {
  try {
    const access = await repo.canViewProfile(req.params.profileId, req.user.userId);
    if (!access.allowed) {
      const status = access.reason === 'not_found' ? 404 : 403;
      const code = access.reason === 'not_found' ? ErrorCodes.NOT_FOUND : ErrorCodes.FORBIDDEN;
      return next(new AppError(status, code, access.reason === 'not_found' ? 'Profile not found' : 'This profile is not available to you.'));
    }
    const p = await repo.findFullById(req.params.profileId);
    if (!p) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    if (!access.owner) {
      const canSeePrivatePhoto = p.photo_privacy === 'all' || (p.photo_privacy === 'matches_only' && await repo.hasAcceptedInterestWithViewer(p.profile_id, req.user.userId));
      if (!canSeePrivatePhoto) {
        p.primary_photo_url = null;
      }
    }
    p.completion_score = await repo.calcCompletion(p.profile_id);
    repo.recordView(req.params.profileId, req.user.userId).catch(() => {});
    res.json({ success: true, data: p });
  } catch (err) { next(err); }
};
exports.updateProfile = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    await repo.update(req.params.profileId, req.body);
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.getPhotos = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    res.json({ success: true, data: await repo.getPhotos(req.params.profileId) });
  } catch (err) { next(err); }
};
exports.uploadPhotos = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    if (!req.files || !req.files.length) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'No photos uploaded'));
    const urls = await media.savePhotos(req.files, req.params.profileId);
    res.json({ success: true, data: { photoUrls: urls } });
  } catch (err) { next(err); }
};
exports.deletePhoto = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    await repo.deletePhoto(req.params.profileId, req.params.photoId);
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.setPrimaryPhoto = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    const updated = await repo.setPrimaryPhoto(req.params.profileId, req.params.photoId);
    if (!updated) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Photo not found'));
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.getPreferences = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    res.json({ success: true, data: await repo.getPreferences(req.params.profileId) });
  } catch (err) { next(err); }
};
exports.updatePreferences = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    await repo.upsertPreferences(req.params.profileId, req.body);
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.getCompletion = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    const score = await repo.calcCompletion(req.params.profileId);
    res.json({ success: true, data: { completionScore: score } });
  } catch (err) { next(err); }
};
exports.updatePrivacy = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    await repo.updatePrivacy(req.params.profileId, req.body);
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.updateProfileStatus = async (req, res, next) => {
  try {
    const status = normalizeProfileStatus(req.body?.profileStatus || req.body?.profile_status || req.body?.status);
    if (!status) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Profile status must be active or inactive.'));
    const profile = await repo.findByUserId(req.user.userId);
    if (!profile) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const updated = await repo.updateProfileStatus(profile.profile_id, status);
    if (!updated) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Profile status could not be updated.'));
    res.json({
      success: true,
      data: {
        profileId: updated.profile_id,
        profileStatus: updated.profile_status,
        profileVisibility: updated.profile_visibility
      }
    });
  } catch (err) { next(err); }
};
exports.recordView = async (req, res, next) => { try { await repo.recordView(req.params.profileId, req.user.userId); res.json({ success: true }); } catch (err) { next(err); } };
exports.getViewers = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    res.json({ success: true, data: await repo.getViewers(req.params.profileId) });
  } catch (err) { next(err); }
};
exports.blockProfile = async (req, res, next) => {
  try {
    const ok = await repo.blockProfile(req.user.userId, req.params.profileId);
    if (!ok) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.reportProfile = async (req, res, next) => {
  try {
    const ok = await repo.reportProfile(req.user.userId, req.params.profileId, req.body?.reason, req.body?.description);
    if (!ok) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.getVerifications = async (req, res, next) => {
  try {
    const profile = await requireOwnedProfile(req.params.profileId, req.user.userId);
    res.json({ success: true, data: await repo.getVerificationRequests(profile.user_id) });
  } catch (err) { next(err); }
};
exports.submitVerification = async (req, res, next) => {
  try {
    const profile = await requireOwnedProfile(req.params.profileId, req.user.userId);
    const type = normalizeVerificationType(req.body?.type);
    if (!type) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Verification type must be profile, identity, photo, education, income, or family.'));
    const documentUrl = normalizeDocumentUrl(req.body);
    if (documentUrl === false) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Document URL must be an HTTPS URL or a SoulMatch upload path.'));
    const photoCount = await repo.getPhotoCount(req.params.profileId);
    if (photoCount < 1) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Add at least one profile photo before requesting verification.'));
    const completionScore = await repo.calcCompletion(req.params.profileId);
    if (completionScore < 60) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Complete your profile to at least 60% before requesting verification.'));

    const result = await repo.createVerificationRequest(profile, { type, documentUrl });
    if (result.status === 'already_verified') {
      return res.json({ success: true, data: null, message: 'Your profile is already verified.' });
    }
    if (result.status === 'already_pending') {
      return res.json({ success: true, data: result.verification, message: 'Your verification request is already in review.' });
    }
    res.json({ success: true, data: result.verification, message: 'Verification request submitted for admin review.' });
  } catch (err) { next(err); }
};
