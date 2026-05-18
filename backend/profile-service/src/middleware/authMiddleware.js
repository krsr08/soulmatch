const jwt = require('jsonwebtoken');
const verifyOptions = () => ({
  issuer: process.env.JWT_ISSUER || 'soulmatch-auth',
  audience: process.env.JWT_AUDIENCE || 'soulmatch-api',
  clockTolerance: Number(process.env.JWT_CLOCK_TOLERANCE_SECONDS || 30)
});

exports.authenticate = (req, res, next) => {
  const h = req.headers['authorization'];
  if (!h || !h.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: {
        code: 'AUTH_REQUIRED',
        message: 'Sign in to manage your profile.'
      }
    });
  }
  try {
    req.user = jwt.verify(h.split(' ')[1], process.env.JWT_SECRET, verifyOptions());
    next();
  } catch {
    res.status(401).json({
      success: false,
      error: {
        code: 'INVALID_TOKEN',
        message: 'Your session is invalid or has expired. Please sign in again.'
      }
    });
  }
};
