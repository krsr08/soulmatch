const jwt = require('jsonwebtoken');
const ADMIN_ROLES = ['admin', 'super_admin', 'moderator', 'support_agent', 'marketing_manager'];

exports.authenticateAdmin = (req, res, next) => {
  const h = req.headers['authorization'];
  if (!h || !h.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: {
        code: 'ADMIN_AUTH_REQUIRED',
        message: 'Admin sign-in is required to access this route.'
      }
    });
  }
  try {
    const d = jwt.verify(h.split(' ')[1], process.env.ADMIN_JWT_SECRET||process.env.JWT_SECRET);
    if (!ADMIN_ROLES.includes(d.role)) {
      return res.status(403).json({
        success: false,
        error: {
          code: 'FORBIDDEN',
          message: 'This action requires admin privileges.'
        }
      });
    }
    req.admin = d;
    next();
  } catch {
    res.status(401).json({
      success: false,
      error: {
        code: 'INVALID_ADMIN_TOKEN',
        message: 'Your admin session is invalid or has expired. Please sign in again.'
      }
    });
  }
};

exports.authorizeAdminRoles = (...roles) => (req, res, next) => {
  const role = req.admin?.role;
  if (!role || (roles.length && !roles.includes(role))) {
    return res.status(403).json({
      success: false,
      error: {
        code: 'FORBIDDEN',
        message: 'Your admin role does not allow this action.'
      }
    });
  }
  return next();
};
