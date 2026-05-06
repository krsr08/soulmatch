ALTER TABLE users
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(16) DEFAULT 'member';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_user_type_check;

ALTER TABLE users
    ADD CONSTRAINT users_user_type_check
    CHECK (user_type IN ('member', 'agent', 'admin'));

ALTER TABLE advisors
    ADD COLUMN IF NOT EXISTS user_id UUID UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS agent_code VARCHAR(40) UNIQUE,
    ADD COLUMN IF NOT EXISTS business_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS referral_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS profile_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS years_experience INTEGER,
    ADD COLUMN IF NOT EXISTS fee_preferences JSONB DEFAULT '{}'::JSONB,
    ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(24) DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS onboarding_rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS contact_views_used INTEGER DEFAULT 0;

ALTER TABLE advisors
    DROP CONSTRAINT IF EXISTS advisors_onboarding_status_check;

ALTER TABLE advisors
    ADD CONSTRAINT advisors_onboarding_status_check
    CHECK (onboarding_status IN ('pending', 'approved', 'rejected', 'more_info'));

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS created_by_advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(24) DEFAULT 'draft',
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS review_notes TEXT,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

ALTER TABLE profiles
    DROP CONSTRAINT IF EXISTS profiles_review_status_check;

ALTER TABLE profiles
    ADD CONSTRAINT profiles_review_status_check
    CHECK (review_status IN ('draft', 'submitted', 'under_review', 'verified', 'rejected'));

CREATE TABLE IF NOT EXISTS advisor_kyc_documents (
    advisor_kyc_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisor_id UUID NOT NULL REFERENCES advisors(advisor_id) ON DELETE CASCADE,
    document_type VARCHAR(32) NOT NULL,
    document_side VARCHAR(16) DEFAULT 'single',
    file_url TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'uploaded',
    review_comment TEXT,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT advisor_kyc_document_type_check CHECK (document_type IN ('aadhaar', 'pan', 'voter_id')),
    CONSTRAINT advisor_kyc_document_side_check CHECK (document_side IN ('front', 'back', 'single')),
    CONSTRAINT advisor_kyc_document_status_check CHECK (status IN ('not_uploaded', 'uploaded', 'under_review', 'verified', 'rejected'))
);

CREATE TABLE IF NOT EXISTS profile_documents (
    profile_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    document_type VARCHAR(40) NOT NULL,
    document_side VARCHAR(16) DEFAULT 'single',
    file_url TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'uploaded',
    review_comment TEXT,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT profile_document_type_check CHECK (document_type IN ('aadhaar', 'pan', 'voter_id', 'education_certificate', 'horoscope_pdf', 'divorce_decree')),
    CONSTRAINT profile_document_side_check CHECK (document_side IN ('front', 'back', 'single')),
    CONSTRAINT profile_document_status_check CHECK (status IN ('not_uploaded', 'uploaded', 'under_review', 'verified', 'rejected'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_documents_profile_type_side
    ON profile_documents(profile_id, document_type, document_side);

CREATE INDEX IF NOT EXISTS idx_advisors_user_id
    ON advisors(user_id);

CREATE INDEX IF NOT EXISTS idx_advisors_onboarding_status
    ON advisors(onboarding_status, kyc_status, status);

CREATE INDEX IF NOT EXISTS idx_profiles_created_by_advisor
    ON profiles(created_by_advisor_id, review_status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_profile_documents_review_status
    ON profile_documents(status, document_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_advisor_kyc_documents_review_status
    ON advisor_kyc_documents(status, document_type, created_at DESC);
