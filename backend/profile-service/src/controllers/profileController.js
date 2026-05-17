const repo = require('../repositories/profileRepository');
const media = require('../services/mediaService');
const aiAssist = require('../services/aiAssistService');
const secureDocuments = require('../services/secureDocumentService');
const { invalidateAllMatchFeeds } = require('../services/feedCache');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const { redactProfileForViewer } = require('../../../shared/profileVisibility');

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
const AGENT_REVIEW_STATUSES = new Set(['draft', 'submitted', 'under_review', 'verified', 'rejected']);
const AGENT_KYC_DOCUMENT_TYPES = new Set(['aadhaar', 'pan', 'voter_id', 'cancelled_cheque']);
const AGENT_DOCUMENT_SIDES = new Set(['front', 'back', 'single']);
const AGENT_TERMS_VERSION = 'agent-terms-2026-05-10-v1';

function requireAgentAccount(req) {
  if (req.user?.userType !== 'agent') {
    throw new AppError(403, ErrorCodes.FORBIDDEN, 'This action is available only to approved agent accounts.');
  }
}

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

async function requireEditableProfileForUser(req, profileId) {
  if (req.user?.userType === 'agent') {
    const profile = await repo.getManagedProfileByAgentUserId(req.user.userId, profileId);
    if (!profile) throw new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found');
    return profile;
  }
  return requireOwnedProfile(profileId, req.user.userId);
}

function validateStepData(step, data) {
  switch (step) {
    case 1:
      if (isBlank(data.firstName)) return 'First name is required.';
      if (isBlank(data.lastName)) return 'Last name is required.';
      if (isBlank(data.dob)) return 'Date of birth is required in DD-MM-YYYY format.';
      if (isBlank(data.gender)) return 'Gender is required.';
      if (isBlank(data.religion)) return 'Religion is required.';
      if (isBlank(data.caste)) return 'Community / caste is required.';
      if (isBlank(data.motherTongue)) return 'Mother tongue is required.';
      if (isBlank(data.maritalStatus)) return 'Marital status is required.';
      return null;
    case 2:
      if (!hasPositiveNumber(data.heightCm)) return 'Height must be a valid number.';
      if (!hasPositiveNumber(data.weightKg)) return 'Weight must be a valid number.';
      if (isBlank(data.complexion)) return 'Complexion is required.';
      if (isBlank(data.bodyType)) return 'Body type is required.';
      if (isBlank(data.bloodGroup)) return 'Blood group is required.';
      return null;
    case 3: {
      const isEmployed = data.isEmployed === true || String(data.isEmployed).toLowerCase() === 'true';
      if (isBlank(data.educationLevel)) return 'Highest qualification is required.';
      if (!isEmployed) {
        return null;
      }
      if (isBlank(data.occupation)) return 'Occupation is required when employed.';
      if (isBlank(data.annualIncome)) return 'Annual income is required when employed.';
      if (isBlank(data.workingCity)) return 'Working city is required when employed.';
      if (isBlank(data.workingState)) return 'Working state is required when employed.';
      if (!/^\d{6}$/.test(String(data.workingPincode || '').trim())) return 'Working pincode must be a valid 6-digit code.';
      return null;
    }
    case 4:
      if (isBlank(data.fatherOccupation)) return 'Father occupation is required.';
      if (isBlank(data.motherOccupation)) return 'Mother occupation is required.';
      if (data.numBrothers == null || Number.isNaN(Number(data.numBrothers))) return 'Number of brothers is required.';
      if (data.numSisters == null || Number.isNaN(Number(data.numSisters))) return 'Number of sisters is required.';
      if (isBlank(data.familyType)) return 'Family type is required.';
      if (isBlank(data.familyCity)) return 'Family city is required.';
      if (!isBlank(data.familyPincode) && !/^\d{6}$/.test(String(data.familyPincode).trim())) return 'Family pincode must be a valid 6-digit code.';
      return null;
    case 5:
      if (isBlank(data.diet)) return 'Diet is required.';
      if (isBlank(data.aboutMe) || data.aboutMe.trim().length < 30) {
        return 'About me should be at least 30 characters.';
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

function formatDateOfBirthForDisplay(value) {
  const parts = parseDateParts(value);
  if (!parts) return value || null;
  const day = String(parts.day).padStart(2, '0');
  const month = String(parts.month).padStart(2, '0');
  return `${day}-${month}-${parts.year}`;
}

function formatProfileForResponse(profile) {
  if (!profile) return profile;
  return {
    ...profile,
    dob: formatDateOfBirthForDisplay(profile.dob)
  };
}

function normalizeStepData(step, data) {
  const normalized = { ...data };
    if (step === 1) {
      const dob = normalizeDateOfBirth(data.dob);
      if (!dob) {
        return {
          error: 'Enter date of birth as DD-MM-YYYY. Age must be between 18 and 80 years.'
        };
      }
      normalized.dob = dob;
    }
    if (step === 3) {
      normalized.isEmployed = data.isEmployed === true || String(data.isEmployed).toLowerCase() === 'true';
      normalized.workingPincode = String(data.workingPincode || '').trim();
    }
    if (step === 4) {
      normalized.familyPincode = String(data.familyPincode || '').trim();
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

function normalizeAgentReviewStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return AGENT_REVIEW_STATUSES.has(normalized) ? normalized : null;
}

function resolveUploadUrls(files = []) {
  return files.map((file) => {
    if (file.fileUrl) return file.fileUrl;
    const normalized = file.path.replace(/\\/g, '/');
    return normalized.startsWith('/uploads/') ? normalized : `/uploads/${normalized.split('/').slice(-1)[0]}`;
  });
}

function normalizeAgentKycDocumentType(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return AGENT_KYC_DOCUMENT_TYPES.has(normalized) ? normalized : null;
}

function normalizeAgentDocumentSide(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return AGENT_DOCUMENT_SIDES.has(normalized) ? normalized : 'single';
}

function parseAgentKycMeta(rawValue) {
  if (!rawValue) return [];
  if (Array.isArray(rawValue)) return rawValue;
  try {
    const parsed = JSON.parse(String(rawValue));
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

function coerceBoolean(value) {
  return value === true || value === 'true' || value === 1 || value === '1';
}

function normalizeAgentTermsVersion(value) {
  const normalized = String(value || AGENT_TERMS_VERSION).trim();
  return normalized ? normalized.slice(0, 64) : AGENT_TERMS_VERSION;
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
  const rawOptIn = body.isOptedIn ?? body.is_opted_in ?? body.enabled ?? false;
  const isOptedIn = rawOptIn === true ||
    rawOptIn === 1 ||
    String(rawOptIn).trim().toLowerCase() === 'true';
  const shareMode = String(body.shareMode || body.share_mode || 'single').trim().toLowerCase() === 'multiple'
    ? 'multiple'
    : 'single';
  const selectedAdvisorIds = Array.isArray(body.selectedAdvisorIds)
    ? body.selectedAdvisorIds
    : Array.isArray(body.selected_advisor_ids)
      ? body.selected_advisor_ids
      : typeof body.selectedAdvisorIds === 'string'
        ? body.selectedAdvisorIds.split(',').map((item) => item.trim())
        : typeof body.selected_advisor_ids === 'string'
          ? body.selected_advisor_ids.split(',').map((item) => item.trim())
          : [];
  if (!supportLevel) return null;
  return {
    isOptedIn,
    supportLevel,
    shareMode,
    selectedAdvisorIds: [...new Set(selectedAdvisorIds.map((item) => String(item || '').trim()).filter(Boolean))],
    preferredContactWindow: String(body.preferredContactWindow || body.preferred_contact_window || '').trim(),
    familyContactName: String(body.familyContactName || body.family_contact_name || '').trim(),
    familyContactPhone: String(body.familyContactPhone || body.family_contact_phone || '').trim(),
    notes: String(body.notes || '').trim()
  };
}

function decorateAssistDirectory(status, directory = { stats: {}, agents: [] }, recommendations = []) {
  const selectedAdvisorIds = new Set(status?.selectedAdvisorIds || []);
  const decorate = (advisor) => ({
    ...advisor,
    isSelected: selectedAdvisorIds.has(advisor.advisorId)
  });
  return {
    ...status,
    recommendations: recommendations.map(decorate),
    agentStats: directory.stats,
    agents: (directory.agents || []).map(decorate)
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
    await invalidateAllMatchFeeds();
    res.json({ success: true, data: { profileId: result.profile_id, completionScore: score, step: normalizedStep } });
  } catch (err) { next(err); }
};
exports.getMyProfile = async (req, res, next) => {
  try {
    const p = await repo.findFullByUserId(req.user.userId);
    if (p?.profile_id) p.completion_score = await repo.calcCompletion(p.profile_id);
    await attachTrustSummary(p);
    res.json({ success: true, data: formatProfileForResponse(p), isNewProfile: !p });
  } catch (err) { next(err); }
};
exports.getAssistStatus = async (req, res, next) => {
  try {
    const status = await repo.getAssistStatusByUserId(req.user.userId);
    if (!status) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const recommendations = await repo.listAdvisorRecommendations(status.profileId, 3);
    const directory = await repo.getActiveAdvisorDirectory(24);
    res.json({
      success: true,
      data: decorateAssistDirectory(status, directory, recommendations)
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
    const updated = await repo.upsertAssistStatus(req.user.userId, { ...payload, audit: auditMeta(req) });
    if (!updated) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const status = await repo.getAssistStatusByUserId(req.user.userId);
    const recommendations = status?.profileId ? await repo.listAdvisorRecommendations(status.profileId, 3) : [];
    const directory = await repo.getActiveAdvisorDirectory(24);
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
      data: decorateAssistDirectory(status, directory, recommendations),
      message: payload.isOptedIn && payload.supportLevel === 'advisor_assisted'
        ? payload.selectedAdvisorIds?.length
          ? `SoulMatch Assist updated. Your profile is now shared with ${payload.selectedAdvisorIds.length === 1 ? 'the selected agent' : `${payload.selectedAdvisorIds.length} selected agents`}.`
          : 'SoulMatch Assist has been updated. Choose one or more agents to share your profile.'
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
    let p = await repo.findFullById(req.params.profileId);
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
    p = await repo.decorateContactPrivacy(p, req.user.userId, access.owner);
    p = redactProfileForViewer(p, {
      owner: access.owner,
      canViewPhoto: p.can_view_photo !== false,
      canViewContact: ['owner', 'unlocked'].includes(p.contact_access_status)
    });
    if (!access.owner) {
      const meteredView = await repo.recordView(req.params.profileId, req.user.userId);
      if (meteredView?.allowed === false && meteredView.reason === 'limit_reached') {
        return next(new AppError(403, ErrorCodes.FORBIDDEN, meteredView.message || 'Limit reached. Extend your subscription to continue.'));
      }
    }
    res.json({ success: true, data: formatProfileForResponse(p) });
  } catch (err) { next(err); }
};

exports.unlockContact = async (req, res, next) => {
  try {
    const access = await repo.canViewProfile(req.params.profileId, req.user.userId);
    if (!access.allowed) {
      const status = access.reason === 'not_found' ? 404 : 403;
      const code = access.reason === 'not_found' ? ErrorCodes.NOT_FOUND : ErrorCodes.FORBIDDEN;
      return next(new AppError(status, code, access.reason === 'not_found' ? 'Profile not found' : 'This profile is not available to you.'));
    }
    const result = await repo.unlockContactDetails(req.params.profileId, req.user.userId);
    if (result.status === 'not_found') {
      return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    }
    const blockedStatuses = new Set(['owner_masked', 'upgrade_required', 'limit_reached']);
    if (blockedStatuses.has(result.status)) {
      return res.status(result.status === 'owner_masked' ? 200 : 403).json({
        success: result.status === 'owner_masked',
        data: result,
        error: result.status === 'owner_masked' ? null : {
          code: ErrorCodes.FORBIDDEN,
          message: result.message
        },
        message: result.message
      });
    }
    res.json({ success: true, data: result, message: result.message });
  } catch (err) { next(err); }
};

exports.suggestProfileBio = async (req, res, next) => {
  try {
    const profile = await requireOwnedProfile(req.params.profileId, req.user.userId);
    const fullProfile = await repo.findFullById(profile.profile_id);
    const result = await aiAssist.suggestBio({
      profile: fullProfile,
      currentBio: req.body?.currentBio || req.body?.current_bio || fullProfile?.about_me || ''
    });
    await repo.recordAiAssistEvent({
      userId: req.user.userId,
      profileId: profile.profile_id,
      eventType: 'ai_bio_suggestions',
      provider: result.provider,
      model: result.model,
      source: result.source,
      metadata: {
        suggestionCount: result.suggestions.length
      }
    });
    res.json({
      success: true,
      data: {
        ...result,
        notice: result.source === 'ai'
          ? 'Suggestions generated from your existing profile details. Please review before saving.'
          : 'AI suggestions are currently unavailable, so SoulMatch used local profile guidance.'
      }
    });
  } catch (err) { next(err); }
};

exports.generateIcebreakers = async (req, res, next) => {
  try {
    const access = await repo.canViewProfile(req.params.profileId, req.user.userId);
    if (!access.allowed || access.owner) {
      const status = access.reason === 'not_found' ? 404 : 403;
      const code = access.reason === 'not_found' ? ErrorCodes.NOT_FOUND : ErrorCodes.FORBIDDEN;
      return next(new AppError(status, code, access.reason === 'not_found' ? 'Profile not found' : 'This profile is not available for ice-breakers.'));
    }
    const [sourceProfile, targetProfile] = await Promise.all([
      repo.findFullByUserId(req.user.userId),
      repo.findFullById(req.params.profileId)
    ]);
    if (!sourceProfile || !targetProfile) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Profile not found'));
    const result = await aiAssist.generateIcebreakers({ sourceProfile, targetProfile });
    await repo.recordAiAssistEvent({
      userId: req.user.userId,
      profileId: sourceProfile.profile_id,
      targetProfileId: targetProfile.profile_id,
      eventType: 'ai_icebreakers',
      provider: result.provider,
      model: result.model,
      source: result.source,
      metadata: {
        suggestionCount: result.suggestions.length
      }
    });
    res.json({
      success: true,
      data: {
        ...result,
        safetyNote: 'Use these as a starting point. Avoid sharing private contact or financial details in early conversations.'
      }
    });
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
    await requireEditableProfileForUser(req, req.params.profileId);
    res.json({ success: true, data: await repo.getPhotos(req.params.profileId) });
  } catch (err) { next(err); }
};

exports.redirectMedia = async (req, res, next) => {
  try {
    await media.redirectToSignedMedia(req.params.token, res);
  } catch (err) { next(err); }
};

exports.uploadPhotos = async (req, res, next) => {
  try {
    await requireEditableProfileForUser(req, req.params.profileId);
    if (!req.files || !req.files.length) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'No photos uploaded'));
    const before = await repo.getPhotos(req.params.profileId);
    const urls = await media.savePhotos(req.files, req.params.profileId);
    await repo.recordConsentEvent({
      userId: req.user?.userId,
      profileId: req.params.profileId,
      consentType: 'photo_upload',
      status: 'granted',
      purpose: 'Member or agent uploaded profile photos for matrimonial discovery and privacy-controlled profile display.',
      metadata: {
        fileCount: req.files.length,
        uploadedUrlCount: urls.length
      },
      audit: auditMeta(req)
    });
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
    await requireEditableProfileForUser(req, req.params.profileId);
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
    await requireEditableProfileForUser(req, req.params.profileId);
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
    await invalidateAllMatchFeeds();
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
    await invalidateAllMatchFeeds();
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
exports.recordView = async (req, res, next) => {
  try {
    const result = await repo.recordView(req.params.profileId, req.user.userId);
    if (result?.allowed === false && result.reason === 'limit_reached') {
      return next(new AppError(403, ErrorCodes.FORBIDDEN, result.message || 'Limit reached. Extend your subscription to continue.'));
    }
    res.json({ success: true, data: result || { allowed: true } });
  } catch (err) { next(err); }
};
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
    if (type === 'profile') {
      const photoCount = await repo.getPhotoCount(req.params.profileId);
      if (photoCount < 1) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Add at least one profile photo before requesting verification.'));
      const completionScore = await repo.calcCompletion(req.params.profileId);
      if (completionScore < 60) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Complete your profile to at least 60% before requesting verification.'));
    }

    const result = await repo.createVerificationRequest(profile, { type, documentUrl, audit: auditMeta(req) });
    if (result.status === 'already_verified') {
      return res.json({ success: true, data: null, message: 'Your profile is already verified.' });
    }
    if (result.status === 'already_pending') {
      return res.json({ success: true, data: result.verification, message: 'Your verification request is already in review.' });
    }
    res.json({ success: true, data: result.verification, message: 'Verification request submitted for admin review.' });
  } catch (err) { next(err); }
};

exports.submitVerificationUpload = async (req, res, next) => {
  try {
    const profile = await requireOwnedProfile(req.params.profileId, req.user.userId);
    const type = normalizeVerificationType(req.body?.type);
    if (!type) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Verification type must be profile, identity, photo, education, income, or family.'));
    const uploadedUrl = resolveUploadUrls(req.file ? [req.file] : [])[0] || null;
    const documentUrl = uploadedUrl || normalizeDocumentUrl(req.body);
    if (documentUrl === false) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Document URL must be an HTTPS URL or a SoulMatch upload path.'));
    if (!documentUrl) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Upload a verification document before submitting.'));
    if (type === 'profile') {
      const photoCount = await repo.getPhotoCount(req.params.profileId);
      if (photoCount < 1) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Add at least one profile photo before requesting verification.'));
      const completionScore = await repo.calcCompletion(req.params.profileId);
      if (completionScore < 60) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Complete your profile to at least 60% before requesting verification.'));
    }

    const result = await repo.createVerificationRequest(profile, { type, documentUrl, audit: auditMeta(req) });
    if (result.status === 'already_verified') {
      return res.json({ success: true, data: null, message: 'Your profile is already verified.' });
    }
    if (result.status === 'already_pending') {
      return res.json({ success: true, data: result.verification, message: 'Your verification request is already in review.' });
    }
    res.json({ success: true, data: result.verification, message: 'Verification document submitted for admin review.' });
  } catch (err) { next(err); }
};

exports.getAgentProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const profile = await repo.getAgentProfileByUserId(req.user.userId);
    res.json({ success: true, data: profile, isNewAgent: !profile });
  } catch (err) { next(err); }
};

exports.upsertAgentOnboarding = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const body = req.body || {};
    if (isBlank(body.fullName) || isBlank(body.phone) || isBlank(body.city) || isBlank(body.state) || isBlank(body.businessName)) {
      return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Full name, phone, city, state, and business name are required.'));
    }
    const encryptedFiles = await secureDocuments.encryptUploadedFiles(req.files || []);
    const uploadUrls = resolveUploadUrls(encryptedFiles);
    const uploadedMeta = parseAgentKycMeta(body.kycDocumentMeta || body.kyc_document_meta);
    const inferredUploadDocuments = uploadUrls.map((fileUrl, index) => {
      const meta = uploadedMeta[index] || {};
      const fallbackType = index === 0 ? 'aadhaar' : index === 1 ? 'pan' : 'cancelled_cheque';
      const fileMeta = encryptedFiles[index] || {};
      return {
        documentType: normalizeAgentKycDocumentType(meta.documentType || meta.type) || fallbackType,
        documentSide: normalizeAgentDocumentSide(meta.documentSide || meta.side),
        fileUrl,
        isEncrypted: fileMeta.isEncrypted === true,
        encryptionAlgorithm: fileMeta.encryptionAlgorithm,
        encryptionKeyRef: fileMeta.encryptionKeyRef,
        encryptionIv: fileMeta.encryptionIv,
        contentSha256: fileMeta.contentSha256,
        originalFileName: fileMeta.originalFileName,
        mimeType: fileMeta.mimeType,
        fileSizeBytes: fileMeta.fileSizeBytes,
        extractedMetadata: meta.extractedMetadata || meta.extracted_metadata || {},
        verificationMetadata: {
          source: 'agent_upload',
          digilockerStatus: meta.digilockerStatus || 'not_connected',
          ocrStatus: meta.ocrStatus || 'pending_vendor'
        }
      };
    });
    const inlineDocuments = Array.isArray(body.kycDocuments)
      ? body.kycDocuments
      : parseAgentKycMeta(body.kycDocuments);
    const kycDocuments = inlineDocuments.concat(inferredUploadDocuments);
    const termsAccepted = coerceBoolean(body.termsAccepted || body.terms_accepted);
    // Mobile onboarding saves encrypted KYC and bank documents as a draft before
    // the final terms screen. The repository keeps the profile out of admin
    // review until termsAccepted is true.
    const saved = await repo.upsertAgentOnboarding(req.user.userId, {
      ...body,
      kycDocuments,
      termsAccepted,
      termsVersion: normalizeAgentTermsVersion(body.termsVersion || body.terms_version),
      audit: auditMeta(req)
    });
    await auditUserChange(req, {
      entityType: 'agent_onboarding',
      entityId: saved?.advisorId,
      action: 'agent_onboarding.submit',
      beforeData: {},
      afterData: saved || {}
    });
    res.json({ success: true, data: saved, message: 'Agent onboarding submitted for review.' });
  } catch (err) { next(err); }
};

exports.updateAgentProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const before = await repo.getAgentProfileByUserId(req.user.userId);
    const saved = await repo.updateAgentProfileByUserId(req.user.userId, {
      ...(req.body || {}),
      audit: auditMeta(req)
    });
    if (!saved) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found'));
    await auditUserChange(req, {
      entityType: 'agent_profile',
      entityId: saved.advisorId,
      action: 'agent_profile.update',
      beforeData: before || {},
      afterData: saved
    });
    res.json({ success: true, data: saved });
  } catch (err) { next(err); }
};

exports.createAgentPennyDropOrder = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const result = await repo.createAgentPennyDropOrder(req.user.userId);
    if (result.status === 'advisor_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found.'));
    if (result.status === 'bank_document_required') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Upload a cancelled cheque before bank verification.'));
    if (result.status === 'gateway_not_configured') return next(new AppError(503, ErrorCodes.INTERNAL_ERROR, 'Agent bank verification payment is not configured.'));
    if (result.status === 'gateway_error') return next(new AppError(502, ErrorCodes.INTERNAL_ERROR, 'Could not create bank verification order.'));
    res.json({ success: true, data: result.order });
  } catch (err) { next(err); }
};

exports.verifyAgentPennyDropPayment = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const result = await repo.verifyAgentPennyDropPayment(req.user.userId, req.body || {});
    if (result.status === 'advisor_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found.'));
    if (result.status === 'invalid_payment') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment details are incomplete.'));
    if (result.status === 'order_mismatch') return next(new AppError(409, ErrorCodes.VALIDATION_ERROR, 'Payment order does not match the latest bank verification order.'));
    if (result.status === 'signature_mismatch') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Payment signature could not be verified.'));
    res.json({ success: true, data: result.profile, message: 'Bank access payment confirmed. Final verification is pending review.' });
  } catch (err) { next(err); }
};

exports.getAgentMembership = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const membership = await repo.getAgentMembershipByUserId(req.user.userId);
    if (!membership) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent membership not found'));
    res.json({ success: true, data: membership });
  } catch (err) { next(err); }
};

exports.getAgentMembershipPlans = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    res.json({ success: true, data: await repo.getAgentMembershipPlans() });
  } catch (err) { next(err); }
};

exports.listManagedProfiles = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    res.json({ success: true, data: await repo.listManagedProfilesByAgentUserId(req.user.userId) });
  } catch (err) { next(err); }
};

exports.getManagedProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const profile = await repo.getManagedProfileByAgentUserId(req.user.userId, req.params.profileId);
    if (!profile) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    res.json({ success: true, data: profile });
  } catch (err) { next(err); }
};

exports.createManagedProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const result = await repo.createManagedProfileByAgentUserId(req.user.userId, req.body || {});
    if (result.status === 'advisor_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found'));
    if (result.status === 'advisor_not_approved') return next(new AppError(403, ErrorCodes.FORBIDDEN, 'Your agent account must be active and not rejected before creating member profiles.'));
    if (result.status === 'profile_limit_reached') return next(new AppError(403, ErrorCodes.FORBIDDEN, `Your current plan allows up to ${result.limit} active profiles.`));
    if (result.status === 'duplicate_contact') return next(new AppError(409, ErrorCodes.VALIDATION_ERROR, 'This mobile number or email is already linked to another profile.'));
    await auditUserChange(req, {
      profileId: result.profile?.profile_id,
      entityType: 'managed_profile',
      entityId: result.profile?.profile_id,
      action: 'managed_profile.create',
      beforeData: {},
      afterData: result.profile || {}
    });
    res.status(201).json({ success: true, data: result.profile });
  } catch (err) { next(err); }
};

exports.updateManagedProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const before = await repo.getManagedProfileByAgentUserId(req.user.userId, req.params.profileId);
    if (!before) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    const updated = await repo.updateManagedProfileByAgentUserId(req.user.userId, req.params.profileId, req.body || {});
    if (updated?.status === 'duplicate_contact') return next(new AppError(409, ErrorCodes.VALIDATION_ERROR, 'This mobile number or email is already linked to another profile.'));
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'managed_profile',
      entityId: req.params.profileId,
      action: 'managed_profile.update',
      beforeData: before,
      afterData: updated || {}
    });
    res.json({ success: true, data: updated });
  } catch (err) { next(err); }
};

exports.deleteManagedProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const result = await repo.deleteManagedProfileByAgentUserId(req.user.userId, req.params.profileId);
    if (result === false) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    if (result?.status === 'not_draft') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Only draft profiles can be deleted.'));
    res.json({ success: true });
  } catch (err) { next(err); }
};

exports.submitManagedProfile = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const result = await repo.submitManagedProfileByAgentUserId(req.user.userId, req.params.profileId);
    if (result.status === 'advisor_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found'));
    if (result.status === 'not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    if (result.status === 'incomplete_profile') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, `Complete more profile details before submitting. Current completion: ${result.completionScore}%.`));
    if (result.status === 'photos_required') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Upload at least one profile photo before submitting.'));
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'managed_profile',
      entityId: req.params.profileId,
      action: 'managed_profile.submit',
      beforeData: {},
      afterData: result.profile || {}
    });
    res.json({ success: true, data: result.profile, message: 'Profile submitted for admin review.' });
  } catch (err) { next(err); }
};

exports.listManagedProfileDocuments = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const documents = await repo.listManagedProfileDocumentsByAgentUserId(req.user.userId, req.params.profileId);
    if (!documents) return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    res.json({ success: true, data: documents });
  } catch (err) { next(err); }
};

exports.upsertManagedProfileDocument = async (req, res, next) => {
  try {
    requireAgentAccount(req);
    const uploadUrls = resolveUploadUrls(req.files || []);
    const fileUrl = uploadUrls[0] || normalizeDocumentUrl(req.body);
    if (fileUrl === false) return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Document URL must be an HTTPS URL or a SoulMatch upload path.'));
    const result = await repo.upsertManagedProfileDocumentByAgentUserId(req.user.userId, req.params.profileId, {
      ...req.body,
      fileUrl,
      audit: auditMeta(req)
    });
    if (result.status === 'advisor_not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Agent profile not found'));
    if (result.status === 'not_found') return next(new AppError(404, ErrorCodes.NOT_FOUND, 'Managed profile not found'));
    if (result.status === 'invalid_document') return next(new AppError(400, ErrorCodes.VALIDATION_ERROR, 'Document type and file are required.'));
    await auditUserChange(req, {
      profileId: req.params.profileId,
      entityType: 'managed_profile_document',
      entityId: result.document?.profile_document_id,
      action: 'managed_profile_document.upsert',
      beforeData: {},
      afterData: result.document || {}
    });
    res.json({ success: true, data: result.document });
  } catch (err) { next(err); }
};
