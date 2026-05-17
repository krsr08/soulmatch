const assert = require('node:assert/strict');
const test = require('node:test');

process.env.JWT_SECRET = process.env.JWT_SECRET || 'test_access_secret_32_chars_minimum';
process.env.REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET || 'test_refresh_secret_32_chars_minimum';
process.env.JWT_ISSUER = 'soulmatch-auth-test';
process.env.JWT_AUDIENCE = 'soulmatch-api-test';
process.env.JWT_REFRESH_AUDIENCE = 'soulmatch-refresh-test';

const tokenService = require('../src/services/tokenService');

test('generatePair signs access and refresh tokens with issuer, audience, subject, and jti', () => {
  const tokens = tokenService.generatePair({
    userId: '11111111-1111-4111-8111-111111111111',
    phone: '+919999999999',
    userType: 'member'
  });

  const access = tokenService.verifyAccess(tokens.accessToken);
  const refresh = tokenService.verifyRefresh(tokens.refreshToken);

  assert.equal(access.iss, 'soulmatch-auth-test');
  assert.equal(access.aud, 'soulmatch-api-test');
  assert.equal(access.sub, '11111111-1111-4111-8111-111111111111');
  assert.equal(access.userId, '11111111-1111-4111-8111-111111111111');
  assert.match(access.jti, /^[0-9a-f-]{36}$/i);

  assert.equal(refresh.iss, 'soulmatch-auth-test');
  assert.equal(refresh.aud, 'soulmatch-refresh-test');
  assert.equal(refresh.sub, '11111111-1111-4111-8111-111111111111');
  assert.match(refresh.jti, /^[0-9a-f-]{36}$/i);
  assert.notEqual(access.jti, refresh.jti);
});

test('verifyAccess rejects tokens signed for the wrong audience', () => {
  const jwt = require('jsonwebtoken');
  const token = jwt.sign(
    { userId: '11111111-1111-4111-8111-111111111111', sub: '11111111-1111-4111-8111-111111111111' },
    process.env.JWT_SECRET,
    { issuer: 'soulmatch-auth-test', audience: 'wrong-audience' }
  );

  assert.throws(() => tokenService.verifyAccess(token), /Invalid or expired token/);
});
