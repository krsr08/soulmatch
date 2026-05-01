const logger = require('../utils/logger');

function notFoundHandler(req, res) {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: 'The requested admin endpoint was not found.'
    }
  });
}

function errorHandler(err, req, res, next) {
  logger.error(err.stack || err.message);
  res.status(err.statusCode || 500).json({
    success: false,
    error: {
      code: err.errorCode || 'INTERNAL_ERROR',
      message: err.statusCode && err.statusCode < 500
        ? err.message
        : 'We could not complete the admin request right now. Please try again shortly.'
    }
  });
}

module.exports = {
  errorHandler,
  notFoundHandler
};
