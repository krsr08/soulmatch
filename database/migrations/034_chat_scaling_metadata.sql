-- Phase 2 P2-07: chat scaling metadata and admin search mirror.

CREATE TABLE IF NOT EXISTS chat_conversation_metadata (
  chat_id TEXT PRIMARY KEY,
  participant_user_ids TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  last_message_preview TEXT,
  last_message_type TEXT,
  last_message_at TIMESTAMPTZ,
  last_sender_user_id TEXT,
  message_count INTEGER NOT NULL DEFAULT 0,
  source TEXT,
  interest_id TEXT,
  archived_until TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_metadata_participants
  ON chat_conversation_metadata USING GIN (participant_user_ids);

CREATE INDEX IF NOT EXISTS idx_chat_metadata_last_message
  ON chat_conversation_metadata (last_message_at DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_chat_metadata_source
  ON chat_conversation_metadata (source);
