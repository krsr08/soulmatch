-- Phase 3 release gates: duplicate-account signals and operational indexes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS phone_hash TEXT,
  ADD COLUMN IF NOT EXISTS device_id_hash TEXT,
  ADD COLUMN IF NOT EXISTS duplicate_signal JSONB DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS idx_users_phone_hash ON users(phone_hash) WHERE phone_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_device_id_hash ON users(device_id_hash) WHERE device_id_hash IS NOT NULL;

UPDATE users
SET duplicate_signal = COALESCE(duplicate_signal, '{}'::jsonb)
WHERE duplicate_signal IS NULL;
