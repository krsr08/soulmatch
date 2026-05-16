CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(15) UNIQUE,
    email VARCHAR(255) UNIQUE,
    google_id VARCHAR(255) UNIQUE,
    fcm_token TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    is_banned BOOLEAN DEFAULT FALSE,
    referred_by_code VARCHAR(32),
    acquisition_source VARCHAR(80),
    referred_at TIMESTAMP,
    user_type VARCHAR(16) DEFAULT 'member',
    role_selected_at TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT users_user_type_check CHECK (user_type IN ('member', 'agent', 'admin'))
);

CREATE TABLE IF NOT EXISTS profiles (
    profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    dob DATE,
    gender VARCHAR(10),
    religion VARCHAR(50),
    caste VARCHAR(100),
    mother_tongue VARCHAR(50),
    marital_status VARCHAR(30) DEFAULT 'never_married',
    completion_score INTEGER DEFAULT 0,
    is_published BOOLEAN DEFAULT FALSE,
    is_partner_pref_set BOOLEAN DEFAULT FALSE,
    profile_status VARCHAR(16) DEFAULT 'active' CHECK (profile_status IN ('active', 'inactive')),
    profile_created_by VARCHAR(16) DEFAULT 'self' CHECK (profile_created_by IN ('self', 'mediator')),
    verification_status VARCHAR(24) DEFAULT 'pending',
    admin_status VARCHAR(24) DEFAULT 'active',
    moderation_score NUMERIC(5,2) DEFAULT 0,
    primary_photo_url TEXT,
    video_url TEXT,
    photo_privacy VARCHAR(20) DEFAULT 'all',
    contact_privacy VARCHAR(20) DEFAULT 'visible' CHECK (contact_privacy IN ('visible', 'masked')),
    profile_visibility VARCHAR(20) DEFAULT 'all',
    hide_last_seen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS physical_details (
    physical_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    height_cm INTEGER,
    weight_kg INTEGER,
    complexion VARCHAR(30),
    body_type VARCHAR(30),
    blood_group VARCHAR(5),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS education_career (
    ec_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    education_level VARCHAR(50),
    occupation VARCHAR(100),
    annual_income VARCHAR(50),
    working_city VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS family_details (
    family_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    father_occupation VARCHAR(100),
    mother_occupation VARCHAR(100),
    num_brothers INTEGER DEFAULT 0,
    num_sisters INTEGER DEFAULT 0,
    family_type VARCHAR(30),
    family_city VARCHAR(100),
    family_state VARCHAR(100),
    family_locality VARCHAR(120),
    family_pincode VARCHAR(12),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS horoscope_details (
    horoscope_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    rashi VARCHAR(30),
    nakshatra VARCHAR(30),
    is_manglik BOOLEAN DEFAULT FALSE,
    birth_city VARCHAR(100),
    gotra VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS lifestyle_details (
    lifestyle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    diet VARCHAR(30),
    smoking VARCHAR(20) DEFAULT 'never',
    drinking VARCHAR(20) DEFAULT 'never',
    about_me TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_photos (
    photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    s3_key TEXT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT FALSE,
    sequence_order INTEGER,
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_photo_access_requests (
    photo_access_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    requester_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    requester_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'pending',
    message TEXT,
    requested_at TIMESTAMP DEFAULT NOW(),
    responded_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_notified_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT profile_photo_access_status_check CHECK (status IN ('pending', 'approved', 'declined', 'cancelled'))
);

CREATE TABLE IF NOT EXISTS partner_preferences (
    pref_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    age_min INTEGER DEFAULT 18,
    age_max INTEGER DEFAULT 50,
    religion VARCHAR(50),
    manglik_pref VARCHAR(20) DEFAULT 'any',
    education_levels TEXT[] DEFAULT ARRAY[]::TEXT[],
    occupations TEXT[] DEFAULT ARRAY[]::TEXT[],
    annual_income_min INTEGER,
    annual_income_max INTEGER,
    height_min_cm INTEGER,
    height_max_cm INTEGER,
    locations TEXT[] DEFAULT ARRAY[]::TEXT[],
    location_radius_km INTEGER DEFAULT 50,
    diet_prefs TEXT[] DEFAULT ARRAY[]::TEXT[],
    marital_statuses TEXT[] DEFAULT ARRAY[]::TEXT[],
    family_types TEXT[] DEFAULT ARRAY[]::TEXT[],
    relocation_open BOOLEAN,
    timeline VARCHAR(40),
    deal_breakers TEXT[] DEFAULT ARRAY[]::TEXT[],
    good_to_have TEXT[] DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT partner_preferences_age_range_check CHECK (age_min IS NULL OR age_max IS NULL OR age_min <= age_max),
    CONSTRAINT partner_preferences_height_range_check CHECK (height_min_cm IS NULL OR height_max_cm IS NULL OR height_min_cm <= height_max_cm)
);

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

CREATE TABLE IF NOT EXISTS saved_searches (
    search_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    label VARCHAR(120) NOT NULL,
    age_min INTEGER,
    age_max INTEGER,
    religion VARCHAR(50),
    city VARCHAR(100),
    gender VARCHAR(10),
    diet VARCHAR(30),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    plan_id VARCHAR(20) NOT NULL DEFAULT 'free',
    start_date TIMESTAMP NOT NULL DEFAULT NOW(),
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    payment_id VARCHAR(255),
    amount_paid DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    subscription_id UUID REFERENCES subscriptions(subscription_id),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    amount DECIMAL(10, 2),
    currency VARCHAR(5) DEFAULT 'INR',
    gateway VARCHAR(40) DEFAULT 'razorpay',
    payment_method VARCHAR(40),
    payment_instrument VARCHAR(120),
    provider_status VARCHAR(40),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payment_orders (
    payment_order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    plan_id VARCHAR(40) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    gateway VARCHAR(40) NOT NULL DEFAULT 'razorpay',
    provider_order_id VARCHAR(255) UNIQUE NOT NULL,
    provider_payment_id VARCHAR(255),
    signature TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'created',
    support_contacted BOOLEAN DEFAULT FALSE,
    support_comments VARCHAR(2000),
    support_contacted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'::JSONB,
    webhook_payload JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}'::JSONB,
    status VARCHAR(24) NOT NULL DEFAULT 'queued',
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS interests (
    interest_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID REFERENCES profiles(profile_id),
    receiver_id UUID REFERENCES profiles(profile_id),
    status VARCHAR(20) DEFAULT 'pending',
    sent_at TIMESTAMP DEFAULT NOW(),
    responded_at TIMESTAMP,
    UNIQUE(sender_id, receiver_id)
);

CREATE TABLE IF NOT EXISTS shortlists (
    shortlist_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    added_by UUID REFERENCES users(user_id),
    profile_id UUID REFERENCES profiles(profile_id),
    added_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(added_by, profile_id)
);

CREATE TABLE IF NOT EXISTS blocks (
    block_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID REFERENCES users(user_id),
    blocked_id UUID REFERENCES users(user_id),
    blocked_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

CREATE TABLE IF NOT EXISTS reports (
    report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID REFERENCES users(user_id),
    reported_id UUID REFERENCES users(user_id),
    reason VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS profile_views (
    view_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    viewer_id UUID REFERENCES profiles(profile_id),
    viewed_profile_id UUID REFERENCES profiles(profile_id),
    viewed_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS member_subscription_usage (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    plan_id VARCHAR(24) NOT NULL DEFAULT 'bronze',
    period_started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    period_ends_at TIMESTAMP NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
    profile_views_used INTEGER NOT NULL DEFAULT 0,
    contact_unlocks_used INTEGER NOT NULL DEFAULT 0,
    shortlists_used INTEGER NOT NULL DEFAULT 0,
    interests_used INTEGER NOT NULL DEFAULT 0,
    spotlight_boosts_used INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS member_meter_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    target_profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    period_key DATE NOT NULL,
    metadata JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, target_profile_id, event_type, period_key)
);

CREATE TABLE IF NOT EXISTS verifications (
    verification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id),
    type VARCHAR(30),
    status VARCHAR(20) DEFAULT 'pending',
    document_url TEXT,
    reviewer_email VARCHAR(255),
    review_note TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS success_stories (
    story_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID REFERENCES users(user_id),
    user2_id UUID REFERENCES users(user_id),
    story_text TEXT,
    couple_photo_url TEXT,
    wedding_date DATE,
    is_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

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
    kyc_status VARCHAR(24) DEFAULT 'pending' CHECK (kyc_status IN ('pending', 'approved', 'rejected')),
    status VARCHAR(24) DEFAULT 'active' CHECK (status IN ('active', 'paused', 'suspended')),
    membership_plan VARCHAR(24) DEFAULT 'starter',
    membership_expires_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
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

CREATE TABLE IF NOT EXISTS advisor_kyc_documents (
    advisor_kyc_document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisor_id UUID NOT NULL REFERENCES advisors(advisor_id) ON DELETE CASCADE,
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

CREATE TABLE IF NOT EXISTS assisted_match_profiles (
    assisted_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    is_opted_in BOOLEAN DEFAULT FALSE,
    support_level VARCHAR(24) DEFAULT 'self_service' CHECK (support_level IN ('self_service', 'family_assisted', 'advisor_assisted')),
    request_status VARCHAR(24) DEFAULT 'not_requested' CHECK (request_status IN ('not_requested', 'waiting_assignment', 'assigned', 'paused')),
    preferred_contact_window VARCHAR(80),
    family_contact_name VARCHAR(120),
    family_contact_phone VARCHAR(20),
    notes TEXT,
    assigned_advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    assigned_at TIMESTAMP,
    next_review_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
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

CREATE TABLE IF NOT EXISTS family_match_decisions (
    family_decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    owner_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    target_profile_id UUID NOT NULL REFERENCES profiles(profile_id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'considering',
    family_vote VARCHAR(16) DEFAULT 'discuss' CHECK (family_vote IN ('approve', 'reject', 'discuss')),
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

CREATE TABLE IF NOT EXISTS family_match_decision_comments (
    family_comment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_decision_id UUID NOT NULL REFERENCES family_match_decisions(family_decision_id) ON DELETE CASCADE,
    author_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    vote VARCHAR(16) NOT NULL DEFAULT 'discuss' CHECK (vote IN ('approve', 'reject', 'discuss')),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

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

CREATE TABLE IF NOT EXISTS app_config (
    config_key VARCHAR(80) PRIMARY KEY,
    config_value JSONB NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT NOW(),
    updated_by VARCHAR(255) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS landing_pages (
    landing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(80) UNIQUE NOT NULL,
    title VARCHAR(160) NOT NULL,
    subtitle VARCHAR(255),
    description TEXT NOT NULL,
    hero_image_url TEXT,
    preview_image_url TEXT,
    cta_label VARCHAR(80) DEFAULT 'Get started',
    cta_url TEXT,
    seo_title VARCHAR(160),
    seo_description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS referral_codes (
    referral_code_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) UNIQUE NOT NULL,
    owner_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    campaign_name VARCHAR(120),
    channel VARCHAR(80),
    reward_points INTEGER DEFAULT 0,
    max_redemptions INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS referral_redemptions (
    redemption_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referral_code_id UUID REFERENCES referral_codes(referral_code_id) ON DELETE CASCADE,
    referred_user_id UUID REFERENCES users(user_id) ON DELETE CASCADE UNIQUE,
    referrer_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reward_status VARCHAR(20) DEFAULT 'pending',
    reward_granted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS analytics_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(80) NOT NULL,
    service_name VARCHAR(80) NOT NULL,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(120),
    payload JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_email VARCHAR(255) NOT NULL,
    admin_role VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    metadata JSONB DEFAULT '{}'::JSONB,
    ip_address VARCHAR(80),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_change_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    action VARCHAR(120) NOT NULL,
    before_data JSONB DEFAULT '{}'::JSONB,
    after_data JSONB DEFAULT '{}'::JSONB,
    changed_fields JSONB DEFAULT '[]'::JSONB,
    source VARCHAR(40) DEFAULT 'member_app',
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    severity VARCHAR(20) DEFAULT 'medium',
    title VARCHAR(180) NOT NULL,
    body TEXT,
    source VARCHAR(80) DEFAULT 'admin-service',
    status VARCHAR(24) DEFAULT 'open',
    metadata JSONB DEFAULT '{}'::JSONB,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

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

CREATE TABLE IF NOT EXISTS admin_campaigns (
    campaign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(180) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    audience JSONB DEFAULT '{}'::JSONB,
    template_key VARCHAR(120),
    scheduled_at TIMESTAMP,
    status VARCHAR(24) DEFAULT 'draft',
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_console_users (
    admin_user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(160),
    role VARCHAR(80) NOT NULL DEFAULT 'admin',
    status VARCHAR(24) NOT NULL DEFAULT 'active',
    created_by VARCHAR(255),
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT admin_console_users_status_check CHECK (status IN ('active', 'suspended', 'deleted'))
);

CREATE TABLE IF NOT EXISTS admin_role_change_logs (
    role_change_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_email VARCHAR(255),
    change_description TEXT NOT NULL,
    modification_details JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deployment_audit_logs (
    deployment_audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_actor VARCHAR(255) DEFAULT 'system',
    release_description TEXT NOT NULL DEFAULT 'Automated production deployment',
    change_details TEXT,
    release_version VARCHAR(80),
    deployment_status VARCHAR(40) NOT NULL DEFAULT 'unknown',
    change_type VARCHAR(20) NOT NULL DEFAULT 'Both',
    source_commit VARCHAR(80),
    source_branch VARCHAR(120),
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT deployment_audit_status_check CHECK (deployment_status IN ('success', 'failed', 'in_progress', 'unknown')),
    CONSTRAINT deployment_audit_change_type_check CHECK (change_type IN ('FE', 'BE', 'Both', 'Infra'))
);

CREATE TABLE IF NOT EXISTS subscription_invoices (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(80) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE SET NULL,
    advisor_id UUID REFERENCES advisors(advisor_id) ON DELETE SET NULL,
    subscription_id UUID REFERENCES subscriptions(subscription_id) ON DELETE SET NULL,
    transaction_id UUID REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    plan_id VARCHAR(40),
    amount DECIMAL(10, 2) DEFAULT 0,
    currency VARCHAR(5) DEFAULT 'INR',
    status VARCHAR(24) DEFAULT 'issued',
    payment_method VARCHAR(40),
    transaction_ref VARCHAR(255),
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    issued_at TIMESTAMP DEFAULT NOW(),
    invoice_payload JSONB DEFAULT '{}'::JSONB
);

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_gender ON profiles(gender);
CREATE INDEX IF NOT EXISTS idx_profiles_religion ON profiles(religion);
CREATE INDEX IF NOT EXISTS idx_profiles_published ON profiles(is_published);
CREATE INDEX IF NOT EXISTS idx_saved_searches_user ON saved_searches(user_id);
CREATE INDEX IF NOT EXISTS idx_interests_sender ON interests(sender_id);
CREATE INDEX IF NOT EXISTS idx_interests_receiver ON interests(receiver_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_one_active_per_user ON subscriptions(user_id) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_transactions_user_status_created ON transactions(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_status ON payment_orders(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_orders_provider ON payment_orders(provider_order_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_razorpay_payment_id ON transactions(razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_verifications_status_created ON verifications(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_verifications_user_status ON verifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocked ON blocks(blocked_id);
CREATE INDEX IF NOT EXISTS idx_profile_photos_profile ON profile_photos(profile_id);
CREATE INDEX IF NOT EXISTS idx_photo_access_target_status ON profile_photo_access_requests(target_profile_id, status, requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_photo_access_requester_status ON profile_photo_access_requests(requester_user_id, status, requested_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_photo_access_pending_pair ON profile_photo_access_requests(target_profile_id, requester_user_id) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_reports_status_created ON reports(status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_views_pair ON profile_views(viewer_id, viewed_profile_id);
CREATE INDEX IF NOT EXISTS idx_partner_preferences_profile_updated ON partner_preferences(profile_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_match_feedback_user_target_created ON match_feedback(user_id, target_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_match_feedback_action_created ON match_feedback(action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_details_state_city_pincode ON family_details(family_state, family_city, family_pincode);
CREATE INDEX IF NOT EXISTS idx_referral_codes_owner ON referral_codes(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_referral_redemptions_code ON referral_redemptions(referral_code_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_service_name ON analytics_events(service_name);
CREATE INDEX IF NOT EXISTS idx_analytics_created_at ON analytics_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_change_audit_profile_created ON user_change_audit_logs(profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_change_audit_user_created ON user_change_audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_alerts_status ON admin_alerts(status, severity);
CREATE INDEX IF NOT EXISTS idx_admin_console_users_role_status ON admin_console_users(role, status);
CREATE INDEX IF NOT EXISTS idx_admin_role_change_logs_created ON admin_role_change_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_deployment_audit_logs_created ON deployment_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscription_invoices_user ON subscription_invoices(user_id, issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_profiles_admin_status ON profiles(admin_status, verification_status);
CREATE INDEX IF NOT EXISTS idx_profiles_profile_status ON profiles(profile_status);
CREATE INDEX IF NOT EXISTS idx_profiles_match_search ON profiles(gender, is_published, admin_status, profile_status, verification_status, religion, caste);
CREATE INDEX IF NOT EXISTS idx_advisors_status_city ON advisors(status, kyc_status, city, state, pincode);
CREATE INDEX IF NOT EXISTS idx_advisor_service_areas_lookup ON advisor_service_areas(city, state, pincode, locality);
CREATE INDEX IF NOT EXISTS idx_advisor_kyc_documents_review_status ON advisor_kyc_documents(status, document_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_profile_documents_review_status ON profile_documents(status, document_type, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_documents_profile_type_side ON profile_documents(profile_id, document_type, document_side);
CREATE INDEX IF NOT EXISTS idx_assisted_match_profiles_status ON assisted_match_profiles(request_status, support_level, assigned_advisor_id);
CREATE INDEX IF NOT EXISTS idx_consent_events_user_type_created ON consent_events(user_id, consent_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_consent_events_profile_type_created ON consent_events(profile_id, consent_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_assisted_assignment_events_profile ON assisted_match_assignment_events(profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_match_decisions_owner_status ON family_match_decisions(owner_profile_id, status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_match_decisions_target ON family_match_decisions(target_profile_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_family_decision_comments_decision_created ON family_match_decision_comments(family_decision_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_reports_status_created ON chat_message_reports(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_reports_reported ON chat_message_reports(reported_user_id, status, created_at DESC);

ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_photo_privacy_check;
ALTER TABLE profiles
    ADD CONSTRAINT profiles_photo_privacy_check
    CHECK (photo_privacy IN ('all', 'matches_only', 'request_only', 'private'));

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES
    (
        'branding',
        '{
          "appTitle": "SoulMatch",
          "tagline": "Serious matchmaking for modern families",
          "logoUrl": "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&w=240&q=80",
          "squareLogoUrl": "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&w=240&q=80",
          "previewImageUrl": "https://images.unsplash.com/photo-1529636798458-92182e662485?auto=format&fit=crop&w=1200&q=80",
          "shareBaseUrl": "https://app.soulmatch.app"
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'theme',
        '{
          "primary": "#D4285A",
          "secondary": "#F5A623",
          "accent": "#16324F",
          "background": "#FFF8F4",
          "surface": "#FFFFFF"
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'feature_flags',
        '{
          "chat": true,
          "videoCalling": true,
          "maintenanceMode": false
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'payment_gateways',
        '{
          "razorpay": { "enabled": true, "label": "Razorpay" },
          "stripe": { "enabled": false, "label": "Stripe" }
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'maintenance',
        '{
          "enabled": false,
          "title": "Scheduled maintenance",
          "message": "We are tuning SoulMatch for a smoother experience. Please check back shortly.",
          "startsAt": null,
          "endsAt": null
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'monetization',
        '{
          "currency": "INR",
          "premiumLimits": {
            "dailySwipes": {
              "free": 25,
              "silver": 80,
              "gold": 200,
              "platinum": 500
            },
            "dailyInterests": {
              "free": 5,
              "silver": 20,
              "gold": 999,
              "platinum": 999
            },
            "videoCallsPerMonth": {
              "free": 0,
              "silver": 1,
              "gold": 8,
              "platinum": 30
            }
          },
          "plans": [
            {
              "planId": "free",
              "name": "Free",
              "price": 0,
              "duration": "lifetime",
              "durationDays": 0,
              "features": ["5 interests/day", "Basic search", "View profiles"]
            },
            {
              "planId": "silver",
              "name": "Silver",
              "price": 499,
              "duration": "monthly",
              "durationDays": 30,
              "features": ["20 interests/day", "Advanced search", "See viewers"]
            },
            {
              "planId": "gold",
              "name": "Gold",
              "price": 999,
              "duration": "quarterly",
              "durationDays": 90,
              "features": ["Unlimited interests", "Video calling", "Priority search"]
            },
            {
              "planId": "platinum",
              "name": "Platinum",
              "price": 1499,
              "duration": "yearly",
              "durationDays": 365,
              "features": ["All Gold features", "Anonymous browsing", "Unlimited boosts"]
            }
          ]
        }'::JSONB,
        TRUE,
        'bootstrap'
    ),
    (
        'notification_templates',
        '{
          "someone_liked_you": {
            "title": "Someone liked you",
            "body": "{{name}} liked your profile. Open SoulMatch to respond."
          },
          "match_made": {
            "title": "It''s a match",
            "body": "{{name}} accepted your interest. Say hello on SoulMatch."
          },
          "interest_declined": {
            "title": "Interest update",
            "body": "{{name}} declined your interest. Keep exploring compatible matches."
          },
          "payment_success": {
            "title": "Membership activated",
            "body": "Your {{planName}} membership is now active."
          }
        }'::JSONB,
        FALSE,
        'bootstrap'
    ),
    (
        'seo_defaults',
        '{
          "baseUrl": "https://app.soulmatch.app",
          "defaultTitle": "SoulMatch | Premium matchmaking for modern families",
          "defaultDescription": "Find meaningful matrimonial matches with verified profiles, premium discovery, and family-friendly trust signals.",
          "defaultImageUrl": "https://images.unsplash.com/photo-1529636798458-92182e662485?auto=format&fit=crop&w=1200&q=80",
          "twitterHandle": "@soulmatch"
        }'::JSONB,
        FALSE,
        'bootstrap'
    ),
    (
        'analytics',
        '{
          "enabled": true,
          "retentionDays": 180,
          "dashboardLookbackDays": 30
        }'::JSONB,
        FALSE,
        'bootstrap'
    ),
    (
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
        'bootstrap'
    ),
    (
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
        'bootstrap'
    )
ON CONFLICT (config_key) DO NOTHING;
