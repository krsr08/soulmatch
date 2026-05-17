const assert = require('node:assert/strict');
const test = require('node:test');
const { evaluateProfileVisibility, redactProfileForViewer } = require('../../shared/profileVisibility');

test('blocked users cannot view a profile', () => {
  const result = evaluateProfileVisibility({
    profile_id: 'profile-a',
    user_id: 'user-a',
    blocked: true,
    is_published: true,
    admin_status: 'active'
  }, 'user-b');

  assert.equal(result.allowed, false);
  assert.equal(result.reason, 'blocked');
});

test('null admin status defaults to active and hidden profile is denied', () => {
  const visible = evaluateProfileVisibility({
    profile_id: 'profile-a',
    user_id: 'user-a',
    is_published: true,
    admin_status: null,
    profile_visibility: 'all'
  }, 'user-b');
  const hidden = evaluateProfileVisibility({
    profile_id: 'profile-a',
    user_id: 'user-a',
    is_published: true,
    profile_visibility: 'hidden'
  }, 'user-b');

  assert.equal(visible.allowed, true);
  assert.equal(hidden.allowed, false);
  assert.equal(hidden.reason, 'hidden');
});

test('redaction removes private photo, contact, and last seen for non-owner', () => {
  const result = redactProfileForViewer({
    primary_photo_url: '/uploads/private.jpg',
    phone: '+919876543210',
    email: 'member@example.com',
    masked_phone: '********10',
    masked_email: 'm***@example.com',
    hide_last_seen: true,
    last_login: '2026-05-17T12:00:00Z'
  }, { owner: false, canViewPhoto: false, canViewContact: false });

  assert.equal(result.primary_photo_url, null);
  assert.equal(result.phone, '********10');
  assert.equal(result.email, 'm***@example.com');
  assert.equal(result.last_login, null);
});
