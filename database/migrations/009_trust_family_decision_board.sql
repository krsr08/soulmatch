CREATE TABLE IF NOT EXISTS family_match_decisions (
    family_decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    owner_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    target_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'considering',
    note TEXT,
    next_step VARCHAR(120),
    next_step_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT family_match_decisions_status_check CHECK (
        status IN (
            'considering',
            'family_review',
            'call_scheduled',
            'spoken',
            'accepted',
            'declined',
            'archived'
        )
    ),
    UNIQUE(owner_profile_id, target_profile_id)
);

CREATE INDEX IF NOT EXISTS idx_family_match_decisions_owner_status
    ON family_match_decisions(owner_profile_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_match_decisions_target
    ON family_match_decisions(target_profile_id, updated_at DESC);

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'trust_engine',
    '{
      "enabled": true,
      "scoreBands": {
        "high": 80,
        "medium": 55
      },
      "signals": [
        "phone_verified",
        "profile_completion",
        "admin_verification",
        "photos_added",
        "family_location",
        "low_report_risk"
      ]
    }'::JSONB,
    FALSE,
    'migration'
)
ON CONFLICT (config_key) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    updated_at = NOW(),
    updated_by = 'migration';
