CREATE TABLE IF NOT EXISTS user_change_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    action VARCHAR(120) NOT NULL,
    before_data JSONB DEFAULT '{}'::JSONB,
    after_data JSONB DEFAULT '{}'::JSONB,
    changed_fields JSONB DEFAULT '[]'::JSONB,
    source VARCHAR(40) DEFAULT 'member_app',
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_photo_access_requests (
    photo_access_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    requester_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    requester_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'pending',
    message TEXT,
    requested_at TIMESTAMP DEFAULT NOW(),
    responded_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_notified_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT profile_photo_access_status_check CHECK (status IN ('pending', 'approved', 'declined', 'cancelled'))
);

UPDATE profiles
SET photo_privacy = 'all'
WHERE photo_privacy IS NULL
   OR photo_privacy NOT IN ('all', 'matches_only', 'request_only', 'private');

ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_photo_privacy_check;
ALTER TABLE profiles
    ADD CONSTRAINT profiles_photo_privacy_check
    CHECK (photo_privacy IN ('all', 'matches_only', 'request_only', 'private'));

CREATE INDEX IF NOT EXISTS idx_user_change_audit_profile_created
    ON user_change_audit_logs(profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_change_audit_user_created
    ON user_change_audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_photo_access_target_status
    ON profile_photo_access_requests(target_profile_id, status, requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_photo_access_requester_status
    ON profile_photo_access_requests(requester_user_id, status, requested_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_photo_access_pending_pair
    ON profile_photo_access_requests(target_profile_id, requester_user_id)
    WHERE status = 'pending';
