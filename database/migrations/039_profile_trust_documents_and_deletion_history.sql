ALTER TABLE education_career
    ADD COLUMN IF NOT EXISTS no_education BOOLEAN DEFAULT FALSE;

ALTER TABLE profile_documents
    ADD COLUMN IF NOT EXISTS encrypted_reference_value TEXT,
    ADD COLUMN IF NOT EXISTS reference_value_hash TEXT,
    ADD COLUMN IF NOT EXISTS reference_value_last4 VARCHAR(8),
    ADD COLUMN IF NOT EXISTS encryption_algorithm VARCHAR(64),
    ADD COLUMN IF NOT EXISTS encryption_key_ref VARCHAR(128),
    ADD COLUMN IF NOT EXISTS encryption_iv TEXT,
    ADD COLUMN IF NOT EXISTS content_sha256 TEXT,
    ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS verification_id UUID REFERENCES verifications(verification_id) ON DELETE SET NULL;

ALTER TABLE profile_documents DROP CONSTRAINT IF EXISTS profile_document_type_check;
ALTER TABLE profile_documents
    ADD CONSTRAINT profile_document_type_check
    CHECK (document_type IN ('aadhaar', 'pan', 'voter_id', 'education_certificate', 'income_payslip', 'horoscope_pdf', 'divorce_decree'));

CREATE INDEX IF NOT EXISTS idx_profile_documents_profile_status
    ON profile_documents(profile_id, status, document_type);

CREATE INDEX IF NOT EXISTS idx_profile_documents_reference_hash
    ON profile_documents(reference_value_hash)
    WHERE reference_value_hash IS NOT NULL;

CREATE TABLE IF NOT EXISTS deleted_account_history (
    deleted_account_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    profile_ids UUID[] DEFAULT ARRAY[]::UUID[],
    user_type VARCHAR(20),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    display_name VARCHAR(255),
    reason TEXT,
    deleted_at TIMESTAMP DEFAULT NOW(),
    source VARCHAR(80),
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX IF NOT EXISTS idx_deleted_account_history_deleted_at
    ON deleted_account_history(deleted_at DESC);
