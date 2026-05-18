const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const source = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'repositories', 'profileRepository.js'),
  'utf8'
);

test('blocking a profile invalidates pending interests between both profiles', () => {
  assert.match(source, /exports\.blockProfile/);
  assert.match(source, /UPDATE interests[\s\S]*status='blocked'/);
  assert.match(source, /WHERE status='pending'/);
  assert.match(source, /sender_id=\$1 AND receiver_id=\$2/);
});
