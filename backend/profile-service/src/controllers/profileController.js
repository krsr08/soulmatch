const repo = require('../repositories/profileRepository');
const media = require('../services/mediaService');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');

const ALLOWED_VERIFICATION_TYPES = new Set(['profile', 'identity', 'photo', 'education', 'income', 'family']);
const ALLOWED_ASSIST_SUPPORT_LEVELS = new Set(['self_service', 'family_assisted', 'advisor_assisted']);
const ALLOWED_MATCH_FEEDBACK_ACTIONS = new Set([
  'viewed',
  'shortlisted',
  'passed',
  'not_interested',
  'more_like_this',
  'less_like_this',
  'reported_mismatch',
  'blocked'
]);
const isBlank = (value) => typeof value !== 'string' || !value.trim();
const hasPositiveNumber = (value) => Number.isFinite(Number(value)) && Number(value) > 0;
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const INDIAN_DATE_RE = /^\d{2}[-/]\d{2}[-/]\d{4}$/;

function auditMeta(req) {
  return {
    source: req.get('x-client-source') || 'member_app',
    ipAddress: req.ip || req.socket?.remoteAddress || null,
    userAgent: req.get('user-agent') || null
  };
}

async function auditUserChange(req, options) {
  await repo.recordUserChange({
    userId: req.user?.userId,
    ...auditMeta(req),
    ...options
  });
}

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

function parseDateParts(value) {
  const raw = String(value || '').trim();
  if (ISO_DATE_RE.test(raw)) {
    const [year, month, day] = raw.split('-').map(Number);
    return { year, month, day };
  }
  if (INDIAN_DATE_RE.test(raw)) {
    const [day, month, year] = raw.replace(/\//g, '-').split('-').map(Number);
    return { year, month, day };
  }
  return null;
}

function normalizeDateOfBirth(value) {
  const parts = parseDateParts(value);
  if (!parts) return null;
  const date = new Date(Date.UTC(parts.year, parts.month - 1, parts.day));
  const isRealDate = date.getUTCFullYear() === parts.year &&
    date.getUTCMonth() === parts.month - 1 &&
    date.getUTCDate() === parts.day;
  if (!isRealDate) return null;

  const today = new Date();
  const youngestAllowed = new Date(Date.UTC(today.getUTCFullYear() - 18, today.getUTCMonth(), today.getUTCDate()));
  const oldestAllowed = new Date(Date.UTC(today.getUTCFullYear() - 80, today.getUTCMonth(), today.getUTCDate()));
  if (date > youngestAllowed || date < oldestAllowed) return null;
  return date.toISOString().slice(0, 10);
}

function normalizeStepData(step, data) {
  const normalized = { ...data };
  if (step === 1) {
    const dob = normalizeDateOfBirth(data.dob);
    if (!dob) {
      return {
        error: 'Enter date of birth as YYYY-MM-DD or DD-MM-YYYY. Age must be between 18 and 80 years.'
      };
    }
    normalized.dob = dob;
  }
  return { data: normalized };
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

function normalizeFamilyDecisionPayload(body = {}) {
  const status = String(body.status || 'family_review').trim().toLowerCase();
  if (!['considering', 'family_review', 'call_scheduled', 'spoken', 'accepted', 'declined', 'archived'].includes(status)) {
    return null;
  }
  const familyVote = String(body.familyVote || body.family_vote || 'discuss').trim().toLowerCase();
  if (!['approve', 'reject', 'discuss'].includes(familyVote)) return null;
  return {
    status,
    familyVote,
    note: String(body.note || '').trim(),
    nextStep: String(body.nextStep || body.next_step || '').trim(),
    nextStepAt: body.nextStepAt || body.next_step_at || null
  };
}

function normalizeFamilyCommentPayload(body = {}) {
  const vote = String(body.vote || 'discuss').trim().toLowerCase();
  if (!['approve', 'reject', 'discuss'].includes(vote)) return null;
  const comment = String(body.comment || '').trim();
  if (!comment && vote === 'discuss') return null;
  return { vote, comment };
}

async function attachTrustSummary(profile) {
  if (!profile?.profile_id) return profile;
  const trust = await repo.getTrustSummary(profile.profile_id);
  profile.trust_score = trust.score;
  profile.trust_level = trust.level;
  profile.trust_signals = trust.signals;
  profile.trust_warnings = trust.warnings;
  profile.trust_factors = trust.factors || [];
  profile.trust_explanation = trust.explanation || null;
  profile.seriousness_score = trust.seriousness?.score || 0;
  profile.seriousness_level = trust.seriousness?.level || 'low';
  profile.seriousness_signals = trust.seriousness?.signals || [];
  profile.seriousness_warnings = trust.seriousness?.warnings || [];
  return profile;
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
    const normalizedStepData = normalizeStepData(normalizedStep, data);
    if (normalizedStepData.error) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, normalizedStepData.error));
    const dataForSave = normalizedStepData.data || data;
    const validationError = validateStepData(normalizedStep, dataForSave);
    if (validationError) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, validationError));
    const before = profile?.profile_id ? await repo.findFullById(profile.profile_id) : null;
    let result;
    switch (normalizedStep) {
      case 1: result = await repo.upsertBasicInfo(userId, dataForSave); break;
      case 2: result = await repo.upsertPhysical(userId, dataForSave); break;
      case 3: result = await repo.upsertEducation(userId, dataForSave); break;
      case 4: result = await repo.upsertFamily(userId, dataForSave); break;
      case 5: result = await repo.upsertLifestyle(userId, dataForSave); break;
      case 6: result = await repo.upsertHoroscope(userId, dataForSave); break;
      default: return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Step must be 1-6'));
    }
    const score = await repo.calcCompletion(result.profile_id);
    if (normalizedStep === 6 || score >= 60) await repo.setPublished(result.profile_id, true);
    const after = await repo.findFullById(result.profile_id);
    await auditUserChange(req, {
      profileId: result.profile_id,
      entityType: 'profile',
      entityId: result.profile_id,
      action: `profile.step_${normalizedStep}.save`,
      beforeData: before || {},
      afterData: after || {}
    });
    res.json({ success: true, data: { profileId: result.profile_id, completionScore: score, step: normalizedStep } });
  } catch (err) { next(err); }
};
exports.getMyProfile = async (req, res, next) => {
  try {
    const p = await repo.findFullByUserId(req.user.userId);
    if (p?.profile_id) p.completion_score = await repo.calcCompletion(p.profile_id);
    await attachTrustSummary(p);
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
    const before = await repo.getAssistStatusByUserId(req.user.userId);
    const updated = await repo.upsertAssistStatus(req.user.userId, payload);
    if (!updated) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const status = await repo.getAssistStatusByUserId(req.user.userId);
    const recommendations = status?.profileId ? await repo.listAdvisorRecommendations(status.profileId, 3) : [];
    await auditUserChange(req, {
      profileId: status?.profileId,
      entityType: 'assist_settings',
      entityId: status?.profileId,
      action: 'assist_settings.update',
      beforeData: before || {},
      afterData: status || {}
    });
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
      const photoState = await repo.getPhotoAccessState(p.profile_id, req.user.userId);
      p.can_view_photo = photoState.canViewPhoto;
      p.photo_access_status = photoState.photoAccessStatus;
      p.photo_access_request_id = photoState.photoAccessRequestId || null;
      if (!photoState.canViewPhoto) {
        p.primary_photo_url = null;
      }
    } else {
      p.can_view_photo = true;
      p.photo_access_status = 'owner';
    }
    p.completion_score = await repo.calcCompletion(p.profile_id);
    await attachTrustSummary(p);
    repo.recordView(req.params.profileId, req.user.userId).catch(() => {});
    res.json({ success: true, data: p });
  } catch (err) { next(err); }
};
exports.updateProfile = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    const before = await repo.findFullById(req.params.profileId);
    await repo.update(req.params.profileId, req.body);
    const after = await repo.findFullById(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'profile',
      entityId: req.params.profileId,
      action: 'profile.update',
      beforeData: before || {},
      afterData: after || {}
    });
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
    const before = await repo.getPhotos(req.params.profileId);
    const urls = await media.savePhotos(req.files, req.params.profileId);
    const after = await repo.getPhotos(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'profile_photos',
      entityId: req.params.profileId,
      action: 'profile_photos.upload',
      beforeData: { photos: before },
      afterData: { photos: after, uploadedUrls: urls }
    });
    res.json({ success: true, data: { photoUrls: urls } });
  } catch (err) { next(err); }
};
exports.deletePhoto = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    const before = await repo.getPhotos(req.params.profileId);
    await repo.deletePhoto(req.params.profileId, req.params.photoId);
    const after = await repo.getPhotos(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'profile_photos',
      entityId: req.params.photoId,
      action: 'profile_photos.delete',
      beforeData: { photos: before },
      afterData: { photos: after }
    });
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.setPrimaryPhoto = async (req, res, next) => {
  try {
    await requireOwnedProfile(req.params.profileId, req.user.userId);
    const before = await repo.getPhotos(req.params.profileId);
    const updated = await repo.setPrimaryPhoto(req.params.profileId, req.params.photoId);
    if (!updated) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Photo not found'));
    const after = await repo.getPhotos(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'profile_photos',
      entityId: req.params.photoId,
      action: 'profile_photos.set_primary',
      beforeData: { photos: before },
      afterData: { photos: after }
    });
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
    const before = await repo.getPreferences(req.params.profileId);
    await repo.upsertPreferences(req.params.profileId, req.body);
    const after = await repo.getPreferences(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'partner_preferences',
      entityId: req.params.profileId,
      action: 'partner_preferences.update',
      beforeData: before || {},
      afterData: after || {}
    });
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.recordMatchFeedback = async (req, res, next) => {
  try {
    const action = String(req.body?.action || 'viewed').trim();
    if (!ALLOWED_MATCH_FEEDBACK_ACTIONS.has(action)) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Match feedback action is not supported.'));
    }
    const result = await repo.recordMatchFeedback(req.user.userId, req.params.profileId, {
      ...req.body,
      action
    });
    if (result.status === 'source_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Your profile was not found.'));
    if (result.status === 'target_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Match profile was not found.'));
    if (result.status === 'own_profile') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Your own profile cannot be used as match feedback.'));
    await auditUserChange(req, {
      profileId: result.sourceProfile.profile_id,
      entityType: 'match_feedback',
      entityId: result.feedback.feedback_id,
      action: `match_feedback.${action}`,
      beforeData: {},
      afterData: result.feedback
    });
    res.json({ success: true, data: result.feedback, message: 'Match feedback recorded.' });
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
    const before = await repo.findById(req.params.profileId);
    await repo.updatePrivacy(req.params.profileId, req.body);
    const after = await repo.findById(req.params.profileId);
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'privacy_settings',
      entityId: req.params.profileId,
      action: 'privacy_settings.update',
      beforeData: before || {},
      afterData: after || {}
    });
    res.json({ success: true });
  } catch (err) { next(err); }
};
exports.updateProfileStatus = async (req, res, next) => {
  try {
    const status = normalizeProfileStatus(req.body?.profileStatus || req.body?.profile_status || req.body?.status);
    if (!status) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Profile status must be active or inactive.'));
    const profile = await repo.findByUserId(req.user.userId);
    if (!profile) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const before = { profileStatus: profile.profile_status, profileVisibility: profile.profile_visibility };
    const updated = await repo.updateProfileStatus(profile.profile_id, status);
    if (!updated) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Profile status could not be updated.'));
    await auditUserChange(req, {
      profileId: profile.profile_id,
      entityType: 'profile_status',
      entityId: profile.profile_id,
      action: 'profile_status.update',
      beforeData: before,
      afterData: {
        profileStatus: updated.profile_status,
        profileVisibility: updated.profile_visibility
      }
    });
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
exports.requestPhotoAccess = async (req, res, next) => {
  try {
    const access = await repo.canViewProfile(req.params.profileId, req.user.userId);
    if (!access.allowed) {
      const status = access.reason === 'not_found' ? 404 : 403;
      const code = access.reason === 'not_found' ? ErrorCodes.NOT_FOUND : ErrorCodes.FORBIDDEN;
      return next(new AppError(status, code, access.reason === 'not_found' ? 'Profile not found' : 'This profile is not available to you.'));
    }
    if (access.owner) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'You already own this profile photo.'));
    const photoState = await repo.getPhotoAccessState(req.params.profileId, req.user.userId);
    if (photoState.canViewPhoto) {
      return res.json({
        success: true,
        data: photoState,
        message: 'You can already view this profile photo.'
      });
    }
    const result = await repo.requestPhotoAccess(req.params.profileId, req.user.userId, req.body?.message);
    if (result.status === 'not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    if (result.status === 'own_profile') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'You already own this profile photo.'));
    await auditUserChange(req, {
      profileId: result.requesterProfile?.profile_id,
      entityType: 'photo_access_request',
      entityId: result.request?.photo_access_request_id,
      action: 'photo_access.request',
      beforeData: {},
      afterData: result.request || {}
    });
    res.json({
      success: true,
      data: {
        requestId: result.request?.photo_access_request_id,
        status: result.request?.status || result.status
      },
      message: result.request?.status === 'pending'
        ? 'Photo access request sent.'
        : 'Photo access request already exists.'
    });
  } catch (err) { next(err); }
};
exports.getPhotoAccessRequests = async (req, res, next) => {
  try {
    const requests = await repo.getPhotoAccessRequestsForOwner(req.user.userId);
    res.json({ success: true, data: requests });
  } catch (err) { next(err); }
};
exports.respondPhotoAccessRequest = async (req, res, next) => {
  try {
    const status = String(req.body?.status || '').trim().toLowerCase();
    if (!['approved', 'declined'].includes(status)) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Photo access status must be approved or declined.'));
    }
    const result = await repo.respondPhotoAccessRequest(req.params.requestId, req.user.userId, status);
    if (!result) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Photo access request not found.'));
    await auditUserChange(req, {
      profileId: result.after.target_profile_id,
      entityType: 'photo_access_request',
      entityId: result.after.photo_access_request_id,
      action: `photo_access.${status}`,
      beforeData: result.before || {},
      afterData: result.after || {}
    });
    res.json({
      success: true,
      data: {
        requestId: result.after.photo_access_request_id,
        status: result.after.status
      },
      message: status === 'approved' ? 'Photo access approved.' : 'Photo access declined.'
    });
  } catch (err) { next(err); }
};
exports.getFamilyDecisions = async (req, res, next) => {
  try {
    const decisions = await repo.listFamilyDecisions(req.user.userId);
    if (!decisions) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    res.json({ success: true, data: decisions });
  } catch (err) { next(err); }
};
exports.upsertFamilyDecision = async (req, res, next) => {
  try {
    const payload = normalizeFamilyDecisionPayload(req.body);
    if (!payload) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Family decision status is not valid.'));
    const access = await repo.canViewProfile(req.params.targetProfileId, req.user.userId);
    if (!access.allowed || access.owner) {
      const status = access.reason === 'not_found' ? 404 : 403;
      const code = access.reason === 'not_found' ? ErrorCodes.NOT_FOUND : ErrorCodes.FORBIDDEN;
      return next(new AppError(status, code, access.reason === 'not_found' ? 'Profile not found' : 'This profile cannot be added to your family board.'));
    }
    const result = await repo.upsertFamilyDecision(req.user.userId, req.params.targetProfileId, payload);
    if (result.status === 'owner_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    if (result.status === 'target_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Target profile not found'));
    if (result.status === 'own_profile') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Your own profile cannot be added to family review.'));
    await auditUserChange(req, {
      profileId: result.owner.profile_id,
      entityType: 'family_match_decision',
      entityId: result.after.family_decision_id,
      action: `family_decision.${result.status}`,
      beforeData: result.before || {},
      afterData: result.after || {}
    });
    res.json({
      success: true,
      data: {
        familyDecisionId: result.after.family_decision_id,
        targetProfileId: result.after.target_profile_id,
        status: result.after.status,
        familyVote: result.after.family_vote || 'discuss',
        note: result.after.note || '',
        nextStep: result.after.next_step || '',
        nextStepAt: result.after.next_step_at || null,
        updatedAt: result.after.updated_at
      },
      message: result.status === 'created' ? 'Added to your family decision board.' : 'Family decision updated.'
    });
  } catch (err) { next(err); }
};
exports.addFamilyDecisionComment = async (req, res, next) => {
  try {
    const payload = normalizeFamilyCommentPayload(req.body);
    if (!payload) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Family comment needs a vote of approve, reject, or discuss and a comment when discussing.'));
    const result = await repo.addFamilyDecisionComment(req.user.userId, req.params.familyDecisionId, payload);
    if (result.status === 'not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Family decision not found'));
    await auditUserChange(req, {
      profileId: result.decision.owner_profile_id,
      entityType: 'family_match_decision_comment',
      entityId: result.comment.family_comment_id,
      action: `family_decision.comment.${payload.vote}`,
      beforeData: {},
      afterData: result.comment
    });
    res.json({ success: true, data: result.comment, message: 'Family input recorded.' });
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
