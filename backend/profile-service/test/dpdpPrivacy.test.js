const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert/strict');

const root = path.join(__dirname, '..', '..', '..');
const controllerSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'profileController.js'), 'utf8');
const repoSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'repositories', 'profileRepository.js'), 'utf8');
const routesSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'routes', 'profileRoutes.js'), 'utf8');
const migrationSource = fs.readFileSync(path.join(root, 'database', 'migrations', '036_dpdp_export_delete.sql'), 'utf8');

test('profile service exposes authenticated DPDP export and delete routes before profile id route', () => {
  const exportIndex = routesSource.indexOf("router.get('/export'");
  const deleteIndex = routesSource.indexOf("router.post('/delete-account'");
  const profileIndex = routesSource.indexOf("router.get('/:profileId'");

  assert.ok(exportIndex > -1, 'export route missing');
  assert.ok(deleteIndex > -1, 'delete route missing');
  assert.ok(profileIndex > -1, 'profile route missing');
  assert.ok(exportIndex < profileIndex, 'export route must be registered before /:profileId');
  assert.ok(deleteIndex < profileIndex, 'delete route must be registered before /:profileId');
  assert.match(controllerSource, /exports\.exportMyData/);
  assert.match(controllerSource, /exports\.deleteMyAccount/);
});

test('account deletion anonymizes PII and withdraws consent instead of hard deleting history', () => {
  assert.match(repoSource, /exports\.deleteAccount/);
  assert.match(repoSource, /consentType:\s*'account_deletion'/);
  assert.match(repoSource, /phone=NULL/);
  assert.match(repoSource, /email=NULL/);
  assert.match(repoSource, /google_id=NULL/);
  assert.match(repoSource, /is_active=false/);
  assert.match(repoSource, /profile_visibility='hidden'/);
  assert.match(repoSource, /admin_status='deleted'/);
});

test('DPDP migration expands consent ledger and adds deletion metadata', () => {
  assert.match(migrationSource, /ALTER TABLE users[\s\S]*deleted_at/);
  assert.match(migrationSource, /ALTER TABLE profiles[\s\S]*consent_notice_version/);
  assert.match(migrationSource, /'signup_terms'/);
  assert.match(migrationSource, /'data_export'/);
  assert.match(migrationSource, /'account_deletion'/);
  assert.match(migrationSource, /'agent_terms_acceptance'/);
});
