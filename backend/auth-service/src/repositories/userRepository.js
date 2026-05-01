const { getDB } = require('../config/database');
const { randomUUID } = require('crypto');
exports.findByPhone = async (phone) => { const db = await getDB(); const r = await db.query('SELECT * FROM users WHERE phone=$1 AND is_active=true LIMIT 1', [phone]); return r.rows[0] || null; };
exports.findByGoogleId = async (googleId) => { const db = await getDB(); const r = await db.query('SELECT * FROM users WHERE google_id=$1 AND is_active=true LIMIT 1', [googleId]); return r.rows[0] || null; };
exports.findById = async (userId) => { const db = await getDB(); const r = await db.query('SELECT * FROM users WHERE user_id=$1 LIMIT 1', [userId]); return r.rows[0] || null; };
exports.create = async (data) => {
  const db = await getDB();
  const r = await db.query(
    `INSERT INTO users (
       user_id, phone, email, google_id, is_verified, referred_by_code, acquisition_source, referred_at
     ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,
    [
      randomUUID(),
      data.phone || null,
      data.email || null,
      data.google_id || null,
      data.is_verified || false,
      data.referred_by_code || null,
      data.acquisition_source || null,
      data.referred_at || null
    ]
  );
  return r.rows[0];
};
exports.updateLastLogin = async (userId) => { const db = await getDB(); await db.query('UPDATE users SET last_login=NOW() WHERE user_id=$1', [userId]); };
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
