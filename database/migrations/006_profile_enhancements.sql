ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS is_partner_pref_set BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS profile_status VARCHAR(16) DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS profile_created_by VARCHAR(16) DEFAULT 'self';

UPDATE profiles
SET
    is_partner_pref_set = COALESCE(is_partner_pref_set, FALSE),
    profile_status = CASE WHEN profile_status IN ('active', 'inactive') THEN profile_status ELSE 'active' END,
    profile_created_by = CASE WHEN profile_created_by IN ('self', 'mediator') THEN profile_created_by ELSE 'self' END;

UPDATE profiles p
SET is_partner_pref_set = TRUE
WHERE EXISTS (
    SELECT 1
    FROM partner_preferences pp
    WHERE pp.profile_id = p.profile_id
);

CREATE INDEX IF NOT EXISTS idx_profiles_profile_status ON profiles(profile_status);
DROP INDEX IF EXISTS idx_profiles_match_search;
CREATE INDEX IF NOT EXISTS idx_profiles_match_search ON profiles(gender, is_published, admin_status, profile_status, verification_status, religion, caste);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'profiles_profile_status_check'
    ) THEN
        ALTER TABLE profiles
            ADD CONSTRAINT profiles_profile_status_check
            CHECK (profile_status IN ('active', 'inactive'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'profiles_profile_created_by_check'
    ) THEN
        ALTER TABLE profiles
            ADD CONSTRAINT profiles_profile_created_by_check
            CHECK (profile_created_by IN ('self', 'mediator'));
    END IF;
END $$;
