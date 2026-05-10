const crypto = require('crypto');

function timingSafeEquals(left, right) {
  const a = Buffer.from(String(left || ''));
  const b = Buffer.from(String(right || ''));
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

exports.authenticateService = (req, res, next) => {
  const expected = process.env.INTERNAL_SERVICE_SECRET;
  const provided = req.headers['x-internal-service-secret'];
  if (!expected || !provided || !timingSafeEquals(provided, expected)) {
    return res.status(401).json({
      success: false,
      error: {
        code: 'SERVICE_AUTH_REQUIRED',
        message: 'Internal service authentication is required.'
      }
    });
  }
  return next();
};
