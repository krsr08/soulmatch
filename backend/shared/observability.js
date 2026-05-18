const crypto = require('crypto');

const DEFAULT_BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10];

function normalizeRoute(req) {
  const baseUrl = req.baseUrl || '';
  const routePath = req.route?.path || req.path || req.originalUrl || '/';
  const raw = `${baseUrl}${routePath}` || '/';
  return raw
    .replace(/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/gi, ':uuid')
    .replace(/\b\d{4,}\b/g, ':id')
    .split('?')[0];
}

function labelValue(value) {
  return String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\n/g, ' ');
}

function metricLine(name, labels, value) {
  const labelText = Object.entries(labels || {})
    .map(([key, val]) => `${key}="${labelValue(val)}"`)
    .join(',');
  return `${name}{${labelText}} ${value}`;
}

function createHttpMetrics(serviceName) {
  const requests = new Map();
  const durations = new Map();

  function key(labels) {
    return JSON.stringify(labels);
  }

  function observe(labels, seconds) {
    const requestKey = key(labels);
    requests.set(requestKey, (requests.get(requestKey) || 0) + 1);
    const existing = durations.get(requestKey) || {
      labels,
      count: 0,
      sum: 0,
      buckets: DEFAULT_BUCKETS.map((le) => ({ le, count: 0 }))
    };
    existing.count += 1;
    existing.sum += seconds;
    existing.buckets.forEach((bucket) => {
      if (seconds <= bucket.le) bucket.count += 1;
    });
    durations.set(requestKey, existing);
  }

  function render() {
    const lines = [
      '# HELP soulmatch_http_requests_total Total HTTP requests by service, method, route, and status.',
      '# TYPE soulmatch_http_requests_total counter'
    ];
    for (const [requestKey, count] of requests.entries()) {
      lines.push(metricLine('soulmatch_http_requests_total', JSON.parse(requestKey), count));
    }
    lines.push(
      '# HELP soulmatch_http_request_duration_seconds HTTP request duration histogram.',
      '# TYPE soulmatch_http_request_duration_seconds histogram'
    );
    for (const metric of durations.values()) {
      metric.buckets.forEach((bucket) => {
        lines.push(metricLine('soulmatch_http_request_duration_seconds_bucket', { ...metric.labels, le: bucket.le }, bucket.count));
      });
      lines.push(metricLine('soulmatch_http_request_duration_seconds_bucket', { ...metric.labels, le: '+Inf' }, metric.count));
      lines.push(metricLine('soulmatch_http_request_duration_seconds_sum', metric.labels, metric.sum.toFixed(6)));
      lines.push(metricLine('soulmatch_http_request_duration_seconds_count', metric.labels, metric.count));
    }
    lines.push(
      '# HELP soulmatch_process_uptime_seconds Node.js process uptime in seconds.',
      '# TYPE soulmatch_process_uptime_seconds gauge',
      metricLine('soulmatch_process_uptime_seconds', { service: serviceName }, process.uptime().toFixed(0))
    );
    return lines.join('\n') + '\n';
  }

  return { observe, render };
}

function requestId(req) {
  return req.headers['x-request-id'] || req.headers['x-correlation-id'] || crypto.randomUUID();
}

function jsonRequestLog(payload) {
  process.stdout.write(`${JSON.stringify({ level: 'info', type: 'http_request', ...payload })}\n`);
}

function installExpressObservability(app, { serviceName, extraMetrics } = {}) {
  const service = serviceName || process.env.SERVICE_NAME || 'soulmatch-service';
  const metrics = createHttpMetrics(service);

  app.use((req, res, next) => {
    const id = requestId(req);
    req.requestId = id;
    res.setHeader('x-request-id', id);
    const startedAt = process.hrtime.bigint();
    res.on('finish', () => {
      const durationSeconds = Number(process.hrtime.bigint() - startedAt) / 1e9;
      const route = normalizeRoute(req);
      const statusClass = `${Math.floor(res.statusCode / 100)}xx`;
      metrics.observe({
        service,
        method: req.method,
        route,
        status: String(res.statusCode),
        status_class: statusClass
      }, durationSeconds);
      jsonRequestLog({
        service,
        requestId: id,
        method: req.method,
        route,
        status: res.statusCode,
        durationMs: Math.round(durationSeconds * 1000),
        ip: req.ip || req.socket?.remoteAddress || null
      });
    });
    next();
  });

  app.get('/metrics', async (req, res, next) => {
    try {
      const extras = typeof extraMetrics === 'function' ? await extraMetrics() : '';
      res.type('text/plain').send(metrics.render() + (extras ? `${extras.trim()}\n` : ''));
    } catch (error) {
      next(error);
    }
  });
}

module.exports = {
  installExpressObservability,
  metricLine
};
