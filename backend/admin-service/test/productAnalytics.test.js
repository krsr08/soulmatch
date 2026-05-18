const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const test = require('node:test');

const adminSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'adminController.js'), 'utf8');
const publicSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'publicController.js'), 'utf8');
const sharedSource = fs.readFileSync(path.join(__dirname, '..', '..', 'shared', 'controlPlane.js'), 'utf8');
const androidAnalyticsSource = fs.readFileSync(
  path.join(__dirname, '..', '..', '..', 'android', 'app', 'src', 'main', 'java', 'com', 'soulmatch', 'app', 'ui', 'viewmodels', 'AnalyticsViewModel.kt'),
  'utf8'
);

test('admin analytics funnel is backed by production tables', () => {
  const funnelFunction = adminSource.slice(
    adminSource.indexOf('exports.getAnalyticsFunnel'),
    adminSource.indexOf('exports.getAnalyticsEvents')
  );
  assert.match(funnelFunction, /users\.created_at/);
  assert.match(funnelFunction, /profiles\.is_published/);
  assert.match(funnelFunction, /interests\.sent_at/);
  assert.match(funnelFunction, /chat_conversation_metadata\.created_at/);
  assert.match(funnelFunction, /transactions/);
  assert.match(funnelFunction, /payment_orders/);
  assert.match(funnelFunction, /subscriptions/);
  assert.doesNotMatch(funnelFunction, /respondDegraded/);
});

test('server analytics events are explicitly signed in shared control plane', () => {
  assert.match(sharedSource, /async function recordServerAnalyticsEvent/);
  assert.match(sharedSource, /serverSigned:\s*true/);
  assert.match(sharedSource, /schemaVersion:\s*'2026-05-17'/);
});

test('public analytics accepts bounded batches while ignoring user identity', () => {
  assert.match(publicSource, /const PUBLIC_ANALYTICS_MAX_BATCH/);
  assert.match(publicSource, /Array\.isArray\(body\.events\)/);
  assert.match(publicSource, /userId:\s*null/);
});

test('android analytics batches client events before sending', () => {
  assert.match(androidAnalyticsSource, /pendingEvents/);
  assert.match(androidAnalyticsSource, /trackAnalyticsBatch/);
  assert.match(androidAnalyticsSource, /AnalyticsBatchRequest/);
  assert.match(androidAnalyticsSource, /FLUSH_DELAY_MS/);
});
