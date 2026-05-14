const assert = require('node:assert/strict');
const test = require('node:test');

const { DEFAULT_CONFIG, getPlanById, getPublicRuntimeConfig } = require('../shared/controlPlane');

test('default upgrade packages expose canonical paid plan ids', () => {
  const packages = DEFAULT_CONFIG.monetization.upgradePackageGroups.flatMap((group) => group.packages);
  assert.deepEqual(
    packages.map((pkg) => pkg.planId),
    ['silver', 'gold', 'platinum']
  );
});

test('getPlanById resolves package ids to canonical subscription plans', () => {
  const plan = getPlanById(DEFAULT_CONFIG.monetization, '101');
  assert.equal(plan.planId, 'silver');
  assert.equal(plan.price, 499);
  assert.equal(plan.durationDays, 30);
});

test('getPlanById resolves canonical plan ids directly', () => {
  const plan = getPlanById(DEFAULT_CONFIG.monetization, 'gold');
  assert.equal(plan.planId, 'gold');
  assert.equal(plan.price, 2499);
});

test('runtime config includes upgrade packages for Android checkout', () => {
  const config = getPublicRuntimeConfig(DEFAULT_CONFIG);
  assert.ok(config.monetization.upgradePackageGroups.length >= 3);
  assert.equal(config.monetization.upgradePackageGroups[0].packages[0].planId, 'silver');
  assert.ok(config.monetization.membershipFeatureMatrix.length >= 4);
});
