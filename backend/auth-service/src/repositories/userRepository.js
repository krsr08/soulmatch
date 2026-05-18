const { getDB } = require('../config/database');
const { createHmac, randomUUID } = require('crypto');

const LEGAL_NOTICE_VERSION = process.env.LEGAL_NOTICE_VERSION || process.env.PRIVACY_POLICY_VERSION || 'dpdp-2026-05-10-v1';

function cleanPhone(phone) {
  return String(phone || '').trim().replace(/[\s()-]/g, '');
}

function normalizePhone(phone) {
  const raw = cleanPhone(phone);
  if (!raw) return null;
  const digits = raw.replace(/\D/g, '');
  if (!digits) return null;
  if (raw.startsWith('+')) return `+${digits}`;
  if (raw.startsWith('00')) return `+${digits.slice(2)}`;
  if (digits.length === 10) return `+91${digits}`;
  return `+${digits}`;
}

function buildPhoneCandidates(phone) {
  const raw = cleanPhone(phone);
  const digits = raw.replace(/\D/g, '');
  const candidates = [];

  const push = (value) => {
    if (value && !candidates.includes(value)) candidates.push(value);
  };

  push(raw);
  if (digits) {
    push(digits);
    push(`+${digits}`);
    if (digits.startsWith('00') && digits.length > 2) push(`+${digits.slice(2)}`);
    if (digits.length === 10) push(`+91${digits}`);
    if (digits.startsWith('91') && digits.length === 12) push(digits.slice(2));
    if (digits.length > 10) push(digits.slice(-10));
  }

  return candidates;
}

function accountHash(value) {
  const raw = String(value || '').trim().toLowerCase();
  if (!raw) return null;
  const secret = process.env.ACCOUNT_HASH_SECRET || 'development-account-hash-secret';
  return createHmac('sha256', secret).update(raw).digest('hex');
}

exports.accountHash = accountHash;
exports.normalizePhone = normalizePhone;
function normalizeUserType(userType) {
  const normalized = String(userType || 'member').trim().toLowerCase();
  return ['member', 'agent', 'admin'].includes(normalized) ? normalized : 'member';
}

exports.normalizeUserType = normalizeUserType;
exports.findByPhone = async (phone) => {
  const db = await getDB();
  const candidates = buildPhoneCandidates(phone);
  if (candidates.length === 0) return null;
  const r = await db.query(
    'SELECT * FROM users WHERE phone = ANY($1::varchar[]) AND is_active=true LIMIT 1',
    [candidates]
  );
  return r.rows[0] || null;
};
exports.findByGoogleId = async (googleId) => { const db = await getDB(); const r = await db.query('SELECT * FROM users WHERE google_id=$1 AND is_active=true LIMIT 1', [googleId]); return r.rows[0] || null; };
exports.findByEmail = async (email) => {
  const db = await getDB();
  const normalized = String(email || '').trim().toLowerCase();
  if (!normalized) return null;
  const r = await db.query('SELECT * FROM users WHERE LOWER(email)=$1 AND is_active=true LIMIT 1', [normalized]);
  return r.rows[0] || null;
};
exports.findById = async (userId) => { const db = await getDB(); const r = await db.query('SELECT * FROM users WHERE user_id=$1 LIMIT 1', [userId]); return r.rows[0] || null; };
exports.attachGoogleId = async (userId, googleId) => {
  const db = await getDB();
  const r = await db.query(
    'UPDATE users SET google_id = COALESCE(google_id, $2), updated_at = NOW() WHERE user_id=$1 RETURNING *',
    [userId, googleId]
  );
  return r.rows[0] || null;
};
exports.updateUserType = async (userId, userType) => {
  const db = await getDB();
  const normalized = normalizeUserType(userType);
  const r = await db.query(
    'UPDATE users SET user_type=$1, role_selected_at=NOW(), updated_at=NOW() WHERE user_id=$2 RETURNING *',
    [normalized, userId]
  );
  return r.rows[0] || null;
};
exports.markRoleSelected = async (userId) => {
  const db = await getDB();
  const r = await db.query(
    'UPDATE users SET role_selected_at=COALESCE(role_selected_at, NOW()), updated_at=NOW() WHERE user_id=$1 RETURNING *',
    [userId]
  );
  return r.rows[0] || null;
};
exports.create = async (data) => {
  const db = await getDB();
  const normalizedPhone = normalizePhone(data.phone);
  const userType = normalizeUserType(data.user_type);
  const phoneHash = accountHash(normalizedPhone);
  const deviceHash = accountHash(data.device_id);
  const r = await db.query(
    `INSERT INTO users (
       user_id, phone, email, google_id, is_verified, referred_by_code, acquisition_source, referred_at,
       user_type, role_selected_at, phone_hash, device_id_hash, duplicate_signal
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13::jsonb) RETURNING *`,
    [
      randomUUID(),
      normalizedPhone || null,
      data.email || null,
      data.google_id || null,
      data.is_verified || false,
      data.referred_by_code || null,
      data.acquisition_source || null,
      data.referred_at || null,
      userType,
      data.role_selected_at || null,
      phoneHash,
      deviceHash,
      JSON.stringify({
        phoneHashPresent: Boolean(phoneHash),
        deviceHashPresent: Boolean(deviceHash),
        detectedAt: new Date().toISOString()
      })
    ]
  );
  return r.rows[0];
};
exports.updateLastLogin = async (userId) => { const db = await getDB(); await db.query('UPDATE users SET last_login=NOW() WHERE user_id=$1', [userId]); };
exports.recordAccountSignal = async (userId, { phone, deviceId } = {}) => {
  const db = await getDB();
  const phoneHash = accountHash(normalizePhone(phone));
  const deviceHash = accountHash(deviceId);
  await db.query(
    `UPDATE users
     SET phone_hash = COALESCE(phone_hash, $2),
         device_id_hash = COALESCE($3, device_id_hash),
         duplicate_signal = COALESCE(duplicate_signal, '{}'::jsonb) || $4::jsonb,
         updated_at = NOW()
     WHERE user_id=$1`,
    [
      userId,
      phoneHash,
      deviceHash,
      JSON.stringify({
        phoneHashPresent: Boolean(phoneHash),
        deviceHashPresent: Boolean(deviceHash),
        lastSignalAt: new Date().toISOString()
      })
    ]
  );
};
exports.findReferralCode = async (code) => {
  const db = await getDB();
  const r = await db.query(
    `SELECT *
     FROM referral_codes
     WHERE code=$1
       AND is_active=true
       AND (expires_at IS NULL OR expires_at > NOW())
     LIMIT 1`,
    [code]
  );
  return r.rows[0] || null;
};
exports.recordReferralRedemption = async ({ referralCodeId, referredUserId, referrerUserId, metadata }) => {
  const db = await getDB();
  await db.query(
    `INSERT INTO referral_redemptions (referral_code_id, referred_user_id, referrer_user_id, metadata)
     VALUES ($1,$2,$3,$4::jsonb)
     ON CONFLICT (referred_user_id)
     DO NOTHING`,
    [referralCodeId, referredUserId, referrerUserId || null, JSON.stringify(metadata || {})]
  );
};

exports.recordSignupConsent = async (userId, { method = 'unknown', ipAddress = null, userAgent = null } = {}) => {
  const db = await getDB();
  await db.query(
    `INSERT INTO consent_events (
       consent_event_id,
       user_id,
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
     VALUES ($1,$2,'signup_terms','granted',$3,$4,$5::jsonb,'auth-service',$6,$7,NOW())`,
    [
      randomUUID(),
      userId,
      'Accepted SoulMatch Terms and Privacy Policy during account creation.',
      LEGAL_NOTICE_VERSION,
      JSON.stringify({ method }),
      ipAddress,
      userAgent
    ]
  );
};
