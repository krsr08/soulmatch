CREATE TABLE IF NOT EXISTS consent_events (
    consent_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    consent_type VARCHAR(48) NOT NULL,
    status VARCHAR(24) NOT NULL,
    purpose TEXT NOT NULL,
    notice_version VARCHAR(48) NOT NULL,
    metadata JSONB DEFAULT '{}'::JSONB,
    source VARCHAR(40) DEFAULT 'member_app',
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT consent_events_type_check CHECK (
        consent_type IN (
            'photo_upload',
            'kyc_upload',
            'agent_kyc_upload',
            'agent_profile_share',
            'soulmatch_assistance'
        )
    ),
    CONSTRAINT consent_events_status_check CHECK (status IN ('granted', 'withdrawn', 'updated'))
);

CREATE INDEX IF NOT EXISTS idx_consent_events_user_type_created
    ON consent_events(user_id, consent_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_consent_events_profile_type_created
    ON consent_events(profile_id, consent_type, created_at DESC);

ALTER TABLE assisted_match_profiles
    ADD COLUMN IF NOT EXISTS consent_notice_version VARCHAR(48),
    ADD COLUMN IF NOT EXISTS consent_granted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS consent_withdrawn_at TIMESTAMP;

ALTER TABLE advisor_kyc_documents
    ADD COLUMN IF NOT EXISTS consent_event_id UUID REFERENCES consent_events(consent_event_id) ON DELETE SET NULL;

ALTER TABLE profile_documents
    ADD COLUMN IF NOT EXISTS consent_event_id UUID REFERENCES consent_events(consent_event_id) ON DELETE SET NULL;
