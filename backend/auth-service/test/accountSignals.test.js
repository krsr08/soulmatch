const assert = require('node:assert/strict');
const test = require('node:test');

const userRepo = require('../src/repositories/userRepository');

test('accountHash is stable and does not expose phone numbers', () => {
  process.env.ACCOUNT_HASH_SECRET = 'test-account-secret';
  const first = userRepo.accountHash(userRepo.normalizePhone('+91 94400 11122'));
  const second = userRepo.accountHash(userRepo.normalizePhone('+919440011122'));
  assert.equal(first, second);
  assert.equal(/94400|11122|\+91/.test(first), false);
});
