const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert/strict');

const root = path.join(__dirname, '..');
const welcome = fs.readFileSync(path.join(root, 'android', 'app', 'src', 'main', 'java', 'com', 'soulmatch', 'app', 'ui', 'screens', 'auth', 'WelcomeScreen.kt'), 'utf8');
const otp = fs.readFileSync(path.join(root, 'android', 'app', 'src', 'main', 'java', 'com', 'soulmatch', 'app', 'ui', 'screens', 'auth', 'OTPVerificationScreen.kt'), 'utf8');
const wizard = fs.readFileSync(path.join(root, 'android', 'app', 'src', 'main', 'java', 'com', 'soulmatch', 'app', 'ui', 'screens', 'auth', 'ProfileWizardScreen.kt'), 'utf8');
const profileController = fs.readFileSync(path.join(root, 'backend', 'profile-service', 'src', 'controllers', 'profileController.js'), 'utf8');

test('welcome screen does not advertise unimplemented Apple login or false E2E encryption', () => {
  assert.doesNotMatch(welcome, /Apple|Sign in with Apple|Continue with Apple/i);
  assert.doesNotMatch(welcome, /E2E|end-to-end/i);
});

test('OTP resend countdown is keyed to the resend generation and phone', () => {
  assert.match(otp, /LaunchedEffect\(phone,\s*resendCycle\)/);
  assert.match(otp, /countdown\s*=\s*30/);
  assert.match(otp, /canResend\s*=\s*false/);
  assert.match(otp, /resendCycle\+\+/);
});

test('profile wizard and backend reject under-age date of birth', () => {
  assert.match(wizard, /youngestAllowed\s*=\s*today\.minusYears\(18\)/);
  assert.match(wizard, /Age must be 18 or above/);
  assert.match(profileController, /youngestAllowed/);
  assert.match(profileController, /Age must be between 18 and 80 years/);
});
