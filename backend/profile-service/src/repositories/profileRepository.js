const { getDB } = require('../config/database');
const { randomUUID } = require('crypto');
const { scoreAdvisorCandidate, normalizeList } = require('../services/assistAllocationService');
const logger = require('../utils/logger');

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
          verificationType: data.type
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
  if (!profile) return { allowed: false, reason: 'not_found' };
  if (profile.user_id === viewerUserId) return { allowed: true, owner: true, profile };
  if (profile.blocked) return { allowed: false, reason: 'blocked', profile };
  if (!profile.is_published || profile.admin_status !== 'active') return { allowed: false, reason: 'not_available', profile };
  if (profile.profile_status === 'inactive') return { allowed: false, reason: 'inactive', profile };
  if (profile.profile_visibility === 'hidden') return { allowed: false, reason: 'hidden', profile };
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
exports.upsertEducation = async (userId, data) => { const db = await getDB(); const p = await exports.findByUserId(userId); await db.query('INSERT INTO education_career (profile_id,education_level,occupation,annual_income,working_city) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET education_level=$2,occupation=$3,annual_income=$4,working_city=$5', [p.profile_id,data.educationLevel,data.occupation,data.annualIncome,data.workingCity]); return p; };
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
exports.upsertPreferences = async (profileId, data) => { const db = await getDB(); await db.query('INSERT INTO partner_preferences (profile_id,age_min,age_max,religion,manglik_pref) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (profile_id) DO UPDATE SET age_min=$2,age_max=$3,religion=$4,manglik_pref=$5,updated_at=NOW()', [profileId,data.ageMin||18,data.ageMax||50,data.religion,data.manglikPref||'any']); await db.query('UPDATE profiles SET is_partner_pref_set=true,updated_at=NOW() WHERE profile_id=$1', [profileId]); };
exports.getPreferences = async (profileId) => { const db = await getDB(); const r = await db.query('SELECT * FROM partner_preferences WHERE profile_id=$1', [profileId]); return r.rows[0] || null; };
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
  return {
    profileId: row.profile_id,
    isOptedIn: row.is_opted_in === true,
    supportLevel: row.support_level || 'self_service',
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
       FOR UPDATE`,
      [userId]
    );
    const profile = profileResult.rows[0];
    if (!profile) {
      await client.query('ROLLBACK');
      return null;
    }

    const isOptedIn = payload.isOptedIn === true;
    const supportLevel = payload.supportLevel || 'self_service';
    let requestStatus = 'not_requested';
    let assignedAdvisorId = null;
    let assignedAt = null;
    let eventType = 'assist_updated';
    let eventScore = null;
    let eventMetadata = {
      supportLevel,
      isOptedIn
    };

    if (isOptedIn && supportLevel === 'advisor_assisted') {
      const { candidates } = await fetchAdvisorCandidates(client, profile.profile_id);
      const topCandidate = candidates[0] || null;
      if (topCandidate) {
        requestStatus = 'assigned';
        assignedAdvisorId = topCandidate.advisor_id;
        assignedAt = new Date();
        eventType = 'advisor_assigned';
        eventScore = topCandidate.score;
        eventMetadata = {
          ...eventMetadata,
          reasons: topCandidate.reasons
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
         request_status,
         preferred_contact_window,
         family_contact_name,
         family_contact_phone,
         notes,
         assigned_advisor_id,
         assigned_at,
         next_review_at,
         created_at,
         updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW() + INTERVAL '7 days',NOW(),NOW())
       ON CONFLICT (profile_id) DO UPDATE SET
         is_opted_in=$3,
         support_level=$4,
         request_status=$5,
         preferred_contact_window=$6,
         family_contact_name=$7,
         family_contact_phone=$8,
         notes=$9,
         assigned_advisor_id=$10,
         assigned_at=$11,
         next_review_at=NOW() + INTERVAL '7 days',
         updated_at=NOW()
       RETURNING *`,
      [
        randomUUID(),
        profile.profile_id,
        isOptedIn,
        supportLevel,
        requestStatus,
        payload.preferredContactWindow || null,
        payload.familyContactName || null,
        payload.familyContactPhone || null,
        payload.notes || null,
        assignedAdvisorId,
        assignedAt
      ]
    );

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
exports.findFullByUserId = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT
       p.*,
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       pd.height_cm,
       pd.weight_kg,
       pd.complexion,
       pd.body_type,
       pd.blood_group,
       ec.education_level,
       ec.occupation,
       ec.annual_income,
       ec.working_city,
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
       EXTRACT(YEAR FROM AGE(p.dob))::int AS age,
       pd.height_cm,
       pd.weight_kg,
       pd.complexion,
       pd.body_type,
       pd.blood_group,
       ec.education_level,
       ec.occupation,
       ec.annual_income,
       ec.working_city,
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
     LEFT JOIN physical_details pd ON p.profile_id=pd.profile_id
     LEFT JOIN education_career ec ON p.profile_id=ec.profile_id
     LEFT JOIN family_details fd ON p.profile_id=fd.profile_id
     LEFT JOIN lifestyle_details ld ON p.profile_id=ld.profile_id
     LEFT JOIN horoscope_details hd ON p.profile_id=hd.profile_id
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
  const photoPrivacy = allowedPhoto.includes(data.photoPrivacy) ? data.photoPrivacy : null;
  const profileVisibility = allowedVisibility.includes(data.profileVisibility) ? data.profileVisibility : null;
  await db.query('UPDATE profiles SET photo_privacy=COALESCE($1,photo_privacy),profile_visibility=COALESCE($2,profile_visibility),updated_at=NOW() WHERE profile_id=$3', [photoPrivacy,profileVisibility,profileId]);
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
  if (!viewer) return;
  await db.query('INSERT INTO profile_views (viewer_id,viewed_profile_id) VALUES ($1,$2) ON CONFLICT (viewer_id, viewed_profile_id) DO UPDATE SET viewed_at=NOW()', [viewer.profile_id, viewedProfileId]);
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
            AND NULLIF(BTRIM(occupation), '') IS NOT NULL
            AND NULLIF(BTRIM(working_city), '') IS NOT NULL
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
