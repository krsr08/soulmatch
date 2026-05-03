const test = require('node:test');
const assert = require('node:assert/strict');

const { buildTrustSummary } = require('../src/repositories/profileRepository');

test('buildTrustSummary gives high score for verified complete low-risk profiles', () => {
  const summary = buildTrustSummary({
    is_verified: true,
    completion_score: 95,
    verification_status: 'verified',
    approved_verifications: 2,
    photo_count: 4,
    family_city: 'Hyderabad',
    profile_status: 'active',
    report_count: 0,
    last_login: new Date().toISOString()
  });

  assert.equal(summary.level, 'high');
  assert.ok(summary.score >= 80);
  assert.ok(summary.signals.includes('Admin verified profile'));
  assert.ok(summary.signals.includes('No open safety reports'));
});

test('buildTrustSummary reduces trust when safety reports are open', () => {
  const summary = buildTrustSummary({
    is_verified: true,
    completion_score: 95,
    verification_status: 'verified',
    photo_count: 4,
    family_city: 'Hyderabad',
    profile_status: 'active',
    report_count: 3
  });

  assert.ok(summary.score < 80);
  assert.ok(summary.warnings.includes('3 open safety reports'));
});
