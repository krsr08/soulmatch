ALTER TABLE users
    ADD COLUMN IF NOT EXISTS referred_by_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS acquisition_source VARCHAR(80),
    ADD COLUMN IF NOT EXISTS referred_at TIMESTAMP;

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

CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_views_pair ON profile_views(viewer_id, viewed_profile_id);
CREATE INDEX IF NOT EXISTS idx_referral_codes_owner ON referral_codes(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_referral_redemptions_code ON referral_redemptions(referral_code_id);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_service_name ON analytics_events(service_name);
CREATE INDEX IF NOT EXISTS idx_analytics_created_at ON analytics_events(created_at DESC);

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
        'migration'
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
        'migration'
    ),
    (
        'feature_flags',
        '{
          "chat": true,
          "videoCalling": true,
          "maintenanceMode": false
        }'::JSONB,
        TRUE,
        'migration'
    ),
    (
        'payment_gateways',
        '{
          "razorpay": { "enabled": true, "label": "Razorpay" },
          "stripe": { "enabled": false, "label": "Stripe" }
        }'::JSONB,
        TRUE,
        'migration'
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
        'migration'
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
        'migration'
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
        'migration'
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
        'migration'
    ),
    (
        'analytics',
        '{
          "enabled": true,
          "retentionDays": 180,
          "dashboardLookbackDays": 30
        }'::JSONB,
        FALSE,
        'migration'
    )
ON CONFLICT (config_key) DO NOTHING;

UPDATE app_config
SET
    config_value = jsonb_set(
        config_value,
        '{interest_declined}',
        '{
          "title": "Interest update",
          "body": "{{name}} declined your interest. Keep exploring compatible matches."
        }'::JSONB,
        TRUE
    ),
    updated_at = NOW(),
    updated_by = 'migration'
WHERE config_key = 'notification_templates'
  AND NOT (config_value ? 'interest_declined');

INSERT INTO landing_pages (
    slug,
    title,
    subtitle,
    description,
    hero_image_url,
    preview_image_url,
    cta_label,
    cta_url,
    seo_title,
    seo_description,
    is_active
)
VALUES
    (
        'home',
        'SoulMatch for serious families',
        'Verified profiles, premium discovery, and a thoughtful path to marriage.',
        'SoulMatch brings together trusted matrimonial discovery, elegant storytelling, and premium relationship signals for families who want clarity.',
        'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1600&q=80',
        'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=80',
        'Open app',
        'https://app.soulmatch.app',
        'SoulMatch | Verified matrimonial discovery',
        'Share a polished SoulMatch landing page with dynamic branding and campaign attribution ready for WhatsApp and LinkedIn.',
        TRUE
    )
ON CONFLICT (slug) DO NOTHING;
