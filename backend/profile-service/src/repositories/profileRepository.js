const { getDB } = require('../config/database');
const { createHash, createHmac, randomUUID } = require('crypto');
const { scoreAdvisorCandidate, normalizeList } = require('../services/assistAllocationService');
const { getConfigSection } = require('../../shared/controlPlane');
const { consumeMeter, ensureUsageRecord, getActivePlanId, getEntitlements, periodKey } = require('../../shared/memberEntitlements');
const { evaluateProfileVisibility } = require('../../shared/profileVisibility');
const logger = require('../utils/logger');
const DPDP_NOTICE_VERSION = 'dpdp-2026-05-10-v1';

function parseJsonList(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return normalizeList(value);
    }
  }
  return [];
}

function toComparableJson(value) {
  if (value === undefined || value === null) return null;
  return JSON.parse(JSON.stringify(value));
}

function buildChangedFields(beforeData = {}, afterData = {}) {
  const before = toComparableJson(beforeData) || {};
  const after = toComparableJson(afterData) || {};
  const keys = new Set([...Object.keys(before), ...Object.keys(after)]);
  return [...keys].filter((key) => JSON.stringify(before[key] ?? null) !== JSON.stringify(after[key] ?? null));
}

async function notifyMember(db, userId, title, body, data = {}) {
  const payload = { userId, title, body, data };
  const notificationUrl = process.env.NOTIFICATION_API_URL;
  const internalSecret = process.env.INTERNAL_SERVICE_SECRET;
  if (notificationUrl && internalSecret) {
    try {
      const response = await fetch(`${notificationUrl.replace(/\/$/, '')}/send`, {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'x-internal-service-secret': internalSecret
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(5000)
      });
      if (response.ok) return true;
      logger.warn(`Notification service rejected profile notification: ${response.status}`);
    } catch (error) {
      logger.warn(`Notification service unavailable for profile notification: ${error.message}`);
    }
  }

  await db.query(
    `INSERT INTO notifications (notification_id,user_id,title,body,data,status,created_at)
     VALUES ($1,$2,$3,$4,$5::jsonb,'queued',NOW())`,
    [randomUUID(), userId, title, body, JSON.stringify(data || {})]
  );
  return false;
}

async function insertConsentEvent(client, {
  userId = null,
  profileId = null,
  consentType,
  status = 'granted',
  purpose,
  noticeVersion = DPDP_NOTICE_VERSION,
  metadata = {},
  audit = {}
}) {
  if (!consentType || !purpose) return null;
  const result = await client.query(
    `INSERT INTO consent_events (
       consent_event_id,
       user_id,
       profile_id,
       consent_type,
       status,
       purpose,
       notice_version,
       metadata,
       source,
       ip_address,
       user_agent,
       created_at
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8::jsonb,$9,$10,$11,NOW())
     RETURNING consent_event_id`,
    [
      randomUUID(),
      userId,
      profileId,
      consentType,
      status,
      purpose,
      noticeVersion,
      JSON.stringify(metadata || {}),
      audit.source || 'member_app',
      audit.ipAddress || null,
      audit.userAgent || null
    ]
  );
  return result.rows[0]?.consent_event_id || null;
}

function clampScore(value) {
  return Math.max(0, Math.min(100, Math.round(Number(value) || 0)));
}

function trustLevelForScore(score) {
  if (score >= 80) return 'high';
  if (score >= 55) return 'medium';
  return 'low';
}

function toBoolean(value) {
  return value === true || value === 'true' || value === 1 || value === '1';
}

function toIntegerOrNull(value) {
  if (value === undefined || value === null || value === '') return null;
  const parsed = parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function toIntegerOrDefault(value, fallback) {
  const parsed = toIntegerOrNull(value);
  return parsed === null ? fallback : parsed;
}

function toTextArray(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || '').trim()).filter(Boolean);
  }
  if (!value || typeof value !== 'string') return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return toTextArray(parsed);
  } catch (_) {
    // Fall back to comma-separated text.
  }
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function cleanShortText(value, maxLength = 120) {
  if (value === undefined || value === null) return null;
  const cleaned = String(value).trim();
  return cleaned ? cleaned.slice(0, maxLength) : null;
}

function parseVerificationTypes(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.map((item) => String(item || '').toLowerCase()).filter(Boolean);
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return parseVerificationTypes(parsed);
    } catch (_) {
      return value.split(',').map((item) => item.trim().toLowerCase()).filter(Boolean);
    }
  }
  return [];
}

function hasAnyVerification(types, names) {
  return names.some((name) => types.includes(name));
}

function buildTrustSummary(signals = {}) {
  const verificationStatus = String(signals.verification_status || signals.verificationStatus || 'pending').toLowerCase();
  const completionScore = clampScore(signals.completion_score ?? signals.completionScore);
  const photoCount = Number(signals.photo_count ?? signals.photoCount ?? 0);
  const approvedVerifications = Number(signals.approved_verifications ?? signals.approvedVerifications ?? 0);
  const pendingVerifications = Number(signals.pending_verifications ?? signals.pendingVerifications ?? 0);
  const reportCount = Number(signals.report_count ?? signals.reportCount ?? 0);
  const isPhoneVerified = toBoolean(signals.is_verified) || toBoolean(signals.isPhoneVerified);
  const hasEmail = Boolean(String(signals.email || '').trim());
  const isFirebaseVerified = toBoolean(signals.firebase_verified) || toBoolean(signals.firebaseVerified) || Boolean(signals.google_id || signals.googleId);
  const hasFamilyLocation = Boolean(signals.family_city || signals.familyCity || signals.family_pincode || signals.familyPincode);
  const profileStatus = String(signals.profile_status || signals.profileStatus || 'active').toLowerCase();
  const lastLogin = signals.last_login || signals.lastLogin;
  const recentlyActive = lastLogin ? Date.now() - new Date(lastLogin).getTime() <= 1000 * 60 * 60 * 24 * 30 : false;
  const approvedTypes = parseVerificationTypes(signals.approved_verification_types || signals.approvedVerificationTypes);
  if (approvedVerifications > 0 && verificationStatus === 'verified' && !approvedTypes.includes('profile')) approvedTypes.push('profile');
  const hasAdminVerification = verificationStatus === 'verified' || hasAnyVerification(approvedTypes, ['profile', 'identity']);
  const hasDocumentVerification = hasAnyVerification(approvedTypes, ['document', 'identity']);
  const hasEducationVerification = hasAnyVerification(approvedTypes, ['education']);
  const hasIncomeVerification = hasAnyVerification(approvedTypes, ['income']);
  const hasFamilyVerification = hasAnyVerification(approvedTypes, ['family']);
  const trustSignals = [];
  const warnings = [];
  const factors = [];
  let score = 0;

  const addFactor = (key, label, points, status, detail) => {
    factors.push({ key, label, points, status, detail });
    if (points > 0) score += points;
    if (points < 0) score += points;
  };

  if (isPhoneVerified) {
    addFactor('phone_verified', 'Phone verified', 12, 'positive', 'Mobile number has been verified.');
    trustSignals.push('Phone verified');
  } else {
    addFactor('phone_verified', 'Phone verification', 0, 'missing', 'Mobile number is not marked verified yet.');
    warnings.push('Phone is not verified');
  }

  addFactor(
    'email_verification',
    'Email verification',
    hasEmail ? 5 : 0,
    hasEmail ? 'positive' : 'missing',
    hasEmail ? 'Email is linked to this account.' : 'No email is linked to this account.'
  );
  if (hasEmail) trustSignals.push('Email linked');

  if (isFirebaseVerified) {
    addFactor('firebase_verified', 'Firebase / Google verified', 8, 'positive', 'Firebase or Google identity is linked.');
    trustSignals.push('Firebase identity linked');
  } else {
    addFactor('firebase_verified', 'Firebase verification', 0, 'missing', 'No Firebase or Google identity signal is linked.');
  }

  if (completionScore >= 90) {
    addFactor('profile_completion', 'Profile completion', 15, 'positive', `${completionScore}% profile completion.`);
    trustSignals.push('Profile is highly complete');
  } else if (completionScore >= 70) {
    addFactor('profile_completion', 'Profile completion', 12, 'positive', `${completionScore}% profile completion.`);
    trustSignals.push('Profile has strong detail');
  } else if (completionScore >= 50) {
    addFactor('profile_completion', 'Profile completion', 8, 'partial', `${completionScore}% profile completion.`);
  } else {
    addFactor('profile_completion', 'Profile completion', 0, 'missing', `${completionScore}% profile completion.`);
    warnings.push('Profile details are still incomplete');
  }

  if (hasAdminVerification) {
    addFactor('admin_verification', 'Admin verification', 15, 'positive', 'SoulMatch admin reviewed this member.');
    trustSignals.push('Admin verified profile');
  } else if (pendingVerifications > 0 || verificationStatus === 'pending') {
    addFactor('admin_verification', 'Admin verification', 5, 'pending', 'Verification review is pending.');
    trustSignals.push('Verification review pending');
  } else {
    addFactor('admin_verification', 'Admin verification', 0, 'missing', 'Admin verification is not completed.');
    warnings.push('Admin verification is not completed');
  }

  addFactor(
    'document_verification',
    'Document verification',
    hasDocumentVerification ? 8 : 0,
    hasDocumentVerification ? 'positive' : 'missing',
    hasDocumentVerification ? 'Identity/document evidence was approved.' : 'No approved document verification yet.'
  );
  if (hasDocumentVerification) trustSignals.push('Document verified');

  addFactor(
    'education_verification',
    'Education verification',
    hasEducationVerification ? 8 : 0,
    hasEducationVerification ? 'positive' : 'missing',
    hasEducationVerification ? 'Education evidence was approved.' : 'No approved education verification yet.'
  );
  if (hasEducationVerification) trustSignals.push('Education verified');

  addFactor(
    'income_verification',
    'Income verification',
    hasIncomeVerification ? 8 : 0,
    hasIncomeVerification ? 'positive' : 'missing',
    hasIncomeVerification ? 'Income evidence was approved.' : 'No approved income verification yet.'
  );
  if (hasIncomeVerification) trustSignals.push('Income verified');

  addFactor(
    'family_verification',
    'Family verification',
    hasFamilyVerification ? 8 : hasFamilyLocation ? 4 : 0,
    hasFamilyVerification ? 'positive' : hasFamilyLocation ? 'partial' : 'missing',
    hasFamilyVerification ? 'Family details were approved.' : hasFamilyLocation ? 'Family location is available but not verified.' : 'Family details are not verified.'
  );
  if (hasFamilyVerification) trustSignals.push('Family verified');

  if (photoCount >= 3) {
    addFactor('photos_added', 'Photo count', 10, 'positive', `${photoCount} profile photos added.`);
    trustSignals.push('Multiple photos added');
  } else if (photoCount >= 1 || signals.primary_photo_url || signals.primaryPhotoUrl) {
    addFactor('photos_added', 'Photo count', 7, 'positive', 'At least one profile photo is available.');
    trustSignals.push('Profile photo added');
  } else {
    addFactor('photos_added', 'Photo count', 0, 'missing', 'No profile photo is available.');
    warnings.push('No profile photo added');
  }

  if (profileStatus === 'active') {
    addFactor('profile_active', 'Profile active', 5, 'positive', 'Profile is currently active.');
  } else {
    addFactor('profile_active', 'Profile active', -8, 'warning', 'Inactive profiles are hidden from discovery.');
    warnings.push('Profile is currently inactive');
  }

  if (recentlyActive) {
    addFactor('recent_activity', 'Recent activity', 6, 'positive', 'Member was active in the last 30 days.');
    trustSignals.push('Recently active');
  } else {
    addFactor('recent_activity', 'Recent activity', 0, 'partial', 'No recent activity signal in the last 30 days.');
  }

  if (reportCount === 0) {
    addFactor('safety_reports', 'Safety reports', 10, 'positive', 'No open safety reports.');
    trustSignals.push('No open safety reports');
  } else {
    addFactor('safety_reports', 'Safety reports', -Math.min(35, reportCount * 12), 'warning', `${reportCount} open safety report${reportCount === 1 ? '' : 's'}.`);
    warnings.push(`${reportCount} open safety report${reportCount === 1 ? '' : 's'}`);
  }

  const finalScore = clampScore(score);
  return {
    score: finalScore,
    level: trustLevelForScore(finalScore),
    signals: trustSignals.slice(0, 8),
    warnings: warnings.slice(0, 4),
    factors,
    explanation: {
      summary: `Trust score is ${finalScore}% based on verification, profile quality, photos, safety, and activity.`,
      approvedVerificationTypes: approvedTypes
    }
  };
}

function buildSeriousnessSummary(signals = {}) {
  const completionScore = clampScore(signals.completion_score ?? signals.completionScore);
  const verificationStatus = String(signals.verification_status || signals.verificationStatus || 'pending').toLowerCase();
  const lastLogin = signals.last_login || signals.lastLogin;
  const recentlyActive = lastLogin ? Date.now() - new Date(lastLogin).getTime() <= 1000 * 60 * 60 * 24 * 14 : false;
  const received = Number(signals.received_interests ?? signals.receivedInterests ?? 0);
  const responded = Number(signals.responded_interests ?? signals.respondedInterests ?? 0);
  const accepted = Number(signals.accepted_interests ?? signals.acceptedInterests ?? 0);
  const declined = Number(signals.declined_interests ?? signals.declinedInterests ?? 0);
  const ignored = Number(signals.ignored_interests ?? signals.ignoredInterests ?? 0);
  const familyBoardItems = Number(signals.family_board_items ?? signals.familyBoardItems ?? 0);
  const reportCount = Number(signals.report_count ?? signals.reportCount ?? 0);
  const responseRate = received > 0 ? responded / received : (completionScore >= 70 ? 0.65 : 0.35);
  const decisionCount = accepted + declined;
  const declineRatio = decisionCount > 0 ? declined / decisionCount : 0;
  const signalsOut = [];
  const warnings = [];
  let score = 0;

  score += Math.min(20, Math.round(completionScore * 0.2));
  if (completionScore >= 80) signalsOut.push('Profile detail is strong');

  if (recentlyActive) {
    score += 15;
    signalsOut.push('Recently active');
  } else {
    warnings.push('No recent activity signal');
  }

  score += Math.round(Math.min(1, responseRate) * 25);
  if (responseRate >= 0.75) signalsOut.push('Responds to interests');
  if (responseRate < 0.35 && received > 2) warnings.push('Low response rate');

  if (verificationStatus === 'verified') {
    score += 10;
    signalsOut.push('Verified member');
  }

  if (accepted > 0) {
    score += Math.min(10, accepted * 4);
    signalsOut.push('Accepts suitable interests');
  }
  if (declineRatio <= 0.7) score += 8;
  if (declineRatio > 0.8 && decisionCount >= 3) warnings.push('Declines most received interests');

  if (familyBoardItems > 0) {
    score += 10;
    signalsOut.push('Uses family decision board');
  }

  if (ignored > 0) {
    score -= Math.min(20, ignored * 5);
    warnings.push(`${ignored} older interest${ignored === 1 ? '' : 's'} waiting without response`);
  }
  if (reportCount > 0) score -= Math.min(25, reportCount * 10);

  const finalScore = clampScore(score);
  return {
    score: finalScore,
    level: finalScore >= 80 ? 'high' : finalScore >= 55 ? 'medium' : 'low',
    signals: signalsOut.slice(0, 6),
    warnings: warnings.slice(0, 4),
    metrics: {
      responseRate: Number((responseRate * 100).toFixed(1)),
      receivedInterests: received,
      respondedInterests: responded,
      ignoredInterests: ignored,
      familyBoardItems
    }
  };
}

function normalizeFamilyDecisionStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['considering', 'family_review', 'call_scheduled', 'spoken', 'accepted', 'declined', 'archived'].includes(normalized)
    ? normalized
    : null;
}

function normalizeFamilyVote(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['approve', 'reject', 'discuss'].includes(normalized) ? normalized : 'discuss';
}

function mapFamilyDecision(row) {
  if (!row) return null;
  const trust = buildTrustSummary(row);
  const comments = Array.isArray(row.comments) ? row.comments : [];
  return {
    familyDecisionId: row.family_decision_id,
    ownerProfileId: row.owner_profile_id,
    targetProfileId: row.target_profile_id,
    status: row.status,
    familyVote: row.family_vote || 'discuss',
    note: row.note || '',
    nextStep: row.next_step || '',
    nextStepAt: row.next_step_at || null,
    updatedAt: row.updated_at,
    targetName: `${row.first_name || ''} ${row.last_name || ''}`.trim() || 'Member',
    targetAge: row.age || 0,
    targetLocation: row.working_city || row.family_city || '',
    targetOccupation: row.occupation || '',
    targetPhotoUrl: row.primary_photo_url || null,
    isVerified: row.verification_status === 'verified',
    trustScore: trust.score,
    trustLevel: trust.level,
    trustSignals: trust.signals,
    trustFactors: trust.factors,
    comments
  };
}

exports.buildTrustSummary = buildTrustSummary;
exports.buildSeriousnessSummary = buildSeriousnessSummary;

exports.recordUserChange = async ({
  userId,
  profileId,
  entityType,
  entityId,
  action,
  beforeData = {},
  afterData = {},
  source = 'member_app',
  ipAddress = null,
  userAgent = null
}) => {
  const db = await getDB();
  const changedFields = buildChangedFields(beforeData, afterData);
  await db.query(
    `INSERT INTO user_change_audit_logs (
       audit_id,
       user_id,
       profile_id,
       entity_type,
       entity_id,
       action,
       before_data,
       after_data,
       changed_fields,
       source,
       ip_address,
       user_agent,
       created_at
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7::jsonb,$8::jsonb,$9::jsonb,$10,$11,$12,NOW())`,
    [
      randomUUID(),
      userId || null,
      profileId || null,
      entityType,
      entityId || null,
      action,
      JSON.stringify(toComparableJson(beforeData) || {}),
      JSON.stringify(toComparableJson(afterData) || {}),
      JSON.stringify(changedFields),
      source,
      ipAddress,
      userAgent
    ]
  );
};

exports.getTrustSummary = async (profileId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       p.profile_id,
       p.completion_score,
       p.verification_status,
       p.profile_status,
       p.primary_photo_url,
       u.email,
       u.is_verified,
       u.google_id,
       (u.google_id IS NOT NULL) AS firebase_verified,
       u.last_login,
       fd.family_city,
       fd.family_pincode,
       COALESCE((SELECT COUNT(*)::int FROM profile_photos pp WHERE pp.profile_id=p.profile_id), 0) AS photo_count,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), 0) AS approved_verifications,
       COALESCE((SELECT array_agg(DISTINCT v.type) FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), ARRAY[]::text[]) AS approved_verification_types,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status='pending'), 0) AS pending_verifications,
       COALESCE((SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id=p.profile_id), 0) AS received_interests,
       COALESCE((SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id=p.profile_id AND i.status IN ('accepted','declined')), 0) AS responded_interests,
       COALESCE((SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id=p.profile_id AND i.status='accepted'), 0) AS accepted_interests,
       COALESCE((SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id=p.profile_id AND i.status='declined'), 0) AS declined_interests,
       COALESCE((SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id=p.profile_id AND i.status='pending' AND i.sent_at < NOW() - INTERVAL '7 days'), 0) AS ignored_interests,
       COALESCE((SELECT COUNT(*)::int FROM family_match_decisions fmd WHERE fmd.owner_profile_id=p.profile_id AND fmd.status!='archived'), 0) AS family_board_items,
       COALESCE((SELECT COUNT(*)::int FROM reports rp WHERE rp.reported_id=p.user_id AND rp.status IN ('pending','open','reviewing')), 0) AS report_count
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN family_details fd ON fd.profile_id=p.profile_id
     WHERE p.profile_id=$1
     LIMIT 1`,
    [profileId]
  );
  const row = r.rows[0] || {};
  return {
    ...buildTrustSummary(row),
    seriousness: buildSeriousnessSummary(row)
  };
};

exports.findByUserId = async (userId) => { const db = await getDB(); const r = await db.query('SELECT * FROM profiles WHERE user_id=$1 LIMIT 1', [userId]); return r.rows[0] || null; };
exports.findById = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT * FROM profiles WHERE profile_id=$1 LIMIT 1', [profileId]); return r.rows[0] || null; };
exports.getVerificationRequests = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       verification_id,
       user_id,
       type,
       status,
       document_url,
       reviewer_email,
       review_note,
       reviewed_at,
       created_at
     FROM verifications
     WHERE user_id=$1
     ORDER BY created_at DESC`,
    [userId]
  );
  return r.rows;
};
exports.createVerificationRequest = async (profile, data) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const lockedProfile = await client.query(
      'SELECT profile_id,user_id,verification_status,completion_score,primary_photo_url FROM profiles WHERE profile_id=$1 AND user_id=$2 FOR UPDATE',
      [profile.profile_id, profile.user_id]
    );
    const current = lockedProfile.rows[0];
    if (!current) {
      await client.query('ROLLBACK');
      return { status: 'not_found' };
    }
    if (current.verification_status === 'verified') {
      await client.query('COMMIT');
      return { status: 'already_verified' };
    }
    const pending = await client.query(
      `SELECT
         verification_id,
         user_id,
         type,
         status,
         document_url,
         reviewer_email,
         review_note,
         reviewed_at,
         created_at
       FROM verifications
       WHERE user_id=$1 AND status='pending'
       ORDER BY created_at DESC
       LIMIT 1`,
      [profile.user_id]
    );
    if (pending.rows[0]) {
      await client.query('COMMIT');
      return { status: 'already_pending', verification: pending.rows[0] };
    }
    const verificationId = randomUUID();
    const documentUrl = data.documentUrl || current.primary_photo_url || null;
    const consentEventId = documentUrl ? await insertConsentEvent(client, {
      userId: profile.user_id,
      profileId: profile.profile_id,
      consentType: 'kyc_upload',
      status: 'granted',
      purpose: 'Member submitted an identity or profile verification document for admin review.',
      metadata: {
        verificationType: data.type || 'profile',
        hasDocumentUrl: Boolean(documentUrl)
      },
      audit: data.audit || {}
    }) : null;
    const inserted = await client.query(
      `INSERT INTO verifications (verification_id,user_id,type,status,document_url,created_at)
       VALUES ($1,$2,$3,'pending',$4,NOW())
       RETURNING verification_id,user_id,type,status,document_url,reviewer_email,review_note,reviewed_at,created_at`,
      [verificationId, profile.user_id, data.type, documentUrl]
    );
    await client.query(
      "UPDATE profiles SET verification_status='pending', updated_at=NOW() WHERE profile_id=$1",
      [profile.profile_id]
    );
    await client.query(
      `INSERT INTO admin_alerts (alert_id,severity,title,body,source,metadata,created_at)
       VALUES ($1,'medium','Profile verification requested',$2,'profile-service',$3::jsonb,NOW())`,
      [
        randomUUID(),
        `${profile.first_name || 'A member'} ${profile.last_name || ''}`.trim() + ' submitted a profile verification request.',
        JSON.stringify({
          type: 'profile_verification',
          verificationId,
          profileId: profile.profile_id,
          userId: profile.user_id,
          verificationType: data.type,
          consentEventId
        })
      ]
    );
    await client.query('COMMIT');
    return { status: 'created', verification: inserted.rows[0] };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};
exports.canViewProfile = async (profileId, viewerUserId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT p.profile_id,p.user_id,p.is_published,p.admin_status,p.profile_visibility,
            COALESCE(p.profile_status,'active') AS profile_status,
            EXISTS (
              SELECT 1 FROM blocks b
              WHERE (b.blocker_id=$2 AND b.blocked_id=p.user_id)
                 OR (b.blocker_id=p.user_id AND b.blocked_id=$2)
            ) AS blocked
     FROM profiles p
     WHERE p.profile_id=$1
     LIMIT 1`,
    [profileId, viewerUserId]
  );
  const profile = r.rows[0];
  const visibility = evaluateProfileVisibility(profile, viewerUserId);
  if (!visibility.allowed && visibility.reason === 'matches_only') {
    if (await exports.hasAcceptedInterestWithViewer(profileId, viewerUserId)) {
      return { allowed: true, owner: false, profile };
    }
  }
  if (!visibility.allowed) return { ...visibility, profile };
  return { allowed: true, owner: false, profile };
};
exports.hasAcceptedInterestWithViewer = async (profileId, viewerUserId) => {
  const db = await getDB();
  const viewer = await exports.findByUserId(viewerUserId);
  if (!viewer) return false;
  const r = await db.query(
    `SELECT EXISTS(
       SELECT 1 FROM interests
       WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1))
         AND status='accepted'
     ) AS ok`,
    [viewer.profile_id, profileId]
  );
  return Boolean(r.rows[0]?.ok);
};
exports.getPhotoAccessState = async (profileId, viewerUserId) => {
  const db = await getDB();
  const profileResult = await db.query(
    `SELECT profile_id,user_id,photo_privacy
     FROM profiles
     WHERE profile_id=$1
     LIMIT 1`,
    [profileId]
  );
  const profile = profileResult.rows[0];
  if (!profile) return { canViewPhoto: false, photoAccessStatus: 'not_found', photoPrivacy: 'private' };
  if (profile.user_id === viewerUserId) {
    return { canViewPhoto: true, photoAccessStatus: 'owner', photoPrivacy: profile.photo_privacy || 'all' };
  }
  const privacy = profile.photo_privacy || 'all';
  if (privacy === 'all') {
    return { canViewPhoto: true, photoAccessStatus: 'visible', photoPrivacy: privacy };
  }
  if (privacy === 'matches_only' && await exports.hasAcceptedInterestWithViewer(profileId, viewerUserId)) {
    return { canViewPhoto: true, photoAccessStatus: 'accepted_match', photoPrivacy: privacy };
  }
  const request = await db.query(
    `SELECT photo_access_request_id,status,requested_at,responded_at,expires_at
     FROM profile_photo_access_requests
     WHERE target_profile_id=$1 AND requester_user_id=$2
     ORDER BY requested_at DESC
     LIMIT 1`,
    [profileId, viewerUserId]
  );
  const latest = request.rows[0];
  if (latest?.status === 'approved' && (!latest.expires_at || new Date(latest.expires_at).getTime() > Date.now())) {
    return {
      canViewPhoto: true,
      photoAccessStatus: 'approved',
      photoAccessRequestId: latest.photo_access_request_id,
      photoPrivacy: privacy
    };
  }
  return {
    canViewPhoto: false,
    photoAccessStatus: latest?.status || 'not_requested',
    photoAccessRequestId: latest?.photo_access_request_id || null,
    photoPrivacy: privacy
  };
};
exports.requestPhotoAccess = async (targetProfileId, requesterUserId, message = '') => {
  const db = await getDB();
  const client = await db.connect();
  let savedRequest = null;
  let targetProfile = null;
  let requesterProfile = null;
  try {
    await client.query('BEGIN');
    const targetResult = await client.query(
      'SELECT profile_id,user_id,first_name,last_name,photo_privacy FROM profiles WHERE profile_id=$1 LIMIT 1',
      [targetProfileId]
    );
    targetProfile = targetResult.rows[0];
    const requesterResult = await client.query(
      'SELECT profile_id,user_id,first_name,last_name FROM profiles WHERE user_id=$1 LIMIT 1',
      [requesterUserId]
    );
    requesterProfile = requesterResult.rows[0];
    if (!targetProfile || !requesterProfile) {
      await client.query('ROLLBACK');
      return { status: 'not_found' };
    }
    if (targetProfile.user_id === requesterUserId) {
      await client.query('ROLLBACK');
      return { status: 'own_profile' };
    }
    const existing = await client.query(
      `SELECT *
       FROM profile_photo_access_requests
       WHERE target_profile_id=$1 AND requester_user_id=$2
       ORDER BY requested_at DESC
       LIMIT 1`,
      [targetProfile.profile_id, requesterUserId]
    );
    const latest = existing.rows[0];
    if (latest?.status === 'pending') {
      await client.query(
        `UPDATE profile_photo_access_requests
         SET last_notified_at=NOW()
         WHERE photo_access_request_id=$1`,
        [latest.photo_access_request_id]
      );
      savedRequest = latest;
    } else if (latest?.status === 'approved' && (!latest.expires_at || new Date(latest.expires_at).getTime() > Date.now())) {
      savedRequest = latest;
    } else {
      const inserted = await client.query(
        `INSERT INTO profile_photo_access_requests (
           photo_access_request_id,
           target_profile_id,
           target_user_id,
           requester_profile_id,
           requester_user_id,
           status,
           message,
           requested_at,
           last_notified_at
         )
         VALUES ($1,$2,$3,$4,$5,'pending',$6,NOW(),NOW())
         RETURNING *`,
        [
          randomUUID(),
          targetProfile.profile_id,
          targetProfile.user_id,
          requesterProfile.profile_id,
          requesterProfile.user_id,
          String(message || '').trim() || null
        ]
      );
      savedRequest = inserted.rows[0];
    }
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }

  if (targetProfile?.user_id && savedRequest?.status === 'pending') {
    await notifyMember(
      db,
      targetProfile.user_id,
      'Photo access requested',
      `${requesterProfile?.first_name || 'A member'} requested permission to view your profile photo.`,
      {
        type: 'photo_access_requested',
        requestId: savedRequest.photo_access_request_id,
        profileId: requesterProfile?.profile_id || '',
        requesterUserId
      }
    ).catch((error) => logger.warn(`Photo access notification skipped: ${error.message}`));
  }

  return {
    status: savedRequest?.status || 'pending',
    request: savedRequest,
    targetProfile,
    requesterProfile
  };
};
exports.getPhotoAccessRequestsForOwner = async (ownerUserId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       par.photo_access_request_id,
       par.target_profile_id,
       par.requester_profile_id,
       par.requester_user_id,
       par.status,
       par.message,
       par.requested_at,
       par.responded_at,
       p.first_name,
       p.last_name,
       p.primary_photo_url,
       ec.occupation,
       ec.working_city
     FROM profile_photo_access_requests par
     JOIN profiles p ON p.profile_id=par.requester_profile_id
     LEFT JOIN education_career ec ON ec.profile_id=p.profile_id
     WHERE par.target_user_id=$1
     ORDER BY par.requested_at DESC
     LIMIT 50`,
    [ownerUserId]
  );
  return r.rows;
};
exports.respondPhotoAccessRequest = async (requestId, ownerUserId, status) => {
  const db = await getDB();
  const normalizedStatus = String(status || '').trim().toLowerCase();
  if (!['approved', 'declined'].includes(normalizedStatus)) return null;
  const beforeResult = await db.query(
    'SELECT * FROM profile_photo_access_requests WHERE photo_access_request_id=$1 AND target_user_id=$2 LIMIT 1',
    [requestId, ownerUserId]
  );
  const before = beforeResult.rows[0];
  if (!before) return null;
  const updated = await db.query(
    `UPDATE profile_photo_access_requests
     SET status=$1, responded_at=NOW()
     WHERE photo_access_request_id=$2 AND target_user_id=$3
     RETURNING *`,
    [normalizedStatus, requestId, ownerUserId]
  );
  const row = updated.rows[0];
  if (row?.requester_user_id) {
    const ownerProfile = await exports.findByUserId(ownerUserId);
    const title = normalizedStatus === 'approved' ? 'Photo access approved' : 'Photo access declined';
    const body = normalizedStatus === 'approved'
      ? `${ownerProfile?.first_name || 'A member'} allowed you to view their profile photo.`
      : `${ownerProfile?.first_name || 'A member'} declined your photo access request.`;
    await notifyMember(db, row.requester_user_id, title, body, {
      type: normalizedStatus === 'approved' ? 'photo_access_approved' : 'photo_access_declined',
      requestId: row.photo_access_request_id,
      profileId: row.target_profile_id,
      ownerUserId
    }).catch((error) => logger.warn(`Photo access response notification skipped: ${error.message}`));
  }
  return { before, after: row };
};
exports.getPhotos = async (profileId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       photo_id,
       photo_url,
       is_primary,
       sequence_order,
       uploaded_at
     FROM profile_photos
     WHERE profile_id=$1
     ORDER BY is_primary DESC, sequence_order ASC, uploaded_at ASC`,
    [profileId]
  );
  return r.rows;
};
const normalizeProfileCreatedBy = (value) => ['self', 'mediator'].includes(String(value || '').toLowerCase()) ? String(value).toLowerCase() : 'self';
const normalizeProfileStatus = (value) => ['active', 'inactive'].includes(String(value || '').toLowerCase()) ? String(value).toLowerCase() : null;
exports.upsertBasicInfo = async (userId, data) => {
  const db = await getDB();
  const ex = await exports.findByUserId(userId);
  const profileCreatedBy = normalizeProfileCreatedBy(data.profileCreatedBy || data.profile_created_by);
  if (ex) { await db.query('UPDATE profiles SET first_name=$1,last_name=$2,dob=$3,gender=$4,religion=$5,caste=$6,mother_tongue=$7,marital_status=$8,profile_created_by=$9,updated_at=NOW() WHERE user_id=$10', [data.firstName,data.lastName,data.dob,data.gender,data.religion,data.caste,data.motherTongue,data.maritalStatus||'never_married',profileCreatedBy,userId]); return ex; }
  const r = await db.query('INSERT INTO profiles (profile_id,user_id,first_name,last_name,dob,gender,religion,caste,mother_tongue,marital_status,profile_created_by) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) RETURNING *', [randomUUID(),userId,data.firstName,data.lastName,data.dob,data.gender,data.religion,data.caste,data.motherTongue,data.maritalStatus||'never_married',profileCreatedBy]);
  return r.rows[0];
};
exports.upsertPhysical = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO physical_details (profile_id,height_cm,weight_kg,complexion,body_type,blood_group) VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (profile_id) DO UPDATE SET height_cm=$2,weight_kg=$3,complexion=$4,body_type=$5,blood_group=$6', [p.profile_id,data.heightCm,data.weightKg,data.complexion,data.bodyType,data.bloodGroup]); return p; };
exports.upsertEducation = async (userId, data) => {
  const db = await getDB();
  const p = await exports.findByUserId(userId);
  const isEmployed = data.isEmployed === true || String(data.isEmployed).toLowerCase() === 'true';
  await db.query(
    `INSERT INTO education_career (
       profile_id,
       education_level,
       is_employed,
       occupation,
       annual_income,
       working_city,
       working_state,
       working_pincode
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
     ON CONFLICT (profile_id) DO UPDATE SET
       education_level=$2,
       is_employed=$3,
       occupation=$4,
       annual_income=$5,
       working_city=$6,
       working_state=$7,
       working_pincode=$8`,
    [
      p.profile_id,
      data.educationLevel,
      isEmployed,
      isEmployed ? data.occupation : null,
      isEmployed ? data.annualIncome : null,
      isEmployed ? data.workingCity : null,
      isEmployed ? data.workingState : null,
      isEmployed ? data.workingPincode : null
    ]
  );
  return p;
};
exports.upsertFamily = async (userId, data) => {
  const db = await getDB();
  const p = await exports.findByUserId(userId);
  await db.query(
    `INSERT INTO family_details (
       profile_id,
       father_occupation,
       mother_occupation,
       num_brothers,
       num_sisters,
       family_type,
       family_city,
       family_state,
       family_locality,
       family_pincode
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
     ON CONFLICT (profile_id) DO UPDATE SET
       father_occupation=$2,
       mother_occupation=$3,
       num_brothers=$4,
       num_sisters=$5,
       family_type=$6,
       family_city=$7,
       family_state=$8,
       family_locality=$9,
       family_pincode=$10`,
    [
      p.profile_id,
      data.fatherOccupation,
      data.motherOccupation,
      data.numBrothers || 0,
      data.numSisters || 0,
      data.familyType,
      data.familyCity,
      data.familyState || null,
      data.familyLocality || null,
      data.familyPincode || null
    ]
  );
  return p;
};
exports.upsertLifestyle = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO lifestyle_details (profile_id,diet,smoking,drinking,about_me) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET diet=$2,smoking=$3,drinking=$4,about_me=$5', [p.profile_id,data.diet,data.smoking||'never',data.drinking||'never',data.aboutMe]); return p; };
exports.upsertHoroscope = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO horoscope_details (profile_id,rashi,nakshatra,is_manglik,birth_city,gotra) VALUES ($1,$2,$3,$4,$5,$6) ON CONFLICT (profile_id) DO UPDATE SET rashi=$2,nakshatra=$3,is_manglik=$4,birth_city=$5,gotra=$6', [p.profile_id,data.rashi,data.nakshatra,data.isManglik||false,data.birthCity,data.gotra]); return p; };
exports.upsertPreferences = async (profileId, data = {}) => {
  const db = await getDB();
  const payload = {
    ageMin: toIntegerOrDefault(data.ageMin ?? data.age_min, 18),
    ageMax: toIntegerOrDefault(data.ageMax ?? data.age_max, 50),
    religion: cleanShortText(data.religion, 50),
    manglikPref: cleanShortText(data.manglikPref ?? data.manglik_pref, 20) || 'any',
    educationLevels: toTextArray(data.educationLevels ?? data.education_levels),
    occupations: toTextArray(data.occupations),
    annualIncomeMin: toIntegerOrNull(data.annualIncomeMin ?? data.annual_income_min),
    annualIncomeMax: toIntegerOrNull(data.annualIncomeMax ?? data.annual_income_max),
    heightMinCm: toIntegerOrNull(data.heightMinCm ?? data.height_min_cm),
    heightMaxCm: toIntegerOrNull(data.heightMaxCm ?? data.height_max_cm),
    locations: toTextArray(data.locations),
    locationRadiusKm: toIntegerOrDefault(data.locationRadiusKm ?? data.location_radius_km, 50),
    dietPrefs: toTextArray(data.dietPrefs ?? data.diet_prefs),
    maritalStatuses: toTextArray(data.maritalStatuses ?? data.marital_statuses),
    familyTypes: toTextArray(data.familyTypes ?? data.family_types),
    relocationOpen: data.relocationOpen ?? data.relocation_open ?? null,
    timeline: cleanShortText(data.timeline, 40),
    dealBreakers: toTextArray(data.dealBreakers ?? data.deal_breakers),
    goodToHave: toTextArray(data.goodToHave ?? data.good_to_have)
  };
  if (payload.ageMin > payload.ageMax) {
    const originalMin = payload.ageMin;
    payload.ageMin = payload.ageMax;
    payload.ageMax = originalMin;
  }
  if (payload.heightMinCm !== null && payload.heightMaxCm !== null && payload.heightMinCm > payload.heightMaxCm) {
    const originalMin = payload.heightMinCm;
    payload.heightMinCm = payload.heightMaxCm;
    payload.heightMaxCm = originalMin;
  }
  await db.query(
    `INSERT INTO partner_preferences (
       profile_id,
       age_min,
       age_max,
       religion,
       manglik_pref,
       education_levels,
       occupations,
       annual_income_min,
       annual_income_max,
       height_min_cm,
       height_max_cm,
       locations,
       location_radius_km,
       diet_prefs,
       marital_statuses,
       family_types,
       relocation_open,
       timeline,
       deal_breakers,
       good_to_have
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20)
     ON CONFLICT (profile_id) DO UPDATE SET
       age_min=$2,
       age_max=$3,
       religion=$4,
       manglik_pref=$5,
       education_levels=$6,
       occupations=$7,
       annual_income_min=$8,
       annual_income_max=$9,
       height_min_cm=$10,
       height_max_cm=$11,
       locations=$12,
       location_radius_km=$13,
       diet_prefs=$14,
       marital_statuses=$15,
       family_types=$16,
       relocation_open=$17,
       timeline=$18,
       deal_breakers=$19,
       good_to_have=$20,
       updated_at=NOW()`,
    [
      profileId,
      payload.ageMin,
      payload.ageMax,
      payload.religion,
      payload.manglikPref,
      payload.educationLevels,
      payload.occupations,
      payload.annualIncomeMin,
      payload.annualIncomeMax,
      payload.heightMinCm,
      payload.heightMaxCm,
      payload.locations,
      payload.locationRadiusKm,
      payload.dietPrefs,
      payload.maritalStatuses,
      payload.familyTypes,
      payload.relocationOpen,
      payload.timeline,
      payload.dealBreakers,
      payload.goodToHave
    ]
  );
  await db.query('UPDATE profiles SET is_partner_pref_set=true,updated_at=NOW() WHERE profile_id=$1', [profileId]);
};
exports.getPreferences = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT * FROM partner_preferences WHERE profile_id=$1', [profileId]); return r.rows[0] || null; };
exports.recordMatchFeedback = async (userId, targetProfileId, data = {}) => {
  const db = await getDB();
  const sourceProfile = await exports.findByUserId(userId);
  const targetProfile = await exports.findById(targetProfileId);
  if (!sourceProfile) return { status: 'source_not_found' };
  if (!targetProfile) return { status: 'target_not_found' };
  if (sourceProfile.profile_id === targetProfile.profile_id) return { status: 'own_profile' };

  const feedbackId = randomUUID();
  const action = cleanShortText(data.action, 40) || 'viewed';
  const reason = cleanShortText(data.reason, 120);
  const note = cleanShortText(data.note, 500);
  const metadata = data.metadata && typeof data.metadata === 'object' ? data.metadata : {};
  const result = await db.query(
    `INSERT INTO match_feedback (
       feedback_id,
       user_id,
       source_profile_id,
       target_profile_id,
       action,
       reason,
       note,
       metadata
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8::jsonb)
     RETURNING feedback_id, user_id, source_profile_id, target_profile_id, action, reason, note, metadata, created_at`,
    [
      feedbackId,
      userId,
      sourceProfile.profile_id,
      targetProfile.profile_id,
      action,
      reason,
      note,
      JSON.stringify(metadata)
    ]
  );
  return {
    status: 'recorded',
    sourceProfile,
    targetProfile,
    feedback: result.rows[0]
  };
};
exports.getAssistStatusByUserId = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       p.profile_id,
       p.first_name,
       p.last_name,
       p.religion,
       p.caste,
       p.mother_tongue,
       fd.family_city,
       fd.family_state,
       fd.family_locality,
       fd.family_pincode,
       amp.is_opted_in,
       amp.support_level,
       amp.share_mode,
       amp.request_status,
       amp.preferred_contact_window,
       amp.family_contact_name,
       amp.family_contact_phone,
       amp.notes,
       amp.assigned_at,
       amp.next_review_at,
       a.advisor_id,
       a.full_name AS advisor_name,
       a.phone AS advisor_phone,
       a.email AS advisor_email,
       a.service_label,
       a.bio AS advisor_bio,
       a.city AS advisor_city,
       a.state AS advisor_state,
       a.pincode AS advisor_pincode,
       a.languages AS advisor_languages,
       a.communities AS advisor_communities,
       a.average_rating,
       a.success_rate
     FROM profiles p
     LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
     LEFT JOIN assisted_match_profiles amp ON amp.profile_id = p.profile_id
     LEFT JOIN advisors a ON a.advisor_id = amp.assigned_advisor_id
     WHERE p.user_id = $1
     LIMIT 1`,
    [userId]
  );
  const row = r.rows[0];
  if (!row) return null;
  const selectedAdvisorIds = await listSelectedAdvisorIdsForProfile(db, row.profile_id);
  return {
    profileId: row.profile_id,
    isOptedIn: row.is_opted_in === true,
    supportLevel: row.support_level || 'self_service',
    shareMode: normalizeAssistShareMode(row.share_mode),
    selectedAdvisorIds,
    requestStatus: row.request_status || 'not_requested',
    preferredContactWindow: row.preferred_contact_window || '',
    familyContactName: row.family_contact_name || '',
    familyContactPhone: row.family_contact_phone || '',
    notes: row.notes || '',
    assignedAt: row.assigned_at || null,
    nextReviewAt: row.next_review_at || null,
    location: {
      city: row.family_city || '',
      state: row.family_state || '',
      locality: row.family_locality || '',
      pincode: row.family_pincode || ''
    },
    readiness: {
      hasCity: Boolean(row.family_city),
      hasPincode: Boolean(row.family_pincode),
      canAutoAssign: Boolean(row.family_city || row.family_pincode)
    },
    advisor: row.advisor_id ? {
      advisorId: row.advisor_id,
      fullName: row.advisor_name,
      phone: row.advisor_phone,
      email: row.advisor_email,
      serviceLabel: row.service_label,
      bio: row.advisor_bio,
      city: row.advisor_city,
      state: row.advisor_state,
      pincode: row.advisor_pincode,
      languages: parseJsonList(row.advisor_languages),
      communities: parseJsonList(row.advisor_communities),
      averageRating: Number(row.average_rating || 0),
      successRate: Number(row.success_rate || 0)
    } : null
  };
};

async function fetchAdvisorCandidates(client, profileId) {
  const profileResult = await client.query(
    `SELECT
       p.profile_id,
       p.religion,
       p.caste,
       p.mother_tongue,
       fd.family_city,
       fd.family_state,
       fd.family_locality,
       fd.family_pincode
     FROM profiles p
     LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
     WHERE p.profile_id = $1
     LIMIT 1`,
    [profileId]
  );
  const profile = profileResult.rows[0];
  if (!profile) return { profile: null, candidates: [] };

  const candidatesResult = await client.query(
    `SELECT
       a.advisor_id,
       a.full_name,
       a.phone,
       a.email,
       a.service_label,
       a.bio,
       a.city,
       a.state,
       a.pincode,
       a.languages,
       a.communities,
       a.max_active_assignments,
       a.success_rate,
       a.complaint_score,
       a.average_rating,
       a.kyc_status,
       a.status,
       a.membership_expires_at,
       asa.locality,
       asa.priority,
       asa.is_primary,
       COALESCE(active_counts.active_assignments, 0) AS active_assignments
     FROM advisors a
     JOIN advisor_service_areas asa ON asa.advisor_id = a.advisor_id
     LEFT JOIN (
       SELECT assigned_advisor_id, COUNT(*) AS active_assignments
       FROM assisted_match_profiles
       WHERE assigned_advisor_id IS NOT NULL AND request_status = 'assigned'
       GROUP BY assigned_advisor_id
     ) active_counts ON active_counts.assigned_advisor_id = a.advisor_id`
  );

  const ranked = candidatesResult.rows
    .map((candidate) => {
      const ranking = scoreAdvisorCandidate(profile, candidate);
      if (!ranking) return null;
      return {
        ...candidate,
        languages: parseJsonList(candidate.languages),
        communities: parseJsonList(candidate.communities),
        score: ranking.score,
        reasons: ranking.reasons
      };
    })
    .filter(Boolean)
    .sort((left, right) => right.score - left.score);

  return { profile, candidates: ranked };
}

exports.listAdvisorRecommendations = async (profileId, limit = 3) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const { candidates } = await fetchAdvisorCandidates(client, profileId);
    return candidates.slice(0, limit).map((candidate) => ({
      advisorId: candidate.advisor_id,
      fullName: candidate.full_name,
      phone: candidate.phone,
      email: candidate.email,
      serviceLabel: candidate.service_label,
      bio: candidate.bio,
      city: candidate.city,
      state: candidate.state,
      pincode: candidate.pincode,
      locality: candidate.locality || '',
      languages: candidate.languages,
      communities: candidate.communities,
      averageRating: Number(candidate.average_rating || 0),
      successRate: Number(candidate.success_rate || 0),
      activeAssignments: Number(candidate.active_assignments || 0),
      score: candidate.score,
      reasons: candidate.reasons
    }));
  } finally {
    client.release();
  }
};

exports.upsertAssistStatus = async (userId, payload) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const profileResult = await client.query(
      `SELECT
         p.profile_id,
         p.first_name,
         p.last_name,
         p.religion,
         p.caste,
         p.mother_tongue,
         fd.family_city,
         fd.family_state,
         fd.family_locality,
         fd.family_pincode
        FROM profiles p
        LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
        WHERE p.user_id = $1
        FOR UPDATE OF p`,
       [userId]
     );
    const profile = profileResult.rows[0];
    if (!profile) {
      await client.query('ROLLBACK');
      return null;
    }

    const isOptedIn = payload.isOptedIn === true;
    const supportLevel = payload.supportLevel || 'self_service';
    const shareMode = normalizeAssistShareMode(payload.shareMode);
    const requestedAdvisorIds = [...new Set((Array.isArray(payload.selectedAdvisorIds) ? payload.selectedAdvisorIds : []).map((item) => String(item || '').trim()).filter(Boolean))];
    let selectedAdvisorIds = [];
    let requestStatus = 'not_requested';
    let assignedAdvisorId = null;
    let assignedAt = null;
    let eventType = 'assist_updated';
    let eventScore = null;
    let eventMetadata = {
      supportLevel,
      isOptedIn,
      shareMode
    };

    if (isOptedIn && supportLevel === 'advisor_assisted') {
      if (requestedAdvisorIds.length > 0) {
        const selectedResult = await client.query(
          `SELECT advisor_id
           FROM advisors
           WHERE advisor_id = ANY($1::uuid[])
             AND status = 'active'`,
          [requestedAdvisorIds]
        );
        selectedAdvisorIds = selectedResult.rows.map((row) => row.advisor_id).filter(Boolean);
        if (shareMode === 'single') selectedAdvisorIds = selectedAdvisorIds.slice(0, 1);
      }
      if (selectedAdvisorIds.length > 0) {
        requestStatus = 'assigned';
        assignedAdvisorId = selectedAdvisorIds[0];
        assignedAt = new Date();
        eventType = selectedAdvisorIds.length > 1 ? 'advisors_selected' : 'advisor_selected';
        eventMetadata = {
          ...eventMetadata,
          selectedAdvisorIds
        };
      } else {
        requestStatus = 'waiting_assignment';
        eventType = 'waiting_assignment';
      }
    } else if (isOptedIn && supportLevel === 'family_assisted') {
      requestStatus = 'not_requested';
      eventType = 'family_assisted_enabled';
    }

    const upserted = await client.query(
      `INSERT INTO assisted_match_profiles (
         assisted_profile_id,
         profile_id,
         is_opted_in,
         support_level,
         share_mode,
         request_status,
         preferred_contact_window,
         family_contact_name,
         family_contact_phone,
         notes,
         assigned_advisor_id,
         assigned_at,
         next_review_at,
         consent_notice_version,
         consent_granted_at,
         consent_withdrawn_at,
         created_at,
         updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,NOW() + INTERVAL '7 days',$13,$14,$15,NOW(),NOW())
       ON CONFLICT (profile_id) DO UPDATE SET
         is_opted_in=$3,
         support_level=$4,
         share_mode=$5,
         request_status=$6,
         preferred_contact_window=$7,
         family_contact_name=$8,
         family_contact_phone=$9,
         notes=$10,
         assigned_advisor_id=$11,
         assigned_at=$12,
         next_review_at=NOW() + INTERVAL '7 days',
         consent_notice_version=$13,
         consent_granted_at=COALESCE($14, assisted_match_profiles.consent_granted_at),
         consent_withdrawn_at=$15,
         updated_at=NOW()
       RETURNING *`,
      [
        randomUUID(),
        profile.profile_id,
        isOptedIn,
        supportLevel,
        shareMode,
        requestStatus,
        payload.preferredContactWindow || null,
        payload.familyContactName || null,
        payload.familyContactPhone || null,
        payload.notes || null,
        assignedAdvisorId,
        assignedAt,
        DPDP_NOTICE_VERSION,
        isOptedIn ? new Date() : null,
        isOptedIn ? null : new Date()
      ]
    );

    await insertConsentEvent(client, {
      userId,
      profileId: profile.profile_id,
      consentType: 'soulmatch_assistance',
      status: isOptedIn ? 'granted' : 'withdrawn',
      purpose: 'Member controls SoulMatch Assistance preference and whether the app can surface agent discovery/offline support options.',
      metadata: {
        supportLevel,
        requestStatus,
        shareMode
      },
      audit: payload.audit || {}
    });

    await client.query('DELETE FROM assisted_match_profile_advisors WHERE assisted_profile_id = $1', [upserted.rows[0].assisted_profile_id]);
    if (isOptedIn && supportLevel === 'advisor_assisted' && selectedAdvisorIds.length > 0) {
      await insertConsentEvent(client, {
        userId,
        profileId: profile.profile_id,
        consentType: 'agent_profile_share',
        status: 'granted',
        purpose: 'Member chose specific active agents who may receive profile context for direct offline support.',
        metadata: {
          shareMode,
          selectedAdvisorIds
        },
        audit: payload.audit || {}
      });
      for (const advisorId of selectedAdvisorIds) {
        await client.query(
          `INSERT INTO assisted_match_profile_advisors (
             assisted_profile_agent_id,
             assisted_profile_id,
             profile_id,
             advisor_id,
             status,
             selected_at,
             created_at,
             updated_at
           )
           VALUES ($1,$2,$3,$4,'selected',NOW(),NOW(),NOW())`,
          [randomUUID(), upserted.rows[0].assisted_profile_id, profile.profile_id, advisorId]
        );
      }
    } else {
      await insertConsentEvent(client, {
        userId,
        profileId: profile.profile_id,
        consentType: 'agent_profile_share',
        status: 'withdrawn',
        purpose: 'Member did not allow profile sharing with agents in the current SoulMatch Assistance settings.',
        metadata: {
          shareMode,
          selectedAdvisorIds: []
        },
        audit: payload.audit || {}
      });
    }

    await client.query(
      `INSERT INTO assisted_match_assignment_events (
         assignment_event_id,
         profile_id,
         advisor_id,
         event_type,
         score,
         metadata,
         created_at
       )
       VALUES ($1,$2,$3,$4,$5,$6::jsonb,NOW())`,
      [
        randomUUID(),
        profile.profile_id,
        assignedAdvisorId,
        eventType,
        eventScore,
        JSON.stringify(eventMetadata)
      ]
    );

    await client.query('COMMIT');
    return upserted.rows[0];
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};

exports.listFamilyDecisions = async (ownerUserId) => {
  const db = await getDB();
  const owner = await exports.findByUserId(ownerUserId);
  if (!owner) return null;
  const r = await db.query(
    `SELECT
       fmd.family_decision_id,
       fmd.owner_profile_id,
       fmd.target_profile_id,
       fmd.status,
       COALESCE(fmd.family_vote,'discuss') AS family_vote,
       fmd.note,
       fmd.next_step,
       fmd.next_step_at,
       fmd.updated_at,
       p.first_name,
       p.last_name,
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       p.primary_photo_url,
       p.completion_score,
       p.verification_status,
       p.profile_status,
       u.is_verified,
       u.google_id,
       (u.google_id IS NOT NULL) AS firebase_verified,
       u.last_login,
       ec.occupation,
       ec.working_city,
       fd.family_city,
       fd.family_pincode,
       COALESCE((SELECT COUNT(*)::int FROM profile_photos pp WHERE pp.profile_id=p.profile_id), 0) AS photo_count,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), 0) AS approved_verifications,
       COALESCE((SELECT array_agg(DISTINCT v.type) FROM verifications v WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')), ARRAY[]::text[]) AS approved_verification_types,
       COALESCE((SELECT COUNT(*)::int FROM verifications v WHERE v.user_id=p.user_id AND v.status='pending'), 0) AS pending_verifications,
       COALESCE((SELECT COUNT(*)::int FROM reports rp WHERE rp.reported_id=p.user_id AND rp.status IN ('pending','open','reviewing')), 0) AS report_count,
       COALESCE((
         SELECT json_agg(
           json_build_object(
             'familyCommentId', fmdc.family_comment_id,
             'vote', fmdc.vote,
             'comment', fmdc.comment,
             'createdAt', fmdc.created_at
           )
           ORDER BY fmdc.created_at DESC
         )
         FROM family_match_decision_comments fmdc
         WHERE fmdc.family_decision_id=fmd.family_decision_id
       ), '[]'::json) AS comments
     FROM family_match_decisions fmd
     JOIN profiles p ON p.profile_id=fmd.target_profile_id
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN education_career ec ON ec.profile_id=p.profile_id
     LEFT JOIN family_details fd ON fd.profile_id=p.profile_id
     WHERE fmd.owner_profile_id=$1
       AND fmd.status!='archived'
     ORDER BY fmd.updated_at DESC
     LIMIT 100`,
    [owner.profile_id]
  );
  return r.rows.map(mapFamilyDecision);
};

exports.upsertFamilyDecision = async (ownerUserId, targetProfileId, payload = {}) => {
  const status = normalizeFamilyDecisionStatus(payload.status) || 'family_review';
  const familyVote = normalizeFamilyVote(payload.familyVote || payload.family_vote);
  const note = String(payload.note || '').trim() || null;
  const nextStep = String(payload.nextStep || payload.next_step || '').trim() || null;
  const nextStepAt = payload.nextStepAt || payload.next_step_at || null;
  const db = await getDB();
  const owner = await exports.findByUserId(ownerUserId);
  if (!owner) return { status: 'owner_not_found' };
  const target = await exports.findById(targetProfileId);
  if (!target) return { status: 'target_not_found' };
  if (target.user_id === ownerUserId) return { status: 'own_profile' };

  const beforeResult = await db.query(
    'SELECT * FROM family_match_decisions WHERE owner_profile_id=$1 AND target_profile_id=$2 LIMIT 1',
    [owner.profile_id, targetProfileId]
  );
  const before = beforeResult.rows[0] || null;
  const saved = await db.query(
    `INSERT INTO family_match_decisions (
       family_decision_id,
       owner_user_id,
       owner_profile_id,
       target_profile_id,
       status,
       family_vote,
       note,
       next_step,
       next_step_at,
       created_at,
       updated_at
     )
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW(),NOW())
     ON CONFLICT (owner_profile_id, target_profile_id) DO UPDATE SET
       status=$5,
       family_vote=$6,
       note=COALESCE($7, family_match_decisions.note),
       next_step=COALESCE($8, family_match_decisions.next_step),
       next_step_at=COALESCE($9, family_match_decisions.next_step_at),
       updated_at=NOW()
     RETURNING *`,
    [
      randomUUID(),
      ownerUserId,
      owner.profile_id,
      targetProfileId,
      status,
      familyVote,
      note,
      nextStep,
      nextStepAt
    ]
  );
  const savedRow = saved.rows[0];
  const beforeReminder = before?.next_step_at ? new Date(before.next_step_at).getTime() : null;
  const nextReminder = savedRow.next_step_at ? new Date(savedRow.next_step_at).getTime() : null;
  if (nextReminder && nextReminder !== beforeReminder) {
    await notifyMember(
      db,
      ownerUserId,
      'Family board reminder set',
      `${nextStep || 'Next family step'} is scheduled for ${new Date(savedRow.next_step_at).toLocaleString('en-IN')}.`,
      {
        type: 'family_board_reminder',
        familyDecisionId: savedRow.family_decision_id,
        targetProfileId,
        targetUserId: target.user_id,
        nextStep: savedRow.next_step || '',
        nextStepAt: savedRow.next_step_at
      }
    ).catch((error) => logger.warn(`Family board reminder notification skipped: ${error.message}`));
  }
  return {
    status: before ? 'updated' : 'created',
    owner,
    target,
    before,
    after: savedRow
  };
};

exports.addFamilyDecisionComment = async (ownerUserId, familyDecisionId, payload = {}) => {
  const db = await getDB();
  const owner = await exports.findByUserId(ownerUserId);
  if (!owner) return { status: 'not_found' };
  const decision = await db.query(
    'SELECT * FROM family_match_decisions WHERE family_decision_id=$1 AND owner_profile_id=$2 LIMIT 1',
    [familyDecisionId, owner.profile_id]
  );
  if (!decision.rows[0]) return { status: 'not_found' };
  const vote = normalizeFamilyVote(payload.vote);
  const comment = String(payload.comment || '').trim() || null;
  const inserted = await db.query(
    `INSERT INTO family_match_decision_comments (
       family_comment_id,
       family_decision_id,
       author_user_id,
       vote,
       comment,
       created_at
     )
     VALUES ($1,$2,$3,$4,$5,NOW())
     RETURNING family_comment_id, family_decision_id, author_user_id, vote, comment, created_at`,
    [randomUUID(), familyDecisionId, ownerUserId, vote, comment]
  );
  await db.query(
    `UPDATE family_match_decisions
     SET family_vote=$2,
         updated_at=NOW()
     WHERE family_decision_id=$1`,
    [familyDecisionId, vote]
  );
  return { status: 'created', decision: decision.rows[0], comment: inserted.rows[0] };
};
exports.findFullByUserId = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       p.*,
       u.phone,
       u.email,
       u.is_verified AS is_phone_verified,
       u.last_login,
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       pd.height_cm,
       pd.weight_kg,
       pd.complexion,
       pd.body_type,
       pd.blood_group,
        ec.education_level,
        ec.is_employed,
        ec.occupation,
        ec.annual_income,
        ec.working_city,
        ec.working_state,
        ec.working_pincode,
       fd.father_occupation,
       fd.mother_occupation,
       fd.num_brothers,
       fd.num_sisters,
       fd.family_type,
       fd.family_city,
       fd.family_state,
       fd.family_locality,
       fd.family_pincode,
       ld.diet,
       ld.smoking,
       ld.drinking,
       ld.about_me,
       hd.rashi,
       hd.nakshatra,
       hd.is_manglik,
       hd.birth_city,
       hd.gotra
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
     LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
     LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
     LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
     LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
     WHERE p.user_id=$1
     LIMIT 1`,
    [userId]
  );
  return r.rows[0] || null;
};
exports.findFullById = async (profileId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       p.*,
       u.phone,
       u.email,
       u.is_verified AS is_phone_verified,
       u.last_login,
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       pd.height_cm,
       pd.weight_kg,
       pd.complexion,
       pd.body_type,
       pd.blood_group,
        ec.education_level,
        ec.is_employed,
        ec.occupation,
        ec.annual_income,
        ec.working_city,
        ec.working_state,
        ec.working_pincode,
       fd.father_occupation,
       fd.mother_occupation,
       fd.num_brothers,
       fd.num_sisters,
       fd.family_type,
       fd.family_city,
       fd.family_state,
       fd.family_locality,
       fd.family_pincode,
       ld.diet,
       ld.smoking,
       ld.drinking,
       ld.about_me,
       hd.rashi,
       hd.nakshatra,
       hd.is_manglik,
       hd.birth_city,
       hd.gotra,
       CASE WHEN pp.profile_id IS NULL THEN NULL ELSE json_build_object(
         'age_min', pp.age_min,
         'age_max', pp.age_max,
         'religion', pp.religion,
         'manglik_pref', pp.manglik_pref,
         'education_levels', COALESCE(pp.education_levels, ARRAY[]::text[]),
         'occupations', COALESCE(pp.occupations, ARRAY[]::text[]),
         'annual_income_min', pp.annual_income_min,
         'annual_income_max', pp.annual_income_max,
         'height_min_cm', pp.height_min_cm,
         'height_max_cm', pp.height_max_cm,
         'locations', COALESCE(pp.locations, ARRAY[]::text[]),
         'location_radius_km', pp.location_radius_km,
         'diet_prefs', COALESCE(pp.diet_prefs, ARRAY[]::text[]),
         'marital_statuses', COALESCE(pp.marital_statuses, ARRAY[]::text[]),
         'family_types', COALESCE(pp.family_types, ARRAY[]::text[]),
         'relocation_open', pp.relocation_open,
         'timeline', pp.timeline,
         'deal_breakers', COALESCE(pp.deal_breakers, ARRAY[]::text[]),
         'good_to_have', COALESCE(pp.good_to_have, ARRAY[]::text[])
       ) END AS partner_preferences
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
     LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
     LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
     LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
     LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
     LEFT JOIN partner_preferences pp ON p.profile_id=pp.profile_id
     WHERE p.profile_id=$1
     LIMIT 1`,
    [profileId]
  );
  return r.rows[0] || null;
};
exports.update = async (profileId, data) => {
  const db = await getDB();
  const profileCreatedBy = data.profileCreatedBy || data.profile_created_by ? normalizeProfileCreatedBy(data.profileCreatedBy || data.profile_created_by) : null;
  await db.query('UPDATE profiles SET first_name=COALESCE($1,first_name),last_name=COALESCE($2,last_name),profile_created_by=COALESCE($3,profile_created_by),updated_at=NOW() WHERE profile_id=$4', [data.firstName,data.lastName,profileCreatedBy,profileId]);
};
exports.updatePrivacy = async (profileId, data) => {
  const db = await getDB();
  const allowedPhoto = ['all', 'matches_only', 'request_only', 'private'];
  const allowedVisibility = ['all', 'matches_only', 'hidden'];
  const allowedContact = ['visible', 'masked'];
  const photoPrivacy = allowedPhoto.includes(data.photoPrivacy) ? data.photoPrivacy : null;
  const profileVisibility = allowedVisibility.includes(data.profileVisibility) ? data.profileVisibility : null;
  const contactPrivacy = allowedContact.includes(data.contactPrivacy) ? data.contactPrivacy : null;
  const hideLastSeen = typeof data.hideLastSeen === 'boolean' ? data.hideLastSeen : null;
  await db.query(
    'UPDATE profiles SET photo_privacy=COALESCE($1,photo_privacy),profile_visibility=COALESCE($2,profile_visibility),hide_last_seen=COALESCE($3,hide_last_seen),contact_privacy=COALESCE($4,contact_privacy),updated_at=NOW() WHERE profile_id=$5',
    [photoPrivacy, profileVisibility, hideLastSeen, contactPrivacy, profileId]
  );
};

function maskPhone(phone) {
  const digits = String(phone || '').replace(/\D/g, '');
  if (!digits) return '';
  const last = digits.slice(-2);
  return `${'*'.repeat(Math.max(digits.length - 2, 4))}${last}`;
}

function maskEmail(email) {
  const value = String(email || '').trim();
  const [name, domain] = value.split('@');
  if (!name || !domain) return value ? '••••' : '';
  return `${name.slice(0, 1)}${'*'.repeat(Math.max(name.length - 1, 3))}@${domain}`;
}

async function hasCurrentContactUnlock(db, viewerUserId, targetProfileId) {
  const result = await db.query(
    `SELECT 1
     FROM member_meter_events
     WHERE user_id=$1
       AND target_profile_id=$2
       AND event_type='contact_unlock'
       AND period_key=$3::date
     LIMIT 1`,
    [viewerUserId, targetProfileId, periodKey()]
  );
  return Boolean(result.rows[0]);
}

async function getContactEntitlementState(db, viewerUserId, targetProfileId, targetUserId, contactPrivacy) {
  if (viewerUserId === targetUserId) {
    return {
      status: 'owner',
      canUnmask: true,
      reason: 'owner',
      message: 'These are your contact details.'
    };
  }
  const monetization = await getConfigSection(db, 'monetization');
  const planId = await getActivePlanId(db, viewerUserId);
  const entitlements = getEntitlements(monetization, planId);
  const usage = await ensureUsageRecord(db, viewerUserId, entitlements.planId);
  if (contactPrivacy === 'masked') {
    return {
      status: 'owner_masked',
      canUnmask: false,
      reason: 'owner_masked',
      planId: entitlements.planId,
      entitlements,
      usage,
      message: 'This member has chosen to keep contact details private. You can connect through chat.'
    };
  }
  if (Number(entitlements.contactDetails || 0) <= 0) {
    return {
      status: 'upgrade_required',
      canUnmask: false,
      reason: 'upgrade_required',
      planId: entitlements.planId,
      entitlements,
      usage,
      message: 'Upgrade to Silver or above to view contact details.'
    };
  }
  const unlocked = await hasCurrentContactUnlock(db, viewerUserId, targetProfileId);
  const used = Number(usage.contact_unlocks_used || 0);
  const limit = Number(entitlements.contactDetails || 0);
  return {
    status: unlocked ? 'unlocked' : 'masked',
    canUnmask: unlocked || used < limit,
    reason: unlocked ? 'already_unlocked' : used >= limit ? 'limit_reached' : 'available',
    planId: entitlements.planId,
    entitlements,
    usage,
    remaining: Math.max(limit - used, 0),
    message: used >= limit && !unlocked
      ? 'Limit reached. Extend your subscription to continue.'
      : 'Contact details are masked until you unlock them.'
  };
}

exports.decorateContactPrivacy = async (profile, viewerUserId, owner = false) => {
  if (!profile) return profile;
  const db = await getDB();
  const contactPrivacy = profile.contact_privacy || 'visible';
  const state = await getContactEntitlementState(db, viewerUserId, profile.profile_id, profile.user_id, contactPrivacy);
  const canShow = owner || state.status === 'owner' || state.status === 'unlocked';
  const phone = profile.phone || '';
  const email = profile.email || '';
  return {
    ...profile,
    contact_privacy: contactPrivacy,
    contact_access_status: state.status,
    can_unmask_contact: Boolean(state.canUnmask),
    contact_unlocks_remaining: state.remaining ?? null,
    contact_access_message: state.message,
    masked_phone: maskPhone(phone),
    masked_email: maskEmail(email),
    phone: canShow ? phone : maskPhone(phone),
    email: canShow ? email : maskEmail(email)
  };
};

exports.unlockContactDetails = async (profileId, viewerUserId) => {
  const db = await getDB();
  const profile = await exports.findFullById(profileId);
  if (!profile) return { status: 'not_found' };
  const contactPrivacy = profile.contact_privacy || 'visible';
  const state = await getContactEntitlementState(db, viewerUserId, profile.profile_id, profile.user_id, contactPrivacy);
  const phone = profile.phone || '';
  const email = profile.email || '';
  if (state.status === 'owner' || state.status === 'unlocked') {
    return {
      status: state.status,
      canUnmask: true,
      phone,
      email,
      maskedPhone: maskPhone(phone),
      maskedEmail: maskEmail(email),
      remaining: state.remaining ?? null,
      message: state.message
    };
  }
  if (!state.canUnmask) {
    return {
      status: state.status,
      canUnmask: false,
      maskedPhone: maskPhone(phone),
      maskedEmail: maskEmail(email),
      remaining: state.remaining ?? 0,
      message: state.message
    };
  }
  const consumed = await consumeMeter(db, {
    userId: viewerUserId,
    targetProfileId: profile.profile_id,
    eventType: 'contact_unlock',
    limit: state.entitlements.contactDetails,
    metadata: { targetUserId: profile.user_id }
  });
  if (!consumed.allowed) {
    return {
      status: 'limit_reached',
      canUnmask: false,
      maskedPhone: maskPhone(phone),
      maskedEmail: maskEmail(email),
      remaining: 0,
      message: 'Limit reached. Extend your subscription to continue.'
    };
  }
  return {
    status: 'unlocked',
    canUnmask: true,
    phone,
    email,
    maskedPhone: maskPhone(phone),
    maskedEmail: maskEmail(email),
    remaining: consumed.remaining,
    message: 'Contact details unlocked for this billing cycle.'
  };
};

exports.recordConsentEvent = async (event) => {
  const db = await getDB();
  return insertConsentEvent(db, event);
};
exports.recordAiAssistEvent = async ({
  userId,
  profileId,
  targetProfileId = null,
  eventType,
  provider = 'local',
  model = 'fallback',
  source = 'rules',
  metadata = {}
}) => {
  if (!userId || !eventType) return;
  const db = await getDB();
  await db.query(
    `INSERT INTO analytics_events (event_type, service_name, user_id, payload, created_at)
     VALUES ($1,'profile-service',$2,$3::jsonb,NOW())`,
    [
      eventType,
      userId,
      JSON.stringify({
        profileId,
        targetProfileId,
        provider,
        model,
        source,
        ...metadata
      })
    ]
  ).catch((error) => logger.warn(`AI assist event logging failed: ${error.message}`));
};
exports.updateProfileStatus = async (profileId, status) => {
  const normalized = normalizeProfileStatus(status);
  if (!normalized) return null;
  const db = await getDB();
  const r = await db.query(
    'UPDATE profiles SET profile_status=$1, updated_at=NOW() WHERE profile_id=$2 RETURNING profile_id, profile_status, profile_visibility',
    [normalized, profileId]
  );
  return r.rows[0] || null;
};
exports.setPublished = async (profileId, published) => { const db = await getDB(); await db.query('UPDATE profiles SET is_published=$1,updated_at=NOW() WHERE profile_id=$2', [published, profileId]); };
exports.updateVideoUrl = async (profileId, url) => { const db = await getDB(); await db.query('UPDATE profiles SET video_url=$1 WHERE profile_id=$2', [url,profileId]); };
exports.getPhotoCount = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT COUNT(*) FROM profile_photos WHERE profile_id=$1', [profileId]); return parseInt(r.rows[0].count); };
exports.setPrimaryPhoto = async (profileId, photoId) => {
  const db = await getDB();
  const photo = await db.query('SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND photo_id=$2 LIMIT 1', [profileId, photoId]);
  if (!photo.rows[0]) return false;
  await db.query('UPDATE profile_photos SET is_primary=false WHERE profile_id=$1', [profileId]);
  await db.query('UPDATE profile_photos SET is_primary=true WHERE profile_id=$1 AND photo_id=$2', [profileId, photoId]);
  await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [photo.rows[0].photo_url, profileId]);
  return true;
};
exports.deletePhoto = async (profileId, photoId) => {
  const db = await getDB();
  await db.query('DELETE FROM profile_photos WHERE photo_id=$1 AND profile_id=$2', [photoId,profileId]);
  const primary = await db.query('SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND is_primary=true LIMIT 1', [profileId]);
  if (primary.rows[0]) {
    await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [primary.rows[0].photo_url, profileId]);
    return;
  }
  const fallback = await db.query('SELECT photo_id,photo_url FROM profile_photos WHERE profile_id=$1 ORDER BY sequence_order ASC, uploaded_at ASC LIMIT 1', [profileId]);
  if (!fallback.rows[0]) {
    await db.query('UPDATE profiles SET primary_photo_url=NULL,updated_at=NOW() WHERE profile_id=$1', [profileId]);
    return;
  }
  await db.query(
    'UPDATE profile_photos SET is_primary = CASE WHEN photo_id=$2 THEN true ELSE false END WHERE profile_id=$1',
    [profileId, fallback.rows[0].photo_id]
  );
  await db.query('UPDATE profiles SET primary_photo_url=$1,updated_at=NOW() WHERE profile_id=$2', [fallback.rows[0].photo_url, profileId]);
};
exports.recordView = async (viewedProfileId, viewerUserId) => {
  const db = await getDB();
  const viewer = await exports.findByUserId(viewerUserId);
  if (!viewer) return { allowed: false, reason: 'viewer_profile_missing' };
  if (viewer.profile_id === viewedProfileId) return { allowed: true, owner: true };
  const monetization = await getConfigSection(db, 'monetization');
  const planId = await getActivePlanId(db, viewerUserId);
  const entitlements = getEntitlements(monetization, planId);
  await ensureUsageRecord(db, viewerUserId, entitlements.planId);
  const consumed = await consumeMeter(db, {
    userId: viewerUserId,
    targetProfileId: viewedProfileId,
    eventType: 'profile_view',
    limit: entitlements.profileViews,
    metadata: { viewerProfileId: viewer.profile_id }
  });
  if (!consumed.allowed) {
    return {
      allowed: false,
      reason: 'limit_reached',
      message: 'Limit reached. Extend your subscription to continue.'
    };
  }
  await db.query('INSERT INTO profile_views (viewer_id,viewed_profile_id) VALUES ($1,$2) ON CONFLICT (viewer_id, viewed_profile_id) DO UPDATE SET viewed_at=NOW()', [viewer.profile_id, viewedProfileId]);
  return { allowed: true, remaining: consumed.remaining };
};
exports.getViewers = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT p.profile_id,p.user_id,p.first_name,p.last_name,p.primary_photo_url,pv.viewed_at FROM profile_views pv JOIN profiles p ON p.profile_id=pv.viewer_id WHERE pv.viewed_profile_id=$1 ORDER BY pv.viewed_at DESC LIMIT 50', [profileId]); return r.rows; };
exports.blockProfile = async (blockerUserId, profileId) => {
  const db = await getDB();
  const target = await exports.findById(profileId);
  if (!target) return false;
  await db.query(
    'INSERT INTO blocks (block_id,blocker_id,blocked_id) VALUES ($1,$2,$3) ON CONFLICT (blocker_id, blocked_id) DO NOTHING',
    [randomUUID(), blockerUserId, target.user_id]
  );
  return true;
};
exports.reportProfile = async (reporterUserId, profileId, reason, description) => {
  const db = await getDB();
  const target = await exports.findById(profileId);
  if (!target) return false;
  await db.query(
    'INSERT INTO reports (report_id,reporter_id,reported_id,reason,description,status) VALUES ($1,$2,$3,$4,$5,$6)',
    [randomUUID(), reporterUserId, target.user_id, reason || 'other', description || null, 'pending']
  );
  return true;
};
exports.calcCompletion = async (profileId) => {
  const db = await getDB();
  const c = await db.query(`
    SELECT
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM profiles
          WHERE profile_id = $1
            AND NULLIF(BTRIM(first_name), '') IS NOT NULL
            AND NULLIF(BTRIM(last_name), '') IS NOT NULL
            AND dob IS NOT NULL
            AND NULLIF(BTRIM(religion), '') IS NOT NULL
            AND NULLIF(BTRIM(mother_tongue), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS basic,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM physical_details
          WHERE profile_id = $1
            AND height_cm IS NOT NULL
            AND height_cm > 0
            AND NULLIF(BTRIM(complexion), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS physical,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM education_career
          WHERE profile_id = $1
            AND NULLIF(BTRIM(education_level), '') IS NOT NULL
            AND (
              COALESCE(is_employed, FALSE) = FALSE
              OR (
                NULLIF(BTRIM(occupation), '') IS NOT NULL
                AND NULLIF(BTRIM(annual_income), '') IS NOT NULL
                AND NULLIF(BTRIM(working_city), '') IS NOT NULL
                AND NULLIF(BTRIM(working_state), '') IS NOT NULL
                AND NULLIF(BTRIM(working_pincode), '') IS NOT NULL
              )
            )
        ) THEN 1 ELSE 0
      END AS education,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM family_details
          WHERE profile_id = $1
            AND NULLIF(BTRIM(family_type), '') IS NOT NULL
            AND NULLIF(BTRIM(family_city), '') IS NOT NULL
        ) THEN 1 ELSE 0
      END AS family,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM lifestyle_details
          WHERE profile_id = $1
            AND NULLIF(BTRIM(diet), '') IS NOT NULL
            AND LENGTH(BTRIM(COALESCE(about_me, ''))) >= 30
        ) THEN 1 ELSE 0
      END AS lifestyle,
      CASE
        WHEN EXISTS (
          SELECT 1
          FROM horoscope_details
          WHERE profile_id = $1
            AND (
              NULLIF(BTRIM(COALESCE(rashi, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(nakshatra, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(birth_city, '')), '') IS NOT NULL
              OR NULLIF(BTRIM(COALESCE(gotra, '')), '') IS NOT NULL
              OR is_manglik IS TRUE
            )
        ) THEN 1 ELSE 0
      END AS horoscope,
      (SELECT COUNT(*) FROM profile_photos WHERE profile_id = $1) AS photos,
      (SELECT video_url FROM profiles WHERE profile_id = $1) AS video
  `, [profileId]);
  const w = { basic:20, physical:10, education:15, family:10, lifestyle:10, horoscope:10, photos:15, video:10 };
  const v = c.rows[0]; let score = 0;
  if (parseInt(v.basic)) score+=w.basic; if (parseInt(v.physical)) score+=w.physical;
  if (parseInt(v.education)) score+=w.education; if (parseInt(v.family)) score+=w.family;
  if (parseInt(v.lifestyle)) score+=w.lifestyle; if (parseInt(v.horoscope)) score+=w.horoscope;
  if (parseInt(v.photos)) score+=w.photos; if (v.video) score+=w.video;
  await db.query('UPDATE profiles SET completion_score=$1 WHERE profile_id=$2', [score,profileId]);
  return score;
};

const AGENT_PLAN_LIMITS = {
  free: { planId: 'free', monthlyPrice: 0, profilesAllowed: 5, visibleMatches: 10, contactViews: 0, hasAnalytics: false, hasRelationshipManager: false, featuredBadge: false },
  silver: { planId: 'silver', monthlyPrice: 999, profilesAllowed: 25, visibleMatches: 50, contactViews: 20, hasAnalytics: false, hasRelationshipManager: false, featuredBadge: false },
  gold: { planId: 'gold', monthlyPrice: 2499, profilesAllowed: 100, visibleMatches: -1, contactViews: 100, hasAnalytics: true, hasRelationshipManager: false, featuredBadge: true },
  platinum: { planId: 'platinum', monthlyPrice: 4999, profilesAllowed: -1, visibleMatches: -1, contactViews: -1, hasAnalytics: true, hasRelationshipManager: true, featuredBadge: true }
};

function normalizeAgentPlan(planId) {
  const normalized = String(planId || 'free').trim().toLowerCase();
  return AGENT_PLAN_LIMITS[normalized] ? normalized : 'free';
}

function normalizeAgentReviewStatus(value) {
  const normalized = String(value || 'draft').trim().toLowerCase();
  return ['draft', 'submitted', 'under_review', 'verified', 'rejected'].includes(normalized) ? normalized : 'draft';
}

function normalizeProfileDocumentStatus(value) {
  const normalized = String(value || 'uploaded').trim().toLowerCase();
  return ['not_uploaded', 'uploaded', 'under_review', 'verified', 'rejected'].includes(normalized) ? normalized : 'uploaded';
}

function normalizeProfileDocumentType(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['aadhaar', 'pan', 'voter_id', 'education_certificate', 'horoscope_pdf', 'divorce_decree'].includes(normalized) ? normalized : null;
}

function normalizeDocumentSide(value) {
  const normalized = String(value || 'single').trim().toLowerCase();
  return ['front', 'back', 'single'].includes(normalized) ? normalized : 'single';
}

function normalizeOnboardingStatus(value) {
  const normalized = String(value || 'pending').trim().toLowerCase();
  return ['draft', 'pending', 'under_review', 'approved', 'rejected', 'more_info'].includes(normalized) ? normalized : 'pending';
}

function normalizeNameForMatch(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[^a-z\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function resolveNameMatchStatus(primaryName, compareName) {
  const left = normalizeNameForMatch(primaryName);
  const right = normalizeNameForMatch(compareName);
  if (!left || !right) return 'pending';
  return left === right || left.includes(right) || right.includes(left) ? 'matched' : 'manual_review';
}

function hashSensitiveValue(value) {
  const raw = String(value || '').replace(/\s+/g, '');
  if (!raw) return null;
  const pepper = process.env.DOCUMENT_HASH_PEPPER || process.env.INTERNAL_SERVICE_SECRET || process.env.JWT_SECRET || 'soulmatch-local-hash';
  return createHash('sha256').update(`${pepper}:${raw}`).digest('hex');
}

function lastFourDigits(value) {
  const digits = String(value || '').replace(/\D/g, '');
  return digits ? digits.slice(-4) : null;
}

function normalizeAssistShareMode(value) {
  return String(value || 'single').trim().toLowerCase() === 'multiple' ? 'multiple' : 'single';
}

async function listSelectedAdvisorIdsForProfile(db, profileId) {
  const result = await db.query(
    `SELECT advisor_id
     FROM assisted_match_profile_advisors
     WHERE profile_id = $1
       AND status = 'selected'
     ORDER BY selected_at ASC, created_at ASC`,
    [profileId]
  );
  return result.rows.map((row) => row.advisor_id).filter(Boolean);
}

async function getAdvisorByUserId(userId, client = null) {
  const db = client || await getDB();
  const result = await db.query(
    `SELECT *
     FROM advisors
     WHERE user_id = $1
     LIMIT 1`,
    [userId]
  );
  return result.rows[0] || null;
}

async function requireAgentAdvisor(userId, client = null) {
  const advisor = await getAdvisorByUserId(userId, client);
  if (!advisor) return null;
  return advisor;
}

async function buildAgentProfile(advisorId, client = null) {
  const db = client || await getDB();
  const result = await db.query(
    `SELECT
       a.advisor_id,
       a.user_id,
       'agent' AS user_type,
       a.agent_code,
       a.full_name,
       a.phone,
       a.email,
       a.business_name,
       a.referral_code,
       a.service_label,
       a.bio,
       a.city,
       a.state,
       a.pincode,
       a.profile_photo_url,
       a.years_experience,
       a.membership_plan,
       a.membership_expires_at,
       a.auto_renew,
       a.contact_views_used,
       a.languages,
       a.communities,
       a.fee_preferences,
       a.status,
       a.kyc_status,
       a.onboarding_status,
       a.onboarding_rejection_reason,
       a.approved_at,
       a.rejected_at,
       a.aadhaar_verification_status,
       a.pan_verification_status,
       a.kyc_name_match_status,
       a.bank_verification_status,
       a.bank_name,
       a.bank_account_last4,
       a.bank_ifsc,
       a.bank_name_match_status,
       a.penny_drop_status,
       a.penny_drop_order_id,
       a.penny_drop_amount_paise,
       a.penny_drop_name_match_status,
       a.terms_accepted_at,
       a.terms_version,
       a.fraud_review_status,
       COALESCE(
         json_agg(
           json_build_object(
             'advisorServiceAreaId', asa.advisor_service_area_id,
             'city', asa.city,
             'state', asa.state,
             'locality', asa.locality,
             'pincode', asa.pincode,
             'radiusKm', asa.radius_km,
             'priority', asa.priority,
             'isPrimary', asa.is_primary
           )
           ORDER BY asa.is_primary DESC, asa.priority DESC, asa.created_at ASC
         ) FILTER (WHERE asa.advisor_service_area_id IS NOT NULL),
         '[]'::json
       ) AS service_areas,
       COALESCE(
         json_agg(
           json_build_object(
             'advisorKycDocumentId', akd.advisor_kyc_document_id,
             'documentType', akd.document_type,
             'documentSide', akd.document_side,
             'fileUrl', akd.file_url,
             'status', akd.status,
             'reviewComment', akd.review_comment,
             'isEncrypted', COALESCE(akd.is_encrypted, false),
             'encryptionAlgorithm', akd.encryption_algorithm,
             'contentSha256', akd.content_sha256,
             'originalFileName', akd.original_file_name,
             'mimeType', akd.mime_type,
             'fileSizeBytes', akd.file_size_bytes,
             'extractedMetadata', COALESCE(akd.extracted_metadata, '{}'::jsonb),
             'uploadedAt', akd.uploaded_at,
             'reviewedAt', akd.reviewed_at
           )
           ORDER BY akd.created_at ASC
         ) FILTER (WHERE akd.advisor_kyc_document_id IS NOT NULL),
         '[]'::json
       ) AS kyc_documents
     FROM advisors a
     LEFT JOIN advisor_service_areas asa ON asa.advisor_id = a.advisor_id
     LEFT JOIN advisor_kyc_documents akd ON akd.advisor_id = a.advisor_id
     WHERE a.advisor_id = $1
     GROUP BY a.advisor_id`,
    [advisorId]
  );
  const row = result.rows[0] || null;
  if (!row) return null;
  return {
    advisorId: row.advisor_id,
    userId: row.user_id,
    userType: row.user_type,
    agentCode: row.agent_code || '',
    fullName: row.full_name || '',
    phone: row.phone || '',
    email: row.email || '',
    businessName: row.business_name || '',
    referralCode: row.referral_code || '',
    serviceLabel: row.service_label || 'SoulMatch Advisor',
    bio: row.bio || '',
    city: row.city || '',
    state: row.state || '',
    pincode: row.pincode || '',
    profilePhotoUrl: row.profile_photo_url || '',
    yearsExperience: row.years_experience || 0,
    membershipPlan: normalizeAgentPlan(row.membership_plan),
    membershipExpiresAt: row.membership_expires_at,
    autoRenew: row.auto_renew === true,
    contactViewsUsed: Number(row.contact_views_used || 0),
    languages: parseJsonList(row.languages),
    communities: parseJsonList(row.communities),
    feePreferences: row.fee_preferences || {},
    status: row.status || 'active',
    kycStatus: row.kyc_status || 'pending',
    onboardingStatus: row.onboarding_status || 'pending',
    onboardingRejectionReason: row.onboarding_rejection_reason || '',
    approvedAt: row.approved_at,
    rejectedAt: row.rejected_at,
    aadhaarVerificationStatus: row.aadhaar_verification_status || 'not_started',
    panVerificationStatus: row.pan_verification_status || 'not_started',
    kycNameMatchStatus: row.kyc_name_match_status || 'pending',
    bankVerificationStatus: row.bank_verification_status || 'not_started',
    bankName: row.bank_name || '',
    bankAccountLast4: row.bank_account_last4 || '',
    bankIfsc: row.bank_ifsc || '',
    bankNameMatchStatus: row.bank_name_match_status || 'pending',
    pennyDropStatus: row.penny_drop_status || 'not_started',
    pennyDropOrderId: row.penny_drop_order_id || '',
    pennyDropAmountPaise: Number(row.penny_drop_amount_paise || 100),
    pennyDropNameMatchStatus: row.penny_drop_name_match_status || 'pending',
    termsAcceptedAt: row.terms_accepted_at,
    termsVersion: row.terms_version || '',
    fraudReviewStatus: row.fraud_review_status || 'pending',
    serviceAreas: row.service_areas || [],
    kycDocuments: row.kyc_documents || [],
    isOnboarded: row.onboarding_status === 'approved'
  };
}

exports.getAgentProfileByUserId = async (userId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return null;
  return buildAgentProfile(advisor.advisor_id);
};

exports.upsertAgentOnboarding = async (userId, payload = {}) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const existing = await getAdvisorByUserId(userId, client);
    let advisorId = existing?.advisor_id || randomUUID();
    const kycDocuments = Array.isArray(payload.kycDocuments) ? payload.kycDocuments : [];
    const currentDocumentTypes = kycDocuments
      .map((document) => String(document.documentType || document.document_type || '').trim().toLowerCase())
      .filter(Boolean);
    const persistedDocumentTypes = existing
      ? new Set(
        (await client.query(
          'SELECT document_type FROM advisor_kyc_documents WHERE advisor_id = $1',
          [existing.advisor_id]
        )).rows
          .map((document) => String(document.document_type || '').trim().toLowerCase())
          .filter(Boolean)
      )
      : new Set();
    const uploadedDocumentTypes = new Set([...persistedDocumentTypes, ...currentDocumentTypes]);
    const hasAadhaar = uploadedDocumentTypes.has('aadhaar');
    const hasPan = uploadedDocumentTypes.has('pan');
    const hasCancelledCheque = uploadedDocumentTypes.has('cancelled_cheque');
    const hasRequiredFraudDocs = hasAadhaar && hasPan && hasCancelledCheque;
    const termsAccepted = payload.termsAccepted === true || payload.termsAccepted === 'true';
    const onboardingStatus = hasRequiredFraudDocs && termsAccepted ? 'under_review' : 'draft';
    const languagesJson = payload.languages !== undefined ? JSON.stringify(toTextArray(payload.languages)) : JSON.stringify([]);
    const yearsExperience = payload.yearsExperience !== undefined ? toIntegerOrDefault(payload.yearsExperience, 0) : 0;
    const aadhaarDocument = kycDocuments.find((document) => String(document.documentType || document.document_type || '').trim().toLowerCase() === 'aadhaar');
    const panDocument = kycDocuments.find((document) => String(document.documentType || document.document_type || '').trim().toLowerCase() === 'pan');
    const chequeDocument = kycDocuments.find((document) => String(document.documentType || document.document_type || '').trim().toLowerCase() === 'cancelled_cheque');
    const aadhaarMeta = aadhaarDocument?.extractedMetadata || aadhaarDocument?.extracted_metadata || {};
    const panMeta = panDocument?.extractedMetadata || panDocument?.extracted_metadata || {};
    const chequeMeta = chequeDocument?.extractedMetadata || chequeDocument?.extracted_metadata || {};
    const aadhaarName = aadhaarMeta.fullName || aadhaarMeta.name || null;
    const panName = panMeta.fullName || panMeta.name || null;
    const chequeName = chequeMeta.accountHolderName || chequeMeta.name || null;
    const bankAccountNumber = chequeMeta.accountNumber || chequeMeta.bankAccountNumber || null;
    const kycNameMatchStatus = resolveNameMatchStatus(aadhaarName, panName);
    const bankNameMatchStatus = resolveNameMatchStatus(aadhaarName || payload.fullName, chequeName);
    if (!existing) {
      await client.query(
        `INSERT INTO advisors (
           advisor_id, user_id, full_name, phone, email, city, state, business_name, referral_code,
           service_label, years_experience, languages, onboarding_status, kyc_status, membership_plan,
           aadhaar_verification_status, pan_verification_status, kyc_name_match_status,
           bank_verification_status, bank_name, bank_account_last4, bank_account_hash, bank_ifsc, bank_name_match_status,
           penny_drop_status, penny_drop_amount_paise, penny_drop_name_match_status,
           terms_accepted_at, terms_ip_address, terms_user_agent, terms_version, fraud_review_status,
           draft_saved_at, created_at, updated_at
         )
         VALUES (
           $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12::jsonb,$13,'pending','free',
           $14,$15,$16,$17,$18,$19,$20,$21,$22,
           'not_started',100,'pending',$23,$24,$25,$26,$27,NOW(),NOW(),NOW()
         )`,
        [
          advisorId,
          userId,
          payload.fullName,
          payload.phone,
          payload.email || null,
          payload.city,
          payload.state || null,
          payload.businessName || null,
          payload.referralCode || null,
          payload.serviceLabel || 'SoulMatch Agent',
          yearsExperience,
          languagesJson,
          onboardingStatus,
          hasAadhaar ? 'under_review' : 'not_started',
          hasPan ? 'under_review' : 'not_started',
          kycNameMatchStatus,
          hasCancelledCheque ? (bankAccountNumber ? 'pending_penny_drop' : 'pending_ocr') : 'not_started',
          cleanShortText(chequeMeta.bankName || chequeMeta.bank_name, 160),
          lastFourDigits(bankAccountNumber),
          hashSensitiveValue(bankAccountNumber),
          cleanShortText(chequeMeta.ifsc || chequeMeta.ifscCode || chequeMeta.bankIfsc, 24),
          bankNameMatchStatus,
          termsAccepted ? new Date() : null,
          payload.audit?.ipAddress || null,
          payload.audit?.userAgent || null,
          cleanShortText(payload.termsVersion, 64),
          hasRequiredFraudDocs && termsAccepted ? 'in_progress' : 'pending'
        ]
      );
    } else {
      advisorId = existing.advisor_id;
      await client.query(
        `UPDATE advisors
         SET full_name = COALESCE($2, full_name),
             phone = COALESCE($3, phone),
             email = COALESCE($4, email),
             city = COALESCE($5, city),
             state = COALESCE($6, state),
             business_name = COALESCE($7, business_name),
             referral_code = COALESCE($8, referral_code),
             service_label = COALESCE($9, service_label),
             years_experience = COALESCE($10, years_experience),
             languages = COALESCE($11::jsonb, languages),
             onboarding_status = CASE
               WHEN $12::boolean THEN $13
               ELSE onboarding_status
             END,
             onboarding_rejection_reason = CASE WHEN $12::boolean THEN NULL ELSE onboarding_rejection_reason END,
             aadhaar_verification_status = CASE WHEN $14::boolean THEN 'under_review' ELSE aadhaar_verification_status END,
             pan_verification_status = CASE WHEN $15::boolean THEN 'under_review' ELSE pan_verification_status END,
             kyc_name_match_status = COALESCE($16, kyc_name_match_status),
             bank_verification_status = CASE
               WHEN $17::boolean AND $18 IS NOT NULL THEN 'pending_penny_drop'
               WHEN $17::boolean THEN 'pending_ocr'
               ELSE bank_verification_status
             END,
             bank_name = COALESCE($19, bank_name),
             bank_account_last4 = COALESCE($20, bank_account_last4),
             bank_account_hash = COALESCE($21, bank_account_hash),
             bank_ifsc = COALESCE($22, bank_ifsc),
             bank_name_match_status = COALESCE($23, bank_name_match_status),
             terms_accepted_at = CASE WHEN $24::boolean THEN NOW() ELSE terms_accepted_at END,
             terms_ip_address = CASE WHEN $24::boolean THEN $25 ELSE terms_ip_address END,
             terms_user_agent = CASE WHEN $24::boolean THEN $26 ELSE terms_user_agent END,
             terms_version = CASE WHEN $24::boolean THEN $27 ELSE terms_version END,
             fraud_review_status = CASE WHEN $12::boolean THEN $28 ELSE fraud_review_status END,
             draft_saved_at = NOW(),
             updated_at = NOW()
         WHERE advisor_id = $1`,
        [
          advisorId,
          payload.fullName,
          payload.phone,
          payload.email || null,
          payload.city,
          payload.state || null,
          payload.businessName || null,
          payload.referralCode || null,
          payload.serviceLabel || null,
          payload.yearsExperience !== undefined ? toIntegerOrDefault(payload.yearsExperience, 0) : null,
          payload.languages !== undefined ? JSON.stringify(toTextArray(payload.languages)) : null,
          kycDocuments.length > 0 || termsAccepted,
          onboardingStatus,
          hasAadhaar,
          hasPan,
          kycNameMatchStatus,
          hasCancelledCheque,
          bankAccountNumber || null,
          cleanShortText(chequeMeta.bankName || chequeMeta.bank_name, 160),
          lastFourDigits(bankAccountNumber),
          hashSensitiveValue(bankAccountNumber),
          cleanShortText(chequeMeta.ifsc || chequeMeta.ifscCode || chequeMeta.bankIfsc, 24),
          bankNameMatchStatus,
          termsAccepted,
          payload.audit?.ipAddress || null,
          payload.audit?.UserAgent || payload.audit?.userAgent || null,
          cleanShortText(payload.termsVersion, 64),
          hasRequiredFraudDocs && termsAccepted ? 'in_progress' : 'pending'
        ]
      );
      if (kycDocuments.length > 0) {
        await client.query('DELETE FROM advisor_kyc_documents WHERE advisor_id = $1', [advisorId]);
      }
      await client.query('DELETE FROM advisor_service_areas WHERE advisor_id = $1', [advisorId]);
    }

    const serviceAreas = Array.isArray(payload.serviceAreas) && payload.serviceAreas.length
      ? payload.serviceAreas
      : [{ city: payload.city, state: payload.state || '', pincode: payload.pincode || '', locality: payload.locality || '', radiusKm: 25, isPrimary: true, priority: 10 }];
    for (const [index, area] of serviceAreas.entries()) {
      if (!area?.city) continue;
      await client.query(
        `INSERT INTO advisor_service_areas (
           advisor_service_area_id, advisor_id, state, city, locality, pincode, radius_km, priority, is_primary, created_at
         )
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())`,
        [randomUUID(), advisorId, area.state || null, area.city, area.locality || null, area.pincode || null, toIntegerOrDefault(area.radiusKm ?? area.radius_km, 25), toIntegerOrDefault(area.priority, index === 0 ? 10 : 0), area.isPrimary !== false]
      );
    }

    const kycConsentEventId = kycDocuments.length ? await insertConsentEvent(client, {
      userId,
      profileId: null,
      consentType: 'agent_kyc_upload',
      status: 'granted',
      purpose: 'Agent uploaded business/KYC documents so admins can review agency identity and platform trust eligibility.',
      metadata: {
        advisorId,
        documentCount: kycDocuments.length
      },
      audit: payload.audit || {}
    }) : null;
    if (termsAccepted) {
      await insertConsentEvent(client, {
        userId,
        profileId: null,
        consentType: 'agent_terms_acceptance',
        status: 'granted',
        purpose: 'Agent accepted SoulMatch agent terms, intermediary disclaimer, DPDP data usage notice, and advance fee restrictions.',
        noticeVersion: cleanShortText(payload.termsVersion, 64) || DPDP_NOTICE_VERSION,
        metadata: { advisorId, termsVersion: payload.termsVersion || null },
        audit: payload.audit || {}
      });
      await client.query(
        `INSERT INTO advisor_terms_acceptances (
           advisor_terms_acceptance_id, advisor_id, user_id, terms_version, ip_address, user_agent, metadata, accepted_at
         )
         VALUES ($1,$2,$3,$4,$5,$6,$7::jsonb,NOW())`,
        [
          randomUUID(),
          advisorId,
          userId,
          cleanShortText(payload.termsVersion, 64) || DPDP_NOTICE_VERSION,
          payload.audit?.ipAddress || null,
          payload.audit?.userAgent || null,
          JSON.stringify({
            safeHarbourAccepted: true,
            advanceFeeBanAccepted: true,
            intermediaryDisclaimerAccepted: true
          })
        ]
      );
    }
    for (const document of kycDocuments) {
      const documentType = ['aadhaar', 'pan', 'voter_id', 'cancelled_cheque'].includes(String(document.documentType || document.document_type || '').trim().toLowerCase())
        ? String(document.documentType || document.document_type).trim().toLowerCase()
        : null;
      const fileUrl = cleanShortText(document.fileUrl || document.file_url, 2048);
      if (!documentType || !fileUrl) continue;
      await client.query(
        `INSERT INTO advisor_kyc_documents (
           advisor_kyc_document_id, advisor_id, document_type, document_side, file_url, status, consent_event_id,
           is_encrypted, encryption_algorithm, encryption_key_ref, encryption_iv, content_sha256,
           original_file_name, mime_type, file_size_bytes, extracted_metadata, verification_metadata,
           uploaded_at, created_at, updated_at
         )
         VALUES ($1,$2,$3,$4,$5,'under_review',$6,$7,$8,$9,$10,$11,$12,$13,$14,$15::jsonb,$16::jsonb,NOW(),NOW(),NOW())`,
        [
          randomUUID(),
          advisorId,
          documentType,
          normalizeDocumentSide(document.documentSide || document.document_side),
          fileUrl,
          kycConsentEventId,
          document.isEncrypted === true || document.is_encrypted === true,
          cleanShortText(document.encryptionAlgorithm || document.encryption_algorithm, 40),
          cleanShortText(document.encryptionKeyRef || document.encryption_key_ref, 120),
          document.encryptionIv || document.encryption_iv || null,
          cleanShortText(document.contentSha256 || document.content_sha256, 128),
          cleanShortText(document.originalFileName || document.original_file_name, 500),
          cleanShortText(document.mimeType || document.mime_type, 120),
          toIntegerOrNull(document.fileSizeBytes || document.file_size_bytes),
          JSON.stringify(document.extractedMetadata || document.extracted_metadata || {}),
          JSON.stringify(document.verificationMetadata || document.verification_metadata || {})
        ]
      );
    }

    await client.query(
      'UPDATE users SET user_type = $2, role_selected_at = COALESCE(role_selected_at, NOW()), updated_at = NOW() WHERE user_id = $1',
      [userId, 'agent']
    );
    await client.query('COMMIT');
    return buildAgentProfile(advisorId);
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};

exports.getActiveAdvisorDirectory = async (limit = 24) => {
  const db = await getDB();
  const result = await db.query(
    `WITH advisor_rows AS (
       SELECT
         a.advisor_id,
         a.full_name,
         a.phone,
         a.email,
         a.service_label,
         a.bio,
         COALESCE(sa.city, a.city, '') AS city,
         COALESCE(sa.state, a.state, '') AS state,
         COALESCE(sa.pincode, a.pincode, '') AS pincode,
         COALESCE(sa.locality, '') AS locality,
         a.languages,
         a.communities,
         a.average_rating,
         a.success_rate,
         a.membership_plan,
         a.business_name,
         a.profile_photo_url,
         a.kyc_status,
         a.onboarding_status,
         COALESCE(assignments.active_assignments, 0) AS active_assignments
       FROM advisors a
       LEFT JOIN LATERAL (
         SELECT asa.city, asa.state, asa.pincode, asa.locality
         FROM advisor_service_areas asa
         WHERE asa.advisor_id = a.advisor_id
         ORDER BY asa.is_primary DESC, asa.priority DESC, asa.created_at ASC
         LIMIT 1
       ) sa ON TRUE
       LEFT JOIN (
         SELECT assigned_advisor_id, COUNT(*)::int AS active_assignments
         FROM assisted_match_profiles
         WHERE assigned_advisor_id IS NOT NULL
           AND is_opted_in = TRUE
           AND request_status IN ('assigned', 'waiting_assignment')
         GROUP BY assigned_advisor_id
       ) assignments ON assignments.assigned_advisor_id = a.advisor_id
       WHERE a.status = 'active'
     )
     SELECT * FROM advisor_rows
     ORDER BY
       CASE WHEN onboarding_status = 'approved' OR kyc_status = 'approved' THEN 0 ELSE 1 END,
       average_rating DESC,
       success_rate DESC,
       full_name ASC
     LIMIT $1`,
    [limit]
  );
  const agents = result.rows.map((row) => ({
    advisorId: row.advisor_id,
    fullName: row.full_name || '',
    phone: row.phone || '',
    email: row.email || '',
    serviceLabel: row.business_name || row.service_label || 'SoulMatch Agent',
    bio: row.bio || '',
    city: row.city || '',
    state: row.state || '',
    pincode: row.pincode || '',
    locality: row.locality || '',
    languages: parseJsonList(row.languages),
    communities: parseJsonList(row.communities),
    averageRating: Number(row.average_rating || 0),
    successRate: Number(row.success_rate || 0),
    activeAssignments: Number(row.active_assignments || 0),
    score: row.onboarding_status === 'approved' || row.kyc_status === 'approved' ? 1 : 0,
    reasons: [
      row.membership_plan ? `${String(row.membership_plan).toUpperCase()} plan` : '',
      row.city ? `${row.city}${row.state ? `, ${row.state}` : ''}` : '',
      row.onboarding_status === 'approved' || row.kyc_status === 'approved' ? 'Verified agent' : 'Verification in progress'
    ].filter(Boolean)
  }));
  const verifiedCount = agents.filter((agent) => agent.score > 0).length;
  return {
    stats: {
      activeCount: agents.length,
      verifiedCount,
      unverifiedCount: Math.max(0, agents.length - verifiedCount)
    },
    agents
  };
};

exports.createAgentPennyDropOrder = async (userId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return { status: 'advisor_not_found' };
  const db = await getDB();
  if (!advisor.bank_verification_status || advisor.bank_verification_status === 'not_started') {
    const cheque = await db.query(
      `SELECT advisor_kyc_document_id
       FROM advisor_kyc_documents
       WHERE advisor_id = $1 AND document_type = 'cancelled_cheque'
       LIMIT 1`,
      [advisor.advisor_id]
    );
    if (!cheque.rowCount) {
      return { status: 'bank_document_required' };
    }
  }
  const keyId = process.env.RAZORPAY_KEY_ID;
  const keySecret = process.env.RAZORPAY_KEY_SECRET;
  if (!keyId || !keySecret) return { status: 'gateway_not_configured' };

  const amount = 100;
  const receipt = `agent_penny_${advisor.advisor_id}_${Date.now()}`.slice(0, 40);
  const response = await fetch('https://api.razorpay.com/v1/orders', {
    method: 'POST',
    headers: {
      Authorization: `Basic ${Buffer.from(`${keyId}:${keySecret}`).toString('base64')}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      amount,
      currency: 'INR',
      receipt,
      notes: {
        purpose: 'agent_reverse_penny_drop',
        advisorId: advisor.advisor_id,
        userId
      }
    }),
    signal: AbortSignal.timeout(10000)
  });
  const order = await response.json().catch(() => null);
  if (!response.ok || !order?.id) {
    logger.warn(`Agent penny-drop order failed for ${advisor.advisor_id}: ${response.status}`);
    return { status: 'gateway_error' };
  }
  await db.query(
    `UPDATE advisors
     SET penny_drop_status = 'pending',
         penny_drop_order_id = $2,
         penny_drop_amount_paise = $3,
         bank_verification_status = 'pending_penny_drop',
         updated_at = NOW()
     WHERE advisor_id = $1`,
    [advisor.advisor_id, order.id, amount]
  );
  return {
    status: 'created',
    order: {
      orderId: order.id,
      amount,
      currency: 'INR',
      planId: 'agent_penny_drop',
      gateway: 'razorpay',
      keyId
    }
  };
};

exports.verifyAgentPennyDropPayment = async (userId, payload = {}) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return { status: 'advisor_not_found' };
  const orderId = cleanShortText(payload.orderId || payload.order_id, 120);
  const paymentId = cleanShortText(payload.paymentId || payload.payment_id, 120);
  const signature = cleanShortText(payload.signature || payload.razorpay_signature, 255);
  const keySecret = process.env.RAZORPAY_KEY_SECRET;
  if (!orderId || !paymentId || !signature || !keySecret) return { status: 'invalid_payment' };
  if (advisor.penny_drop_order_id && advisor.penny_drop_order_id !== orderId) return { status: 'order_mismatch' };

  const expected = createHmac('sha256', keySecret).update(`${orderId}|${paymentId}`).digest('hex');
  if (expected !== signature) {
    const db = await getDB();
    await db.query(
      `UPDATE advisors
       SET penny_drop_status = 'failed',
           bank_verification_status = 'rejected',
           fraud_review_status = 'needs_resubmission',
           updated_at = NOW()
       WHERE advisor_id = $1`,
      [advisor.advisor_id]
    );
    return { status: 'signature_mismatch' };
  }

  const db = await getDB();
  await db.query(
    `UPDATE advisors
     SET penny_drop_status = 'paid',
         penny_drop_payment_id = $2,
         bank_verification_status = 'payment_confirmed',
         fraud_review_status = 'in_progress',
         updated_at = NOW()
     WHERE advisor_id = $1`,
    [advisor.advisor_id, paymentId]
  );
  return { status: 'verified', profile: await buildAgentProfile(advisor.advisor_id) };
};

exports.updateAgentProfileByUserId = async (userId, payload = {}) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const advisor = await requireAgentAdvisor(userId, client);
    if (!advisor) {
      await client.query('ROLLBACK');
      return null;
    }
    await client.query(
      `UPDATE advisors
       SET full_name = COALESCE($2, full_name),
           email = COALESCE($3, email),
           phone = COALESCE($4, phone),
           profile_photo_url = COALESCE($5, profile_photo_url),
           bio = COALESCE($6, bio),
           city = COALESCE($7, city),
           state = COALESCE($8, state),
           pincode = COALESCE($9, pincode),
           business_name = COALESCE($10, business_name),
           referral_code = COALESCE($11, referral_code),
           years_experience = COALESCE($12, years_experience),
           languages = COALESCE($13::jsonb, languages),
           communities = COALESCE($14::jsonb, communities),
           fee_preferences = COALESCE($15::jsonb, fee_preferences),
           status = COALESCE($16, status),
           terms_accepted_at = CASE WHEN $17::boolean THEN NOW() ELSE terms_accepted_at END,
           terms_ip_address = CASE WHEN $17::boolean THEN $18 ELSE terms_ip_address END,
           terms_user_agent = CASE WHEN $17::boolean THEN $19 ELSE terms_user_agent END,
           terms_version = CASE WHEN $17::boolean THEN $20 ELSE terms_version END,
           updated_at = NOW()
       WHERE advisor_id = $1`,
      [
        advisor.advisor_id,
        payload.fullName || null,
        payload.email || null,
        payload.phone || null,
        payload.profilePhotoUrl || null,
        payload.bio || null,
        payload.city || null,
        payload.state || null,
        payload.pincode || null,
        payload.businessName || null,
        payload.referralCode || null,
        payload.yearsExperience !== undefined ? toIntegerOrDefault(payload.yearsExperience, 0) : null,
        payload.languages !== undefined ? JSON.stringify(toTextArray(payload.languages)) : null,
        payload.communities !== undefined ? JSON.stringify(toTextArray(payload.communities)) : null,
        payload.feePreferences !== undefined ? JSON.stringify(payload.feePreferences || {}) : null,
        cleanShortText(payload.status, 16),
        payload.termsAccepted === true || payload.termsAccepted === 'true',
        payload.audit?.ipAddress || null,
        payload.audit?.userAgent || null,
        cleanShortText(payload.termsVersion, 64) || DPDP_NOTICE_VERSION
      ]
    );
    if (payload.termsAccepted === true || payload.termsAccepted === 'true') {
      await insertConsentEvent(client, {
        userId,
        profileId: null,
        consentType: 'agent_terms_acceptance',
        status: 'granted',
        purpose: 'Agent accepted SoulMatch agent terms from profile update.',
        noticeVersion: cleanShortText(payload.termsVersion, 64) || DPDP_NOTICE_VERSION,
        metadata: { advisorId: advisor.advisor_id, termsVersion: payload.termsVersion || null },
        audit: payload.audit || {}
      });
      await client.query(
        `INSERT INTO advisor_terms_acceptances (
           advisor_terms_acceptance_id, advisor_id, user_id, terms_version, ip_address, user_agent, metadata, accepted_at
         )
         VALUES ($1,$2,$3,$4,$5,$6,$7::jsonb,NOW())`,
        [
          randomUUID(),
          advisor.advisor_id,
          userId,
          cleanShortText(payload.termsVersion, 64) || DPDP_NOTICE_VERSION,
          payload.audit?.ipAddress || null,
          payload.audit?.userAgent || null,
          JSON.stringify({ profileUpdateAcceptance: true })
        ]
      );
    }
    if (Array.isArray(payload.serviceAreas)) {
      await client.query('DELETE FROM advisor_service_areas WHERE advisor_id = $1', [advisor.advisor_id]);
      for (const [index, area] of payload.serviceAreas.entries()) {
        if (!area?.city) continue;
        await client.query(
          `INSERT INTO advisor_service_areas (
             advisor_service_area_id, advisor_id, state, city, locality, pincode, radius_km, priority, is_primary, created_at
           )
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())`,
          [randomUUID(), advisor.advisor_id, area.state || null, area.city, area.locality || null, area.pincode || null, toIntegerOrDefault(area.radiusKm ?? area.radius_km, 25), toIntegerOrDefault(area.priority, index === 0 ? 10 : 0), area.isPrimary !== false]
        );
      }
    }
    await client.query('COMMIT');
    return buildAgentProfile(advisor.advisor_id);
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
};

exports.getAgentMembershipByUserId = async (userId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return null;
  const planId = normalizeAgentPlan(advisor.membership_plan);
  return {
    ...AGENT_PLAN_LIMITS[planId],
    autoRenew: advisor.auto_renew === true,
    contactViewsUsed: Number(advisor.contact_views_used || 0),
    membershipExpiresAt: advisor.membership_expires_at,
    onboardingStatus: advisor.onboarding_status || 'pending'
  };
};

exports.getAgentMembershipPlans = async () => Object.values(AGENT_PLAN_LIMITS);

exports.listManagedProfilesByAgentUserId = async (userId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return [];
  const db = await getDB();
  const result = await db.query(
    `WITH accessible_profiles AS (
       SELECT
         p.profile_id,
         'managed'::text AS profile_source,
         0 AS source_rank
       FROM profiles p
       WHERE p.created_by_advisor_id = $1
       UNION ALL
       SELECT
         amp.profile_id,
         'assist'::text AS profile_source,
         1 AS source_rank
       FROM assisted_match_profile_advisors ampa
       JOIN assisted_match_profiles amp ON amp.assisted_profile_id = ampa.assisted_profile_id
       WHERE ampa.advisor_id = $1
         AND ampa.status = 'selected'
         AND amp.is_opted_in = TRUE
     )
     SELECT DISTINCT ON (p.profile_id)
       p.profile_id,
       p.user_id,
       p.first_name,
       p.last_name,
       p.gender,
       p.dob,
       p.religion,
       p.caste,
       p.mother_tongue,
       p.primary_photo_url,
       p.completion_score,
       CASE
         WHEN ap.profile_source = 'assist' AND p.review_status = 'draft' THEN
           CASE WHEN p.verification_status = 'verified' THEN 'verified' ELSE 'submitted' END
         ELSE p.review_status
       END AS review_status,
       p.verification_status,
       p.rejection_reason,
       ap.profile_source,
       p.created_at,
       p.updated_at,
       ec.occupation,
       ec.annual_income,
       ec.working_city,
       fd.family_city,
       fd.family_state,
       COALESCE(view_stats.view_count, 0) AS view_count,
       COALESCE(match_stats.match_count, 0) AS match_count,
       COALESCE(document_stats.verified_count, 0) AS verified_document_count,
       COALESCE(document_stats.total_count, 0) AS total_document_count
     FROM accessible_profiles ap
     JOIN profiles p ON p.profile_id = ap.profile_id
     LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
     LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
     LEFT JOIN (
       SELECT viewed_profile_id, COUNT(*)::int AS view_count
       FROM profile_views
       GROUP BY viewed_profile_id
     ) view_stats ON view_stats.viewed_profile_id = p.profile_id
     LEFT JOIN (
       SELECT sender_id AS profile_id, COUNT(*)::int AS match_count
       FROM interests
       WHERE status = 'accepted'
       GROUP BY sender_id
     ) match_stats ON match_stats.profile_id = p.profile_id
     LEFT JOIN (
       SELECT profile_id,
              COUNT(*)::int AS total_count,
              COUNT(*) FILTER (WHERE status = 'verified')::int AS verified_count
       FROM profile_documents
       GROUP BY profile_id
     ) document_stats ON document_stats.profile_id = p.profile_id
     ORDER BY p.profile_id, ap.source_rank ASC, p.updated_at DESC, p.created_at DESC`,
    [advisor.advisor_id]
  );
  return result.rows.map((row) => ({
    profileId: row.profile_id,
    userId: row.user_id,
    firstName: row.first_name || '',
    lastName: row.last_name || '',
    gender: row.gender || '',
    dob: row.dob,
    religion: row.religion || '',
    caste: row.caste || '',
    motherTongue: row.mother_tongue || '',
    primaryPhotoUrl: row.primary_photo_url || '',
    completionScore: Number(row.completion_score || 0),
    reviewStatus: row.review_status || 'draft',
    verificationStatus: row.verification_status || 'pending',
    rejectionReason: row.rejection_reason || '',
    occupation: row.occupation || '',
    annualIncome: row.annual_income || '',
    city: row.working_city || row.family_city || '',
    state: row.family_state || '',
    viewCount: Number(row.view_count || 0),
    matchCount: Number(row.match_count || 0),
    documentChecklistPercent: Number(row.total_document_count || 0) > 0 ? Math.round((Number(row.verified_document_count || 0) / Number(row.total_document_count || 0)) * 100) : 0,
    profileSource: row.profile_source || 'managed',
    createdAt: row.created_at,
    updatedAt: row.updated_at
  }));
};

exports.getManagedProfileByAgentUserId = async (userId, profileId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return null;
  const profile = await exports.findFullById(profileId);
  if (!profile || profile.created_by_advisor_id !== advisor.advisor_id) return null;
  profile.review_status = profile.review_status || 'draft';
  profile.rejection_reason = profile.rejection_reason || '';
  profile.review_notes = profile.review_notes || '';
  return profile;
};

exports.createManagedProfileByAgentUserId = async (userId, payload = {}) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const advisor = await requireAgentAdvisor(userId, client);
    if (!advisor) {
      await client.query('ROLLBACK');
      return { status: 'advisor_not_found' };
    }
    const advisorStatus = String(advisor.status || 'active').trim().toLowerCase();
    const advisorOnboardingStatus = String(advisor.onboarding_status || 'draft').trim().toLowerCase();
    if (advisorStatus === 'inactive' || advisorOnboardingStatus === 'rejected') {
      await client.query('ROLLBACK');
      return { status: 'advisor_not_approved' };
    }
    const plan = AGENT_PLAN_LIMITS[normalizeAgentPlan(advisor.membership_plan)];
    const countResult = await client.query('SELECT COUNT(*)::int AS total FROM profiles WHERE created_by_advisor_id = $1', [advisor.advisor_id]);
    const totalProfiles = Number(countResult.rows[0]?.total || 0);
    if (plan.profilesAllowed >= 0 && totalProfiles >= plan.profilesAllowed) {
      await client.query('ROLLBACK');
      return { status: 'profile_limit_reached', limit: plan.profilesAllowed };
    }

    const memberUserId = randomUUID();
    const profileId = randomUUID();
    await client.query(
      `INSERT INTO users (user_id, phone, email, is_verified, user_type, role_selected_at, created_at, updated_at)
       VALUES ($1,$2,$3,false,'member',NOW(),NOW(),NOW())`,
      [memberUserId, payload.mobile ? String(payload.mobile).trim() : null, payload.email ? String(payload.email).trim() : null]
    );
    await client.query(
      `INSERT INTO profiles (
         profile_id, user_id, first_name, last_name, dob, gender, religion, caste, mother_tongue, marital_status,
         is_published, profile_created_by, created_by_advisor_id, verification_status, review_status, admin_status, created_at, updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,false,'mediator',$11,'pending','draft','active',NOW(),NOW())`,
      [
        profileId,
        memberUserId,
        payload.firstName || '',
        payload.lastName || '',
        payload.dob || null,
        payload.gender || null,
        payload.religion || null,
        payload.caste || null,
        payload.motherTongue || null,
        payload.maritalStatus || 'never_married',
        advisor.advisor_id
      ]
    );
    await client.query(
      `INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, created_at)
       VALUES ($1,$2,$3,$4,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET height_cm = EXCLUDED.height_cm, weight_kg = EXCLUDED.weight_kg, complexion = EXCLUDED.complexion`,
      [profileId, toIntegerOrNull(payload.heightCm), toIntegerOrNull(payload.weightKg), cleanShortText(payload.complexion, 30)]
    );
    await client.query(
      `INSERT INTO education_career (profile_id, education_level, occupation, annual_income, working_city, created_at)
       VALUES ($1,$2,$3,$4,$5,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET education_level = EXCLUDED.education_level, occupation = EXCLUDED.occupation, annual_income = EXCLUDED.annual_income, working_city = EXCLUDED.working_city`,
      [profileId, cleanShortText(payload.educationLevel, 50), cleanShortText(payload.occupation, 100), cleanShortText(payload.annualIncome, 50), cleanShortText(payload.city || payload.workingCity, 100)]
    );
    await client.query(
      `INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city, family_state, family_pincode, created_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET father_occupation = EXCLUDED.father_occupation, mother_occupation = EXCLUDED.mother_occupation, num_brothers = EXCLUDED.num_brothers, num_sisters = EXCLUDED.num_sisters, family_type = EXCLUDED.family_type, family_city = EXCLUDED.family_city, family_state = EXCLUDED.family_state, family_pincode = EXCLUDED.family_pincode`,
      [profileId, cleanShortText(payload.fatherOccupation, 100), cleanShortText(payload.motherOccupation, 100), toIntegerOrDefault(payload.numBrothers, 0), toIntegerOrDefault(payload.numSisters, 0), cleanShortText(payload.familyType, 30), cleanShortText(payload.city || payload.familyCity, 100), cleanShortText(payload.state || payload.familyState, 100), cleanShortText(payload.pincode || payload.familyPincode, 12)]
    );
    await client.query(
      `INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me, created_at)
       VALUES ($1,$2,$3,$4,$5,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET diet = EXCLUDED.diet, smoking = EXCLUDED.smoking, drinking = EXCLUDED.drinking, about_me = EXCLUDED.about_me`,
      [profileId, cleanShortText(payload.diet, 30), cleanShortText(payload.smoking, 20) || 'never', cleanShortText(payload.drinking, 20) || 'never', cleanShortText(payload.aboutMe || payload.specialNotes, 1000)]
    );
    await client.query(
      `INSERT INTO horoscope_details (profile_id, rashi, gotra, created_at)
       VALUES ($1,$2,$3,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET rashi = EXCLUDED.rashi, gotra = EXCLUDED.gotra`,
      [profileId, cleanShortText(payload.rashi, 30), cleanShortText(payload.subCaste || payload.gotra, 100)]
    );
    await client.query('COMMIT');
    return { status: 'created', profile: await exports.findFullById(profileId) };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    if (error.code === '23505') return { status: 'duplicate_contact' };
    throw error;
  } finally {
    client.release();
  }
};

exports.updateManagedProfileByAgentUserId = async (userId, profileId, payload = {}) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const advisor = await requireAgentAdvisor(userId, client);
    const profile = advisor ? await exports.findById(profileId) : null;
    if (!advisor || !profile || profile.created_by_advisor_id !== advisor.advisor_id) {
      await client.query('ROLLBACK');
      return null;
    }
    await client.query(
      `UPDATE profiles
       SET first_name = COALESCE($2, first_name),
           last_name = COALESCE($3, last_name),
           dob = COALESCE($4, dob),
           gender = COALESCE($5, gender),
           religion = COALESCE($6, religion),
           caste = COALESCE($7, caste),
           mother_tongue = COALESCE($8, mother_tongue),
           marital_status = COALESCE($9, marital_status),
           review_status = CASE WHEN review_status = 'rejected' THEN 'draft' ELSE review_status END,
           rejection_reason = CASE WHEN review_status = 'rejected' THEN NULL ELSE rejection_reason END,
           updated_at = NOW()
       WHERE profile_id = $1`,
      [profileId, payload.firstName || null, payload.lastName || null, payload.dob || null, payload.gender || null, payload.religion || null, payload.caste || null, payload.motherTongue || null, payload.maritalStatus || null]
    );
    if (payload.mobile !== undefined || payload.email !== undefined) {
      await client.query(
        `UPDATE users
         SET phone = COALESCE($2, phone),
             email = COALESCE($3, email),
             updated_at = NOW()
         WHERE user_id = $1`,
        [profile.user_id, payload.mobile ? String(payload.mobile).trim() : null, payload.email ? String(payload.email).trim() : null]
      );
    }
    await client.query(
      `INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, created_at)
       VALUES ($1,$2,$3,$4,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET height_cm = COALESCE(EXCLUDED.height_cm, physical_details.height_cm), weight_kg = COALESCE(EXCLUDED.weight_kg, physical_details.weight_kg), complexion = COALESCE(EXCLUDED.complexion, physical_details.complexion)`,
      [profileId, toIntegerOrNull(payload.heightCm), toIntegerOrNull(payload.weightKg), cleanShortText(payload.complexion, 30)]
    );
    await client.query(
      `INSERT INTO education_career (profile_id, education_level, occupation, annual_income, working_city, created_at)
       VALUES ($1,$2,$3,$4,$5,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET education_level = COALESCE(EXCLUDED.education_level, education_career.education_level), occupation = COALESCE(EXCLUDED.occupation, education_career.occupation), annual_income = COALESCE(EXCLUDED.annual_income, education_career.annual_income), working_city = COALESCE(EXCLUDED.working_city, education_career.working_city)`,
      [profileId, cleanShortText(payload.educationLevel, 50), cleanShortText(payload.occupation, 100), cleanShortText(payload.annualIncome, 50), cleanShortText(payload.city || payload.workingCity, 100)]
    );
    await client.query(
      `INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city, family_state, family_pincode, created_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET father_occupation = COALESCE(EXCLUDED.father_occupation, family_details.father_occupation), mother_occupation = COALESCE(EXCLUDED.mother_occupation, family_details.mother_occupation), num_brothers = COALESCE(EXCLUDED.num_brothers, family_details.num_brothers), num_sisters = COALESCE(EXCLUDED.num_sisters, family_details.num_sisters), family_type = COALESCE(EXCLUDED.family_type, family_details.family_type), family_city = COALESCE(EXCLUDED.family_city, family_details.family_city), family_state = COALESCE(EXCLUDED.family_state, family_details.family_state), family_pincode = COALESCE(EXCLUDED.family_pincode, family_details.family_pincode)`,
      [profileId, cleanShortText(payload.fatherOccupation, 100), cleanShortText(payload.motherOccupation, 100), toIntegerOrNull(payload.numBrothers), toIntegerOrNull(payload.numSisters), cleanShortText(payload.familyType, 30), cleanShortText(payload.city || payload.familyCity, 100), cleanShortText(payload.state || payload.familyState, 100), cleanShortText(payload.pincode || payload.familyPincode, 12)]
    );
    await client.query(
      `INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me, created_at)
       VALUES ($1,$2,$3,$4,$5,NOW())
       ON CONFLICT (profile_id) DO UPDATE SET diet = COALESCE(EXCLUDED.diet, lifestyle_details.diet), smoking = COALESCE(EXCLUDED.smoking, lifestyle_details.smoking), drinking = COALESCE(EXCLUDED.drinking, lifestyle_details.drinking), about_me = COALESCE(EXCLUDED.about_me, lifestyle_details.about_me)`,
      [profileId, cleanShortText(payload.diet, 30), cleanShortText(payload.smoking, 20), cleanShortText(payload.drinking, 20), cleanShortText(payload.aboutMe || payload.specialNotes, 1000)]
    );
    await client.query('COMMIT');
    return exports.findFullById(profileId);
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    if (error.code === '23505') return { status: 'duplicate_contact' };
    throw error;
  } finally {
    client.release();
  }
};

exports.deleteManagedProfileByAgentUserId = async (userId, profileId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return false;
  const profile = await exports.findById(profileId);
  if (!profile || profile.created_by_advisor_id !== advisor.advisor_id) return false;
  if (normalizeAgentReviewStatus(profile.review_status) !== 'draft') return { status: 'not_draft' };
  const db = await getDB();
  await db.query('DELETE FROM users WHERE user_id = $1', [profile.user_id]);
  return true;
};

exports.submitManagedProfileByAgentUserId = async (userId, profileId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return { status: 'advisor_not_found' };
  const profile = await exports.findFullById(profileId);
  if (!profile || profile.created_by_advisor_id !== advisor.advisor_id) return { status: 'not_found' };
  const completionScore = await exports.calcCompletion(profileId);
  const photoCount = await exports.getPhotoCount(profileId);
  if (completionScore < 10) return { status: 'incomplete_profile', completionScore };
  if (photoCount < 1) return { status: 'photos_required' };
  const db = await getDB();
  await db.query(
    `UPDATE profiles
     SET review_status = 'submitted',
         verification_status = 'pending',
         submitted_at = NOW(),
         reviewed_at = NULL,
         rejection_reason = NULL,
         review_notes = NULL,
         updated_at = NOW()
     WHERE profile_id = $1`,
    [profileId]
  );
  return { status: 'submitted', profile: await exports.findFullById(profileId) };
};

exports.listManagedProfileDocumentsByAgentUserId = async (userId, profileId) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return null;
  const profile = await exports.findById(profileId);
  if (!profile || profile.created_by_advisor_id !== advisor.advisor_id) return null;
  const db = await getDB();
  const result = await db.query(
    `SELECT
       profile_document_id,
       document_type,
       document_side,
       file_url,
       status,
       review_comment,
       uploaded_at,
       reviewed_at
     FROM profile_documents
     WHERE profile_id = $1
     ORDER BY document_type ASC, document_side ASC, created_at ASC`,
    [profileId]
  );
  return result.rows.map((row) => ({
    profileDocumentId: row.profile_document_id,
    documentType: row.document_type,
    documentSide: row.document_side,
    fileUrl: row.file_url,
    status: row.status,
    reviewComment: row.review_comment || '',
    uploadedAt: row.uploaded_at,
    reviewedAt: row.reviewed_at
  }));
};

exports.upsertManagedProfileDocumentByAgentUserId = async (userId, profileId, payload = {}) => {
  const advisor = await getAdvisorByUserId(userId);
  if (!advisor) return { status: 'advisor_not_found' };
  const profile = await exports.findById(profileId);
  if (!profile || profile.created_by_advisor_id !== advisor.advisor_id) return { status: 'not_found' };
  const documentType = normalizeProfileDocumentType(payload.documentType || payload.document_type);
  const fileUrl = cleanShortText(payload.fileUrl || payload.file_url, 2048);
  if (!documentType || !fileUrl) return { status: 'invalid_document' };
  const documentSide = normalizeDocumentSide(payload.documentSide || payload.document_side);
  const db = await getDB();
  const consentEventId = await insertConsentEvent(db, {
    userId,
    profileId,
    consentType: 'kyc_upload',
    status: 'granted',
    purpose: 'Agent uploaded supporting documents for a managed member profile verification workflow.',
    metadata: {
      advisorId: advisor.advisor_id,
      managedProfileUserId: profile.user_id,
      documentType,
      documentSide
    },
    audit: payload.audit || {}
  });
  const result = await db.query(
    `INSERT INTO profile_documents (
       profile_document_id, profile_id, advisor_id, document_type, document_side, file_url, status, review_comment, consent_event_id, uploaded_at, created_at, updated_at
     )
     VALUES ($1,$2,$3,$4,$5,$6,'uploaded',NULL,$7,NOW(),NOW(),NOW())
     ON CONFLICT (profile_id, document_type, document_side) DO UPDATE SET
       advisor_id = EXCLUDED.advisor_id,
       file_url = EXCLUDED.file_url,
       status = 'uploaded',
       review_comment = NULL,
       consent_event_id = EXCLUDED.consent_event_id,
       uploaded_at = NOW(),
       reviewed_at = NULL,
       reviewed_by = NULL,
       updated_at = NOW()
     RETURNING *`,
    [randomUUID(), profileId, advisor.advisor_id, documentType, documentSide, fileUrl, consentEventId]
  );
  return { status: 'saved', document: result.rows[0] };
};
