const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const source = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'controllers', 'publicController.js'),
  'utf8'
);

test('public analytics uses an event allowlist and rate limiter', () => {
  assert.match(source, /const ANALYTICS_EVENT_TYPES = new Set/);
  assert.match(source, /isPublicAnalyticsRateLimited\(req\)/);
  assert.match(source, /status\(429\)/);
});

test('public analytics caps payload size and rejects unsupported events', () => {
  assert.match(source, /PUBLIC_ANALYTICS_MAX_PAYLOAD_BYTES/);
  assert.match(source, /PUBLIC_ANALYTICS_MAX_BATCH/);
  assert.match(source, /status\(413\)/);
  assert.match(source, /!ANALYTICS_EVENT_TYPES\.has\(event\.eventType\)/);
});

test('public analytics does not trust caller supplied user ids', () => {
  assert.match(source, /userId:\s*null/);
  assert.doesNotMatch(source, /userId:\s*body\.userId/);
});
