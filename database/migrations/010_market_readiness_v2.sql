ALTER TABLE family_match_decisions
  ADD COLUMN IF NOT EXISTS family_vote VARCHAR(16) DEFAULT 'discuss';

ALTER TABLE family_match_decisions DROP CONSTRAINT IF EXISTS family_match_decisions_family_vote_check;
ALTER TABLE family_match_decisions
  ADD CONSTRAINT family_match_decisions_family_vote_check
  CHECK (family_vote IN ('approve', 'reject', 'discuss'));

UPDATE family_match_decisions
SET family_vote = 'discuss'
WHERE family_vote IS NULL OR family_vote NOT IN ('approve', 'reject', 'discuss');

CREATE TABLE IF NOT EXISTS family_match_decision_comments (
    family_comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_decision_id UUID NOT NULL REFERENCES family_match_decisions(family_decision_id) ON DELETE CASCADE,
    author_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    vote VARCHAR(16) NOT NULL DEFAULT 'discuss' CHECK (vote IN ('approve', 'reject', 'discuss')),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_family_decision_comments_decision_created
    ON family_match_decision_comments(family_decision_id, created_at DESC);

CREATE TABLE IF NOT EXISTS chat_message_reports (
    chat_message_report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id TEXT NOT NULL,
    chat_id TEXT NOT NULL,
    reporter_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reported_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reason VARCHAR(120),
    description TEXT,
    safety_flags JSONB DEFAULT '[]'::JSONB,
    status VARCHAR(24) DEFAULT 'pending' CHECK (status IN ('pending', 'reviewing', 'resolved', 'dismissed')),
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_message_reports_status_created
    ON chat_message_reports(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_reports_reported
    ON chat_message_reports(reported_user_id, status, created_at DESC);

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'trust_engine',
    '{
      "enabled": true,
      "version": 2,
      "scoreBands": { "high": 80, "medium": 55 },
      "signals": [
        "phone_verified",
        "firebase_verified",
        "profile_completion",
        "photos_added",
        "admin_verification",
        "document_verification",
        "education_verification",
        "income_verification",
        "family_verification",
        "safety_reports",
        "recent_activity"
      ],
      "explainability": true
    }'::JSONB,
    FALSE,
    'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
    config_value = app_config.config_value || EXCLUDED.config_value,
    updated_at = NOW(),
    updated_by = 'migration';

UPDATE app_config
SET config_value = jsonb_set(
    config_value,
    '{featureGates}',
    '{
      "verified_plus": { "minPlan": "silver", "ethicalRule": "Trust and safety remain visible to free members." },
      "family_assist": { "minPlan": "gold", "ethicalRule": "Family tools improve coordination, not safety access." },
      "advisor_assisted": { "minPlan": "platinum", "ethicalRule": "Paid advisors increase service depth and local follow-up." },
      "spotlight": { "minPlan": "silver", "ethicalRule": "Boost reach without suppressing organic discovery." },
      "contact_unlock": { "minPlan": "gold", "ethicalRule": "Phone/contact access requires privacy consent." }
    }'::JSONB,
    true
  ),
  updated_at = NOW(),
  updated_by = 'migration'
WHERE config_key = 'monetization';
