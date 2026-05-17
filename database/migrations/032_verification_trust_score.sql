ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS trust_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS trust_level VARCHAR(16) DEFAULT 'low';

ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_trust_level_check;
ALTER TABLE profiles
    ADD CONSTRAINT profiles_trust_level_check
    CHECK (trust_level IN ('low', 'medium', 'high'));

CREATE INDEX IF NOT EXISTS idx_profiles_trust_score
    ON profiles (trust_score DESC, verification_status, admin_status);
