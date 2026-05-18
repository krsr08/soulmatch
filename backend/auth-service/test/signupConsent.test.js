const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert/strict');

const controllerSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'controllers', 'authController.js'), 'utf8');
const repoSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'repositories', 'userRepository.js'), 'utf8');

test('auth service records policy consent for every new account creation path', () => {
  assert.match(repoSource, /exports\.recordSignupConsent/);
  assert.match(repoSource, /'signup_terms'/);
  assert.match(repoSource, /LEGAL_NOTICE_VERSION/);
  assert.match(controllerSource, /recordNewUserConsent\(req,\s*user,\s*'otp'\)/);
  assert.match(controllerSource, /recordNewUserConsent\(req,\s*user,\s*'google'\)/);
  assert.match(controllerSource, /recordNewUserConsent\(req,\s*user,\s*'firebase_phone'\)/);
});

test('signup consent captures audit metadata without storing secret values', () => {
  assert.match(controllerSource, /ipAddress:\s*req\.ip/);
  assert.match(controllerSource, /userAgent:\s*req\.get\('user-agent'\)/);
  assert.doesNotMatch(repoSource, /JWT_SECRET|REFRESH_TOKEN_SECRET|RAZORPAY_KEY_SECRET/);
});
