const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { OAuth2Client } = require('google-auth-library');
const { getRedis } = require('../config/redis');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const REFRESH_PREFIX = 'refresh:';
const REFRESH_REVOKED_PREFIX = 'refresh_revoked:';
const REFRESH_TTL = 7 * 24 * 60 * 60;
const ACCESS_ISSUER = process.env.JWT_ISSUER || 'soulmatch-auth';
const ACCESS_AUDIENCE = process.env.JWT_AUDIENCE || 'soulmatch-api';
const REFRESH_AUDIENCE = process.env.JWT_REFRESH_AUDIENCE || 'soulmatch-refresh';

function normalizePayload(payload = {}) {
  const userId = payload.userId || payload.sub;
  return {
    ...payload,
    sub: userId,
    userId,
    jti: crypto.randomUUID()
  };
}

exports.generatePair = (payload) => ({
  accessToken: jwt.sign(normalizePayload(payload), process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || '15m',
    issuer: ACCESS_ISSUER,
    audience: ACCESS_AUDIENCE
  }),
  refreshToken: jwt.sign(normalizePayload(payload), process.env.REFRESH_TOKEN_SECRET, {
    expiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '7d',
    issuer: ACCESS_ISSUER,
    audience: REFRESH_AUDIENCE
  })
});
exports.verifyAccess = (token) => {
  try {
    return jwt.verify(token, process.env.JWT_SECRET, { issuer: ACCESS_ISSUER, audience: ACCESS_AUDIENCE });
  } catch {
    throw new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid or expired token');
  }
};
exports.verifyRefresh = (token) => {
  try {
    return jwt.verify(token, process.env.REFRESH_TOKEN_SECRET, { issuer: ACCESS_ISSUER, audience: REFRESH_AUDIENCE });
  } catch {
    throw new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid refresh token');
  }
};
exports.storeRefresh = async (userId, token) => {
  const r = await getRedis();
  const current = await r.get(REFRESH_PREFIX + userId);
  if (current && current !== token) {
    await exports.revokeRefresh(current);
  }
  await r.set(REFRESH_PREFIX + userId, token, { EX: REFRESH_TTL });
};
exports.getRefresh = async (userId) => { const r = await getRedis(); return r.get(REFRESH_PREFIX + userId); };
exports.revokeRefresh = async (tokenOrDecoded) => {
  const decoded = typeof tokenOrDecoded === 'string' ? exports.verifyRefresh(tokenOrDecoded) : tokenOrDecoded;
  if (!decoded?.jti) return;
  const r = await getRedis();
  const ttl = Math.max(1, decoded.exp ? decoded.exp - Math.floor(Date.now() / 1000) : REFRESH_TTL);
  await r.set(REFRESH_REVOKED_PREFIX + decoded.jti, '1', { EX: ttl });
};
exports.isRefreshRevoked = async (decoded) => {
  if (!decoded?.jti) return true;
  const r = await getRedis();
  return Boolean(await r.get(REFRESH_REVOKED_PREFIX + decoded.jti));
};
exports.clearRefresh = async (userId) => {
  const r = await getRedis();
  const current = await r.get(REFRESH_PREFIX + userId);
  if (current) {
    await exports.revokeRefresh(current);
  }
  await r.del(REFRESH_PREFIX + userId);
};
exports.verifyGoogleToken = async (token) => {
  const ticket = await googleClient.verifyIdToken({ idToken: token, audience: process.env.GOOGLE_CLIENT_ID });
  return ticket.getPayload();
};

exports._test = { normalizePayload, ACCESS_ISSUER, ACCESS_AUDIENCE, REFRESH_AUDIENCE, REFRESH_PREFIX, REFRESH_REVOKED_PREFIX };
