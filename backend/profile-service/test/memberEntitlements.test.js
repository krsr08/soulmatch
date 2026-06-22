const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');
const { getEntitlements, normalizeEntitlements } = require('../../shared/memberEntitlements');

const controllerSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'profileController.js'), 'utf8');
const repoSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'repositories', 'profileRepository.js'), 'utf8');
const routesSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'routes', 'profileRoutes.js'), 'utf8');

test('canonical entitlements keep Bronze blocked from premium APIs', () => {
  const bronze = getEntitlements({}, 'bronze');
  assert.equal(bronze.chat, false);
  assert.equal(bronze.contactDetails, 0);
  assert.equal(bronze.matchAssistance, false);
  assert.equal(bronze.engagePlus, false);
  assert.equal(bronze.spotlightBoosts, 0);
  const gold = getEntitlements({}, 'gold');
  assert.equal(gold.chat, true);
  assert.equal(gold.matchAssistance, true);
  assert.equal(gold.spotlightBoosts, 1);
});

test('entitlement config string false values stay disabled', () => {
  const entitlements = normalizeEntitlements({
    planId: 'gold',
    engagePlus: 'false',
    matchAssistance: '0',
    chat: 'off',
    verifiedOnly: 'disabled'
  });
  assert.equal(entitlements.engagePlus, false);
  assert.equal(entitlements.matchAssistance, false);
  assert.equal(entitlements.chat, false);
  assert.equal(entitlements.verifiedOnly, false);
});

test('profile premium routes enforce server-side feature gates', () => {
  assert.match(controllerSource, /requireMemberFeature\(req,\s*'matchAssistance'/);
  assert.match(controllerSource, /requireMemberFeature\(req,\s*'engagePlus'/);
  assert.match(controllerSource, /exports\.activateSpotlight/);
  assert.match(repoSource, /consumeMeter\(db,\s*\{[\s\S]*eventType:\s*'spotlight'/);
  assert.match(repoSource, /profile_spotlight_boosts/);
  assert.match(routesSource, /\/:profileId\/spotlight\/activate/);
});
