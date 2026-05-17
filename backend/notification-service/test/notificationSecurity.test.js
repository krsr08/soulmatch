const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const routesSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'routes', 'notificationRoutes.js'),
  'utf8'
);
const controllerSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'controllers', 'notificationController.js'),
  'utf8'
);
const outboxSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'services', 'notificationOutboxService.js'),
  'utf8'
);

test('internal send endpoints require service authentication', () => {
  assert.match(routesSource, /router\.post\('\/send', authenticateService, controller\.sendPush\)/);
  assert.match(routesSource, /router\.post\('\/template', authenticateService, controller\.sendTemplate\)/);
});

test('notification inbox exposes read and device registration routes for signed-in users', () => {
  assert.match(routesSource, /router\.get\('\/', authenticate, controller\.getNotifications\)/);
  assert.match(routesSource, /router\.put\('\/:id\/read', authenticate, controller\.markRead\)/);
  assert.match(routesSource, /router\.post\('\/devices\/fcm-token', authenticate, controller\.registerFcmToken\)/);
});

test('failed invalid FCM tokens are pruned from the user record', () => {
  assert.match(outboxSource, /function isInvalidFcmTokenError/);
  assert.match(outboxSource, /UPDATE users SET fcm_token=NULL/);
});

test('push sends are queued through the outbox and failed events move to DLQ', () => {
  assert.match(controllerSource, /queueNotification/);
  assert.match(outboxSource, /INSERT INTO outbox_events/);
  assert.match(outboxSource, /INSERT INTO notification_dlq/);
});
