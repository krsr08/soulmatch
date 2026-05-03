const test = require('node:test');
const assert = require('node:assert/strict');

const { buildTrustSummary, buildSeriousnessSummary } = require('../src/repositories/profileRepository');

test('buildTrustSummary gives high score for verified complete low-risk profiles', () => {
  const summary = buildTrustSummary({
    is_verified: true,
    firebase_verified: true,
    completion_score: 95,
    verification_status: 'verified',
    approved_verifications: 4,
    approved_verification_types: ['identity', 'education', 'income', 'family'],
    photo_count: 4,
    family_city: 'Hyderabad',
    profile_status: 'active',
    report_count: 0,
    last_login: new Date().toISOString()
  });

  assert.equal(summary.level, 'high');
  assert.ok(summary.score >= 80);
  assert.ok(summary.signals.includes('Admin verified profile'));
  assert.ok(summary.signals.includes('Education verified'));
  assert.ok(summary.factors.some((factor) => factor.key === 'safety_reports' && factor.status === 'positive'));
  assert.ok(summary.factors.some((factor) => factor.key === 'firebase_verified' && factor.status === 'positive'));
  assert.match(summary.explanation.summary, /Trust score is/);
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

test('buildTrustSummary explains missing enterprise trust signals', () => {
  const summary = buildTrustSummary({
    is_verified: false,
    completion_score: 38,
    verification_status: 'pending',
    photo_count: 0,
    profile_status: 'inactive',
    report_count: 0
  });

  assert.equal(summary.level, 'low');
  assert.ok(summary.warnings.includes('Phone is not verified'));
  assert.ok(summary.warnings.includes('Profile details are still incomplete'));
  assert.ok(summary.factors.some((factor) => factor.key === 'profile_active' && factor.status === 'warning'));
});

test('buildSeriousnessSummary rewards responsive verified active users', () => {
  const summary = buildSeriousnessSummary({
    completion_score: 92,
    verification_status: 'verified',
    last_login: new Date().toISOString(),
    received_interests: 10,
    responded_interests: 9,
    accepted_interests: 4,
    declined_interests: 2,
    ignored_interests: 0,
    family_board_items: 3,
    report_count: 0
  });

  assert.equal(summary.level, 'high');
  assert.ok(summary.score >= 80);
  assert.ok(summary.signals.includes('Responds to interests'));
  assert.ok(summary.signals.includes('Uses family decision board'));
});
