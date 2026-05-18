const SCRIPT_TAG_PATTERN = /<\s*script\b[^>]*>[\s\S]*?<\s*\/\s*script\s*>/gi;
const DANGEROUS_URI_PATTERN = /\bjavascript\s*:/gi;
const EVENT_HANDLER_PATTERN = /\son[a-z]+\s*=/gi;

function sanitizeValue(value) {
  if (typeof value === 'string') {
    return value
      .replace(SCRIPT_TAG_PATTERN, '')
      .replace(DANGEROUS_URI_PATTERN, '')
      .replace(EVENT_HANDLER_PATTERN, ' data-removed=');
  }
  if (Array.isArray(value)) return value.map(sanitizeValue);
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, val]) => [key, sanitizeValue(val)])
    );
  }
  return value;
}

exports.sanitizeAdminInput = (req, _res, next) => {
  if (req.body && typeof req.body === 'object') req.body = sanitizeValue(req.body);
  if (req.query && typeof req.query === 'object') req.query = sanitizeValue(req.query);
  next();
};

exports._test = { sanitizeValue };
