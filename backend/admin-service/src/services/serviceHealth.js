const fs = require('fs');

const SERVICES = [
  { key: 'admin', label: 'Admin', envKey: 'ADMIN_SERVICE_URL', dockerUrl: 'http://admin-service:3011/health', localUrl: 'http://localhost:3011/health' },
  { key: 'auth', label: 'Auth', envKey: 'AUTH_SERVICE_URL', dockerUrl: 'http://auth-service:3001/health', localUrl: 'http://localhost:3001/health' },
  { key: 'profile', label: 'Profile', envKey: 'PROFILE_SERVICE_URL', dockerUrl: 'http://profile-service:3002/health', localUrl: 'http://localhost:3002/health' },
  { key: 'matching', label: 'Matching', envKey: 'MATCHING_SERVICE_URL', dockerUrl: 'http://matching-service:3003/health', localUrl: 'http://localhost:3003/health' },
  { key: 'search', label: 'Search', envKey: 'SEARCH_SERVICE_URL', dockerUrl: 'http://search-service:3004/health', localUrl: 'http://localhost:3004/health' },
  { key: 'chat', label: 'Chat', envKey: 'CHAT_SERVICE_URL', dockerUrl: 'http://chat-service:3005/health', localUrl: 'http://localhost:3005/health' },
  { key: 'notification', label: 'Notification', envKey: 'NOTIFICATION_SERVICE_URL', dockerUrl: 'http://notification-service:3006/health', localUrl: 'http://localhost:3006/health' },
  { key: 'payment', label: 'Payment', envKey: 'PAYMENT_SERVICE_URL', dockerUrl: 'http://payment-service:3007/health', localUrl: 'http://localhost:3007/health' }
];

function getServiceUrls(service) {
  const configuredUrl = process.env[service.envKey];
  if (configuredUrl) return [normalizeHealthUrl(configuredUrl)];
  const isDockerRuntime = fs.existsSync('/.dockerenv') || process.env.DOCKER_CONTAINER === 'true';
  return isDockerRuntime
    ? [service.dockerUrl, service.localUrl].filter(Boolean)
    : [service.localUrl].filter(Boolean);
}

function normalizeHealthUrl(value) {
  const url = String(value || '').trim();
  if (!url) return url;
  if (/\/health\/?$/.test(url) || /\/health\/services\/?$/.test(url)) return url;
  return `${url.replace(/\/$/, '')}/health`;
}

async function fetchHealth(url, service) {
  const startedAt = Date.now();
  let timeout;
  try {
    const controller = new AbortController();
    timeout = setTimeout(() => controller.abort(), 3000);
    const response = await fetch(url, { method: 'GET', signal: controller.signal });
    clearTimeout(timeout);
    const body = await response.json().catch(() => null);
    return {
      key: service.key,
      label: service.label,
      url,
      ok: response.ok,
      status: response.ok ? 'healthy' : 'down',
      httpStatus: response.status,
      latencyMs: Date.now() - startedAt,
      service: body?.service || service.key,
      body
    };
  } catch (error) {
    if (timeout) clearTimeout(timeout);
    return {
      key: service.key,
      label: service.label,
      url,
      ok: false,
      status: 'down',
      httpStatus: 0,
      latencyMs: Date.now() - startedAt,
      error: error.message
    };
  }
}

async function probeService(service) {
  if (service.key === 'admin') {
    return {
      key: service.key,
      label: service.label,
      url: service.localUrl,
      ok: true,
      status: 'healthy',
      httpStatus: 200,
      latencyMs: 0,
      service: 'admin-service',
      body: { status: 'ok', service: 'admin-service' },
      self: true
    };
  }
  const urls = getServiceUrls(service);
  const attempts = [];
  for (const url of urls) {
    const result = await fetchHealth(url, service);
    attempts.push(result);
    if (result.ok) return { ...result, attemptedUrls: urls };
  }
  return {
    key: service.key,
    label: service.label,
    url: attempts[0]?.url || urls[0],
    ok: false,
    status: 'down',
    httpStatus: attempts[0]?.httpStatus || 0,
    latencyMs: attempts.reduce((total, item) => total + (item.latencyMs || 0), 0),
    error: attempts.map(item => item.error || `HTTP ${item.httpStatus || item.status}`).join('; '),
    attemptedUrls: urls
  };
}

async function getServiceHealth() {
  const gatewayHealthUrl = process.env.GATEWAY_HEALTH_URL
    || (process.env.API_GATEWAY_URL ? `${process.env.API_GATEWAY_URL.replace(/\/$/, '')}/health/services` : null);
  if (gatewayHealthUrl) {
    const gateway = await fetchHealth(gatewayHealthUrl, { key: 'gateway', label: 'API Gateway' });
    if (gateway.ok && Array.isArray(gateway.body?.services)) {
      return gateway.body.services;
    }
  }
  return Promise.all(SERVICES.map(probeService));
}

module.exports = {
  SERVICES,
  getServiceHealth
};
