const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { getDB } = require('../config/database');
const logger = require('../utils/logger');
const { CONFIG_KEYS, DEFAULT_CONFIG, getConfigMap, getPublicRuntimeConfig, upsertConfigSection } = require('../../shared/controlPlane');
const { getServiceHealth } = require('../services/serviceHealth');
const { broadcastAdminEvent, getRealtimeSnapshot } = require('../realtime/adminRealtime');

function getAdminSecret() {
  return process.env.ADMIN_JWT_SECRET || process.env.JWT_SECRET;
}

function getAdminEmail() {
  return process.env.ADMIN_EMAIL || 'admin@soulmatch.app';
}

async function isValidAdminPassword(password) {
  if (process.env.ADMIN_PASSWORD_HASH) {
    return bcrypt.compare(password, process.env.ADMIN_PASSWORD_HASH);
  }
  if (process.env.NODE_ENV === 'production') return false;
  if (process.env.ADMIN_PASSWORD) return password === process.env.ADMIN_PASSWORD;
  return process.env.NODE_ENV === 'development' && password === 'admin123';
}

function parseNumber(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeListInput(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || '').trim()).filter(Boolean);
  }
  if (typeof value === 'string') {
    return value.split(',').map((item) => item.trim()).filter(Boolean);
  }
  return [];
}

function normalizeAdvisorStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['active', 'paused', 'suspended'].includes(normalized) ? normalized : null;
}

function normalizeAdvisorKycStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['pending', 'approved', 'rejected'].includes(normalized) ? normalized : null;
}

function makeAgentCode() {
  return `SMAGT-${new Date().getFullYear()}-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;
}

function normalizeReviewStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['draft', 'submitted', 'under_review', 'verified', 'rejected'].includes(normalized) ? normalized : null;
}

function normalizeDocumentReviewStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['uploaded', 'under_review', 'verified', 'rejected'].includes(normalized) ? normalized : null;
}

function hasValue(value) {
  return String(value || '').trim().length > 0;
}

function isVerifiedDocument(documents, documentType) {
  return documents.some((document) =>
    String(document.document_type || '').toLowerCase() === documentType &&
    String(document.status || '').toLowerCase() === 'verified'
  );
}

function hasVerifiedAadhaar(documents) {
  const aadhaarDocs = documents.filter((document) =>
    String(document.document_type || '').toLowerCase() === 'aadhaar' &&
    String(document.status || '').toLowerCase() === 'verified'
  );
  if (!aadhaarDocs.length) return false;
  const sides = new Set(aadhaarDocs.map((document) => String(document.document_side || 'single').toLowerCase()));
  return sides.has('single') || (sides.has('front') && sides.has('back'));
}

function managedProfileDocumentReadiness(profile, documents) {
  const required = [
    { key: 'aadhaar', label: 'Aadhaar', ready: hasVerifiedAadhaar(documents) },
    { key: 'pan_or_voter', label: 'PAN or Voter ID', ready: isVerifiedDocument(documents, 'pan') || isVerifiedDocument(documents, 'voter_id') }
  ];
  const hasEducationClaim = hasValue(profile.education_level) || hasValue(profile.occupation) || hasValue(profile.annual_income);
  if (hasEducationClaim) {
    required.push({
      key: 'education_certificate',
      label: 'Education certificate',
      ready: isVerifiedDocument(documents, 'education_certificate')
    });
  }
  if (String(profile.marital_status || '').toLowerCase().includes('divorce')) {
    required.push({
      key: 'divorce_decree',
      label: 'Divorce decree',
      ready: isVerifiedDocument(documents, 'divorce_decree')
    });
  }
  const missing = required.filter((item) => !item.ready).map((item) => item.label);
  return { complete: missing.length === 0, missing };
}

async function publishManagedProfileIfReady(db, profileId, reviewNote = null) {
  const profileResult = await db.query(
    `SELECT
       p.profile_id,
       p.created_by_advisor_id,
       p.review_status,
       p.submitted_at,
       p.marital_status,
       ec.education_level,
       ec.occupation,
       ec.annual_income
     FROM profiles p
     LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
     WHERE p.profile_id = $1
     LIMIT 1`,
    [profileId]
  );
  const profile = profileResult.rows[0];
  if (!profile || !profile.created_by_advisor_id) return { published: false, reason: 'not_agent_managed' };
  const reviewStatus = String(profile.review_status || 'draft').toLowerCase();
  const agentRequestedVisibility = profile.submitted_at || ['submitted', 'under_review', 'verified'].includes(reviewStatus);
  if (!agentRequestedVisibility) {
    return { published: false, reason: 'agent_visibility_not_enabled' };
  }

  const documentsResult = await db.query(
    `SELECT document_type, document_side, status
     FROM profile_documents
     WHERE profile_id = $1`,
    [profileId]
  );
  const readiness = managedProfileDocumentReadiness(profile, documentsResult.rows);
  if (!readiness.complete) {
    await db.query(
      `UPDATE profiles
       SET review_status = CASE WHEN review_status IN ('draft', 'submitted', 'rejected') THEN 'under_review' ELSE review_status END,
           reviewed_at = NOW(),
           review_notes = COALESCE($2, review_notes),
           updated_at = NOW()
       WHERE profile_id = $1`,
      [profileId, reviewNote]
    );
    return { published: false, reason: 'missing_required_documents', missing: readiness.missing };
  }

  const result = await db.query(
    `UPDATE profiles
     SET review_status = 'verified',
         verification_status = 'verified',
         is_published = TRUE,
         admin_status = 'active',
         reviewed_at = NOW(),
         verified_at = COALESCE(verified_at, NOW()),
         rejection_reason = NULL,
         review_notes = COALESCE($2, review_notes),
         updated_at = NOW()
     WHERE profile_id = $1
     RETURNING *`,
    [profileId, reviewNote]
  );
  return { published: true, profile: result.rows[0] };
}

function normalizeAssistRequestStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  return ['not_requested', 'waiting_assignment', 'assigned', 'paused'].includes(normalized) ? normalized : null;
}

function coerceServiceAreas(body = {}) {
  if (Array.isArray(body.serviceAreas) && body.serviceAreas.length) {
    return body.serviceAreas.map((area, index) => ({
      state: area.state || body.state || null,
      city: area.city || body.city || '',
      locality: area.locality || null,
      pincode: area.pincode || null,
      radiusKm: parseNumber(area.radiusKm ?? area.radius_km, 15),
      priority: parseNumber(area.priority, index === 0 ? 10 : 0),
      isPrimary: index === 0 || area.isPrimary === true || area.is_primary === true
    })).filter((area) => area.city);
  }
  if (!body.city) return [];
  return [{
    state: body.state || null,
    city: body.city,
    locality: body.locality || null,
    pincode: body.pincode || null,
    radiusKm: parseNumber(body.radiusKm ?? body.radius_km, 15),
    priority: 10,
    isPrimary: true
  }];
}

function makeReferralCode() {
  return `SM${crypto.randomBytes(4).toString('hex').toUpperCase()}`;
}

async function getAssistedAssignmentRecord(db, assistedProfileId) {
  const result = await db.query(
    `SELECT
       amp.assisted_profile_id,
       amp.profile_id,
       p.user_id,
       amp.is_opted_in,
       amp.support_level,
       amp.request_status,
       amp.assigned_at,
       amp.next_review_at,
       amp.family_contact_name,
       amp.family_contact_phone,
       amp.preferred_contact_window,
       amp.notes,
       p.first_name,
       p.last_name,
       p.religion,
       p.caste,
       p.mother_tongue,
       fd.family_city,
       fd.family_state,
       fd.family_locality,
       fd.family_pincode,
       a.advisor_id,
       a.full_name AS advisor_name,
       a.phone AS advisor_phone,
       a.city AS advisor_city,
       a.state AS advisor_state,
       a.status AS advisor_status
     FROM assisted_match_profiles amp
     JOIN profiles p ON p.profile_id = amp.profile_id
     LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
     LEFT JOIN advisors a ON a.advisor_id = amp.assigned_advisor_id
     WHERE amp.assisted_profile_id = $1
     LIMIT 1`,
    [assistedProfileId]
  );
  return result.rows[0] || null;
}

function respondServerError(res, err, message) {
  logger.error(err.stack || err.message);
  return res.status(500).json({
    success: false,
    error: {
      code: 'INTERNAL_ERROR',
      message
    }
  });
}

function respondDegraded(res, err, data, message) {
  logger.warn(`${message}: ${err.message}`);
  return res.json({
    success: true,
    data,
    meta: {
      degraded: true,
      message
    }
  });
}

const ROLE_PERMISSIONS = {
  super_admin: ['*'],
  admin: ['dashboard:read', 'profiles:write', 'verification:write', 'payments:write', 'config:write', 'cms:write', 'moderation:write', 'analytics:read'],
  moderator: ['dashboard:read', 'profiles:read', 'verification:write', 'moderation:write', 'analytics:read'],
  support_agent: ['dashboard:read', 'profiles:read', 'profiles:support', 'moderation:read'],
  marketing_manager: ['dashboard:read', 'cms:write', 'campaigns:write', 'analytics:read']
};

function getAdminRole() {
  return process.env.ADMIN_ROLE || 'super_admin';
}

async function auditLog(db, req, action, entityType, entityId = null, metadata = {}) {
  try {
    await db.query(
      `INSERT INTO admin_audit_logs (admin_email, admin_role, action, entity_type, entity_id, metadata, ip_address)
       VALUES ($1,$2,$3,$4,$5,$6::jsonb,$7)`,
      [
        req.admin?.email || 'system',
        req.admin?.role || 'admin',
        action,
        entityType,
        entityId,
        JSON.stringify(metadata || {}),
        req.ip
      ]
    );
  } catch (error) {
    logger.warn(`Audit log skipped: ${error.message}`);
  }
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
      logger.warn(`Notification service rejected member notification: ${response.status}`);
    } catch (error) {
      logger.warn(`Notification service unavailable for member notification: ${error.message}`);
    }
  }

  await db.query(
    `INSERT INTO notifications (notification_id,user_id,title,body,data,status,created_at)
     VALUES ($1,$2,$3,$4,$5::jsonb,'queued',NOW())`,
    [crypto.randomUUID(), userId, title, body, JSON.stringify(data || {})]
  );
  return false;
}

function profileScore(body) {
  const checks = [
    body.firstName || body.first_name,
    body.lastName || body.last_name,
    body.dob,
    body.gender,
    body.religion,
    body.caste,
    body.motherTongue || body.mother_tongue,
    body.educationLevel || body.education_level,
    body.occupation,
    body.workingCity || body.working_city,
    body.familyCity || body.family_city,
    body.aboutMe || body.about_me,
    body.primaryPhotoUrl || body.primary_photo_url
  ];
  return Math.min(100, Math.round((checks.filter(Boolean).length / checks.length) * 100));
}

exports.adminLogin = async (req, res) => {
  try {
    const { email, password } = req.body;
    const normalizedEmail = String(email || '').trim().toLowerCase();
    if (normalizedEmail !== getAdminEmail().toLowerCase()) {
      return res.status(401).json({ success: false, error: { code: 'INVALID_CREDENTIALS', message: 'Invalid admin email or password.' } });
    }
    const valid = await isValidAdminPassword(String(password || ''));
    if (!valid) return res.status(401).json({ success: false, error: { code: 'INVALID_CREDENTIALS', message: 'Invalid admin email or password.' } });
    const role = getAdminRole();
    const token = jwt.sign(
      {
        role,
        roles: [role],
        permissions: ROLE_PERMISSIONS[role] || ROLE_PERMISSIONS.admin,
        email: normalizedEmail
      },
      getAdminSecret(),
      { expiresIn: '8h' }
    );
    res.json({ success: true, data: { token, role, permissions: ROLE_PERMISSIONS[role] || ROLE_PERMISSIONS.admin } });
  } catch (err) {
    respondServerError(res, err, 'Unable to complete admin sign-in right now.');
  }
};

exports.getDashboard = async (req, res) => {
  try {
    const db = await getDB();
    const [users, activeUsers, totalProfiles, profiles, pendingApprovals, subs, reports, revenue, revenue30, analytics, referrals, landings, dau, mau] = await Promise.all([
      db.query('SELECT COUNT(*) FROM users'),
      db.query("SELECT COUNT(*) FROM users WHERE last_login >= NOW() - INTERVAL '15 minutes'"),
      db.query('SELECT COUNT(*) FROM profiles'),
      db.query('SELECT COUNT(*) FROM profiles WHERE is_published=true'),
      db.query("SELECT COUNT(*) FROM profiles WHERE is_published=false OR COALESCE(verification_status,'pending')='pending'"),
      db.query("SELECT COUNT(*) FROM subscriptions WHERE is_active=true AND plan_id!='free'"),
      db.query("SELECT COUNT(*) FROM reports WHERE status='pending'"),
      db.query('SELECT COALESCE(SUM(amount_paid),0) AS total FROM subscriptions WHERE is_active=true'),
      db.query("SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE created_at >= NOW() - INTERVAL '30 days' AND status IN ('paid','success','captured')"),
      db.query(
        `SELECT
           COUNT(*) FILTER (WHERE event_type='sign_up') AS signups,
           COUNT(*) FILTER (WHERE event_type='payment_click') AS payment_clicks,
           COUNT(*) FILTER (WHERE event_type='payment_success') AS payment_successes,
           COUNT(*) FILTER (WHERE event_type='match_made') AS matches_made
         FROM analytics_events
         WHERE created_at >= NOW() - INTERVAL '30 days'`
      ),
      db.query('SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE is_active=true) AS active FROM referral_codes'),
      db.query('SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE is_active=true) AS active FROM landing_pages'),
      db.query("SELECT COUNT(DISTINCT user_id) AS total FROM analytics_events WHERE created_at >= CURRENT_DATE AND user_id IS NOT NULL"),
      db.query("SELECT COUNT(DISTINCT user_id) AS total FROM analytics_events WHERE created_at >= NOW() - INTERVAL '30 days' AND user_id IS NOT NULL")
    ]);
    const today = await db.query("SELECT COUNT(*) FROM users WHERE created_at>=NOW()-INTERVAL '24 hours'");
    const health = await getServiceHealth();
    const signups = parseInt(analytics.rows[0].signups || 0, 10);
    const paymentSuccesses = parseInt(analytics.rows[0].payment_successes || 0, 10);
    const matchesMade = parseInt(analytics.rows[0].matches_made || 0, 10);
    res.json({
      success: true,
      data: {
        totalUsers: parseInt(users.rows[0].count, 10),
        activeUsers: parseInt(activeUsers.rows[0].count, 10),
        totalProfiles: parseInt(totalProfiles.rows[0].count, 10),
        activeProfiles: parseInt(profiles.rows[0].count, 10),
        pendingApprovals: parseInt(pendingApprovals.rows[0].count, 10),
        premiumUsers: parseInt(subs.rows[0].count, 10),
        pendingReports: parseInt(reports.rows[0].count, 10),
        newUsersToday: parseInt(today.rows[0].count, 10),
        totalRevenue: parseFloat(revenue.rows[0].total),
        revenue30d: parseFloat(revenue30.rows[0].total),
        dau: parseInt(dau.rows[0].total || 0, 10),
        mau: parseInt(mau.rows[0].total || 0, 10),
        conversionRate: signups ? Number(((paymentSuccesses / signups) * 100).toFixed(2)) : 0,
        matchSuccessRate: signups ? Number(((matchesMade / signups) * 100).toFixed(2)) : 0,
        analytics: {
          signups,
          paymentClicks: parseInt(analytics.rows[0].payment_clicks || 0, 10),
          paymentSuccesses,
          matchesMade
        },
        referrals: {
          totalCodes: parseInt(referrals.rows[0].total || 0, 10),
          activeCodes: parseInt(referrals.rows[0].active || 0, 10)
        },
        landingPages: {
          total: parseInt(landings.rows[0].total || 0, 10),
          active: parseInt(landings.rows[0].active || 0, 10)
        },
        services: health
      }
    });
  } catch (err) {
    logger.error(err.stack || err.message);
    const services = await getServiceHealth().catch(() => []);
    res.json({
      success: true,
      data: {
        totalUsers: 0,
        activeUsers: 0,
        totalProfiles: 0,
        activeProfiles: 0,
        pendingApprovals: 0,
        premiumUsers: 0,
        pendingReports: 0,
        newUsersToday: 0,
        totalRevenue: 0,
        analytics: {
          signups: 0,
          paymentClicks: 0,
          paymentSuccesses: 0,
          matchesMade: 0
        },
        referrals: {
          totalCodes: 0,
          activeCodes: 0
        },
        landingPages: {
          total: 0,
          active: 0
        },
        services,
        degraded: true,
        message: 'Dashboard database queries are unavailable. Showing live service health and safe empty metrics.'
      }
    });
  }
};

exports.getUsers = async (req, res) => {
  try {
    const db = await getDB();
    const page = parseInt(req.query.page, 10) || 1;
    const limit = Math.min(parseInt(req.query.limit, 10) || 20, 100);
    const search = req.query.search || '';
    const result = await db.query(
      `SELECT
         u.user_id,
         u.phone,
         u.email,
         u.is_active,
         u.is_banned,
         u.referred_by_code,
         u.acquisition_source,
         u.created_at,
         p.first_name,
         p.last_name,
         s.plan_id
       FROM users u
       LEFT JOIN profiles p ON p.user_id = u.user_id
       LEFT JOIN subscriptions s ON s.user_id = u.user_id AND s.is_active=true
       WHERE (u.phone ILIKE $1 OR p.first_name ILIKE $1 OR u.email ILIKE $1 OR COALESCE(u.referred_by_code, '') ILIKE $1)
       ORDER BY u.created_at DESC
       LIMIT $2 OFFSET $3`,
      [`%${search}%`, limit, (page - 1) * limit]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    logger.error(err.stack || err.message);
    res.json({
      success: true,
      data: [],
      meta: {
        degraded: true,
        message: 'User database is unavailable. Showing an empty user list until the database connection is restored.'
      }
    });
  }
};

exports.getRealtimeSnapshot = async (req, res) => {
  try {
    const data = await getRealtimeSnapshot();
    res.json({ success: true, data });
  } catch (err) {
    respondDegraded(res, err, {
      generatedAt: new Date().toISOString(),
      totalUsers: 0,
      totalProfiles: 0,
      liveUsers: 0,
      activeUsers: 0,
      activeProfiles: 0,
      pendingApprovals: 0,
      premiumUsers: 0,
      totalRevenue: 0,
      revenue30d: 0,
      newUsersToday: 0,
      paymentsToday: 0,
      revenueToday: 0,
      matchesToday: 0,
      pendingReports: 0,
      fraudAlerts: 0,
      dau: 0,
      mau: 0,
      conversionRate: 0,
      matchSuccessRate: 0,
      analytics: {
        signups: 0,
        paymentClicks: 0,
        paymentSuccesses: 0,
        matchesMade: 0
      }
    }, 'Realtime metrics are unavailable. Showing safe empty metrics.');
  }
};

exports.banUser = async (req, res) => {
  try {
    const db = await getDB();
    await db.query('UPDATE users SET is_banned=true,is_active=false,updated_at=NOW() WHERE user_id=$1', [req.params.id]);
    res.json({ success: true, message: 'User banned' });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.unbanUser = async (req, res) => {
  try {
    const db = await getDB();
    await db.query('UPDATE users SET is_banned=false,is_active=true,updated_at=NOW() WHERE user_id=$1', [req.params.id]);
    res.json({ success: true, message: 'User unbanned' });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.getProfiles = async (req, res) => {
  try {
    const db = await getDB();
    const page = Math.max(parseInt(req.query.page, 10) || 1, 1);
    const limit = Math.min(parseInt(req.query.limit, 10) || 25, 100);
    const search = `%${String(req.query.search || '').trim()}%`;
    const status = String(req.query.status || 'all');
    const filterParams = [
      search,
      req.query.religion || null,
      req.query.caste || null,
      req.query.location || null,
      req.query.profession || null,
      status
    ];
    const filters = [
      ...filterParams,
      limit,
      (page - 1) * limit
    ];
    const whereSql = `WHERE (
          p.first_name ILIKE $1 OR p.last_name ILIKE $1 OR u.phone ILIKE $1 OR u.email ILIKE $1
          OR COALESCE(ec.occupation, '') ILIKE $1 OR COALESCE(ec.working_city, '') ILIKE $1
       )
       AND ($2::text IS NULL OR p.religion ILIKE $2)
       AND ($3::text IS NULL OR p.caste ILIKE $3)
       AND ($4::text IS NULL OR COALESCE(ec.working_city, fd.family_city, '') ILIKE $4)
       AND ($5::text IS NULL OR COALESCE(ec.occupation, '') ILIKE $5)
       AND (
          $6 = 'all'
          OR ($6 = 'published' AND p.is_published=true)
          OR ($6 = 'pending' AND p.is_published=false)
          OR ($6 = 'suspended' AND (u.is_banned=true OR COALESCE(p.admin_status,'active')='suspended'))
       )`;
    const countResult = await db.query(
      `SELECT COUNT(*) AS total
       FROM profiles p
       JOIN users u ON u.user_id = p.user_id
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
       ${whereSql}`,
      filterParams
    );
    const result = await db.query(
      `SELECT
         p.profile_id,
         p.user_id,
         u.phone,
         u.email,
         u.is_active,
         u.is_banned,
         p.first_name,
         p.last_name,
         p.dob,
         p.gender,
         p.religion,
         p.caste,
         p.mother_tongue,
         p.marital_status,
         p.completion_score,
         p.is_published,
         COALESCE(p.verification_status, 'pending') AS verification_status,
         COALESCE(p.admin_status, CASE WHEN u.is_banned THEN 'suspended' ELSE 'active' END) AS admin_status,
         p.primary_photo_url,
         ec.education_level,
         ec.occupation,
         ec.annual_income,
         ec.working_city,
         ph.height_cm,
         fd.family_city,
         fd.family_type,
         ld.diet,
         ld.about_me,
         p.created_at
       FROM profiles p
       JOIN users u ON u.user_id = p.user_id
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN physical_details ph ON ph.profile_id = p.profile_id
       LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
       LEFT JOIN lifestyle_details ld ON ld.profile_id = p.profile_id
       ${whereSql}
       ORDER BY p.created_at DESC
       LIMIT $7 OFFSET $8`,
      filters
    );
    const total = parseInt(countResult.rows[0]?.total || 0, 10);
    res.json({ success: true, data: result.rows, meta: { page, limit, total, totalPages: Math.max(1, Math.ceil(total / limit)) } });
  } catch (err) {
    respondDegraded(res, err, [], 'Profile store is unavailable. Showing an empty profile list.');
  }
};

exports.getAdvisors = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         a.*,
         COALESCE(active_counts.active_assignments, 0) AS active_assignments,
         COALESCE(
           json_agg(
             DISTINCT jsonb_build_object(
               'advisorServiceAreaId', asa.advisor_service_area_id,
               'city', asa.city,
               'state', asa.state,
               'locality', asa.locality,
               'pincode', asa.pincode,
               'radiusKm', asa.radius_km,
               'priority', asa.priority,
               'isPrimary', asa.is_primary
             )
           ) FILTER (WHERE asa.advisor_service_area_id IS NOT NULL),
           '[]'::json
         ) AS service_areas,
         COALESCE(
           json_agg(
             DISTINCT jsonb_build_object(
               'advisorKycDocumentId', akd.advisor_kyc_document_id,
               'documentType', akd.document_type,
               'documentSide', akd.document_side,
               'fileUrl', akd.file_url,
               'status', akd.status,
               'reviewComment', akd.review_comment,
               'uploadedAt', akd.uploaded_at,
               'reviewedAt', akd.reviewed_at
             )
           ) FILTER (WHERE akd.advisor_kyc_document_id IS NOT NULL),
           '[]'::json
         ) AS kyc_documents
       FROM advisors a
       LEFT JOIN advisor_service_areas asa ON asa.advisor_id = a.advisor_id
       LEFT JOIN advisor_kyc_documents akd ON akd.advisor_id = a.advisor_id
       LEFT JOIN (
         SELECT assigned_advisor_id, COUNT(*) AS active_assignments
         FROM assisted_match_profiles
         WHERE assigned_advisor_id IS NOT NULL AND request_status = 'assigned'
         GROUP BY assigned_advisor_id
       ) active_counts ON active_counts.assigned_advisor_id = a.advisor_id
       GROUP BY a.advisor_id, active_counts.active_assignments
       ORDER BY a.status = 'active' DESC, a.updated_at DESC, a.created_at DESC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Advisor roster is unavailable right now.');
  }
};

exports.createAdvisor = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const body = req.body || {};
    if (!body.fullName || !body.phone || !body.city) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Advisor fullName, phone, and city are required.' } });
    }
    const serviceAreas = coerceServiceAreas(body);
    await client.query('BEGIN');
    const advisor = await client.query(
      `INSERT INTO advisors (
         advisor_id, full_name, phone, email, service_label, bio, gender,
         city, state, pincode, languages, communities, max_active_assignments,
         success_rate, complaint_score, average_rating, kyc_status, status,
         membership_plan, membership_expires_at, notes, created_at, updated_at
       )
       VALUES (
         $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11::jsonb,$12::jsonb,$13,$14,$15,$16,$17,$18,$19,$20,$21,NOW(),NOW()
       )
       RETURNING *`,
      [
        crypto.randomUUID(),
        body.fullName,
        body.phone,
        body.email || null,
        body.serviceLabel || 'SoulMatch Advisor',
        body.bio || null,
        body.gender || null,
        body.city,
        body.state || null,
        body.pincode || null,
        JSON.stringify(normalizeListInput(body.languages)),
        JSON.stringify(normalizeListInput(body.communities)),
        Math.max(parseNumber(body.maxActiveAssignments, 25), 1),
        parseNumber(body.successRate, 0),
        parseNumber(body.complaintScore, 0),
        parseNumber(body.averageRating, 0),
        normalizeAdvisorKycStatus(body.kycStatus) || 'pending',
        normalizeAdvisorStatus(body.status) || 'active',
        body.membershipPlan || 'starter',
        body.membershipExpiresAt || null,
        body.notes || null
      ]
    );
    for (const area of serviceAreas) {
      await client.query(
        `INSERT INTO advisor_service_areas (
           advisor_service_area_id, advisor_id, state, city, locality, pincode, radius_km, priority, is_primary, created_at
         ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())`,
        [crypto.randomUUID(), advisor.rows[0].advisor_id, area.state, area.city, area.locality, area.pincode, area.radiusKm, area.priority, area.isPrimary]
      );
    }
    await auditLog(client, req, 'advisor.create', 'advisor', advisor.rows[0].advisor_id, { fullName: body.fullName, phone: body.phone });
    await client.query('COMMIT');
    res.json({ success: true, data: advisor.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to create advisor right now.');
  } finally {
    client.release();
  }
};

exports.updateAdvisor = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const body = req.body || {};
    const advisorId = req.params.id;
    const serviceAreas = coerceServiceAreas(body);
    await client.query('BEGIN');
    const result = await client.query(
      `UPDATE advisors
       SET full_name = COALESCE($2, full_name),
           phone = COALESCE($3, phone),
           email = COALESCE($4, email),
           service_label = COALESCE($5, service_label),
           bio = COALESCE($6, bio),
           gender = COALESCE($7, gender),
           city = COALESCE($8, city),
           state = COALESCE($9, state),
           pincode = COALESCE($10, pincode),
           languages = COALESCE($11::jsonb, languages),
           communities = COALESCE($12::jsonb, communities),
           max_active_assignments = COALESCE($13, max_active_assignments),
           success_rate = COALESCE($14, success_rate),
           complaint_score = COALESCE($15, complaint_score),
           average_rating = COALESCE($16, average_rating),
           kyc_status = COALESCE($17, kyc_status),
           status = COALESCE($18, status),
           membership_plan = COALESCE($19, membership_plan),
           membership_expires_at = COALESCE($20, membership_expires_at),
           notes = COALESCE($21, notes),
           updated_at = NOW()
       WHERE advisor_id = $1
       RETURNING *`,
      [
        advisorId,
        body.fullName || null,
        body.phone || null,
        body.email || null,
        body.serviceLabel || null,
        body.bio || null,
        body.gender || null,
        body.city || null,
        body.state || null,
        body.pincode || null,
        body.languages !== undefined ? JSON.stringify(normalizeListInput(body.languages)) : null,
        body.communities !== undefined ? JSON.stringify(normalizeListInput(body.communities)) : null,
        body.maxActiveAssignments !== undefined ? Math.max(parseNumber(body.maxActiveAssignments, 25), 1) : null,
        body.successRate !== undefined ? parseNumber(body.successRate, 0) : null,
        body.complaintScore !== undefined ? parseNumber(body.complaintScore, 0) : null,
        body.averageRating !== undefined ? parseNumber(body.averageRating, 0) : null,
        normalizeAdvisorKycStatus(body.kycStatus),
        normalizeAdvisorStatus(body.status),
        body.membershipPlan || null,
        body.membershipExpiresAt || null,
        body.notes || null
      ]
    );
    if (!result.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Advisor not found.' } });
    }
    if (serviceAreas.length) {
      await client.query('DELETE FROM advisor_service_areas WHERE advisor_id = $1', [advisorId]);
      for (const area of serviceAreas) {
        await client.query(
          `INSERT INTO advisor_service_areas (
             advisor_service_area_id, advisor_id, state, city, locality, pincode, radius_km, priority, is_primary, created_at
           ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,NOW())`,
          [crypto.randomUUID(), advisorId, area.state, area.city, area.locality, area.pincode, area.radiusKm, area.priority, area.isPrimary]
        );
      }
    }
    await auditLog(client, req, 'advisor.update', 'advisor', advisorId, body);
    await client.query('COMMIT');
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to update advisor right now.');
  } finally {
    client.release();
  }
};

exports.updateAdvisorStatus = async (req, res) => {
  try {
    const db = await getDB();
    const advisorId = req.params.id;
    const status = normalizeAdvisorStatus(req.body?.status);
    const kycStatus = normalizeAdvisorKycStatus(req.body?.kycStatus);
    if (!status && !kycStatus) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Provide advisor status or KYC status.' } });
    }
    const result = await db.query(
      `UPDATE advisors
       SET status = COALESCE($2, status),
           kyc_status = COALESCE($3, kyc_status),
           onboarding_status = CASE
             WHEN $3 = 'approved' THEN 'approved'
             WHEN $3 = 'rejected' THEN 'rejected'
             WHEN $3 = 'pending' THEN 'pending'
             ELSE onboarding_status
           END,
           aadhaar_verification_status = CASE WHEN $3 = 'approved' THEN 'verified' WHEN $3 = 'rejected' THEN 'rejected' ELSE aadhaar_verification_status END,
           pan_verification_status = CASE WHEN $3 = 'approved' THEN 'verified' WHEN $3 = 'rejected' THEN 'rejected' ELSE pan_verification_status END,
           kyc_name_match_status = CASE WHEN $3 = 'approved' THEN 'matched' WHEN $3 = 'rejected' THEN 'manual_review' ELSE kyc_name_match_status END,
           bank_verification_status = CASE WHEN $3 = 'approved' THEN 'verified' WHEN $3 = 'rejected' THEN 'rejected' ELSE bank_verification_status END,
           bank_name_match_status = CASE WHEN $3 = 'approved' THEN 'matched' WHEN $3 = 'rejected' THEN 'manual_review' ELSE bank_name_match_status END,
           penny_drop_status = CASE WHEN $3 = 'approved' THEN 'verified' WHEN $3 = 'rejected' THEN 'failed' ELSE penny_drop_status END,
           penny_drop_name_match_status = CASE WHEN $3 = 'approved' THEN 'matched' WHEN $3 = 'rejected' THEN 'manual_review' ELSE penny_drop_name_match_status END,
           fraud_review_status = CASE WHEN $3 = 'approved' THEN 'cleared' WHEN $3 = 'rejected' THEN 'needs_resubmission' ELSE fraud_review_status END,
           updated_at = NOW()
       WHERE advisor_id = $1
       RETURNING *`,
      [advisorId, status, kycStatus]
    );
    if (!result.rows[0]) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Advisor not found.' } });
    if (kycStatus) {
      const documentStatus = kycStatus === 'approved'
        ? 'verified'
        : kycStatus === 'rejected'
          ? 'rejected'
          : 'under_review';
      await db.query(
        `UPDATE advisor_kyc_documents
         SET status = $2,
             review_comment = COALESCE($3, review_comment),
             reviewed_at = NOW(),
             reviewed_by = $4,
             updated_at = NOW()
         WHERE advisor_id = $1`,
        [
          advisorId,
          documentStatus,
          req.body?.note || null,
          req.admin?.email || 'admin'
        ]
      );
    }
    await auditLog(db, req, 'advisor.status', 'advisor', advisorId, { status, kycStatus });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to update advisor status right now.');
  }
};

exports.approveAgent = async (req, res) => {
  try {
    const db = await getDB();
    const advisorId = req.params.id;
    const result = await db.query(
      `UPDATE advisors
       SET onboarding_status = 'approved',
           kyc_status = 'approved',
           status = COALESCE($2, status),
           aadhaar_verification_status = 'verified',
           pan_verification_status = 'verified',
           kyc_name_match_status = 'matched',
           bank_verification_status = 'verified',
           bank_name_match_status = 'matched',
           penny_drop_status = 'verified',
           penny_drop_name_match_status = 'matched',
           fraud_review_status = 'cleared',
           approved_at = NOW(),
           approved_by = $3,
           rejected_at = NULL,
           onboarding_rejection_reason = NULL,
           agent_code = COALESCE(agent_code, $4),
           updated_at = NOW()
       WHERE advisor_id = $1
       RETURNING *`,
      [advisorId, normalizeAdvisorStatus(req.body?.status) || 'active', req.admin?.email || 'admin', makeAgentCode()]
    );
    const advisor = result.rows[0];
    if (!advisor) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Agent not found.' } });
    await auditLog(db, req, 'agent.approve', 'advisor', advisorId, { agentCode: advisor.agent_code });
    if (advisor.user_id) {
      await notifyMember(
        db,
        advisor.user_id,
        'Agent account approved',
        `Your SoulMatch agent account is approved. Agent ID ${advisor.agent_code} is now active.`,
        { type: 'agent_onboarding', status: 'approved', advisorId, agentCode: advisor.agent_code }
      ).catch((error) => logger.warn(`Agent approval notification skipped: ${error.message}`));
    }
    res.json({ success: true, data: advisor });
  } catch (err) {
    respondServerError(res, err, 'Unable to approve this agent right now.');
  }
};

exports.rejectAgent = async (req, res) => {
  try {
    const db = await getDB();
    const advisorId = req.params.id;
    const note = String(req.body?.note || '').trim();
    const result = await db.query(
      `UPDATE advisors
       SET onboarding_status = 'rejected',
           kyc_status = 'rejected',
           aadhaar_verification_status = 'rejected',
           pan_verification_status = 'rejected',
           bank_verification_status = 'rejected',
           fraud_review_status = 'needs_resubmission',
           onboarding_rejection_reason = $2,
           rejected_at = NOW(),
           updated_at = NOW()
       WHERE advisor_id = $1
       RETURNING *`,
      [advisorId, note || 'Your registration needs clearer business details or KYC documents.']
    );
    const advisor = result.rows[0];
    if (!advisor) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Agent not found.' } });
    await auditLog(db, req, 'agent.reject', 'advisor', advisorId, { note });
    if (advisor.user_id) {
      await notifyMember(
        db,
        advisor.user_id,
        'Agent account needs attention',
        note || 'Your SoulMatch agent registration was declined. Please update your details and submit again.',
        { type: 'agent_onboarding', status: 'rejected', advisorId }
      ).catch((error) => logger.warn(`Agent rejection notification skipped: ${error.message}`));
    }
    res.json({ success: true, data: advisor });
  } catch (err) {
    respondServerError(res, err, 'Unable to reject this agent right now.');
  }
};

exports.getAssistedAssignments = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         amp.assisted_profile_id,
         amp.profile_id,
         amp.is_opted_in,
         amp.support_level,
         amp.request_status,
         amp.assigned_at,
         amp.family_contact_name,
         amp.family_contact_phone,
         amp.notes,
         p.first_name,
         p.last_name,
         p.religion,
         p.caste,
         p.mother_tongue,
         fd.family_city,
         fd.family_state,
         fd.family_locality,
         fd.family_pincode,
         a.advisor_id,
         a.full_name AS advisor_name,
         a.phone AS advisor_phone,
         a.city AS advisor_city,
         a.state AS advisor_state,
         a.status AS advisor_status
       FROM assisted_match_profiles amp
       JOIN profiles p ON p.profile_id = amp.profile_id
       LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
       LEFT JOIN advisors a ON a.advisor_id = amp.assigned_advisor_id
       WHERE amp.is_opted_in = true
       ORDER BY
         CASE amp.request_status
           WHEN 'waiting_assignment' THEN 0
           WHEN 'assigned' THEN 1
           ELSE 2
         END,
         amp.updated_at DESC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Assisted assignment queue is unavailable right now.');
  }
};

exports.updateAssistedAssignment = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const assistedProfileId = req.params.id;
    const existing = await getAssistedAssignmentRecord(client, assistedProfileId);
    if (!existing) {
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Assisted assignment not found.' } });
    }

    const body = req.body || {};
    const hasAdvisorId = Object.prototype.hasOwnProperty.call(body, 'advisorId') || Object.prototype.hasOwnProperty.call(body, 'advisor_id');
    const requestedAdvisorId = hasAdvisorId ? String(body.advisorId || body.advisor_id || '').trim() : existing.advisor_id;
    let nextAdvisorId = requestedAdvisorId || null;
    const nextStatus = normalizeAssistRequestStatus(body.requestStatus || body.request_status) || existing.request_status;
    const nextNotes = body.notes !== undefined ? String(body.notes || '').trim() || null : existing.notes;

    if (!normalizeAssistRequestStatus(nextStatus)) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Invalid assisted request status.' } });
    }

    if (['waiting_assignment', 'not_requested'].includes(nextStatus)) {
      nextAdvisorId = null;
    }

    let advisorRecord = null;
    if (nextAdvisorId) {
      const advisorResult = await client.query(
        `SELECT advisor_id, full_name, status, kyc_status
         FROM advisors
         WHERE advisor_id = $1
         LIMIT 1`,
        [nextAdvisorId]
      );
      advisorRecord = advisorResult.rows[0] || null;
      if (!advisorRecord) {
        return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Selected advisor does not exist.' } });
      }
      if (advisorRecord.status !== 'active' || advisorRecord.kyc_status !== 'approved') {
        return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Advisor must be active and KYC approved before taking assisted members.' } });
      }
    }

    if (nextStatus === 'assigned' && !nextAdvisorId) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Choose an advisor before marking the request as assigned.' } });
    }

    const assignmentChanged = String(existing.advisor_id || '') !== String(nextAdvisorId || '');
    const statusChanged = existing.request_status !== nextStatus;
    const assignedAt = nextAdvisorId
      ? (assignmentChanged || !existing.assigned_at ? new Date() : existing.assigned_at)
      : null;

    await client.query('BEGIN');
    await client.query(
      `UPDATE assisted_match_profiles
       SET request_status = $2,
           assigned_advisor_id = $3,
           assigned_at = $4,
           notes = $5,
           next_review_at = NOW() + INTERVAL '7 days',
           updated_at = NOW()
       WHERE assisted_profile_id = $1`,
      [assistedProfileId, nextStatus, nextAdvisorId, assignedAt, nextNotes]
    );

    const eventType = assignmentChanged
      ? (nextAdvisorId ? 'advisor_reassigned' : 'advisor_unassigned')
      : (statusChanged ? `status_${nextStatus}` : 'assist_note_updated');

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
        crypto.randomUUID(),
        existing.profile_id,
        nextAdvisorId,
        eventType,
        null,
        JSON.stringify({
          previousStatus: existing.request_status,
          nextStatus,
          previousAdvisorId: existing.advisor_id || null,
          nextAdvisorId,
          note: nextNotes || null
        })
      ]
    );

    await auditLog(client, req, 'assist.assignment.update', 'assisted_match_profile', assistedProfileId, {
      profileId: existing.profile_id,
      nextStatus,
      nextAdvisorId
    });
    await client.query('COMMIT');

    const updated = await getAssistedAssignmentRecord(client, assistedProfileId);
    if (updated?.user_id) {
      let title = 'SoulMatch Assist updated';
      let bodyText = 'Your assisted matchmaking request has been updated by the SoulMatch team.';
      if (nextStatus === 'assigned' && updated.advisor_name) {
        title = 'SoulMatch Assist advisor assigned';
        bodyText = `${updated.advisor_name} is now helping with your matchmaking journey.`;
      } else if (nextStatus === 'waiting_assignment') {
        title = 'SoulMatch Assist request in queue';
        bodyText = 'We are matching your family with the right local advisor now.';
      } else if (nextStatus === 'paused') {
        title = 'SoulMatch Assist paused';
        bodyText = 'Your assisted matchmaking request is paused for review. You can still use SoulMatch directly anytime.';
      }
      await notifyMember(db, updated.user_id, title, bodyText, {
        profileId: updated.profile_id,
        assistedProfileId,
        requestStatus: nextStatus,
        advisorId: nextAdvisorId
      }).catch((error) => logger.warn(`Assist notification skipped: ${error.message}`));
    }
    res.json({
      success: true,
      data: updated,
      message: nextAdvisorId
        ? `Assisted member is now routed to ${advisorRecord?.full_name || updated?.advisor_name || 'the selected advisor'}.`
        : 'Assisted assignment queue updated.'
    });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to update the assisted assignment right now.');
  } finally {
    client.release();
  }
};

exports.createProfile = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const body = req.body || {};
    await client.query('BEGIN');
    const user = await client.query(
      `INSERT INTO users (phone, email, is_verified, is_active, acquisition_source, created_at, updated_at)
       VALUES ($1,$2,$3,true,'admin_manual',NOW(),NOW())
       RETURNING user_id, phone, email`,
      [body.phone || null, body.email || null, body.isVerified === true]
    );
    const userId = user.rows[0].user_id;
    const score = profileScore(body);
    const profile = await client.query(
      `INSERT INTO profiles (
         user_id, first_name, last_name, dob, gender, religion, caste, mother_tongue,
         marital_status, completion_score, is_published, primary_photo_url,
         verification_status, admin_status, created_at, updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,'active',NOW(),NOW())
       RETURNING *`,
      [
        userId,
        body.firstName || body.first_name || '',
        body.lastName || body.last_name || '',
        body.dob || null,
        body.gender || '',
        body.religion || '',
        body.caste || '',
        body.motherTongue || body.mother_tongue || '',
        body.maritalStatus || body.marital_status || 'never_married',
        score,
        body.isPublished === true,
        body.primaryPhotoUrl || body.primary_photo_url || null,
        body.verificationStatus || 'pending'
      ]
    );
    const profileId = profile.rows[0].profile_id;
    await client.query(
      `INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, body_type, blood_group)
       VALUES ($1,$2,$3,$4,$5,$6)`,
      [profileId, body.heightCm || body.height_cm || null, body.weightKg || null, body.complexion || '', body.bodyType || '', body.bloodGroup || '']
    );
    await client.query(
      `INSERT INTO education_career (profile_id, education_level, occupation, annual_income, working_city)
       VALUES ($1,$2,$3,$4,$5)`,
      [profileId, body.educationLevel || '', body.occupation || '', body.annualIncome || '', body.workingCity || body.working_city || '']
    );
    await client.query(
      `INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city)
       VALUES ($1,$2,$3,$4,$5,$6,$7)`,
      [profileId, body.fatherOccupation || '', body.motherOccupation || '', body.numBrothers || 0, body.numSisters || 0, body.familyType || '', body.familyCity || '']
    );
    await client.query(
      `INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me)
       VALUES ($1,$2,$3,$4,$5)`,
      [profileId, body.diet || '', body.smoking || 'never', body.drinking || 'never', body.aboutMe || body.about_me || '']
    );
    await client.query(
      `INSERT INTO subscriptions (user_id, plan_id, is_active, amount_paid)
       VALUES ($1,'free',true,0)`,
      [userId]
    );
    await auditLog(client, req, 'profile.create', 'profile', profileId, { phone: body.phone, email: body.email });
    await client.query('COMMIT');
    broadcastAdminEvent('admin:profile_created', { profile: profile.rows[0] });
    res.status(201).json({ success: true, data: profile.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    if (err.code === '23505') {
      return res.status(409).json({ success: false, error: { code: 'CONFLICT', message: 'Phone or email already exists.' } });
    }
    respondServerError(res, err, 'Unable to create profile right now.');
  } finally {
    client.release();
  }
};

exports.bulkCreateProfiles = async (req, res) => {
  try {
    const rows = Array.isArray(req.body?.profiles) ? req.body.profiles : [];
    if (!rows.length) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'profiles[] is required.' } });
    }
    const results = [];
    for (const row of rows.slice(0, 250)) {
      const fakeReq = { ...req, body: row };
      const created = await new Promise((resolve) => {
        const fakeRes = {
          status: () => fakeRes,
          json: (payload) => resolve(payload)
        };
        exports.createProfile(fakeReq, fakeRes);
      });
      results.push(created);
    }
    res.json({ success: true, data: { requested: rows.length, processed: results.length, results } });
  } catch (err) {
    respondServerError(res, err, 'Unable to bulk create profiles right now.');
  }
};

exports.updateProfile = async (req, res) => {
  try {
    const db = await getDB();
    const body = req.body || {};
    const profileId = req.params.id;
    const score = profileScore(body);
    const result = await db.query(
      `UPDATE profiles
       SET first_name=COALESCE($2, first_name),
           last_name=COALESCE($3, last_name),
           religion=COALESCE($4, religion),
           caste=COALESCE($5, caste),
           mother_tongue=COALESCE($6, mother_tongue),
           primary_photo_url=COALESCE($7, primary_photo_url),
           completion_score=GREATEST(completion_score, $8),
           updated_at=NOW()
       WHERE profile_id=$1
       RETURNING *`,
      [
        profileId,
        body.firstName || body.first_name || null,
        body.lastName || body.last_name || null,
        body.religion || null,
        body.caste || null,
        body.motherTongue || body.mother_tongue || null,
        body.primaryPhotoUrl || body.primary_photo_url || null,
        score
      ]
    );
    if (!result.rows[0]) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    await auditLog(db, req, 'profile.update', 'profile', profileId, body);
    broadcastAdminEvent('admin:profile_updated', { profile: result.rows[0] });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to update profile right now.');
  }
};

exports.updateProfileStatus = async (req, res) => {
  try {
    const db = await getDB();
    const action = String(req.body?.action || '').toLowerCase();
    const profileId = req.params.id;
    const reason = String(req.body?.reason || '').trim() || null;
    const updates = {
      approve: { published: true, verification: 'verified', admin: 'active', review: 'verified' },
      reject: { published: false, verification: 'rejected', admin: 'rejected', review: 'rejected' },
      suspend: { published: false, verification: 'pending', admin: 'suspended', review: 'under_review' },
      restore: { published: true, verification: 'verified', admin: 'active', review: 'verified' }
    }[action];
    if (!updates) return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Unsupported profile action.' } });
    const currentResult = await db.query(
      `SELECT created_by_advisor_id, review_status, submitted_at
       FROM profiles
       WHERE profile_id = $1
       LIMIT 1`,
      [profileId]
    );
    const currentProfile = currentResult.rows[0];
    if (!currentProfile) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    const currentReviewStatus = String(currentProfile.review_status || 'draft').toLowerCase();
    const agentRequestedVisibility = currentProfile.submitted_at || ['submitted', 'under_review', 'verified'].includes(currentReviewStatus);
    if (currentProfile.created_by_advisor_id && ['approve', 'restore'].includes(action) && !agentRequestedVisibility) {
      return res.status(409).json({
        success: false,
        error: {
          code: 'PROFILE_NOT_SUBMITTED',
          message: 'Agent-created profile is saved as draft. Ask the agent to enable visibility and submit it before publishing.'
        }
      });
    }
    const result = await db.query(
      `UPDATE profiles
       SET is_published=$2,
           verification_status=$3,
           admin_status=$4,
           review_status = CASE WHEN created_by_advisor_id IS NOT NULL THEN $5 ELSE review_status END,
           reviewed_at = CASE WHEN created_by_advisor_id IS NOT NULL THEN NOW() ELSE reviewed_at END,
           verified_at = CASE WHEN created_by_advisor_id IS NOT NULL AND $5='verified' THEN COALESCE(verified_at, NOW()) ELSE verified_at END,
           rejection_reason = CASE
             WHEN created_by_advisor_id IS NOT NULL AND $5='rejected' THEN COALESCE($6, rejection_reason, 'Profile rejected by admin.')
             WHEN created_by_advisor_id IS NOT NULL AND $5='verified' THEN NULL
             ELSE rejection_reason
           END,
           updated_at=NOW()
       WHERE profile_id=$1
       RETURNING *`,
      [profileId, updates.published, updates.verification, updates.admin, updates.review, reason]
    );
    if (!result.rows[0]) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    await auditLog(db, req, `profile.${action}`, 'profile', profileId, { reason });
    broadcastAdminEvent('admin:profile_status', { action, profile: result.rows[0] });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to update profile status right now.');
  }
};

exports.deleteProfile = async (req, res) => {
  try {
    const db = await getDB();
    const profileId = req.params.id;
    const result = await db.query('DELETE FROM profiles WHERE profile_id=$1 RETURNING profile_id, user_id', [profileId]);
    if (!result.rows[0]) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    await auditLog(db, req, 'profile.delete', 'profile', profileId, {});
    broadcastAdminEvent('admin:profile_deleted', { profileId });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to delete profile right now.');
  }
};

exports.getPendingVerifications = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         v.*,
         u.phone,
         u.email,
         p.profile_id,
         p.first_name,
         p.last_name,
         p.primary_photo_url,
         p.completion_score,
         COALESCE(p.verification_status,'pending') AS profile_verification_status,
         COALESCE(p.admin_status,'active') AS admin_status,
         ec.occupation,
         ec.working_city,
         LEAST(
           100,
           (CASE WHEN u.is_verified THEN 12 ELSE 0 END) +
           (CASE WHEN u.google_id IS NOT NULL THEN 8 ELSE 0 END) +
           (CASE WHEN COALESCE(p.completion_score,0) >= 90 THEN 15 WHEN COALESCE(p.completion_score,0) >= 70 THEN 12 WHEN COALESCE(p.completion_score,0) >= 50 THEN 8 ELSE 0 END) +
           (CASE WHEN COALESCE(p.verification_status,'pending')='verified' THEN 15 ELSE 0 END) +
           (CASE WHEN NULLIF(p.primary_photo_url,'') IS NOT NULL THEN 7 ELSE 0 END) +
           10 -
           LEAST(35, COALESCE(open_reports.total,0) * 12)
         ) AS trust_score,
         COALESCE(open_reports.total,0) AS open_report_count,
         COALESCE(approved_types.types, ARRAY[]::text[]) AS approved_verification_types
       FROM verifications v
       JOIN users u ON v.user_id = u.user_id
       LEFT JOIN profiles p ON p.user_id = v.user_id
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN LATERAL (
         SELECT COUNT(*)::int AS total
         FROM reports r
         WHERE r.reported_id=v.user_id AND r.status IN ('pending','open','reviewing')
       ) open_reports ON TRUE
       LEFT JOIN LATERAL (
         SELECT array_agg(DISTINCT av.type) AS types
         FROM verifications av
         WHERE av.user_id=v.user_id AND av.status IN ('approved','verified')
       ) approved_types ON TRUE
       WHERE v.status='pending'
       ORDER BY v.created_at ASC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Verification queue is unavailable. Showing an empty queue.');
  }
};

exports.approveVerification = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query(
      "UPDATE verifications SET status='verified', reviewer_email=$2, review_note=$3, reviewed_at=NOW() WHERE verification_id=$1 RETURNING *",
      [req.params.id, req.admin?.email || 'admin', req.body?.note || null]
    );
    if (!result.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Verification request not found.' } });
    }
    await client.query("UPDATE users SET is_verified=true, updated_at=NOW() WHERE user_id=$1", [result.rows[0].user_id]);
    await client.query("UPDATE profiles SET verification_status='verified', updated_at=NOW() WHERE user_id=$1", [result.rows[0].user_id]);
    await auditLog(client, req, 'verification.approve', 'verification', req.params.id, { type: result.rows[0].type });
    await client.query('COMMIT');
    await notifyMember(
      db,
      result.rows[0].user_id,
      'Profile verified',
      'Your SoulMatch profile verification is approved. The verified badge is now active.',
      {
        type: 'profile_verification',
        status: 'verified',
        verificationId: result.rows[0].verification_id
      }
    );
    broadcastAdminEvent('admin:verification_updated', { action: 'approve', verification: result.rows[0] });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to approve this verification request right now.');
  } finally {
    client.release();
  }
};

exports.rejectVerification = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    await client.query('BEGIN');
    const note = String(req.body?.note || '').trim();
    const result = await client.query(
      "UPDATE verifications SET status='rejected', reviewer_email=$2, review_note=$3, reviewed_at=NOW() WHERE verification_id=$1 RETURNING *",
      [req.params.id, req.admin?.email || 'admin', note || null]
    );
    if (!result.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Verification request not found.' } });
    }
    await client.query("UPDATE users SET is_verified=false, updated_at=NOW() WHERE user_id=$1", [result.rows[0].user_id]);
    await client.query("UPDATE profiles SET verification_status='rejected', updated_at=NOW() WHERE user_id=$1", [result.rows[0].user_id]);
    await auditLog(client, req, 'verification.reject', 'verification', req.params.id, { type: result.rows[0].type, note: note || null });
    await client.query('COMMIT');
    await notifyMember(
      db,
      result.rows[0].user_id,
      'Verification needs attention',
      note || 'Your SoulMatch profile verification was declined. You can update your profile and request review again.',
      {
        type: 'profile_verification',
        status: 'rejected',
        verificationId: result.rows[0].verification_id
      }
    );
    broadcastAdminEvent('admin:verification_updated', { action: 'reject', verification: result.rows[0] });
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to reject this verification request right now.');
  } finally {
    client.release();
  }
};

exports.getPendingProfileDocuments = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         pd.*,
         p.first_name,
         p.last_name,
         p.review_status,
         p.primary_photo_url,
         a.full_name AS agent_name,
         a.business_name AS agent_business_name,
         a.agent_code
       FROM profile_documents pd
       JOIN profiles p ON p.profile_id = pd.profile_id
       LEFT JOIN advisors a ON a.advisor_id = pd.advisor_id
       WHERE pd.status IN ('uploaded', 'under_review')
       ORDER BY pd.created_at ASC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Profile document queue is unavailable.');
  }
};

exports.approveProfileDocument = async (req, res) => {
  let client;
  try {
    const db = await getDB();
    client = await db.connect();
    const note = String(req.body?.note || '').trim() || null;
    await client.query('BEGIN');
    const result = await client.query(
      `UPDATE profile_documents
       SET status = 'verified',
           review_comment = $2,
           reviewed_at = NOW(),
           reviewed_by = $3,
           updated_at = NOW()
       WHERE profile_document_id = $1
       RETURNING *`,
      [req.params.id, note, req.admin?.email || 'admin']
    );
    const document = result.rows[0];
    if (!document) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile document not found.' } });
    }
    const publishResult = await publishManagedProfileIfReady(client, document.profile_id, note);
    await auditLog(client, req, 'profile_document.approve', 'profile_document', req.params.id, {
      note,
      managedProfilePublished: publishResult.published === true,
      missingRequiredDocuments: publishResult.missing || []
    });
    await client.query('COMMIT');
    if (publishResult.published) {
      broadcastAdminEvent('admin:profile_status', { action: 'auto_publish', profile: publishResult.profile });
    }
    res.json({ success: true, data: document, profileReview: publishResult });
  } catch (err) {
    if (client) await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to approve this profile document right now.');
  } finally {
    if (client) client.release();
  }
};

exports.rejectProfileDocument = async (req, res) => {
  try {
    const db = await getDB();
    const note = String(req.body?.note || '').trim() || 'Document needs a clearer upload or a valid supporting file.';
    const result = await db.query(
      `UPDATE profile_documents
       SET status = 'rejected',
           review_comment = $2,
           reviewed_at = NOW(),
           reviewed_by = $3,
           updated_at = NOW()
       WHERE profile_document_id = $1
       RETURNING *`,
      [req.params.id, note, req.admin?.email || 'admin']
    );
    const document = result.rows[0];
    if (!document) return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile document not found.' } });
    const profileResult = await db.query(
      `UPDATE profiles
       SET review_status = 'rejected',
           rejection_reason = $2,
           reviewed_at = NOW(),
           updated_at = NOW()
       WHERE profile_id = $1
       RETURNING user_id`,
      [document.profile_id, note]
    );
    await auditLog(db, req, 'profile_document.reject', 'profile_document', req.params.id, { note });
    const profileUserId = profileResult.rows[0]?.user_id;
    if (profileUserId) {
      await notifyMember(
        db,
        profileUserId,
        'Profile document rejected',
        note,
        { type: 'profile_document_review', status: 'rejected', profileDocumentId: req.params.id, profileId: document.profile_id }
      ).catch((error) => logger.warn(`Profile document rejection notification skipped: ${error.message}`));
    }
    res.json({ success: true, data: document });
  } catch (err) {
    respondServerError(res, err, 'Unable to reject this profile document right now.');
  }
};

exports.getReports = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT r.*, reporter.phone AS reporter_phone, reported.phone AS reported_phone
       FROM reports r
       JOIN users reporter ON r.reporter_id = reporter.user_id
       JOIN users reported ON r.reported_id = reported.user_id
       WHERE r.status='pending'
       ORDER BY r.created_at ASC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.resolveReport = async (req, res) => {
  try {
    const db = await getDB();
    await db.query("UPDATE reports SET status='resolved' WHERE report_id=$1", [req.params.id]);
    await auditLog(db, req, 'report.resolve', 'report', req.params.id, {});
    broadcastAdminEvent('admin:report_resolved', { reportId: req.params.id });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.getChatLogs = async (req, res) => {
  try {
    const db = await getDB();
    const limit = Math.min(parseInt(req.query.limit, 10) || 50, 200);
    const result = await db.query(
      `SELECT event_id AS log_id, user_id, payload, created_at
       FROM analytics_events
       WHERE event_type IN ('chat_message','chat_report','bot_message','call_request')
       ORDER BY created_at DESC
       LIMIT $1`,
      [limit]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    res.json({ success: true, data: [], meta: { degraded: true, message: 'Chat log store is unavailable.' } });
  }
};

exports.getPayments = async (req, res) => {
  try {
    const db = await getDB();
    const [transactions, plans, coupons] = await Promise.all([
      db.query(
        `SELECT
           t.transaction_id,
           t.user_id,
           u.phone,
           p.first_name,
           p.last_name,
           s.plan_id,
           t.amount,
           t.currency,
           t.status,
           t.razorpay_order_id,
           t.razorpay_payment_id,
           t.created_at
         FROM transactions t
         LEFT JOIN users u ON u.user_id=t.user_id
         LEFT JOIN profiles p ON p.user_id=t.user_id
         LEFT JOIN subscriptions s ON s.subscription_id=t.subscription_id
         ORDER BY t.created_at DESC
         LIMIT 100`
      ),
      db.query('SELECT plan_id, COUNT(*) AS active_users, COALESCE(SUM(amount_paid),0) AS revenue FROM subscriptions WHERE is_active=true GROUP BY plan_id ORDER BY revenue DESC'),
      db.query('SELECT * FROM referral_codes ORDER BY created_at DESC LIMIT 50')
    ]);
    res.json({ success: true, data: { transactions: transactions.rows, plans: plans.rows, coupons: coupons.rows } });
  } catch (err) {
    respondDegraded(res, err, { transactions: [], plans: [], coupons: [] }, 'Payment store is unavailable. Showing empty payment data.');
  }
};

exports.createRefund = async (req, res) => {
  try {
    const db = await getDB();
    await auditLog(db, req, 'payment.refund_requested', 'transaction', req.body?.transactionId || null, req.body || {});
    broadcastAdminEvent('admin:payment_refund_requested', { transactionId: req.body?.transactionId, amount: req.body?.amount });
    res.json({ success: true, data: { status: 'queued', message: 'Refund request queued for payment gateway processing.' } });
  } catch (err) {
    respondServerError(res, err, 'Unable to queue refund right now.');
  }
};

exports.getAlerts = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query('SELECT * FROM admin_alerts ORDER BY created_at DESC LIMIT 100');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    res.json({
      success: true,
      data: [
        { alert_id: 'fallback-1', severity: 'medium', title: 'Monitoring store unavailable', body: 'Admin alerts table could not be reached.', status: 'open', created_at: new Date().toISOString() }
      ]
    });
  }
};

exports.createSystemAlert = async (req, res) => {
  try {
    const db = await getDB();
    const severity = ['critical', 'high', 'medium', 'low'].includes(String(req.body?.severity || '').toLowerCase())
      ? String(req.body.severity).toLowerCase()
      : 'medium';
    const title = String(req.body?.title || 'Production monitor alert').trim().slice(0, 180);
    const body = String(req.body?.body || '').trim().slice(0, 4000);
    const source = String(req.body?.source || 'production-monitor').trim().slice(0, 80);
    const metadata = req.body?.metadata && typeof req.body.metadata === 'object' ? req.body.metadata : {};
    const result = await db.query(
      `INSERT INTO admin_alerts (alert_id, severity, title, body, source, status, metadata, created_at)
       VALUES ($1,$2,$3,$4,$5,'open',$6::jsonb,NOW())
       RETURNING *`,
      [crypto.randomUUID(), severity, title || 'Production monitor alert', body, source, JSON.stringify(metadata)]
    );
    broadcastAdminEvent('admin:alert_created', { alert: result.rows[0] });
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to create system alert right now.');
  }
};

exports.ackAlert = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      "UPDATE admin_alerts SET status='acknowledged', acknowledged_by=$2, acknowledged_at=NOW() WHERE alert_id=$1 RETURNING *",
      [req.params.id, req.admin?.email || 'admin']
    );
    await auditLog(db, req, 'alert.ack', 'admin_alert', req.params.id, {});
    res.json({ success: true, data: result.rows[0] || null });
  } catch (err) {
    respondServerError(res, err, 'Unable to acknowledge alert right now.');
  }
};

exports.getConsentEvents = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         ce.consent_event_id,
         ce.user_id,
         ce.profile_id,
         ce.consent_type,
         ce.status,
         ce.purpose,
         ce.notice_version,
         ce.metadata,
         ce.source,
         ce.ip_address,
         ce.created_at,
         p.first_name,
         p.last_name,
         u.phone,
         u.email
       FROM consent_events ce
       LEFT JOIN profiles p ON p.profile_id = ce.profile_id
       LEFT JOIN users u ON u.user_id = ce.user_id
       ORDER BY ce.created_at DESC
       LIMIT 200`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondServerError(res, err, 'Unable to load consent events right now.');
  }
};

exports.createCampaign = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `INSERT INTO admin_campaigns (name, channel, audience, template_key, scheduled_at, status, created_by)
       VALUES ($1,$2,$3::jsonb,$4,$5,'draft',$6)
       RETURNING *`,
      [
        req.body?.name || 'Untitled campaign',
        req.body?.channel || 'push',
        JSON.stringify(req.body?.audience || {}),
        req.body?.templateKey || null,
        req.body?.scheduledAt || null,
        req.admin?.email || 'admin'
      ]
    );
    await auditLog(db, req, 'campaign.create', 'campaign', result.rows[0].campaign_id, req.body || {});
    broadcastAdminEvent('admin:campaign_created', { campaign: result.rows[0] });
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to create campaign right now.');
  }
};

exports.getAuditLogs = async (req, res) => {
  try {
    const db = await getDB();
    const limit = Math.min(parseInt(req.query.limit, 10) || 100, 500);
    const result = await db.query('SELECT * FROM admin_audit_logs ORDER BY created_at DESC LIMIT $1', [limit]);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    res.json({ success: true, data: [], meta: { degraded: true, message: 'Audit log table is unavailable.' } });
  }
};

exports.getRoles = async (req, res) => {
  res.json({
    success: true,
    data: [
      { role: 'super_admin', label: 'Super Admin', permissions: ROLE_PERMISSIONS.super_admin },
      { role: 'moderator', label: 'Moderator', permissions: ROLE_PERMISSIONS.moderator },
      { role: 'support_agent', label: 'Support Agent', permissions: ROLE_PERMISSIONS.support_agent },
      { role: 'marketing_manager', label: 'Marketing Manager', permissions: ROLE_PERMISSIONS.marketing_manager }
    ],
    current: req.admin?.role || 'admin'
  });
};

exports.getPendingStories = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query('SELECT * FROM success_stories WHERE is_approved=false ORDER BY created_at ASC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.approveStory = async (req, res) => {
  try {
    const db = await getDB();
    await db.query('UPDATE success_stories SET is_approved=true WHERE story_id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: { message: err.message } });
  }
};

exports.getConfig = async (req, res) => {
  try {
    const db = await getDB();
    const config = await getConfigMap(db, true);
    res.json({ success: true, data: { config, runtime: getPublicRuntimeConfig(config) } });
  } catch (err) {
    logger.error(err.stack || err.message);
    res.json({
      success: true,
      data: {
        config: DEFAULT_CONFIG,
        runtime: getPublicRuntimeConfig(DEFAULT_CONFIG),
        degraded: true,
        message: 'Config database is unavailable. Showing default runtime configuration.'
      }
    });
  }
};

exports.updateConfig = async (req, res) => {
  try {
    const db = await getDB();
    const key = req.params.key;
    if (!CONFIG_KEYS.includes(key)) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: `Unknown config key: ${key}` } });
    }
    if (!req.body || typeof req.body !== 'object' || Array.isArray(req.body)) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Config payload must be an object.' } });
    }
    const updated = await upsertConfigSection(db, key, req.body, req.admin?.email || 'admin');
    await auditLog(db, req, 'config.update', 'app_config', key, { key });
    broadcastAdminEvent('admin:config_updated', { key, config: updated });
    res.json({ success: true, data: updated });
  } catch (err) {
    respondServerError(res, err, 'Unable to update configuration right now.');
  }
};

exports.getLandingPages = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query('SELECT * FROM landing_pages ORDER BY updated_at DESC, created_at DESC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondServerError(res, err, 'Unable to load landing pages right now.');
  }
};

exports.upsertLandingPage = async (req, res) => {
  try {
    const db = await getDB();
    const slug = req.params.slug || req.body.slug;
    if (!slug) return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'slug is required.' } });
    const payload = {
      title: req.body.title,
      subtitle: req.body.subtitle || null,
      description: req.body.description,
      heroImageUrl: req.body.heroImageUrl || null,
      previewImageUrl: req.body.previewImageUrl || null,
      ctaLabel: req.body.ctaLabel || 'Open app',
      ctaUrl: req.body.ctaUrl || null,
      seoTitle: req.body.seoTitle || null,
      seoDescription: req.body.seoDescription || null,
      isActive: req.body.isActive !== false
    };
    if (!payload.title || !payload.description) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'title and description are required.' } });
    }
    const result = await db.query(
      `INSERT INTO landing_pages (
         slug, title, subtitle, description, hero_image_url, preview_image_url,
         cta_label, cta_url, seo_title, seo_description, is_active, updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW())
       ON CONFLICT (slug)
       DO UPDATE SET
         title = EXCLUDED.title,
         subtitle = EXCLUDED.subtitle,
         description = EXCLUDED.description,
         hero_image_url = EXCLUDED.hero_image_url,
         preview_image_url = EXCLUDED.preview_image_url,
         cta_label = EXCLUDED.cta_label,
         cta_url = EXCLUDED.cta_url,
         seo_title = EXCLUDED.seo_title,
         seo_description = EXCLUDED.seo_description,
         is_active = EXCLUDED.is_active,
         updated_at = NOW()
       RETURNING *`,
      [
        slug,
        payload.title,
        payload.subtitle,
        payload.description,
        payload.heroImageUrl,
        payload.previewImageUrl,
        payload.ctaLabel,
        payload.ctaUrl,
        payload.seoTitle,
        payload.seoDescription,
        payload.isActive
      ]
    );
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to save the landing page right now.');
  }
};

exports.getReferralCodes = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         rc.*,
         COUNT(rr.redemption_id) AS redemptions,
         owner.phone AS owner_phone
       FROM referral_codes rc
       LEFT JOIN referral_redemptions rr ON rr.referral_code_id = rc.referral_code_id
       LEFT JOIN users owner ON owner.user_id = rc.owner_user_id
       GROUP BY rc.referral_code_id, owner.phone
       ORDER BY rc.created_at DESC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondServerError(res, err, 'Unable to load referral codes right now.');
  }
};

exports.createReferralCode = async (req, res) => {
  try {
    const db = await getDB();
    const code = (req.body.code || makeReferralCode()).toUpperCase();
    const result = await db.query(
      `INSERT INTO referral_codes (
         code, owner_user_id, campaign_name, channel, reward_points, max_redemptions, is_active, expires_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
       RETURNING *`,
      [
        code,
        req.body.ownerUserId || null,
        req.body.campaignName || null,
        req.body.channel || null,
        parseNumber(req.body.rewardPoints, 0),
        parseNumber(req.body.maxRedemptions, 0),
        req.body.isActive !== false,
        req.body.expiresAt || null
      ]
    );
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    if (err.code === '23505') {
      return res.status(409).json({ success: false, error: { code: 'CONFLICT', message: 'Referral code already exists.' } });
    }
    respondServerError(res, err, 'Unable to create the referral code right now.');
  }
};

exports.getAnalyticsFunnel = async (req, res) => {
  try {
    const db = await getDB();
    const lookbackDays = Math.min(parseInt(req.query.days, 10) || 30, 365);
    const result = await db.query(
      `SELECT
         event_type,
         COUNT(*) AS total
       FROM analytics_events
       WHERE created_at >= NOW() - ($1 * INTERVAL '1 day')
       GROUP BY event_type
       ORDER BY total DESC`,
      [lookbackDays]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Analytics funnel is unavailable. Showing empty analytics data.');
  }
};

exports.getAnalyticsEvents = async (req, res) => {
  try {
    const db = await getDB();
    const limit = Math.min(parseInt(req.query.limit, 10) || 50, 250);
    const result = await db.query(
      `SELECT event_id, event_type, service_name, user_id, session_id, payload, created_at
       FROM analytics_events
       ORDER BY created_at DESC
       LIMIT $1`,
      [limit]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Analytics events are unavailable. Showing an empty event stream.');
  }
};

exports.getServiceHealth = async (req, res) => {
  try {
    const data = await getServiceHealth();
    res.json({ success: true, data });
  } catch (err) {
    respondDegraded(res, err, [], 'Service health checks are unavailable.');
  }
};
