const assert = require('node:assert/strict');
const test = require('node:test');

const { _test } = require('../src/middleware/sanitizeInput');

test('sanitizeValue strips script tags and javascript URLs from admin input', () => {
  const sanitized = _test.sanitizeValue({
    bio: '<script>alert(1)</script>Hello',
    link: 'javascript:alert(1)',
    nested: ['<b onclick="bad()">safe</b>'],
  });
  assert.equal(sanitized.bio, 'Hello');
  assert.equal(sanitized.link, 'alert(1)');
  assert.match(sanitized.nested[0], /data-removed/);
});
