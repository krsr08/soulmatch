const test = require('node:test');
const assert = require('node:assert/strict');

const { normalizeList, scoreAdvisorCandidate } = require('../src/services/assistAllocationService');

test('normalizeList splits and normalizes comma separated values', () => {
  assert.deepEqual(
    normalizeList(' Telugu , Hyderabad ,Reddy '),
    ['telugu', 'hyderabad', 'reddy']
  );
});

test('scoreAdvisorCandidate rewards exact local fit with language and community overlap', () => {
  const result = scoreAdvisorCandidate(
    {
      family_pincode: '500081',
      family_locality: 'Madhapur',
      family_city: 'Hyderabad',
      family_state: 'Telangana',
      mother_tongue: 'Telugu',
      religion: 'Hindu',
      caste: 'Reddy'
    },
    {
      status: 'active',
      kyc_status: 'approved',
      membership_expires_at: '2099-01-01T00:00:00.000Z',
      active_assignments: 3,
      max_active_assignments: 30,
      pincode: '500081',
      locality: 'Madhapur',
      city: 'Hyderabad',
      state: 'Telangana',
      languages: ['Telugu', 'English'],
      communities: ['Hindu', 'Reddy'],
      success_rate: 82,
      average_rating: 4.7,
      complaint_score: 0.2,
      priority: 8
    }
  );

  assert.ok(result);
  assert.ok(result.score > 130);
  assert.ok(result.reasons.includes('Exact pincode coverage'));
  assert.ok(result.reasons.includes('Language fit'));
  assert.ok(result.reasons.includes('Community fit'));
});

test('scoreAdvisorCandidate rejects unavailable advisors', () => {
  const inactive = scoreAdvisorCandidate(
    { family_city: 'Hyderabad', family_state: 'Telangana' },
    { status: 'paused', kyc_status: 'approved', city: 'Hyderabad', state: 'Telangana' }
  );
  const full = scoreAdvisorCandidate(
    { family_city: 'Hyderabad', family_state: 'Telangana' },
    {
      status: 'active',
      kyc_status: 'approved',
      city: 'Hyderabad',
      state: 'Telangana',
      active_assignments: 5,
      max_active_assignments: 5
    }
  );

  assert.equal(inactive, null);
  assert.equal(full, null);
});
