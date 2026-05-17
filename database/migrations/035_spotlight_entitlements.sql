-- Phase 2 P2-08: server-enforced member spotlight credits.

CREATE TABLE IF NOT EXISTS profile_spotlight_boosts (
  spotlight_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  plan_id VARCHAR(24) NOT NULL DEFAULT 'bronze',
  starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ends_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'active',
  metadata JSONB DEFAULT '{}'::JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT profile_spotlight_status_check CHECK (status IN ('active', 'expired', 'cancelled'))
);

CREATE INDEX IF NOT EXISTS idx_spotlight_profile_active
  ON profile_spotlight_boosts(profile_id, ends_at DESC)
  WHERE status='active';

CREATE INDEX IF NOT EXISTS idx_spotlight_user_created
  ON profile_spotlight_boosts(user_id, created_at DESC);
