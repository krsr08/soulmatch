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
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
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
    verification_status VARCHAR(24) DEFAULT 'pending',
    admin_status VARCHAR(24) DEFAULT 'active',
    moderation_score NUMERIC(5,2) DEFAULT 0,
    primary_photo_url TEXT,
    video_url TEXT,
    photo_privacy VARCHAR(20) DEFAULT 'all',
    profile_visibility VARCHAR(20) DEFAULT 'all',
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

CREATE TABLE IF NOT EXISTS partner_preferences (
    pref_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(profile_id) ON DELETE CASCADE UNIQUE,
    age_min INTEGER DEFAULT 18,
    age_max INTEGER DEFAULT 50,
    religion VARCHAR(50),
    manglik_pref VARCHAR(20) DEFAULT 'any',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
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

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_gender ON profiles(gender);
CREATE INDEX IF NOT EXISTS idx_profiles_religion ON profiles(religion);
CREATE INDEX IF NOT EXISTS idx_profiles_published ON profiles(is_published);
CREATE INDEX IF NOT EXISTS idx_saved_searches_user ON saved_searches(user_id);
CREATE INDEX IF NOT EXISTS idx_interests_sender ON interests(sender_id);
CREATE INDEX IF NOT EXISTS idx_interests_receiver ON interests(receiver_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_status_created ON transactions(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_status ON payment_orders(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_orders_provider ON payment_orders(provider_order_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_verifications_status_created ON verifications(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_verifications_user_status ON verifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocked ON blocks(blocked_id);
CREATE INDEX IF NOT EXISTS idx_profile_photos_profile ON profile_photos(profile_id);
CREATE INDEX IF NOT EXISTS idx_reports_status_created ON reports(status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_views_pair ON profile_views(viewer_id, viewed_profile_id);
CREATE INDEX IF NOT EXISTS idx_referral_codes_owner ON referral_codes(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_referral_redemptions_code ON referral_redemptions(referral_code_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_service_name ON analytics_events(service_name);
CREATE INDEX IF NOT EXISTS idx_analytics_created_at ON analytics_events(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_alerts_status ON admin_alerts(status, severity);
CREATE INDEX IF NOT EXISTS idx_profiles_admin_status ON profiles(admin_status, verification_status);
CREATE INDEX IF NOT EXISTS idx_profiles_match_search ON profiles(gender, is_published, admin_status, verification_status, religion, caste);

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
    )
ON CONFLICT (config_key) DO NOTHING;
