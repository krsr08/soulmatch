const jwt = require('jsonwebtoken');
const { OAuth2Client } = require('google-auth-library');
const { getRedis } = require('../config/redis');
const { AppError, ErrorCodes } = require('../middleware/errorHandler');
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const REFRESH_PREFIX = 'refresh:'; const REFRESH_TTL = 7 * 24 * 60 * 60;
exports.generatePair = (payload) => ({
  accessToken: jwt.sign(payload, process.env.JWT_SECRET, { expiresIn: process.env.JWT_EXPIRES_IN || '15m' }),
  refreshToken: jwt.sign(payload, process.env.REFRESH_TOKEN_SECRET, { expiresIn: '7d' })
});
exports.verifyAccess = (token) => { try { return jwt.verify(token, process.env.JWT_SECRET); } catch { throw new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid or expired token'); } };
exports.verifyRefresh = (token) => { try { return jwt.verify(token, process.env.REFRESH_TOKEN_SECRET); } catch { throw new AppError(401, ErrorCodes.UNAUTHORIZED, 'Invalid refresh token'); } };
exports.storeRefresh = async (userId, token) => { const r = await getRedis(); await r.set(REFRESH_PREFIX + userId, token, { EX: REFRESH_TTL }); };
exports.getRefresh = async (userId) => { const r = await getRedis(); return r.get(REFRESH_PREFIX + userId); };
exports.clearRefresh = async (userId) => { const r = await getRedis(); await r.del(REFRESH_PREFIX + userId); };
exports.verifyGoogleToken = async (token) => {
  const ticket = await googleClient.verifyIdToken({ idToken: token, audience: process.env.GOOGLE_CLIENT_ID });
  return ticket.getPayload();
};
