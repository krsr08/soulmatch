const logger = require('../utils/logger');

const ErrorCodes = {
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  NOT_FOUND: 'NOT_FOUND',
  FORBIDDEN: 'FORBIDDEN',
  AUTH_REQUIRED: 'AUTH_REQUIRED',
  INVALID_TOKEN: 'INVALID_TOKEN',
  INTERNAL_ERROR: 'INTERNAL_ERROR'
};

class AppError extends Error {
  constructor(statusCode, errorCode, message) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode;
  }
}

function notFoundHandler(req, res) {
  res.status(404).json({
    success: false,
    error: {
      code: ErrorCodes.NOT_FOUND,
      message: 'The requested profile endpoint was not found.'
    }
  });
}

function errorHandler(err, req, res, next) {
  logger.error(err.stack || err.message);
  if (err instanceof AppError) {
    return res.status(err.statusCode).json({
      success: false,
      error: {
        code: err.errorCode,
        message: err.message
      }
    });
  }
  if (err.name === 'MulterError' && err.code === 'LIMIT_FILE_SIZE') {
    return res.status(400).json({
      success: false,
      error: {
        code: ErrorCodes.VALIDATION_ERROR,
        message: 'Each photo must be 5 MB or smaller.'
      }
    });
  }
  if (err.message === 'JPG/PNG/WebP only') {
    return res.status(400).json({
      success: false,
      error: {
        code: ErrorCodes.VALIDATION_ERROR,
        message: 'Only JPG, PNG, or WebP images are supported.'
      }
    });
  }
  if (err.message === 'JPG/PNG/WebP/PDF only' || /Uploaded (image|document)|MIME type/.test(err.message || '')) {
    return res.status(400).json({
      success: false,
      error: {
        code: ErrorCodes.VALIDATION_ERROR,
        message: err.message === 'JPG/PNG/WebP/PDF only'
          ? 'Only JPG, PNG, WebP, or PDF documents are supported.'
          : err.message
      }
    });
  }
  res.status(500).json({
    success: false,
    error: {
      code: ErrorCodes.INTERNAL_ERROR,
      message: 'We could not save the profile changes right now. Please try again shortly.'
    }
  });
}

module.exports = { errorHandler, notFoundHandler, AppError, ErrorCodes };
