ALTER TABLE partner_preferences
  ADD COLUMN IF NOT EXISTS education_levels TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS occupations TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS annual_income_min INTEGER,
  ADD COLUMN IF NOT EXISTS annual_income_max INTEGER,
  ADD COLUMN IF NOT EXISTS height_min_cm INTEGER,
  ADD COLUMN IF NOT EXISTS height_max_cm INTEGER,
  ADD COLUMN IF NOT EXISTS locations TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS location_radius_km INTEGER DEFAULT 50,
  ADD COLUMN IF NOT EXISTS diet_prefs TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS marital_statuses TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS family_types TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS relocation_open BOOLEAN,
  ADD COLUMN IF NOT EXISTS timeline VARCHAR(40),
  ADD COLUMN IF NOT EXISTS deal_breakers TEXT[] DEFAULT ARRAY[]::TEXT[],
  ADD COLUMN IF NOT EXISTS good_to_have TEXT[] DEFAULT ARRAY[]::TEXT[];

ALTER TABLE partner_preferences DROP CONSTRAINT IF EXISTS partner_preferences_age_range_check;
ALTER TABLE partner_preferences
  ADD CONSTRAINT partner_preferences_age_range_check
  CHECK (age_min IS NULL OR age_max IS NULL OR age_min <= age_max);

ALTER TABLE partner_preferences DROP CONSTRAINT IF EXISTS partner_preferences_height_range_check;
ALTER TABLE partner_preferences
  ADD CONSTRAINT partner_preferences_height_range_check
  CHECK (height_min_cm IS NULL OR height_max_cm IS NULL OR height_min_cm <= height_max_cm);

CREATE TABLE IF NOT EXISTS match_feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    source_profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    target_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    reason VARCHAR(120),
    note TEXT,
    metadata JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT match_feedback_action_check CHECK (
      action IN (
        'viewed',
        'shortlisted',
        'passed',
        'not_interested',
        'more_like_this',
        'less_like_this',
        'reported_mismatch',
        'blocked'
      )
    )
);

CREATE INDEX IF NOT EXISTS idx_partner_preferences_profile_updated
  ON partner_preferences(profile_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_match_feedback_user_target_created
  ON match_feedback(user_id, target_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_match_feedback_action_created
  ON match_feedback(action, created_at DESC);

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'partner_preference_engine',
    '{
      "version": 2,
      "mandatoryButSkippable": true,
      "uses": [
        "age",
        "height",
        "religion",
        "education",
        "occupation",
        "income",
        "location",
        "diet",
        "family_type",
        "marital_status",
        "horoscope",
        "timeline",
        "deal_breakers"
      ],
      "feedbackLearning": true
    }'::JSONB,
    FALSE,
    'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
    config_value = EXCLUDED.config_value,
    updated_at = NOW(),
    updated_by = 'migration';
