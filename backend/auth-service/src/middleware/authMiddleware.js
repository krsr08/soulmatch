const tokenService = require('../services/tokenService');
const { AppError, ErrorCodes } = require('./errorHandler');
exports.authenticate = (req, res, next) => {
  const h = req.headers['authorization'];
  if (!h || !h.startsWith('Bearer ')) return next(new AppError(401, ErrorCodes.UNAUTHORIZED, 'Authorization header missing'));
  try { req.user = tokenService.verifyAccess(h.split(' ')[1]); next(); } catch (err) { next(err); }
};
