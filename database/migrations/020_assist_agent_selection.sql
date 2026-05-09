ALTER TABLE assisted_match_profiles
    ADD COLUMN IF NOT EXISTS share_mode VARCHAR(16) DEFAULT 'single';

ALTER TABLE assisted_match_profiles
    DROP CONSTRAINT IF EXISTS assisted_match_profiles_share_mode_check;

ALTER TABLE assisted_match_profiles
    ADD CONSTRAINT assisted_match_profiles_share_mode_check
    CHECK (share_mode IN ('single', 'multiple'));

CREATE TABLE IF NOT EXISTS assisted_match_profile_advisors (
    assisted_profile_agent_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assisted_profile_id UUID NOT NULL REFERENCES assisted_match_profiles(assisted_profile_id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    advisor_id UUID NOT NULL REFERENCES advisors(advisor_id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'selected',
    selected_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT assisted_match_profile_advisors_status_check
        CHECK (status IN ('selected', 'removed'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_assisted_match_profile_advisors_profile_advisor
    ON assisted_match_profile_advisors(profile_id, advisor_id);

CREATE INDEX IF NOT EXISTS idx_assisted_match_profile_advisors_lookup
    ON assisted_match_profile_advisors(advisor_id, status, selected_at DESC);
