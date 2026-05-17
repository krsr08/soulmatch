const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const controllerSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'controllers', 'adminController.js'),
  'utf8'
);
const middlewareSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'middleware', 'adminAuthMiddleware.js'),
  'utf8'
);
const routesSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'routes', 'adminRoutes.js'),
  'utf8'
);
const schemaSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'services', 'adminSchema.js'),
  'utf8'
);
const realtimeSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'realtime', 'adminRealtime.js'),
  'utf8'
);

test('admin sessions use issuer/audience checked httpOnly cookie support', () => {
  assert.match(controllerSource, /res\.cookie\('soulmatch_admin_session'/);
  assert.match(controllerSource, /httpOnly:\s*true/);
  assert.match(controllerSource, /issuer:\s*process\.env\.ADMIN_JWT_ISSUER/);
  assert.match(controllerSource, /audience:\s*process\.env\.ADMIN_JWT_AUDIENCE/);
  assert.match(middlewareSource, /readCookie\(req\.headers\.cookie,\s*'soulmatch_admin_session'\)/);
  assert.match(middlewareSource, /jwt\.verify\(token,\s*process\.env\.ADMIN_JWT_SECRET\|\|process\.env\.JWT_SECRET,\s*adminVerifyOptions\(\)\)/);
});

test('admin login has lockout, optional TOTP, and logout route', () => {
  assert.match(controllerSource, /ADMIN_LOCKOUT_LIMIT/);
  assert.match(controllerSource, /ADMIN_MFA_TOTP_SECRET/);
  assert.match(controllerSource, /verifyTotpCode/);
  assert.match(routesSource, /router\.post\('\/logout', authenticateAdmin, controller\.adminLogout\)/);
  assert.match(controllerSource, /admin\.logout/);
});

test('admin schema includes role and permission tables', () => {
  assert.match(schemaSource, /CREATE TABLE IF NOT EXISTS admin_roles/);
  assert.match(schemaSource, /CREATE TABLE IF NOT EXISTS admin_role_permissions/);
  assert.match(middlewareSource, /exports\.requirePermission/);
});

test('admin realtime socket no longer allows wildcard production origin', () => {
  assert.match(realtimeSource, /realtimeCorsOrigins/);
  assert.doesNotMatch(realtimeSource, /cors:\s*\{\s*origin:\s*'\*'/);
  assert.match(realtimeSource, /jwt\.verify\(token,\s*getAdminSecret\(\),\s*adminVerifyOptions\(\)\)/);
});
