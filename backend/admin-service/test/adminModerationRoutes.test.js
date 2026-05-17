const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');

const controllerSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'adminController.js'), 'utf8');
const routesSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'routes', 'adminRoutes.js'), 'utf8');
const healthSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'services', 'serviceHealth.js'), 'utf8');

test('admin exposes a unified moderation inbox route', () => {
  assert.match(routesSource, /\/moderation\/inbox/);
  assert.match(controllerSource, /exports\.getModerationInbox/);
  assert.match(controllerSource, /chat_message_reports/);
  assert.match(controllerSource, /profile_photos/);
  assert.match(controllerSource, /verifications/);
  assert.match(controllerSource, /ORDER BY severity_score DESC, created_at ASC/);
});

test('service health prefers gateway aggregate health when configured', () => {
  assert.match(healthSource, /GATEWAY_HEALTH_URL/);
  assert.match(healthSource, /API_GATEWAY_URL/);
  assert.match(healthSource, /body\.services/);
});
