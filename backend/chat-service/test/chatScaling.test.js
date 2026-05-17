const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');

const appSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'app.js'), 'utf8');
const socketAdapterSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'services', 'socketRedisAdapter.js'), 'utf8');
const metadataSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'services', 'chatMetadataService.js'), 'utf8');
const archiveSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'services', 'chatArchiveService.js'), 'utf8');
const migrationSource = fs.readFileSync(path.join(__dirname, '..', '..', '..', 'database', 'migrations', '034_chat_scaling_metadata.sql'), 'utf8');

test('Socket.IO can attach the Redis adapter for horizontal scaling', () => {
  assert.match(socketAdapterSource, /createAdapter/);
  assert.match(socketAdapterSource, /createClient/);
  assert.match(appSource, /attachSocketRedisAdapter\(io\)/);
});

test('chat metadata is mirrored to Postgres for admin search', () => {
  assert.match(metadataSource, /chat_conversation_metadata/);
  assert.match(metadataSource, /participant_user_ids/);
  assert.match(migrationSource, /CREATE TABLE IF NOT EXISTS chat_conversation_metadata/);
  assert.match(migrationSource, /USING GIN \(participant_user_ids\)/);
});

test('retention archive job keeps source deletion behind explicit opt-in', () => {
  assert.match(archiveSource, /CHAT_RETENTION_MONTHS \|\| '24'/);
  assert.match(archiveSource, /ArchivedMessage\.bulkWrite/);
  assert.match(archiveSource, /CHAT_ARCHIVE_DELETE_SOURCE === 'true'/);
});
