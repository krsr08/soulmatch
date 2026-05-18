const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const fs = require('fs');
const jwt = require('jsonwebtoken');
const path = require('path');
const { getDB } = require('../config/database');
const logger = require('../utils/logger');
const { CONFIG_KEYS, DEFAULT_CONFIG, getConfigMap, getConfigSection, getPublicRuntimeConfig, resolveConfigKey, upsertConfigSection } = require('../../../shared/controlPlane');
const { getServiceHealth } = require('../services/serviceHealth');
const { broadcastAdminEvent, getRealtimeSnapshot } = require('../realtime/adminRealtime');

function getAdminSecret() {
  return process.env.ADMIN_JWT_SECRET || process.env.JWT_SECRET;
}

function getAdminEmail() {
  return process.env.ADMIN_EMAIL || 'admin@soulmatch.app';
}

const LOGIN_FAILURES = new Map();
const ADMIN_LOCKOUT_LIMIT = Number(process.env.ADMIN_LOCKOUT_LIMIT || 5);
const ADMIN_LOCKOUT_WINDOW_MS = Number(process.env.ADMIN_LOCKOUT_WINDOW_MS || 15 * 60 * 1000);

function loginFailureKey(email, ip) {
  return `${String(email || '').trim().toLowerCase()}|${String(ip || '')}`;
}

function loginFailureState(email, ip) {
  const key = loginFailureKey(email, ip);
  const now = Date.now();
  const bucket = (LOGIN_FAILURES.get(key) || []).filter((stamp) => now - stamp < ADMIN_LOCKOUT_WINDOW_MS);
  LOGIN_FAILURES.set(key, bucket);
  return { key, bucket };
}

function isAdminLoginLocked(email, ip) {
  return loginFailureState(email, ip).bucket.length >= ADMIN_LOCKOUT_LIMIT;
}

function recordAdminLoginFailure(email, ip) {
  const state = loginFailureState(email, ip);
  state.bucket.push(Date.now());
  LOGIN_FAILURES.set(state.key, state.bucket);
}

function clearAdminLoginFailures(email, ip) {
  LOGIN_FAILURES.delete(loginFailureKey(email, ip));
}

function adminJwtOptions() {
  return {
    expiresIn: process.env.ADMIN_SESSION_TTL || '8h',
    issuer: process.env.ADMIN_JWT_ISSUER || 'soulmatch-admin',
    audience: process.env.ADMIN_JWT_AUDIENCE || 'soulmatch-admin-api',
    jwtid: crypto.randomUUID()
  };
}

function setAdminSessionCookie(res, token) {
  const csrfToken = crypto.randomBytes(24).toString('hex');
  res.cookie('soulmatch_admin_session', token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict',
    maxAge: 8 * 60 * 60 * 1000
  });
  res.cookie('soulmatch_admin_csrf', csrfToken, {
    httpOnly: false,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict',
    maxAge: 8 * 60 * 60 * 1000
  });
  return csrfToken;
}

function clearAdminSessionCookie(res) {
  res.clearCookie('soulmatch_admin_session', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict'
  });
  res.clearCookie('soulmatch_admin_csrf', {
    httpOnly: false,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict'
  });
}

function decodeBase32(secret) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  let bits = '';
  String(secret || '').replace(/=+$/g, '').toUpperCase().split('').forEach((char) => {
    const value = alphabet.indexOf(char);
    if (value >= 0) bits += value.toString(2).padStart(5, '0');
  });
  const bytes = [];
  for (let i = 0; i + 8 <= bits.length; i += 8) {
    bytes.push(parseInt(bits.slice(i, i + 8), 2));
  }
  return Buffer.from(bytes);
}

function generateTotp(secret, timeStep) {
  const counter = Buffer.alloc(8);
  counter.writeUInt32BE(0, 0);
  counter.writeUInt32BE(timeStep, 4);
  const hmac = crypto.createHmac('sha1', decodeBase32(secret)).update(counter).digest();
  const offset = hmac[hmac.length - 1] & 0x0f;
  const code = ((hmac.readUInt32BE(offset) & 0x7fffffff) % 1000000).toString().padStart(6, '0');
  return code;
}

function verifyTotpCode(secret, code) {
  const normalized = String(code || '').replace(/\s+/g, '');
  if (!/^\d{6}$/.test(normalized)) return false;
  const step = Math.floor(Date.now() / 30000);
  return [-1, 0, 1].some((drift) => generateTotp(secret, step + drift) === normalized);
}

function maskValue(value) {
  const raw = String(value || '').trim();
  if (!raw) return null;
  if (raw.length <= 8) return 'configured';
  return `${raw.slice(0, 4)}...${raw.slice(-4)}`;
}

function configuredSecret(name, purpose, service, source = 'environment variable', howGenerated = 'Created in the provider console and stored outside source code.') {
  const value = process.env[name];
  return {
    name,
    purpose,
    service,
    source,
    configured: Boolean(value),
    valuePreview: value ? maskValue(value) : null,
    recoveryProcedure: 'Restore this value from the encrypted secrets backup or provider secret store, then restart the affected service.',
    rotationProcedure: 'Generate a replacement in the owning provider, update the secret store, deploy, and revoke the old value.',
    howGenerated
  };
}

function buildSystemInventory(config = {}) {
  return [
    {
      section: 'GitHub Actions',
      description: 'Repository automation and production deployment secrets.',
      items: [
        configuredSecret('AZURE_VM_HOST', 'Production VM host used by GitHub Actions deploy workflow.', 'GitHub Actions'),
        configuredSecret('AZURE_VM_USER', 'SSH username used by deployment workflow.', 'GitHub Actions'),
        configuredSecret('AZURE_VM_SSH_KEY', 'Private SSH key used by deployment workflow.', 'GitHub Actions', 'GitHub Actions secret')
      ]
    },
    {
      section: 'Azure / Production VM',
      description: 'Runtime infrastructure used by SoulMatch services.',
      items: [
        configuredSecret('DATABASE_URL', 'PostgreSQL connection used by backend services.', 'Backend services'),
        configuredSecret('PUBLIC_API_BASE_URL', 'Public API base URL used by clients.', 'Mobile/Admin clients'),
        configuredSecret('ADMIN_API_BASE_URL', 'Admin console API base URL.', 'Admin web')
      ]
    },
    {
      section: 'Firebase / Google Console',
      description: 'Push, Google Sign-In, and service account configuration.',
      items: [
        configuredSecret('FIREBASE_PROJECT_ID', 'Firebase project identifier.', 'Notification service', 'environment variable', 'Generated from Firebase project settings.'),
        configuredSecret('FIREBASE_CLIENT_EMAIL', 'Firebase service account email.', 'Notification service', 'environment variable', 'Generated from Firebase service account JSON.'),
        configuredSecret('FIREBASE_PRIVATE_KEY', 'Firebase service account private key.', 'Notification service', 'environment variable', 'Generated from Firebase service account JSON.'),
        configuredSecret('GOOGLE_WEB_CLIENT_ID', 'OAuth web client used for Google Sign-In.', 'Auth service / Android app', 'Google Cloud OAuth credentials')
      ]
    },
    {
      section: 'Twilio / Mobile OTP',
      description: 'SMS OTP delivery. Mock OTP may be enabled only for local development.',
      items: [
        configuredSecret('OTP_MOCK_ENABLED', 'Controls mock OTP mode for local/test environments.', 'Auth service'),
        configuredSecret('TWILIO_ACCOUNT_SID', 'Twilio account identifier for SMS.', 'Auth service', 'Twilio console'),
        configuredSecret('TWILIO_AUTH_TOKEN', 'Twilio auth token for SMS.', 'Auth service', 'Twilio console'),
        configuredSecret('TWILIO_VERIFY_SERVICE_SID', 'Twilio Verify service identifier.', 'Auth service', 'Twilio console')
      ]
    },
    {
      section: 'Razorpay / Payments',
      description: 'Payment gateway credentials and webhook security.',
      items: [
        configuredSecret('RAZORPAY_KEY_ID', 'Razorpay public key id.', 'Payment service', 'Razorpay dashboard'),
        configuredSecret('RAZORPAY_KEY_SECRET', 'Razorpay API secret.', 'Payment service', 'Razorpay dashboard'),
        configuredSecret('RAZORPAY_WEBHOOK_SECRET', 'Webhook signature validation secret.', 'Payment service', 'Razorpay dashboard')
      ]
    },
    {
      section: 'Admin / Security',
      description: 'Admin console login and service-to-service security.',
      items: [
        configuredSecret('ADMIN_EMAIL', 'Fallback super-admin email login.', 'Admin service'),
        configuredSecret('ADMIN_PASSWORD_HASH', 'Fallback bcrypt password hash for super-admin login.', 'Admin service'),
        configuredSecret('ADMIN_JWT_SECRET', 'JWT signing secret for admin sessions.', 'Admin service'),
        configuredSecret('JWT_SECRET', 'JWT signing secret for member and agent sessions.', 'Auth service'),
        configuredSecret('INTERNAL_SERVICE_SECRET', 'Internal API shared secret between backend services.', 'Backend services')
      ]
    },
    {
      section: 'Runtime Configuration',
      description: 'Public app settings stored in app_config and editable from CMS/Dynamic Configuration.',
      items: Object.keys(config || {}).map((key) => ({
        name: key,
        purpose: `${key.replaceAll('_', ' ')} runtime configuration`,
        service: 'Control plane',
        source: 'app_config',
        configured: config[key] !== undefined,
        valuePreview: 'stored as JSON',
        recoveryProcedure: 'Restore app_config from database backup or exported configuration JSON.',
        rotationProcedure: 'Update the section from Dynamic Configuration and audit the change.',
        howGenerated: 'Managed by the admin console control plane.'
      }))
    }
  ];
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

function parseBool(value, fallback = false) {
  if (value === true || value === 'true' || value === 1 || value === '1') return true;
  if (value === false || value === 'false' || value === 0 || value === '0') return false;
  return fallback;
}

function arrayInput(value) {
  if (Array.isArray(value)) return value.map((item) => String(item || '').trim()).filter(Boolean);
  if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean);
  return [];
}

function normalizePhotoUrl(value) {
  const raw = String(value || '').trim();
  if (!raw) return null;
  if (/^https?:\/\/\S+$/i.test(raw) || raw.startsWith('/uploads/')) return raw;
  return false;
}

function saveAdminPhotoUpload(body, profileId, photoId) {
  const dataUrl = String(body?.photoDataUrl || body?.photo_data_url || '').trim();
  if (!dataUrl) return null;
  const match = dataUrl.match(/^data:(image\/(?:jpeg|jpg|png|webp));base64,([A-Za-z0-9+/=]+)$/);
  if (!match) return false;
  const mime = match[1].toLowerCase();
  const extension = mime.includes('png') ? 'png' : mime.includes('webp') ? 'webp' : 'jpg';
  const bytes = Buffer.from(match[2], 'base64');
  if (!bytes.length || bytes.length > 5 * 1024 * 1024) return false;
  const uploadRoot = process.env.ADMIN_UPLOAD_DIR || process.env.LOCAL_UPLOAD_DIR || path.join(__dirname, '..', 'uploads');
  const relativePath = path.join('admin', String(profileId), `${photoId}.${extension}`);
  const absolutePath = path.join(uploadRoot, relativePath);
  fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
  fs.writeFileSync(absolutePath, bytes);
  return {
    photoUrl: `/uploads/${relativePath.replace(/\\/g, '/')}`,
    s3Key: `admin-upload/${relativePath.replace(/\\/g, '/')}`
  };
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

function trustLevelForScore(score) {
  if (score >= 80) return 'high';
  if (score >= 55) return 'medium';
  return 'low';
}

async function recalculateTrustScore(db, profileIdOrUserId, mode = 'profile') {
  const result = await db.query(
    `SELECT
       p.profile_id,
       LEAST(
         100,
         (CASE WHEN u.is_verified THEN 12 ELSE 0 END) +
         (CASE WHEN u.email IS NOT NULL AND u.email <> '' THEN 5 ELSE 0 END) +
         (CASE WHEN u.google_id IS NOT NULL THEN 8 ELSE 0 END) +
         (CASE WHEN COALESCE(p.completion_score,0) >= 90 THEN 15 WHEN COALESCE(p.completion_score,0) >= 70 THEN 12 WHEN COALESCE(p.completion_score,0) >= 50 THEN 8 ELSE 0 END) +
         (CASE WHEN COALESCE(p.verification_status,'pending')='verified' THEN 15 WHEN COALESCE(p.verification_status,'pending')='pending' THEN 5 ELSE 0 END) +
         (CASE WHEN COALESCE(approved_docs.total,0) > 0 THEN 8 ELSE 0 END) +
         (CASE WHEN COALESCE(approved_types.has_education,false) THEN 8 ELSE 0 END) +
         (CASE WHEN COALESCE(approved_types.has_income,false) THEN 8 ELSE 0 END) +
         (CASE WHEN COALESCE(approved_types.has_family,false) THEN 8 WHEN fd.family_city IS NOT NULL OR fd.family_pincode IS NOT NULL THEN 4 ELSE 0 END) +
         (CASE WHEN COALESCE(approved_photos.total,0) >= 3 THEN 10 WHEN COALESCE(approved_photos.total,0) >= 1 THEN 7 ELSE 0 END) +
         (CASE WHEN COALESCE(p.profile_status,'active')='active' THEN 5 ELSE -8 END) +
         (CASE WHEN u.last_login >= NOW() - INTERVAL '30 days' THEN 6 ELSE 0 END) +
         (CASE WHEN COALESCE(open_reports.total,0)=0 THEN 10 ELSE -LEAST(35, COALESCE(open_reports.total,0) * 12) END)
       )::int AS trust_score
     FROM profiles p
     JOIN users u ON u.user_id=p.user_id
     LEFT JOIN family_details fd ON fd.profile_id=p.profile_id
     LEFT JOIN LATERAL (
       SELECT COUNT(*)::int AS total
       FROM profile_photos pp
       WHERE pp.profile_id=p.profile_id AND pp.is_approved=true
     ) approved_photos ON TRUE
     LEFT JOIN LATERAL (
       SELECT COUNT(*)::int AS total
       FROM verifications v
       WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')
     ) approved_docs ON TRUE
     LEFT JOIN LATERAL (
       SELECT
         BOOL_OR(v.type='education') AS has_education,
         BOOL_OR(v.type='income') AS has_income,
         BOOL_OR(v.type='family') AS has_family
       FROM verifications v
       WHERE v.user_id=p.user_id AND v.status IN ('approved','verified')
     ) approved_types ON TRUE
     LEFT JOIN LATERAL (
       SELECT COUNT(*)::int AS total
       FROM reports r
       WHERE r.reported_id=p.user_id AND r.status IN ('pending','open','reviewing')
     ) open_reports ON TRUE
     WHERE ${mode === 'user' ? 'p.user_id=$1' : 'p.profile_id=$1'}
     LIMIT 1`,
    [profileIdOrUserId]
  );
  const row = result.rows[0];
  if (!row) return null;
  const score = Math.max(0, Math.min(100, Number(row.trust_score) || 0));
  const level = trustLevelForScore(score);
  await db.query(
    'UPDATE profiles SET trust_score=$1, trust_level=$2, updated_at=NOW() WHERE profile_id=$3',
    [score, level, row.profile_id]
  );
  return { profileId: row.profile_id, trustScore: score, trustLevel: level };
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
    const { email, password, totpCode } = req.body;
    const normalizedEmail = String(email || '').trim().toLowerCase();
    if (isAdminLoginLocked(normalizedEmail, req.ip)) {
      return res.status(429).json({
        success: false,
        error: {
          code: 'ADMIN_LOGIN_LOCKED',
          message: 'Too many failed admin sign-in attempts. Please wait and try again.'
        }
      });
    }
    const failLogin = async (db, reason = 'invalid_credentials') => {
      recordAdminLoginFailure(normalizedEmail, req.ip);
      if (db) {
        await auditLog(db, { ...req, admin: { email: normalizedEmail, role: 'unknown' } }, 'admin.login_failed', 'admin_session', null, { reason });
      }
      return res.status(401).json({ success: false, error: { code: 'INVALID_CREDENTIALS', message: 'Invalid admin email or password.' } });
    };
    const db = await getDB().catch((error) => {
      logger.warn(`Admin login database lookup skipped: ${error.message}`);
      return null;
    });
    let role = getAdminRole();
    let permissions = ROLE_PERMISSIONS[role] || ROLE_PERMISSIONS.admin;
    let adminUser = null;

    if (db) {
      const userResult = await db.query(
        `SELECT admin_user_id, email, password_hash, display_name, role, status
         FROM admin_console_users
         WHERE LOWER(email) = LOWER($1) AND status <> 'deleted'
         LIMIT 1`,
        [normalizedEmail]
      );
      adminUser = userResult.rows[0] || null;
      if (adminUser) {
        if (adminUser.status !== 'active') {
          return res.status(403).json({ success: false, error: { code: 'ADMIN_ACCOUNT_SUSPENDED', message: 'This admin account is not active.' } });
        }
        const validDbPassword = await bcrypt.compare(String(password || ''), adminUser.password_hash);
        if (!validDbPassword) {
          return failLogin(db, 'password_mismatch');
        }
        role = adminUser.role || 'admin';
        await db.query('UPDATE admin_console_users SET last_login=NOW(), updated_at=NOW() WHERE admin_user_id=$1', [adminUser.admin_user_id]);
      }
    }

    if (!adminUser) {
      if (normalizedEmail !== getAdminEmail().toLowerCase()) {
        return failLogin(db, 'unknown_email');
      }
      const valid = await isValidAdminPassword(String(password || ''));
      if (!valid) return failLogin(db, 'fallback_password_mismatch');
    }

    if (process.env.ADMIN_MFA_TOTP_SECRET && !verifyTotpCode(process.env.ADMIN_MFA_TOTP_SECRET, totpCode)) {
      return failLogin(db, 'mfa_failed');
    }

    try {
      const configDb = db || await getDB();
      const roleRow = await configDb.query(
        "SELECT permissions FROM admin_roles WHERE role=$1 AND status='active' LIMIT 1",
        [role]
      ).catch(() => ({ rows: [] }));
      if (Array.isArray(roleRow.rows[0]?.permissions)) {
        permissions = roleRow.rows[0].permissions;
      } else {
        const configured = await getConfigSection(configDb, 'admin_roles');
        const match = (configured.roles || []).find((item) => item.role === role);
        if (match && Array.isArray(match.permissions)) permissions = match.permissions;
      }
    } catch (error) {
      logger.warn(`Admin role permissions fallback used: ${error.message}`);
    }
    const token = jwt.sign(
      {
        role,
        roles: [role],
        permissions,
        email: normalizedEmail,
        adminUserId: adminUser?.admin_user_id || null
      },
      getAdminSecret(),
      adminJwtOptions()
    );
    clearAdminLoginFailures(normalizedEmail, req.ip);
    const csrfToken = setAdminSessionCookie(res, token);
    if (db) {
      await auditLog(db, { ...req, admin: { email: normalizedEmail, role } }, 'admin.login_success', 'admin_session', adminUser?.admin_user_id || normalizedEmail, {});
    }
    res.json({ success: true, data: { token, csrfToken, role, permissions, displayName: adminUser?.display_name || null } });
  } catch (err) {
    respondServerError(res, err, 'Unable to complete admin sign-in right now.');
  }
};

exports.adminLogout = async (req, res) => {
  try {
    clearAdminSessionCookie(res);
    const db = await getDB().catch(() => null);
    if (db) {
      await auditLog(db, req, 'admin.logout', 'admin_session', req.admin?.adminUserId || req.admin?.email || null, {});
    }
    res.json({ success: true });
  } catch (err) {
    respondServerError(res, err, 'Unable to complete admin logout right now.');
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
    const [
      memberBreakdown,
      agentBreakdown,
      revenueTrend,
      pendingQueues,
      recentMembers,
      membershipBreakdown,
      agentLeaderboard,
      recentAgents,
      recentAudit
    ] = await Promise.all([
      db.query(
        `SELECT
           COUNT(*)::int AS total,
           COUNT(*) FILTER (WHERE LOWER(COALESCE(p.gender,'')) IN ('male','groom'))::int AS grooms,
           COUNT(*) FILTER (WHERE LOWER(COALESCE(p.gender,'')) IN ('female','bride'))::int AS brides,
           COUNT(*) FILTER (WHERE COALESCE(p.verification_status,'pending')='verified')::int AS verified,
           COUNT(*) FILTER (WHERE p.created_at >= CURRENT_DATE)::int AS new_today,
           COUNT(*) FILTER (WHERE COALESCE(s.plan_id,'free') <> 'free' AND COALESCE(s.is_active,false)=true)::int AS paid,
           COUNT(*) FILTER (WHERE COALESCE(s.plan_id,'free') = 'free' OR s.subscription_id IS NULL)::int AS free,
           COUNT(*) FILTER (WHERE p.created_by_advisor_id IS NOT NULL OR p.profile_created_by='mediator')::int AS agent_created,
           COUNT(*) FILTER (WHERE p.created_by_advisor_id IS NULL AND COALESCE(p.profile_created_by,'self')='self')::int AS self_created
         FROM profiles p
         LEFT JOIN subscriptions s ON s.user_id = p.user_id AND s.is_active = true`
      ),
      db.query(
        `SELECT
           COUNT(*)::int AS total,
           COUNT(*) FILTER (WHERE status='active')::int AS active,
           COUNT(*) FILTER (WHERE kyc_status='approved')::int AS verified,
           COUNT(*) FILTER (WHERE kyc_status='pending' OR onboarding_status IN ('pending','more_info'))::int AS pending,
           COUNT(*) FILTER (WHERE status='suspended' OR onboarding_status='rejected')::int AS suspended
         FROM advisors`
      ),
      db.query(
        `SELECT TO_CHAR(month_bucket, 'Mon') AS label,
                COALESCE(SUM(t.amount),0)::numeric AS revenue
         FROM generate_series(
           date_trunc('month', NOW()) - INTERVAL '5 months',
           date_trunc('month', NOW()),
           INTERVAL '1 month'
         ) AS months(month_bucket)
         LEFT JOIN transactions t
           ON date_trunc('month', t.created_at) = month_bucket
          AND t.status IN ('paid','success','captured')
         GROUP BY month_bucket
         ORDER BY month_bucket`
      ),
      db.query(
        `SELECT
           (SELECT COUNT(*) FROM verifications WHERE status='pending')::int AS member_kyc,
           (SELECT COUNT(*) FROM advisors WHERE kyc_status='pending' OR onboarding_status IN ('pending','more_info'))::int AS agent_kyc,
           (SELECT COUNT(*) FROM reports WHERE status='pending')::int AS reports,
           (SELECT COUNT(*) FROM profile_photos WHERE is_approved=false)::int AS photos,
           (SELECT COUNT(*) FROM payment_orders WHERE status IN ('created','attempted','pending'))::int AS upgrades,
           (SELECT COUNT(*) FROM admin_alerts WHERE status='open')::int AS alerts`
      ),
      db.query(
        `SELECT
           p.profile_id,
           CONCAT('SM-', UPPER(SUBSTRING(REPLACE(p.profile_id::text, '-', '') FROM 1 FOR 8))) AS profile_display_id,
           p.user_id,
           p.first_name,
           p.last_name,
           p.gender,
           p.dob,
           p.created_at,
           p.primary_photo_url,
           COALESCE(p.verification_status,'pending') AS verification_status,
           COALESCE(p.admin_status,'active') AS admin_status,
           COALESCE(s.plan_id,'free') AS plan_id,
           u.phone,
           u.email
         FROM profiles p
         JOIN users u ON u.user_id = p.user_id
         LEFT JOIN subscriptions s ON s.user_id = p.user_id AND s.is_active=true
         ORDER BY p.created_at DESC
         LIMIT 12`
      ),
      db.query(
        `SELECT COALESCE(s.plan_id,'free') AS plan_id,
                COUNT(*)::int AS total,
                COALESCE(SUM(s.amount_paid),0)::numeric AS revenue
         FROM profiles p
         LEFT JOIN subscriptions s ON s.user_id = p.user_id AND s.is_active=true
         GROUP BY COALESCE(s.plan_id,'free')
         ORDER BY total DESC`
      ),
      db.query(
        `SELECT
           a.advisor_id,
           a.full_name,
           a.city,
           a.state,
           a.average_rating,
           COUNT(p.profile_id)::int AS members_added
         FROM advisors a
         LEFT JOIN profiles p ON p.created_by_advisor_id = a.advisor_id
         GROUP BY a.advisor_id
         ORDER BY members_added DESC, a.average_rating DESC NULLS LAST
         LIMIT 5`
      ),
      db.query(
        `SELECT
           a.advisor_id,
           a.user_id,
           a.full_name,
           a.agent_code,
           a.phone,
           a.email,
           a.business_name,
           a.city,
           a.state,
           a.status,
           a.kyc_status,
           a.onboarding_status,
           a.membership_plan,
           a.average_rating,
           a.created_at,
           COUNT(p.profile_id)::int AS profiles_added
         FROM advisors a
         LEFT JOIN profiles p ON p.created_by_advisor_id = a.advisor_id
         GROUP BY a.advisor_id
         ORDER BY a.created_at DESC
         LIMIT 10`
      ),
      db.query(
        `SELECT admin_email, admin_role, action, entity_type, entity_id, ip_address, created_at
         FROM admin_audit_logs
         ORDER BY created_at DESC
         LIMIT 8`
      )
    ]);
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
        adminConsole: {
          members: memberBreakdown.rows[0] || {},
          agents: agentBreakdown.rows[0] || {},
          revenueTrend: revenueTrend.rows || [],
          queues: pendingQueues.rows[0] || {},
          recentMembers: recentMembers.rows || [],
          membershipBreakdown: membershipBreakdown.rows || [],
          agentLeaderboard: agentLeaderboard.rows || [],
          recentAgents: recentAgents.rows || [],
          recentAudit: recentAudit.rows || []
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
         u.user_type,
         u.last_login,
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
         CONCAT('SM-', UPPER(SUBSTRING(REPLACE(p.profile_id::text, '-', '') FROM 1 FOR 8))) AS profile_display_id,
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
         COALESCE(p.trust_score, 0) AS trust_score,
         COALESCE(p.trust_level, 'low') AS trust_level,
         p.is_published,
         p.is_partner_pref_set,
         p.profile_status,
         p.profile_created_by,
         COALESCE(p.verification_status, 'pending') AS verification_status,
         COALESCE(p.admin_status, CASE WHEN u.is_banned THEN 'suspended' ELSE 'active' END) AS admin_status,
         p.review_status,
         p.rejection_reason,
         p.review_notes,
         p.submitted_at,
         p.reviewed_at,
         p.verified_at,
         p.created_by_advisor_id,
         p.primary_photo_url,
         p.photo_privacy,
         p.profile_visibility,
         p.hide_last_seen,
         u.user_type,
         COALESCE(s.plan_id, 'free') AS plan_id,
         s.start_date AS subscription_start_date,
         s.end_date AS subscription_end_date,
         s.amount_paid,
         ec.education_level,
         ec.is_employed,
         ec.occupation,
         ec.annual_income,
         ec.working_city,
         ec.working_state,
         ec.working_pincode,
         ph.height_cm,
         ph.weight_kg,
         ph.complexion,
         ph.body_type,
         ph.blood_group,
         fd.father_occupation,
         fd.mother_occupation,
         fd.num_brothers,
         fd.num_sisters,
         fd.family_city,
         fd.family_state,
         fd.family_locality,
         fd.family_pincode,
         fd.family_type,
         hd.rashi,
         hd.nakshatra,
         hd.is_manglik,
         hd.birth_city,
         hd.gotra,
         ld.diet,
         ld.smoking,
         ld.drinking,
         ld.about_me,
         pp.age_min,
         pp.age_max,
         pp.religion AS preference_religion,
         pp.manglik_pref,
         pp.education_levels,
         pp.occupations,
         pp.annual_income_min,
         pp.annual_income_max,
         pp.height_min_cm,
         pp.height_max_cm,
         pp.locations,
         pp.location_radius_km,
         pp.diet_prefs,
         pp.marital_statuses,
         pp.family_types,
         pp.relocation_open,
         pp.timeline,
         pp.deal_breakers,
         pp.good_to_have,
         a.full_name AS advisor_name,
         a.agent_code AS advisor_code,
         COALESCE((
           SELECT json_agg(json_build_object(
             'photo_id', phs.photo_id,
             'photo_url', phs.photo_url,
             'is_primary', phs.is_primary,
             'is_approved', phs.is_approved,
             'review_status', COALESCE(phs.review_status, CASE WHEN phs.is_approved THEN 'approved' ELSE 'pending' END),
             'review_comment', phs.review_comment,
             'reviewed_at', phs.reviewed_at,
             'reviewed_by', phs.reviewed_by,
             'sequence_order', phs.sequence_order,
             'uploaded_at', phs.uploaded_at
           ) ORDER BY phs.is_primary DESC, phs.sequence_order NULLS LAST, phs.uploaded_at DESC)
           FROM profile_photos phs
           WHERE phs.profile_id = p.profile_id
         ), '[]'::json) AS photos,
         COALESCE((
           SELECT json_agg(json_build_object(
             'profile_document_id', pd.profile_document_id,
             'document_type', pd.document_type,
             'document_side', pd.document_side,
             'file_url', pd.file_url,
             'status', pd.status,
             'review_comment', pd.review_comment,
             'uploaded_at', pd.uploaded_at,
             'reviewed_at', pd.reviewed_at,
             'reviewed_by', pd.reviewed_by
           ) ORDER BY pd.uploaded_at DESC)
           FROM profile_documents pd
           WHERE pd.profile_id = p.profile_id
         ), '[]'::json) AS documents,
         COALESCE((
           SELECT json_agg(json_build_object(
             'interest_id', i.interest_id,
             'direction', CASE WHEN i.sender_id = p.profile_id THEN 'sent' ELSE 'received' END,
             'other_profile_id', CASE WHEN i.sender_id = p.profile_id THEN i.receiver_id ELSE i.sender_id END,
             'status', i.status,
             'sent_at', i.sent_at,
             'responded_at', i.responded_at
           ) ORDER BY i.sent_at DESC)
           FROM interests i
           WHERE i.sender_id = p.profile_id OR i.receiver_id = p.profile_id
         ), '[]'::json) AS interests,
         (SELECT COUNT(*)::int FROM interests i WHERE i.sender_id = p.profile_id) AS interests_sent_count,
         (SELECT COUNT(*)::int FROM interests i WHERE i.receiver_id = p.profile_id) AS interests_received_count,
         (SELECT COUNT(*)::int FROM shortlists sh WHERE sh.profile_id = p.profile_id) AS shortlist_count,
         (SELECT COUNT(*)::int FROM profile_views pv WHERE pv.viewed_profile_id = p.profile_id) AS view_count,
         (SELECT COUNT(*)::int FROM reports r WHERE r.reported_id = p.user_id) AS report_count,
         p.created_at
       FROM profiles p
       JOIN users u ON u.user_id = p.user_id
       LEFT JOIN subscriptions s ON s.user_id = p.user_id AND s.is_active=true
       LEFT JOIN education_career ec ON ec.profile_id = p.profile_id
       LEFT JOIN physical_details ph ON ph.profile_id = p.profile_id
       LEFT JOIN family_details fd ON fd.profile_id = p.profile_id
       LEFT JOIN horoscope_details hd ON hd.profile_id = p.profile_id
       LEFT JOIN lifestyle_details ld ON ld.profile_id = p.profile_id
       LEFT JOIN partner_preferences pp ON pp.profile_id = p.profile_id
       LEFT JOIN advisors a ON a.advisor_id = p.created_by_advisor_id
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
         advisor_id, full_name, phone, email, business_name, years_experience, service_label, bio, gender,
         city, state, pincode, languages, communities, max_active_assignments,
         success_rate, complaint_score, average_rating, kyc_status, status,
         membership_plan, membership_expires_at, notes, created_at, updated_at
       )
       VALUES (
         $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13::jsonb,$14::jsonb,$15,$16,$17,$18,$19,$20,$21,$22,$23,NOW(),NOW()
       )
       RETURNING *`,
      [
        crypto.randomUUID(),
        body.fullName,
        body.phone,
        body.email || null,
        body.businessName || body.business_name || null,
        body.yearsExperience !== undefined || body.years_experience !== undefined ? parseNumber(body.yearsExperience ?? body.years_experience, 0) : null,
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
           business_name = COALESCE($5, business_name),
           years_experience = COALESCE($6, years_experience),
           service_label = COALESCE($7, service_label),
           bio = COALESCE($8, bio),
           gender = COALESCE($9, gender),
           city = COALESCE($10, city),
           state = COALESCE($11, state),
           pincode = COALESCE($12, pincode),
           languages = COALESCE($13::jsonb, languages),
           communities = COALESCE($14::jsonb, communities),
           max_active_assignments = COALESCE($15, max_active_assignments),
           success_rate = COALESCE($16, success_rate),
           complaint_score = COALESCE($17, complaint_score),
           average_rating = COALESCE($18, average_rating),
           kyc_status = COALESCE($19, kyc_status),
           status = COALESCE($20, status),
           membership_plan = COALESCE($21, membership_plan),
           membership_expires_at = COALESCE($22, membership_expires_at),
           notes = COALESCE($23, notes),
           updated_at = NOW()
       WHERE advisor_id = $1
       RETURNING *`,
      [
        advisorId,
        body.fullName || null,
        body.phone || null,
        body.email || null,
        body.businessName || body.business_name || null,
        body.yearsExperience !== undefined || body.years_experience !== undefined ? parseNumber(body.yearsExperience ?? body.years_experience, 0) : null,
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
      `INSERT INTO users (phone, email, is_verified, is_active, user_type, role_selected_at, acquisition_source, created_at, updated_at)
       VALUES ($1,$2,$3,true,'member',NOW(),'admin_manual',NOW(),NOW())
       RETURNING user_id, phone, email`,
      [body.phone || null, body.email || null, body.isVerified === true]
    );
    const userId = user.rows[0].user_id;
    const score = profileScore(body);
    const profile = await client.query(
      `INSERT INTO profiles (
         user_id, first_name, last_name, dob, gender, religion, caste, mother_tongue,
         marital_status, completion_score, is_published, primary_photo_url,
         verification_status, admin_status, profile_status, profile_created_by,
         created_by_advisor_id, review_status, created_at, updated_at
       )
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,'active',$14,$15,$16,$17,NOW(),NOW())
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
        body.verificationStatus || 'pending',
        body.profileStatus || body.profile_status || 'active',
        body.profileCreatedBy || body.profile_created_by || (body.createdByAdvisorId || body.created_by_advisor_id ? 'mediator' : 'self'),
        body.createdByAdvisorId || body.created_by_advisor_id || null,
        body.reviewStatus || body.review_status || 'draft'
      ]
    );
    const profileId = profile.rows[0].profile_id;
    await client.query(
      `INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, body_type, blood_group)
       VALUES ($1,$2,$3,$4,$5,$6)`,
      [profileId, body.heightCm || body.height_cm || null, body.weightKg || null, body.complexion || '', body.bodyType || '', body.bloodGroup || '']
    );
    await client.query(
      `INSERT INTO education_career (profile_id, education_level, is_employed, occupation, annual_income, working_city, working_state, working_pincode)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      [
        profileId,
        body.educationLevel || body.education_level || '',
        parseBool(body.isEmployed ?? body.is_employed, Boolean(body.occupation || body.workingCity || body.working_city)),
        body.occupation || '',
        body.annualIncome || body.annual_income || '',
        body.workingCity || body.working_city || '',
        body.workingState || body.working_state || '',
        body.workingPincode || body.working_pincode || ''
      ]
    );
    await client.query(
      `INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city, family_state, family_locality, family_pincode)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
      [
        profileId,
        body.fatherOccupation || body.father_occupation || '',
        body.motherOccupation || body.mother_occupation || '',
        body.numBrothers || body.num_brothers || 0,
        body.numSisters || body.num_sisters || 0,
        body.familyType || body.family_type || '',
        body.familyCity || body.family_city || '',
        body.familyState || body.family_state || '',
        body.familyLocality || body.family_locality || '',
        body.familyPincode || body.family_pincode || ''
      ]
    );
    await client.query(
      `INSERT INTO horoscope_details (profile_id, rashi, nakshatra, is_manglik, birth_city, gotra)
       VALUES ($1,$2,$3,$4,$5,$6)`,
      [
        profileId,
        body.rashi || '',
        body.nakshatra || '',
        parseBool(body.isManglik ?? body.is_manglik, false),
        body.birthCity || body.birth_city || '',
        body.gotra || ''
      ]
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
  const db = await getDB();
  const client = await db.connect();
  try {
    const body = req.body || {};
    const profileId = req.params.id;
    const score = profileScore(body);
    await client.query('BEGIN');
    const profileResult = await client.query(
      `UPDATE profiles
       SET first_name = COALESCE($2, first_name),
           last_name = COALESCE($3, last_name),
           dob = COALESCE($4, dob),
           gender = COALESCE($5, gender),
           religion = COALESCE($6, religion),
           caste = COALESCE($7, caste),
           mother_tongue = COALESCE($8, mother_tongue),
           marital_status = COALESCE($9, marital_status),
           primary_photo_url = COALESCE($10, primary_photo_url),
           is_published = COALESCE($11, is_published),
           profile_status = COALESCE($12, profile_status),
           profile_created_by = COALESCE($13, profile_created_by),
           verification_status = COALESCE($14, verification_status),
           admin_status = COALESCE($15, admin_status),
           photo_privacy = COALESCE($16, photo_privacy),
           profile_visibility = COALESCE($17, profile_visibility),
           hide_last_seen = COALESCE($18, hide_last_seen),
           review_status = COALESCE($19, review_status),
           review_notes = COALESCE($20, review_notes),
           rejection_reason = COALESCE($21, rejection_reason),
           completion_score = GREATEST(COALESCE(completion_score, 0), $22),
           updated_at = NOW()
       WHERE profile_id = $1
       RETURNING *`,
      [
        profileId,
        body.firstName ?? body.first_name ?? null,
        body.lastName ?? body.last_name ?? null,
        body.dob || null,
        body.gender || null,
        body.religion || null,
        body.caste || null,
        body.motherTongue ?? body.mother_tongue ?? null,
        body.maritalStatus ?? body.marital_status ?? null,
        body.primaryPhotoUrl ?? body.primary_photo_url ?? null,
        body.isPublished !== undefined || body.is_published !== undefined ? parseBool(body.isPublished ?? body.is_published) : null,
        body.profileStatus ?? body.profile_status ?? null,
        body.profileCreatedBy ?? body.profile_created_by ?? null,
        body.verificationStatus ?? body.verification_status ?? null,
        body.adminStatus ?? body.admin_status ?? null,
        body.photoPrivacy ?? body.photo_privacy ?? null,
        body.profileVisibility ?? body.profile_visibility ?? null,
        body.hideLastSeen !== undefined || body.hide_last_seen !== undefined ? parseBool(body.hideLastSeen ?? body.hide_last_seen) : null,
        body.reviewStatus ?? body.review_status ?? null,
        body.reviewNotes ?? body.review_notes ?? null,
        body.rejectionReason ?? body.rejection_reason ?? null,
        score
      ]
    );
    const profile = profileResult.rows[0];
    if (!profile) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    }

    await client.query(
      `UPDATE users
       SET phone = COALESCE($2, phone),
           email = COALESCE($3, email),
           is_active = COALESCE($4, is_active),
           is_banned = COALESCE($5, is_banned),
           updated_at = NOW()
       WHERE user_id = $1`,
      [
        profile.user_id,
        body.phone || null,
        body.email || null,
        body.isActive !== undefined || body.is_active !== undefined ? parseBool(body.isActive ?? body.is_active) : null,
        body.isBanned !== undefined || body.is_banned !== undefined ? parseBool(body.isBanned ?? body.is_banned) : null
      ]
    );

    await client.query(
      `INSERT INTO physical_details (profile_id, height_cm, weight_kg, complexion, body_type, blood_group)
       VALUES ($1,$2,$3,$4,$5,$6)
       ON CONFLICT (profile_id)
       DO UPDATE SET height_cm=COALESCE($2, physical_details.height_cm),
                     weight_kg=COALESCE($3, physical_details.weight_kg),
                     complexion=COALESCE($4, physical_details.complexion),
                     body_type=COALESCE($5, physical_details.body_type),
                     blood_group=COALESCE($6, physical_details.blood_group)`,
      [
        profileId,
        body.heightCm ?? body.height_cm ?? null,
        body.weightKg ?? body.weight_kg ?? null,
        body.complexion ?? null,
        body.bodyType ?? body.body_type ?? null,
        body.bloodGroup ?? body.blood_group ?? null
      ]
    );

    await client.query(
      `INSERT INTO education_career (profile_id, education_level, is_employed, occupation, annual_income, working_city, working_state, working_pincode)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
       ON CONFLICT (profile_id)
       DO UPDATE SET education_level=COALESCE($2, education_career.education_level),
                     is_employed=COALESCE($3, education_career.is_employed),
                     occupation=COALESCE($4, education_career.occupation),
                     annual_income=COALESCE($5, education_career.annual_income),
                     working_city=COALESCE($6, education_career.working_city),
                     working_state=COALESCE($7, education_career.working_state),
                     working_pincode=COALESCE($8, education_career.working_pincode)`,
      [
        profileId,
        body.educationLevel ?? body.education_level ?? null,
        body.isEmployed !== undefined || body.is_employed !== undefined ? parseBool(body.isEmployed ?? body.is_employed) : null,
        body.occupation ?? null,
        body.annualIncome ?? body.annual_income ?? null,
        body.workingCity ?? body.working_city ?? null,
        body.workingState ?? body.working_state ?? null,
        body.workingPincode ?? body.working_pincode ?? null
      ]
    );

    await client.query(
      `INSERT INTO family_details (profile_id, father_occupation, mother_occupation, num_brothers, num_sisters, family_type, family_city, family_state, family_locality, family_pincode)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
       ON CONFLICT (profile_id)
       DO UPDATE SET father_occupation=COALESCE($2, family_details.father_occupation),
                     mother_occupation=COALESCE($3, family_details.mother_occupation),
                     num_brothers=COALESCE($4, family_details.num_brothers),
                     num_sisters=COALESCE($5, family_details.num_sisters),
                     family_type=COALESCE($6, family_details.family_type),
                     family_city=COALESCE($7, family_details.family_city),
                     family_state=COALESCE($8, family_details.family_state),
                     family_locality=COALESCE($9, family_details.family_locality),
                     family_pincode=COALESCE($10, family_details.family_pincode)`,
      [
        profileId,
        body.fatherOccupation ?? body.father_occupation ?? null,
        body.motherOccupation ?? body.mother_occupation ?? null,
        body.numBrothers ?? body.num_brothers ?? null,
        body.numSisters ?? body.num_sisters ?? null,
        body.familyType ?? body.family_type ?? null,
        body.familyCity ?? body.family_city ?? null,
        body.familyState ?? body.family_state ?? null,
        body.familyLocality ?? body.family_locality ?? null,
        body.familyPincode ?? body.family_pincode ?? null
      ]
    );

    await client.query(
      `INSERT INTO horoscope_details (profile_id, rashi, nakshatra, is_manglik, birth_city, gotra)
       VALUES ($1,$2,$3,$4,$5,$6)
       ON CONFLICT (profile_id)
       DO UPDATE SET rashi=COALESCE($2, horoscope_details.rashi),
                     nakshatra=COALESCE($3, horoscope_details.nakshatra),
                     is_manglik=COALESCE($4, horoscope_details.is_manglik),
                     birth_city=COALESCE($5, horoscope_details.birth_city),
                     gotra=COALESCE($6, horoscope_details.gotra)`,
      [
        profileId,
        body.rashi ?? null,
        body.nakshatra ?? null,
        body.isManglik !== undefined || body.is_manglik !== undefined ? parseBool(body.isManglik ?? body.is_manglik) : null,
        body.birthCity ?? body.birth_city ?? null,
        body.gotra ?? null
      ]
    );

    await client.query(
      `INSERT INTO lifestyle_details (profile_id, diet, smoking, drinking, about_me)
       VALUES ($1,$2,$3,$4,$5)
       ON CONFLICT (profile_id)
       DO UPDATE SET diet=COALESCE($2, lifestyle_details.diet),
                     smoking=COALESCE($3, lifestyle_details.smoking),
                     drinking=COALESCE($4, lifestyle_details.drinking),
                     about_me=COALESCE($5, lifestyle_details.about_me)`,
      [
        profileId,
        body.diet ?? null,
        body.smoking ?? null,
        body.drinking ?? null,
        body.aboutMe ?? body.about_me ?? null
      ]
    );

    if (body.preferenceReligion !== undefined || body.ageMin !== undefined || body.age_min !== undefined) {
      await client.query(
        `INSERT INTO partner_preferences (
           profile_id, age_min, age_max, religion, manglik_pref, education_levels, occupations,
           annual_income_min, annual_income_max, height_min_cm, height_max_cm, locations,
           location_radius_km, diet_prefs, marital_statuses, family_types, relocation_open,
           timeline, deal_breakers, good_to_have, updated_at
         )
         VALUES ($1,$2,$3,$4,$5,$6::text[],$7::text[],$8,$9,$10,$11,$12::text[],$13,$14::text[],$15::text[],$16::text[],$17,$18,$19::text[],$20::text[],NOW())
         ON CONFLICT (profile_id)
         DO UPDATE SET age_min=COALESCE($2, partner_preferences.age_min),
                       age_max=COALESCE($3, partner_preferences.age_max),
                       religion=COALESCE($4, partner_preferences.religion),
                       manglik_pref=COALESCE($5, partner_preferences.manglik_pref),
                       education_levels=COALESCE($6::text[], partner_preferences.education_levels),
                       occupations=COALESCE($7::text[], partner_preferences.occupations),
                       annual_income_min=COALESCE($8, partner_preferences.annual_income_min),
                       annual_income_max=COALESCE($9, partner_preferences.annual_income_max),
                       height_min_cm=COALESCE($10, partner_preferences.height_min_cm),
                       height_max_cm=COALESCE($11, partner_preferences.height_max_cm),
                       locations=COALESCE($12::text[], partner_preferences.locations),
                       location_radius_km=COALESCE($13, partner_preferences.location_radius_km),
                       diet_prefs=COALESCE($14::text[], partner_preferences.diet_prefs),
                       marital_statuses=COALESCE($15::text[], partner_preferences.marital_statuses),
                       family_types=COALESCE($16::text[], partner_preferences.family_types),
                       relocation_open=COALESCE($17, partner_preferences.relocation_open),
                       timeline=COALESCE($18, partner_preferences.timeline),
                       deal_breakers=COALESCE($19::text[], partner_preferences.deal_breakers),
                       good_to_have=COALESCE($20::text[], partner_preferences.good_to_have),
                       updated_at=NOW()`,
        [
          profileId,
          body.ageMin ?? body.age_min ?? null,
          body.ageMax ?? body.age_max ?? null,
          body.preferenceReligion ?? body.preference_religion ?? null,
          body.manglikPref ?? body.manglik_pref ?? null,
          body.educationLevels !== undefined || body.education_levels !== undefined ? arrayInput(body.educationLevels ?? body.education_levels) : null,
          body.occupations !== undefined ? arrayInput(body.occupations) : null,
          body.annualIncomeMin ?? body.annual_income_min ?? null,
          body.annualIncomeMax ?? body.annual_income_max ?? null,
          body.heightMinCm ?? body.height_min_cm ?? null,
          body.heightMaxCm ?? body.height_max_cm ?? null,
          body.locations !== undefined ? arrayInput(body.locations) : null,
          body.locationRadiusKm ?? body.location_radius_km ?? null,
          body.dietPrefs !== undefined || body.diet_prefs !== undefined ? arrayInput(body.dietPrefs ?? body.diet_prefs) : null,
          body.maritalStatuses !== undefined || body.marital_statuses !== undefined ? arrayInput(body.maritalStatuses ?? body.marital_statuses) : null,
          body.familyTypes !== undefined || body.family_types !== undefined ? arrayInput(body.familyTypes ?? body.family_types) : null,
          body.relocationOpen !== undefined || body.relocation_open !== undefined ? parseBool(body.relocationOpen ?? body.relocation_open) : null,
          body.timeline ?? null,
          body.dealBreakers !== undefined || body.deal_breakers !== undefined ? arrayInput(body.dealBreakers ?? body.deal_breakers) : null,
          body.goodToHave !== undefined || body.good_to_have !== undefined ? arrayInput(body.goodToHave ?? body.good_to_have) : null
        ]
      );
      await client.query('UPDATE profiles SET is_partner_pref_set=true WHERE profile_id=$1', [profileId]);
    }

    await auditLog(client, req, 'profile.update_360', 'profile', profileId, body);
    await client.query('COMMIT');
    broadcastAdminEvent('admin:profile_updated', { profile });
    res.json({ success: true, data: profile });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to update profile right now.');
  } finally {
    client.release();
  }
};

exports.addProfilePhoto = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const profileId = req.params.id;
    const requestedPhotoId = crypto.randomUUID();
    const uploadedPhoto = saveAdminPhotoUpload(req.body, profileId, requestedPhotoId);
    const photoUrl = uploadedPhoto?.photoUrl || normalizePhotoUrl(req.body?.photoUrl || req.body?.photo_url);
    if (uploadedPhoto === false || photoUrl === false || !photoUrl) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Photo must be a JPEG/PNG/WebP upload, an HTTPS URL, or a SoulMatch upload path.' } });
    }
    await client.query('BEGIN');
    const profileResult = await client.query('SELECT profile_id, primary_photo_url FROM profiles WHERE profile_id=$1 FOR UPDATE', [profileId]);
    if (!profileResult.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Profile not found.' } });
    }
    const existingPrimary = Boolean(profileResult.rows[0].primary_photo_url);
    const makePrimary = parseBool(req.body?.isPrimary ?? req.body?.is_primary, !existingPrimary);
    const photoId = requestedPhotoId;
    if (makePrimary) {
      await client.query('UPDATE profile_photos SET is_primary=false WHERE profile_id=$1', [profileId]);
    }
    const isApproved = parseBool(req.body?.isApproved ?? req.body?.is_approved, true);
    const inserted = await client.query(
      `INSERT INTO profile_photos (photo_id, profile_id, photo_url, s3_key, is_primary, is_approved, review_status, reviewed_at, reviewed_by, sequence_order, uploaded_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,CASE WHEN $6 THEN NOW() ELSE NULL END,CASE WHEN $6 THEN $8 ELSE NULL END,(SELECT COALESCE(MAX(sequence_order),0)+1 FROM profile_photos WHERE profile_id=$2),NOW())
       RETURNING photo_id, profile_id, photo_url, is_primary, is_approved, review_status, review_comment, reviewed_at, reviewed_by, sequence_order, uploaded_at`,
      [photoId, profileId, photoUrl, uploadedPhoto?.s3Key || `admin/manual/${profileId}/${photoId}`, makePrimary, isApproved, isApproved ? 'approved' : 'pending', req.admin?.email || 'admin']
    );
    if (makePrimary) {
      await client.query('UPDATE profiles SET primary_photo_url=$1, updated_at=NOW() WHERE profile_id=$2', [photoUrl, profileId]);
    }
    await auditLog(client, req, 'profile_photo.add', 'profile', profileId, { photoId, photoUrl, makePrimary });
    await client.query('COMMIT');
    broadcastAdminEvent('admin:profile_updated', { profileId });
    res.status(201).json({ success: true, data: inserted.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to add profile photo right now.');
  } finally {
    client.release();
  }
};

exports.updateProfilePhoto = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const profileId = req.params.id;
    const photoId = req.params.photoId;
    const uploadedPhoto = saveAdminPhotoUpload(req.body, profileId, photoId);
    const photoUrl = uploadedPhoto?.photoUrl || (req.body?.photoUrl !== undefined || req.body?.photo_url !== undefined
      ? normalizePhotoUrl(req.body?.photoUrl || req.body?.photo_url)
      : null);
    if (uploadedPhoto === false || photoUrl === false) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Photo must be a JPEG/PNG/WebP upload, an HTTPS URL, or a SoulMatch upload path.' } });
    }
    const makePrimary = req.body?.isPrimary !== undefined || req.body?.is_primary !== undefined
      ? parseBool(req.body?.isPrimary ?? req.body?.is_primary)
      : null;
    await client.query('BEGIN');
    const current = await client.query('SELECT photo_id, photo_url, is_primary FROM profile_photos WHERE profile_id=$1 AND photo_id=$2 FOR UPDATE', [profileId, photoId]);
    if (!current.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Photo not found.' } });
    }
    if (makePrimary === true) {
      await client.query('UPDATE profile_photos SET is_primary=false WHERE profile_id=$1', [profileId]);
    }
    const updated = await client.query(
      `UPDATE profile_photos
       SET photo_url=COALESCE($3, photo_url),
           is_primary=COALESCE($4, is_primary),
           is_approved=COALESCE($5, is_approved),
           review_status=CASE
             WHEN $5 IS TRUE THEN 'approved'
             WHEN $5 IS FALSE THEN 'rejected'
             ELSE COALESCE(review_status, CASE WHEN is_approved THEN 'approved' ELSE 'pending' END)
           END,
           review_comment=COALESCE($7, review_comment),
           reviewed_at=CASE WHEN $5 IS NOT NULL THEN NOW() ELSE reviewed_at END,
           reviewed_by=CASE WHEN $5 IS NOT NULL THEN $8 ELSE reviewed_by END,
           s3_key=COALESCE($6, s3_key)
       WHERE profile_id=$1 AND photo_id=$2
       RETURNING photo_id, profile_id, photo_url, is_primary, is_approved, review_status, review_comment, reviewed_at, reviewed_by, sequence_order, uploaded_at`,
      [
        profileId,
        photoId,
        photoUrl,
        makePrimary,
        req.body?.isApproved !== undefined || req.body?.is_approved !== undefined ? parseBool(req.body?.isApproved ?? req.body?.is_approved) : null,
        uploadedPhoto?.s3Key || null,
        req.body?.reviewComment || req.body?.review_comment || null,
        req.admin?.email || 'admin'
      ]
    );
    if (makePrimary === true || (photoUrl && current.rows[0].is_primary)) {
      const primary = await client.query('SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND is_primary=true LIMIT 1', [profileId]);
      await client.query('UPDATE profiles SET primary_photo_url=$1, updated_at=NOW() WHERE profile_id=$2', [primary.rows[0]?.photo_url || photoUrl || null, profileId]);
    }
    await auditLog(client, req, 'profile_photo.update', 'profile', profileId, { photoId, photoUrl, makePrimary });
    await client.query('COMMIT');
    broadcastAdminEvent('admin:profile_updated', { profileId });
    res.json({ success: true, data: updated.rows[0] });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to update profile photo right now.');
  } finally {
    client.release();
  }
};

exports.deleteProfilePhoto = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const profileId = req.params.id;
    const photoId = req.params.photoId;
    await client.query('BEGIN');
    const removed = await client.query('DELETE FROM profile_photos WHERE profile_id=$1 AND photo_id=$2 RETURNING photo_id, is_primary', [profileId, photoId]);
    if (!removed.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Photo not found.' } });
    }
    if (removed.rows[0].is_primary) {
      const fallback = await client.query(
        'SELECT photo_id, photo_url FROM profile_photos WHERE profile_id=$1 AND is_approved=true ORDER BY sequence_order ASC NULLS LAST, uploaded_at ASC LIMIT 1',
        [profileId]
      );
      if (fallback.rows[0]) {
        await client.query('UPDATE profile_photos SET is_primary = (photo_id=$2) WHERE profile_id=$1', [profileId, fallback.rows[0].photo_id]);
        await client.query('UPDATE profiles SET primary_photo_url=$1, updated_at=NOW() WHERE profile_id=$2', [fallback.rows[0].photo_url, profileId]);
      } else {
        await client.query('UPDATE profiles SET primary_photo_url=NULL, updated_at=NOW() WHERE profile_id=$1', [profileId]);
      }
    }
    await auditLog(client, req, 'profile_photo.delete', 'profile', profileId, { photoId });
    await client.query('COMMIT');
    broadcastAdminEvent('admin:profile_updated', { profileId });
    res.json({ success: true, data: { photoId } });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to delete profile photo right now.');
  } finally {
    client.release();
  }
};

exports.getPendingPhotoModeration = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT
         pp.photo_id,
         pp.profile_id,
         pp.photo_url,
         pp.is_primary,
         pp.is_approved,
         COALESCE(pp.review_status, CASE WHEN pp.is_approved THEN 'approved' ELSE 'pending' END) AS review_status,
         pp.review_comment,
         pp.uploaded_at,
         p.first_name,
         p.last_name,
         p.user_id,
         u.email,
         u.phone
       FROM profile_photos pp
       JOIN profiles p ON p.profile_id=pp.profile_id
       JOIN users u ON u.user_id=p.user_id
       WHERE COALESCE(pp.review_status, CASE WHEN pp.is_approved THEN 'approved' ELSE 'pending' END)='pending'
       ORDER BY pp.uploaded_at ASC
       LIMIT 100`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Photo moderation queue is unavailable.');
  }
};

exports.getModerationInbox = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT *
       FROM (
         SELECT
           'report' AS item_type,
           r.report_id::text AS item_id,
           COALESCE(r.reason, 'Profile report') AS title,
           COALESCE(r.description, 'A member reported this profile.') AS body,
           r.status,
           r.created_at,
           90 AS severity_score,
           r.reported_id::text AS target_id,
           reporter.phone AS reporter_phone,
           reported.phone AS target_phone
         FROM reports r
         LEFT JOIN users reporter ON reporter.user_id=r.reporter_id
         LEFT JOIN users reported ON reported.user_id=r.reported_id
         WHERE COALESCE(r.status,'pending') IN ('pending','reviewing')

         UNION ALL

         SELECT
           'chat_report' AS item_type,
           cmr.chat_message_report_id::text AS item_id,
           COALESCE(cmr.reason, 'Chat message report') AS title,
           COALESCE(cmr.description, cmr.message_id) AS body,
           cmr.status,
           cmr.created_at,
           85 AS severity_score,
           cmr.reported_user_id::text AS target_id,
           reporter.phone AS reporter_phone,
           reported.phone AS target_phone
         FROM chat_message_reports cmr
         LEFT JOIN users reporter ON reporter.user_id=cmr.reporter_user_id
         LEFT JOIN users reported ON reported.user_id=cmr.reported_user_id
         WHERE COALESCE(cmr.status,'pending') IN ('pending','reviewing')

         UNION ALL

         SELECT
           'photo' AS item_type,
           pp.photo_id::text AS item_id,
           'Photo moderation' AS title,
           CONCAT(COALESCE(p.first_name,''), ' ', COALESCE(p.last_name,''), ' profile photo needs review') AS body,
           COALESCE(pp.review_status, CASE WHEN pp.is_approved THEN 'approved' ELSE 'pending' END) AS status,
           pp.uploaded_at AS created_at,
           70 AS severity_score,
           pp.profile_id::text AS target_id,
           NULL AS reporter_phone,
           u.phone AS target_phone
         FROM profile_photos pp
         JOIN profiles p ON p.profile_id=pp.profile_id
         JOIN users u ON u.user_id=p.user_id
         WHERE COALESCE(pp.review_status, CASE WHEN pp.is_approved THEN 'approved' ELSE 'pending' END) IN ('pending','under_review')

         UNION ALL

         SELECT
           'verification' AS item_type,
           v.verification_id::text AS item_id,
           CONCAT('Verification: ', COALESCE(v.type, 'profile')) AS title,
           COALESCE(v.review_note, 'Member verification is waiting for admin review.') AS body,
           v.status,
           v.created_at,
           65 AS severity_score,
           v.user_id::text AS target_id,
           NULL AS reporter_phone,
           u.phone AS target_phone
         FROM verifications v
         LEFT JOIN users u ON u.user_id=v.user_id
         WHERE COALESCE(v.status,'pending') IN ('pending','submitted','under_review')
       ) inbox
       ORDER BY severity_score DESC, created_at ASC
       LIMIT 150`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Unified moderation inbox is unavailable right now.');
  }
};

exports.approveProfilePhoto = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const note = String(req.body?.note || '').trim() || null;
    await client.query('BEGIN');
    const updated = await client.query(
      `UPDATE profile_photos
       SET is_approved=true,
           review_status='approved',
           review_comment=$3,
           reviewed_at=NOW(),
           reviewed_by=$4
       WHERE profile_id=$1 AND photo_id=$2
       RETURNING *`,
      [req.params.id, req.params.photoId, note, req.admin?.email || 'admin']
    );
    const photo = updated.rows[0];
    if (!photo) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Photo not found.' } });
    }
    const profile = await client.query('SELECT user_id, primary_photo_url FROM profiles WHERE profile_id=$1 FOR UPDATE', [req.params.id]);
    if (photo.is_primary || !profile.rows[0]?.primary_photo_url) {
      await client.query('UPDATE profiles SET primary_photo_url=$1, updated_at=NOW() WHERE profile_id=$2', [photo.photo_url, req.params.id]);
    }
    const trust = await recalculateTrustScore(client, req.params.id);
    await auditLog(client, req, 'profile_photo.approve', 'profile_photo', req.params.photoId, { profileId: req.params.id, note });
    await client.query('COMMIT');
    if (profile.rows[0]?.user_id) {
      await notifyMember(db, profile.rows[0].user_id, 'Profile photo approved', 'Your uploaded SoulMatch profile photo is approved.', {
        type: 'profile_photo_review',
        status: 'approved',
        profileId: req.params.id,
        photoId: req.params.photoId
      }).catch((error) => logger.warn(`Photo approval notification skipped: ${error.message}`));
    }
    broadcastAdminEvent('admin:photo_moderation', { action: 'approve', photo, trust });
    res.json({ success: true, data: photo, trust });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to approve profile photo right now.');
  } finally {
    client.release();
  }
};

exports.rejectProfilePhoto = async (req, res) => {
  const db = await getDB();
  const client = await db.connect();
  try {
    const note = String(req.body?.note || '').trim() || 'Photo does not meet SoulMatch profile photo guidelines.';
    await client.query('BEGIN');
    const updated = await client.query(
      `UPDATE profile_photos
       SET is_approved=false,
           review_status='rejected',
           review_comment=$3,
           reviewed_at=NOW(),
           reviewed_by=$4
       WHERE profile_id=$1 AND photo_id=$2
       RETURNING *`,
      [req.params.id, req.params.photoId, note, req.admin?.email || 'admin']
    );
    const photo = updated.rows[0];
    if (!photo) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: { code: 'NOT_FOUND', message: 'Photo not found.' } });
    }
    const profile = await client.query('SELECT user_id, primary_photo_url FROM profiles WHERE profile_id=$1 FOR UPDATE', [req.params.id]);
    if (profile.rows[0]?.primary_photo_url === photo.photo_url) {
      const fallback = await client.query(
        'SELECT photo_url FROM profile_photos WHERE profile_id=$1 AND is_approved=true ORDER BY is_primary DESC, sequence_order ASC NULLS LAST, uploaded_at ASC LIMIT 1',
        [req.params.id]
      );
      await client.query('UPDATE profiles SET primary_photo_url=$1, updated_at=NOW() WHERE profile_id=$2', [fallback.rows[0]?.photo_url || null, req.params.id]);
    }
    const trust = await recalculateTrustScore(client, req.params.id);
    await auditLog(client, req, 'profile_photo.reject', 'profile_photo', req.params.photoId, { profileId: req.params.id, note });
    await client.query('COMMIT');
    if (profile.rows[0]?.user_id) {
      await notifyMember(db, profile.rows[0].user_id, 'Profile photo needs attention', note, {
        type: 'profile_photo_review',
        status: 'rejected',
        profileId: req.params.id,
        photoId: req.params.photoId
      }).catch((error) => logger.warn(`Photo rejection notification skipped: ${error.message}`));
    }
    broadcastAdminEvent('admin:photo_moderation', { action: 'reject', photo, trust });
    res.json({ success: true, data: photo, trust });
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    respondServerError(res, err, 'Unable to reject profile photo right now.');
  } finally {
    client.release();
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
      unverify: { published: true, verification: 'unverified', admin: 'active', review: 'under_review' },
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
    const trust = await recalculateTrustScore(db, profileId);
    await auditLog(db, req, `profile.${action}`, 'profile', profileId, { reason });
    broadcastAdminEvent('admin:profile_status', { action, profile: result.rows[0], trust });
    res.json({ success: true, data: result.rows[0], trust });
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
    const trust = await recalculateTrustScore(client, result.rows[0].user_id, 'user');
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
    broadcastAdminEvent('admin:verification_updated', { action: 'approve', verification: result.rows[0], trust });
    res.json({ success: true, data: result.rows[0], trust });
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
    const trust = await recalculateTrustScore(client, result.rows[0].user_id, 'user');
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
    broadcastAdminEvent('admin:verification_updated', { action: 'reject', verification: result.rows[0], trust });
    res.json({ success: true, data: result.rows[0], trust });
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
    const trust = await recalculateTrustScore(client, document.profile_id);
    await auditLog(client, req, 'profile_document.approve', 'profile_document', req.params.id, {
      note,
      managedProfilePublished: publishResult.published === true,
      missingRequiredDocuments: publishResult.missing || []
    });
    await client.query('COMMIT');
    if (publishResult.published) {
      broadcastAdminEvent('admin:profile_status', { action: 'auto_publish', profile: publishResult.profile });
    }
    res.json({ success: true, data: document, profileReview: publishResult, trust });
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
    const trust = await recalculateTrustScore(db, document.profile_id);
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
    res.json({ success: true, data: document, trust });
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
    const [transactions, plans, coupons, pendingOrders, invoices, revenueSummary] = await Promise.all([
      db.query(
        `SELECT
           t.transaction_id,
           t.user_id,
           u.phone,
           u.email,
           u.user_type,
           p.profile_id,
           CONCAT('SM-', UPPER(SUBSTRING(REPLACE(p.profile_id::text, '-', '') FROM 1 FOR 8))) AS profile_display_id,
           a.advisor_id,
           a.agent_code,
           COALESCE(NULLIF(CONCAT_WS(' ', p.first_name, p.last_name), ''), a.full_name, u.email, u.phone) AS display_name,
           CASE WHEN a.advisor_id IS NOT NULL THEN 'agent' ELSE 'member' END AS owner_type,
           p.first_name,
           p.last_name,
           s.plan_id,
           t.amount,
           t.currency,
           t.status,
           t.gateway,
           t.payment_method,
           t.razorpay_order_id,
           t.razorpay_payment_id,
           t.created_at
         FROM transactions t
         LEFT JOIN users u ON u.user_id=t.user_id
         LEFT JOIN profiles p ON p.user_id=t.user_id
         LEFT JOIN advisors a ON a.user_id=t.user_id
         LEFT JOIN subscriptions s ON s.subscription_id=t.subscription_id
         ORDER BY t.created_at DESC
         LIMIT 100`
      ),
      db.query('SELECT plan_id, COUNT(*) AS active_users, COALESCE(SUM(amount_paid),0) AS revenue FROM subscriptions WHERE is_active=true GROUP BY plan_id ORDER BY revenue DESC'),
      db.query('SELECT * FROM referral_codes ORDER BY created_at DESC LIMIT 50'),
      db.query(
        `SELECT
           po.payment_order_id,
           po.user_id,
           u.phone,
           u.email,
           u.user_type,
           p.profile_id,
           CONCAT('SM-', UPPER(SUBSTRING(REPLACE(p.profile_id::text, '-', '') FROM 1 FOR 8))) AS profile_display_id,
           a.advisor_id,
           a.agent_code,
           COALESCE(NULLIF(CONCAT_WS(' ', p.first_name, p.last_name), ''), a.full_name, u.email, u.phone) AS display_name,
           CASE WHEN a.advisor_id IS NOT NULL THEN 'agent' ELSE 'member' END AS owner_type,
           po.plan_id,
           po.amount,
           po.currency,
           po.status,
           po.gateway,
           po.provider_order_id,
           po.provider_payment_id,
           po.support_contacted,
           po.support_comments,
           po.support_contacted_at,
           po.metadata,
           po.created_at,
           po.updated_at
         FROM payment_orders po
         LEFT JOIN users u ON u.user_id = po.user_id
         LEFT JOIN profiles p ON p.user_id = po.user_id
         LEFT JOIN advisors a ON a.user_id = po.user_id
         WHERE po.status NOT IN ('paid','success','captured')
         ORDER BY po.updated_at DESC, po.created_at DESC
         LIMIT 200`
      ),
      db.query(
        `SELECT
           COALESCE(si.invoice_id::text, t.transaction_id::text) AS invoice_id,
           COALESCE(si.invoice_number, CONCAT('SM-INV-', UPPER(SUBSTRING(REPLACE(t.transaction_id::text, '-', '') FROM 1 FOR 10)))) AS invoice_number,
           t.user_id,
           u.phone,
           u.email,
           p.profile_id,
           CONCAT('SM-', UPPER(SUBSTRING(REPLACE(p.profile_id::text, '-', '') FROM 1 FOR 8))) AS profile_display_id,
           a.advisor_id,
           a.agent_code,
           COALESCE(NULLIF(CONCAT_WS(' ', p.first_name, p.last_name), ''), a.full_name, u.email, u.phone) AS display_name,
           CASE WHEN a.advisor_id IS NOT NULL THEN 'agent' ELSE 'member' END AS owner_type,
           COALESCE(si.plan_id, s.plan_id) AS plan_id,
           COALESCE(si.amount, t.amount) AS amount,
           COALESCE(si.currency, t.currency) AS currency,
           COALESCE(si.status, 'issued') AS status,
           COALESCE(si.payment_method, t.payment_method, t.gateway) AS payment_method,
           COALESCE(si.transaction_ref, t.razorpay_payment_id, t.razorpay_order_id) AS transaction_ref,
           COALESCE(si.valid_from, s.start_date) AS valid_from,
           COALESCE(si.valid_to, s.end_date) AS valid_to,
           COALESCE(si.issued_at, t.created_at) AS issued_at
         FROM transactions t
         LEFT JOIN users u ON u.user_id = t.user_id
         LEFT JOIN subscriptions s ON s.subscription_id = t.subscription_id
         LEFT JOIN profiles p ON p.user_id = t.user_id
         LEFT JOIN advisors a ON a.user_id = t.user_id
         LEFT JOIN subscription_invoices si ON si.transaction_id = t.transaction_id
         WHERE t.status IN ('paid','success','captured')
         ORDER BY COALESCE(si.issued_at, t.created_at) DESC
         LIMIT 200`
      ),
      db.query(
        `SELECT
           DATE_TRUNC('month', t.created_at) AS month,
           CASE WHEN a.advisor_id IS NOT NULL THEN 'agent' ELSE 'member' END AS owner_type,
           COALESCE(s.plan_id, 'unknown') AS plan_id,
           COUNT(*)::int AS paid_count,
           COALESCE(SUM(t.amount),0)::numeric AS revenue
         FROM transactions t
         LEFT JOIN subscriptions s ON s.subscription_id = t.subscription_id
         LEFT JOIN advisors a ON a.user_id = t.user_id
         WHERE t.status IN ('paid','success','captured')
         GROUP BY
           DATE_TRUNC('month', t.created_at),
           CASE WHEN a.advisor_id IS NOT NULL THEN 'agent' ELSE 'member' END,
           COALESCE(s.plan_id, 'unknown')
         ORDER BY month DESC, revenue DESC
         LIMIT 120`
      )
    ]);
    res.json({
      success: true,
      data: {
        transactions: transactions.rows,
        plans: plans.rows,
        coupons: coupons.rows,
        pendingOrders: pendingOrders.rows,
        invoices: invoices.rows,
        revenueSummary: revenueSummary.rows
      }
    });
  } catch (err) {
    respondDegraded(res, err, { transactions: [], plans: [], coupons: [], pendingOrders: [], invoices: [], revenueSummary: [] }, 'Payment store is unavailable. Showing empty payment data.');
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
  const defaultRoles = DEFAULT_CONFIG.admin_roles.roles.map((role) => ({
    ...role,
    permissions: ROLE_PERMISSIONS[role.role] || role.permissions || []
  }));
  try {
    const db = await getDB();
    const roleRows = await db.query(
      `SELECT role, description, status, permissions, updated_at
       FROM admin_roles
       WHERE status <> 'deleted'
       ORDER BY role`
    ).catch(() => ({ rows: [] }));
    const configured = roleRows.rows.length
      ? { roles: roleRows.rows.map((role) => ({
          role: role.role,
          title: role.role,
          description: role.description,
          status: role.status,
          permissions: Array.isArray(role.permissions) ? role.permissions : []
        })) }
      : await getConfigSection(db, 'admin_roles');
    const roles = Array.isArray(configured.roles) && configured.roles.length ? configured.roles : defaultRoles;
    res.json({
      success: true,
      data: roles.map((role) => ({
        ...role,
        permissions: Array.isArray(role.permissions) ? role.permissions : []
      })),
      current: req.admin?.role || 'admin'
    });
  } catch (err) {
    res.json({
      success: true,
      data: defaultRoles,
      current: req.admin?.role || 'admin',
      meta: { degraded: true, message: 'Role configuration database is unavailable. Showing default admin roles.' }
    });
  }
};

exports.getAdminUsers = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `SELECT admin_user_id, email, display_name, role, status, created_by, last_login, created_at, updated_at
       FROM admin_console_users
       WHERE status <> 'deleted'
       ORDER BY updated_at DESC, created_at DESC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondDegraded(res, err, [], 'Admin user store is unavailable. Showing no admin identities.');
  }
};

exports.createAdminUser = async (req, res) => {
  try {
    const db = await getDB();
    const email = String(req.body?.email || '').trim().toLowerCase();
    const password = String(req.body?.password || '');
    const displayName = String(req.body?.displayName || req.body?.display_name || '').trim() || email.split('@')[0];
    const role = String(req.body?.role || 'admin').trim() || 'admin';
    if (!email || !email.includes('@') || password.length < 8) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'A valid email and password with at least 8 characters are required.' } });
    }
    const passwordHash = await bcrypt.hash(password, 12);
    const result = await db.query(
      `INSERT INTO admin_console_users (email, password_hash, display_name, role, status, created_by)
       VALUES ($1,$2,$3,$4,$5,$6)
       ON CONFLICT (email) DO UPDATE SET
         password_hash=EXCLUDED.password_hash,
         display_name=EXCLUDED.display_name,
         role=EXCLUDED.role,
         status=EXCLUDED.status,
         updated_at=NOW()
       RETURNING admin_user_id, email, display_name, role, status, created_by, last_login, created_at, updated_at`,
      [email, passwordHash, displayName, role, req.body?.status || 'active', req.admin?.email || 'admin']
    );
    await auditLog(db, req, 'admin_user.upsert', 'admin_console_user', result.rows[0].admin_user_id, { email, role });
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    respondServerError(res, err, 'Unable to create admin user right now.');
  }
};

exports.updateAdminUser = async (req, res) => {
  try {
    const db = await getDB();
    const id = req.params.id;
    const sets = [];
    const values = [];
    const pushSet = (column, value) => {
      values.push(value);
      sets.push(`${column}=$${values.length}`);
    };
    if (req.body?.email) pushSet('email', String(req.body.email).trim().toLowerCase());
    if (req.body?.displayName !== undefined || req.body?.display_name !== undefined) pushSet('display_name', req.body.displayName ?? req.body.display_name);
    if (req.body?.role) pushSet('role', String(req.body.role).trim());
    if (req.body?.status) pushSet('status', String(req.body.status).trim());
    if (req.body?.password) {
      if (String(req.body.password).length < 8) {
        return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Password must be at least 8 characters.' } });
      }
      pushSet('password_hash', await bcrypt.hash(String(req.body.password), 12));
    }
    if (!sets.length) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'No fields were provided to update.' } });
    }
    values.push(id);
    const result = await db.query(
      `UPDATE admin_console_users SET ${sets.join(', ')}, updated_at=NOW()
       WHERE admin_user_id=$${values.length}
       RETURNING admin_user_id, email, display_name, role, status, created_by, last_login, created_at, updated_at`,
      values
    );
    await auditLog(db, req, 'admin_user.update', 'admin_console_user', id, { fields: sets.map((item) => item.split('=')[0]) });
    res.json({ success: true, data: result.rows[0] || null });
  } catch (err) {
    respondServerError(res, err, 'Unable to update admin user right now.');
  }
};

exports.deleteAdminUser = async (req, res) => {
  try {
    const db = await getDB();
    const result = await db.query(
      `UPDATE admin_console_users
       SET status='deleted', updated_at=NOW()
       WHERE admin_user_id=$1
       RETURNING admin_user_id, email, display_name, role, status, created_at, updated_at`,
      [req.params.id]
    );
    await auditLog(db, req, 'admin_user.delete', 'admin_console_user', req.params.id, { mode: 'soft_delete' });
    res.json({ success: true, data: result.rows[0] || null });
  } catch (err) {
    respondServerError(res, err, 'Unable to delete admin user right now.');
  }
};

exports.getSystemInventory = async (req, res) => {
  try {
    const db = await getDB();
    const [config, deployments, roleChanges] = await Promise.all([
      getConfigMap(db, true).catch(() => DEFAULT_CONFIG),
      db.query(
        `SELECT
           deployment_audit_id,
           created_at AS timestamp,
           admin_actor,
           release_description,
           change_details,
           release_version,
           deployment_status,
           change_type,
           source_commit,
           source_branch
         FROM deployment_audit_logs
         ORDER BY created_at DESC
         LIMIT 100`
      ),
      db.query(
        `SELECT role_change_log_id, admin_email, change_description, modification_details, created_at
         FROM admin_role_change_logs
         ORDER BY created_at DESC
         LIMIT 100`
      )
    ]);
    res.json({
      success: true,
      data: {
        inventory: buildSystemInventory(config),
        deploymentAudit: deployments.rows,
        roleChangeLogs: roleChanges.rows
      }
    });
  } catch (err) {
    respondDegraded(res, err, { inventory: buildSystemInventory(DEFAULT_CONFIG), deploymentAudit: [], roleChangeLogs: [] }, 'System inventory is unavailable.');
  }
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
    const key = resolveConfigKey(req.params.key);
    if (!CONFIG_KEYS.includes(key)) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: `Unknown config key: ${key}` } });
    }
    if (!req.body || typeof req.body !== 'object' || Array.isArray(req.body)) {
      return res.status(400).json({ success: false, error: { code: 'VALIDATION_ERROR', message: 'Config payload must be an object.' } });
    }
    const updated = await upsertConfigSection(db, key, req.body, req.admin?.email || 'admin');
    await auditLog(db, req, 'config.update', 'app_config', key, { key });
    if (key === 'admin_roles') {
      await db.query(
        `INSERT INTO admin_role_change_logs (admin_email, change_description, modification_details)
         VALUES ($1,$2,$3::jsonb)`,
        [
          req.admin?.email || 'admin',
          'Role Master configuration updated',
          JSON.stringify({
            roles: Array.isArray(req.body.roles) ? req.body.roles.map((role) => role.role || role.label) : [],
            propagate: req.body.propagate ?? null
          })
        ]
      ).catch((error) => logger.warn(`Role change log skipped: ${error.message}`));
    }
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
    const lookbackDays = Math.min(Math.max(parseInt(req.query.days, 10) || 30, 1), 365);
    const result = await db.query(
      `WITH params AS (
         SELECT NOW() - ($1 * INTERVAL '1 day') AS since
       ),
       paid_users AS (
         SELECT user_id
         FROM transactions, params
         WHERE created_at >= params.since
           AND status IN ('paid','success','captured')
         UNION
         SELECT user_id
         FROM payment_orders, params
         WHERE updated_at >= params.since
           AND status IN ('paid','success','captured')
         UNION
         SELECT user_id
         FROM subscriptions, params
         WHERE created_at >= params.since
           AND is_active=true
           AND COALESCE(amount_paid,0) > 0
       ),
       funnel AS (
         SELECT
           1 AS sort_order,
           'sign_up'::text AS event_type,
           'Signup completed'::text AS label,
           COUNT(*)::int AS total,
           'users.created_at'::text AS source
         FROM users, params
         WHERE users.created_at >= params.since
           AND users.deleted_at IS NULL
         UNION ALL
         SELECT
           2,
           'profile_published',
           'Profile published',
           COUNT(*)::int,
           'profiles.is_published'
         FROM profiles, params
         WHERE profiles.updated_at >= params.since
           AND profiles.is_published=true
         UNION ALL
         SELECT
           3,
           'interest_sent',
           'Interest sent',
           COUNT(*)::int,
           'interests.sent_at'
         FROM interests, params
         WHERE interests.sent_at >= params.since
         UNION ALL
         SELECT
           4,
           'chat_created',
           'Chat created',
           COUNT(*)::int,
           'chat_conversation_metadata.created_at'
         FROM chat_conversation_metadata, params
         WHERE chat_conversation_metadata.created_at >= params.since
         UNION ALL
         SELECT
           5,
           'payment_success',
           'Payment completed',
           COUNT(DISTINCT user_id)::int,
           'transactions/payment_orders/subscriptions'
         FROM paid_users
       )
       SELECT event_type, label, total, source
       FROM funnel
       ORDER BY sort_order`,
      [lookbackDays]
    );
    res.json({ success: true, data: result.rows, meta: { lookbackDays, generatedAt: new Date().toISOString() } });
  } catch (err) {
    respondServerError(res, err, 'Analytics funnel is unavailable.');
  }
};

exports.getAnalyticsEvents = async (req, res) => {
  try {
    const db = await getDB();
    const limit = Math.min(parseInt(req.query.limit, 10) || 50, 250);
    const result = await db.query(
      `SELECT
         event_id,
         event_type,
         service_name,
         user_id,
         session_id,
         payload,
         (payload->>'serverSigned') = 'true' AS is_server_signed,
         COALESCE(payload->>'recordedBy', service_name) AS source,
         created_at
       FROM analytics_events
       ORDER BY created_at DESC
       LIMIT $1`,
      [limit]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    respondServerError(res, err, 'Analytics events are unavailable.');
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
