const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const chatRoutesSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'routes', 'chatRoutes.js'),
  'utf8'
);
const socketSource = fs.readFileSync(
  path.join(__dirname, '..', 'src', 'socket', 'socketHandlers.js'),
  'utf8'
);

test('message history route requires the requester to be a conversation participant', () => {
  assert.match(chatRoutesSource, /router\.get\('\/:chatId\/messages'/);
  assert.match(chatRoutesSource, /Conversation\.findOne\(\{[\s\S]*chatId:\s*req\.params\.chatId,[\s\S]*participants:\s*req\.user\.userId/);
  assert.match(chatRoutesSource, /status\(403\)/);
});

test('message history pagination is capped to 100 messages', () => {
  assert.match(chatRoutesSource, /const MAX_PAGE_SIZE = 100/);
  assert.match(chatRoutesSource, /Math\.min\(Math\.max\(requestedLimit,\s*1\),\s*MAX_PAGE_SIZE\)/);
});

test('socket message, typing, and read flows validate conversation eligibility', () => {
  assert.match(socketSource, /if \(!await canChat\(socket\.userId,\s*receiverId\)\)/);
  assert.match(socketSource, /if \(!cid \|\| !await isConversationParticipant\(cid,\s*socket\.userId\)\)/);
  assert.match(socketSource, /jwt\.verify\(token,\s*process\.env\.JWT_SECRET,\s*verifyOptions\(\)\)/);
});
