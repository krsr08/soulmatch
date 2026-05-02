CREATE INDEX IF NOT EXISTS idx_verifications_status_created ON verifications(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_verifications_user_status ON verifications(user_id, status);
