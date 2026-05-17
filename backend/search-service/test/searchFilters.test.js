const assert = require('node:assert/strict');
const test = require('node:test');
const { _test } = require('../src/controllers/searchController');

test('normalizes all exposed match filter fields', () => {
  const filters = _test.normalizeFilters({
    ageMin: '24',
    ageMax: '31',
    heightMinCm: '155',
    heightMaxCm: '180',
    religion: 'Hindu',
    caste: 'Reddy',
    country: 'India',
    familyState: 'Telangana',
    city: 'Hyderabad',
    currentlyEmployed: true,
    employedIn: 'Private',
    hasHoroscope: true,
    profilePostedBy: 'agent',
    viewedOnly: true,
    nearby: true
  });

  assert.equal(filters.ageMin, 24);
  assert.equal(filters.heightMinCm, 155);
  assert.equal(filters.community, 'Reddy');
  assert.equal(filters.state, 'Telangana');
  assert.equal(filters.isEmployed, true);
  assert.equal(filters.horoscope, true);
  assert.equal(filters.profilePostedBy, 'agent');
  assert.equal(filters.viewedOnly, true);
  assert.equal(filters.nearbyOnly, true);
});

test('caps search limit to 50', () => {
  assert.deepEqual(_test.paginationFor({ page: '2', limit: '500' }), {
    page: 2,
    limit: 50,
    offset: 50
  });
});
