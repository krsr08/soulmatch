const twilio = require('twilio');
const crypto = require('crypto');
const { getRedis } = require('../config/redis');
const logger = require('../utils/logger');
const OTP_TTL = 300; const LOCK_TTL = 1800; const MAX_ATTEMPTS = 3; const SEND_WINDOW_TTL = 3600; const MAX_SENDS = parseInt(process.env.OTP_SEND_LIMIT || '5', 10);
const OTP_PEPPER = process.env.OTP_HASH_PEPPER || process.env.JWT_SECRET || 'local-otp-pepper';
const K = { otp: p => 'otp:' + p, attempts: p => 'otp_attempts:' + p, lock: p => 'otp_lock:' + p, sends: p => 'otp_sends:' + p };
let twilioClient = undefined;
let twilioConfigWarningShown = false;

function hasUsableTwilioConfig() {
  return Boolean(
    process.env.TWILIO_ACCOUNT_SID &&
    process.env.TWILIO_ACCOUNT_SID.startsWith('AC') &&
    process.env.TWILIO_AUTH_TOKEN &&
    process.env.TWILIO_PHONE_NUMBER
  );
}

function getTwilioClient() {
  if (twilioClient !== undefined) return twilioClient;
  if (!hasUsableTwilioConfig()) {
    if (!twilioConfigWarningShown) {
      logger.warn('Twilio not fully configured. Falling back to non-delivery mode for OTP SMS.');
      twilioConfigWarningShown = true;
    }
    twilioClient = null;
    return twilioClient;
  }
  try {
    twilioClient = twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
  } catch (error) {
    logger.warn('Twilio client could not be initialized: ' + error.message);
    twilioClient = null;
  }
  return twilioClient;
}

function hashOtp(phone, otp) {
  return crypto.createHmac('sha256', OTP_PEPPER).update(`${phone}:${otp}`).digest('hex');
}

exports.generateOTP = () => process.env.MOCK_OTP === 'true' ? (process.env.MOCK_OTP_VALUE || '123456') : String(crypto.randomInt(100000, 1000000));
exports.storeOTP = async (phone, otp) => { const r = await getRedis(); await r.set(K.otp(phone), hashOtp(phone, otp), { EX: OTP_TTL }); };
exports.verifyStoredOTP = async (phone, otp) => {
  const r = await getRedis();
  const stored = await r.get(K.otp(phone));
  if (!stored) return false;
  return crypto.timingSafeEqual(Buffer.from(stored), Buffer.from(hashOtp(phone, otp)));
};
exports.hasOTP = async (phone) => { const r = await getRedis(); return !!(await r.get(K.otp(phone))); };
exports.clearOTP = async (phone) => { const r = await getRedis(); await r.del(K.otp(phone)); };
exports.canSendOTP = async (phone) => {
  const r = await getRedis();
  const count = parseInt((await r.get(K.sends(phone))) || '0', 10);
  return count < MAX_SENDS;
};
exports.incrementSendCount = async (phone) => {
  const r = await getRedis();
  const count = await r.incr(K.sends(phone));
  if (count === 1) await r.expire(K.sends(phone), SEND_WINDOW_TTL);
  return count;
};
exports.getSendTTL = async (phone) => { const r = await getRedis(); return r.ttl(K.sends(phone)); };
exports.getAttempts = async (phone) => { const r = await getRedis(); const v = await r.get(K.attempts(phone)); return v ? parseInt(v) : 0; };
exports.incrementAttempts = async (phone) => {
  const r = await getRedis();
  const count = await r.incr(K.attempts(phone));
  await r.expire(K.attempts(phone), LOCK_TTL);
  if (count >= MAX_ATTEMPTS) await r.set(K.lock(phone), '1', { EX: LOCK_TTL });
};
exports.clearAttempts = async (phone) => { const r = await getRedis(); await r.del(K.attempts(phone)); await r.del(K.lock(phone)); };
exports.isLocked = async (phone) => { const r = await getRedis(); return !!(await r.get(K.lock(phone))); };
exports.getLockTTL = async (phone) => { const r = await getRedis(); return r.ttl(K.lock(phone)); };
exports.sendSMS = async (phone, message) => {
  if (process.env.MOCK_OTP === 'true' || process.env.NODE_ENV === 'test') {
    logger.info('[MOCK SMS] OTP delivery simulated for ' + phone);
    return { delivered: true, mode: 'mock' };
  }
  const client = getTwilioClient();
  if (!client) {
    logger.warn('OTP delivery unavailable for ' + phone + '. Twilio is not configured.');
    return { delivered: false, mode: 'unavailable' };
  }
  await client.messages.create({ body: message, from: process.env.TWILIO_PHONE_NUMBER, to: phone });
  return { delivered: true, mode: 'twilio' };
};
