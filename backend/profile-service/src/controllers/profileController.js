const repo = require('../repositories/profileRepository');
const media = require('../services/mediaService');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');

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
