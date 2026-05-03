ALTER TABLE family_details
    ADD COLUMN IF NOT EXISTS family_state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS family_locality VARCHAR(120),
    ADD COLUMN IF NOT EXISTS family_pincode VARCHAR(12);

CREATE TABLE IF NOT EXISTS advisors (
    advisor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash TEXT,
    service_label VARCHAR(120) DEFAULT 'SoulMatch Advisor',
    bio TEXT,
    gender VARCHAR(20),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    pincode VARCHAR(12),
    languages JSONB DEFAULT '[]'::JSONB,
    communities JSONB DEFAULT '[]'::JSONB,
    max_active_assignments INTEGER DEFAULT 25,
    success_rate NUMERIC(5,2) DEFAULT 0,
    complaint_score NUMERIC(5,2) DEFAULT 0,
    average_rating NUMERIC(5,2) DEFAULT 0,
    kyc_status VARCHAR(24) DEFAULT 'pending',
    status VARCHAR(24) DEFAULT 'active',
    membership_plan VARCHAR(24) DEFAULT 'starter',
    membership_expires_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT advisors_kyc_status_check CHECK (kyc_status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT advisors_status_check CHECK (status IN ('active', 'paused', 'suspended'))
);

CREATE TABLE IF NOT EXISTS advisor_service_areas (
    advisor_service_area_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisor_id UUID NOT NULL REFERENCES advisors(advisor_id) ON DELETE CASCADE,
    state VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(120),
    pincode VARCHAR(12),
    radius_km INTEGER DEFAULT 15,
    priority INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS assisted_match_profiles (
    assisted_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    is_opted_in BOOLEAN DEFAULT FALSE,
    support_level VARCHAR(24) DEFAULT 'self_service',
    request_status VARCHAR(24) DEFAULT 'not_requested',
    preferred_contact_window VARCHAR(80),
    family_contact_name VARCHAR(120),
    family_contact_phone VARCHAR(20),
    notes TEXT,
    assigned_advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    assigned_at TIMESTAMP,
    next_review_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT assisted_match_profiles_support_level_check CHECK (support_level IN ('self_service', 'family_assisted', 'advisor_assisted')),
    CONSTRAINT assisted_match_profiles_request_status_check CHECK (request_status IN ('not_requested', 'waiting_assignment', 'assigned', 'paused'))
);

CREATE TABLE IF NOT EXISTS assisted_match_assignment_events (
    assignment_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    event_type VARCHAR(40) NOT NULL,
    score NUMERIC(8,2),
    metadata JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_family_details_state_city_pincode
    ON family_details(family_state, family_city, family_pincode);
CREATE INDEX IF NOT EXISTS idx_advisors_status_city
    ON advisors(status, kyc_status, city, state, pincode);
CREATE INDEX IF NOT EXISTS idx_advisor_service_areas_lookup
    ON advisor_service_areas(city, state, pincode, locality);
CREATE INDEX IF NOT EXISTS idx_assisted_match_profiles_status
    ON assisted_match_profiles(request_status, support_level, assigned_advisor_id);
CREATE INDEX IF NOT EXISTS idx_assisted_assignment_events_profile
    ON assisted_match_assignment_events(profile_id, created_at DESC);

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'assisted_matchmaking',
    '{
      "enabled": true,
      "advisorPlans": [
        {
          "planId": "advisor_starter",
          "name": "Advisor Starter",
          "monthlyPrice": 1999,
          "maxActiveProfiles": 25,
          "features": ["Up to 25 active assisted members", "Single city service area", "Standard lead allocation"]
        },
        {
          "planId": "advisor_growth",
          "name": "Advisor Growth",
          "monthlyPrice": 4999,
          "maxActiveProfiles": 80,
          "features": ["Up to 80 active assisted members", "Multiple service areas", "Priority allocations"]
        }
      ],
      "memberModes": ["self_service", "family_assisted", "advisor_assisted"],
      "defaultReviewDays": 7
    }'::JSONB,
    FALSE,
    'migration'
)
ON CONFLICT (config_key) DO NOTHING;
