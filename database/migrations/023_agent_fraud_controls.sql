ALTER TABLE advisors
    ADD COLUMN IF NOT EXISTS aadhaar_verification_status VARCHAR(32) DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS pan_verification_status VARCHAR(32) DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS kyc_name_match_status VARCHAR(32) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS bank_verification_status VARCHAR(32) DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS bank_account_last4 VARCHAR(4),
    ADD COLUMN IF NOT EXISTS bank_account_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS bank_ifsc VARCHAR(24),
    ADD COLUMN IF NOT EXISTS bank_name_match_status VARCHAR(32) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS penny_drop_status VARCHAR(32) DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS penny_drop_order_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS penny_drop_payment_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS penny_drop_amount_paise INTEGER DEFAULT 100,
    ADD COLUMN IF NOT EXISTS penny_drop_name_match_status VARCHAR(32) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS terms_ip_address TEXT,
    ADD COLUMN IF NOT EXISTS terms_user_agent TEXT,
    ADD COLUMN IF NOT EXISTS terms_version VARCHAR(64),
    ADD COLUMN IF NOT EXISTS fraud_review_status VARCHAR(32) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS draft_saved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS offboarded_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS data_retention_delete_after TIMESTAMP;

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_onboarding_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_onboarding_status_check
    CHECK (onboarding_status IN ('draft', 'pending', 'under_review', 'approved', 'rejected', 'more_info'));

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_agent_document_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_agent_document_status_check
    CHECK (
        aadhaar_verification_status IN ('not_started', 'uploaded', 'under_review', 'verified', 'rejected', 'vendor_pending', 'vendor_unavailable') AND
        pan_verification_status IN ('not_started', 'uploaded', 'under_review', 'verified', 'rejected', 'vendor_pending', 'vendor_unavailable')
    );

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_agent_match_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_agent_match_status_check
    CHECK (
        kyc_name_match_status IN ('pending', 'matched', 'mismatch', 'manual_review', 'vendor_unavailable') AND
        bank_name_match_status IN ('pending', 'matched', 'mismatch', 'manual_review', 'vendor_unavailable') AND
        penny_drop_name_match_status IN ('pending', 'matched', 'mismatch', 'manual_review', 'vendor_unavailable')
    );

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_bank_verification_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_bank_verification_status_check
    CHECK (bank_verification_status IN ('not_started', 'document_uploaded', 'pending_ocr', 'pending_penny_drop', 'payment_confirmed', 'verified', 'rejected'));

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_penny_drop_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_penny_drop_status_check
    CHECK (penny_drop_status IN ('not_started', 'pending', 'paid', 'verified', 'failed', 'refunded'));

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_fraud_review_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_fraud_review_status_check
    CHECK (fraud_review_status IN ('pending', 'in_progress', 'cleared', 'blocked', 'needs_resubmission'));

ALTER TABLE advisor_kyc_documents
    ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS encryption_algorithm VARCHAR(40),
    ADD COLUMN IF NOT EXISTS encryption_key_ref VARCHAR(120),
    ADD COLUMN IF NOT EXISTS encryption_iv TEXT,
    ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(128),
    ADD COLUMN IF NOT EXISTS original_file_name TEXT,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS file_size_bytes INTEGER,
    ADD COLUMN IF NOT EXISTS extracted_metadata JSONB DEFAULT '{}'::JSONB,
    ADD COLUMN IF NOT EXISTS verification_metadata JSONB DEFAULT '{}'::JSONB;

ALTER TABLE advisor_kyc_documents
    DROP CONSTRAINT IF EXISTS advisor_kyc_document_type_check;

ALTER TABLE advisor_kyc_documents
    ADD CONSTRAINT advisor_kyc_document_type_check
    CHECK (document_type IN ('aadhaar', 'pan', 'voter_id', 'cancelled_cheque'));

CREATE TABLE IF NOT EXISTS advisor_terms_acceptances (
    advisor_terms_acceptance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisor_id UUID NOT NULL REFERENCES advisors(advisor_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    terms_version VARCHAR(64) NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    metadata JSONB DEFAULT '{}'::JSONB,
    accepted_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_advisors_fraud_review
    ON advisors(fraud_review_status, bank_verification_status, penny_drop_status);

CREATE INDEX IF NOT EXISTS idx_advisor_kyc_documents_type_status
    ON advisor_kyc_documents(advisor_id, document_type, status);

CREATE INDEX IF NOT EXISTS idx_advisor_terms_acceptances_advisor
    ON advisor_terms_acceptances(advisor_id, accepted_at DESC);
