ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS consent_notice_version VARCHAR(48),
    ADD COLUMN IF NOT EXISTS consent_granted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS consent_withdrawn_at TIMESTAMP;

ALTER TABLE consent_events DROP CONSTRAINT IF EXISTS consent_events_type_check;
ALTER TABLE consent_events
    ADD CONSTRAINT consent_events_type_check CHECK (
        consent_type IN (
            'signup_terms',
            'privacy_policy',
            'photo_upload',
            'kyc_upload',
            'agent_kyc_upload',
            'agent_terms_acceptance',
            'agent_profile_share',
            'soulmatch_assistance',
            'data_export',
            'account_deletion'
        )
    );

CREATE INDEX IF NOT EXISTS idx_users_deleted_at
    ON users(deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_profiles_consent_withdrawn
    ON profiles(consent_withdrawn_at)
    WHERE consent_withdrawn_at IS NOT NULL;
