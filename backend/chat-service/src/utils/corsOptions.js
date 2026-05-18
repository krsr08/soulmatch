function parseOrigins() {
  const raw = process.env.CORS_ORIGINS || process.env.ALLOWED_ORIGINS || '';
  return raw.split(',').map((origin) => origin.trim()).filter(Boolean);
}

function isLocalDevOrigin(origin) {
  return /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(origin || '');
}

function buildCorsOptions() {
  const allowedOrigins = parseOrigins();
  const isProduction = process.env.NODE_ENV === 'production';
  return {
    origin(origin, callback) {
      if (!origin) return callback(null, true);
      if (allowedOrigins.includes(origin)) return callback(null, true);
      if (!isProduction && (allowedOrigins.length === 0 || isLocalDevOrigin(origin))) return callback(null, true);
      return callback(new Error('CORS origin is not allowed'));
    },
    credentials: true
  };
}

function socketCorsOptions() {
  const allowedOrigins = parseOrigins();
  if (allowedOrigins.length === 0 && process.env.NODE_ENV !== 'production') {
    return { origin: [/^http:\/\/localhost:\d+$/, /^http:\/\/127\.0\.0\.1:\d+$/], credentials: true };
  }
  return { origin: allowedOrigins, credentials: true };
}

module.exports = { buildCorsOptions, parseOrigins, socketCorsOptions };
