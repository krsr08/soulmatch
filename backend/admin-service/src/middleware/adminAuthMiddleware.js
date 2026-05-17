const jwt = require('jsonwebtoken');

function adminVerifyOptions() {
  return {
    issuer: process.env.ADMIN_JWT_ISSUER || 'soulmatch-admin',
    audience: process.env.ADMIN_JWT_AUDIENCE || 'soulmatch-admin-api'
  };
}

function readCookie(header, name) {
  return String(header || '')
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.split('='))
    .find(([key]) => key === name)?.slice(1).join('=') || null;
}

function getToken(req) {
  const h = req.headers['authorization'];
  if (h && h.startsWith('Bearer ')) return h.split(' ')[1];
  return readCookie(req.headers.cookie, 'soulmatch_admin_session');
}

exports.authenticateAdmin = (req, res, next) => {
  const token = getToken(req);
  if (!token) {
    return res.status(401).json({
      success: false,
      error: {
        code: 'ADMIN_AUTH_REQUIRED',
        message: 'Admin sign-in is required to access this route.'
      }
    });
  }
  try {
    const d = jwt.verify(token, process.env.ADMIN_JWT_SECRET||process.env.JWT_SECRET, adminVerifyOptions());
    if (!d.role) {
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

exports.requirePermission = (permission) => (req, res, next) => {
  const permissions = Array.isArray(req.admin?.permissions) ? req.admin.permissions : [];
  if (permissions.includes('*') || permissions.includes(permission)) return next();
  return res.status(403).json({
    success: false,
    error: {
      code: 'FORBIDDEN',
      message: 'Your admin role does not allow this action.'
    }
  });
};

exports.authorizeAdminRoles = (...roles) => (req, res, next) => {
  const role = req.admin?.role;
  const permissions = Array.isArray(req.admin?.permissions) ? req.admin.permissions : [];
  if (!role || (roles.length && !roles.includes(role) && !permissions.includes('*'))) {
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
