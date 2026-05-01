const logger = require('../utils/logger');
const ErrorCodes = { VALIDATION_ERROR:'VALIDATION_ERROR', OTP_EXPIRED:'OTP_EXPIRED', OTP_INVALID:'OTP_INVALID', OTP_LOCKED:'OTP_LOCKED', UNAUTHORIZED:'UNAUTHORIZED', FORBIDDEN:'FORBIDDEN', NOT_FOUND:'NOT_FOUND', SERVICE_UNAVAILABLE:'SERVICE_UNAVAILABLE', INTERNAL_ERROR:'INTERNAL_ERROR' };
class AppError extends Error { constructor(statusCode, errorCode, message) { super(message); this.statusCode = statusCode; this.errorCode = errorCode; } }
const errorHandler = (err, req, res, next) => {
  logger.error(err.message);
  if (err instanceof AppError) return res.status(err.statusCode).json({ success: false, error: { code: err.errorCode, message: err.message } });
  res.status(500).json({ success: false, error: { code: 'INTERNAL_ERROR', message: 'Unexpected error' } });
};
module.exports = { errorHandler, AppError, ErrorCodes };
