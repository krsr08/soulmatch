ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS contact_privacy VARCHAR(20) DEFAULT 'visible';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'profiles_contact_privacy_check'
  ) THEN
    ALTER TABLE profiles
      ADD CONSTRAINT profiles_contact_privacy_check
      CHECK (contact_privacy IN ('visible', 'masked'));
  END IF;
END $$;

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

CREATE INDEX IF NOT EXISTS idx_member_meter_events_user_period
    ON member_meter_events(user_id, event_type, period_key);

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
  'monetization',
  '{
    "plans": [
      {"planId":"free","name":"Bronze","displayName":"Bronze (Free)","price":0,"duration":"lifetime","durationDays":0,"tierRank":0,"features":["Unlimited matches with access up to 80","10 profile views","5 shortlists","5 interests"]},
      {"planId":"silver","name":"Silver","displayName":"Silver","price":299,"duration":"monthly","durationDays":30,"tierRank":1,"features":["Access up to 80 visible matches","30 profile views","15 contact unlocks","Chat enabled","Engage+ insights"]},
      {"planId":"gold","name":"Gold","displayName":"Gold","price":599,"duration":"monthly","durationDays":30,"tierRank":2,"features":["Access up to 80 visible matches","50 profile views","30 contact unlocks","Match assistance","2 spotlight boosts"]},
      {"planId":"platinum","name":"Platinum","displayName":"Platinum","price":999,"duration":"monthly","durationDays":30,"tierRank":3,"features":["Access up to 80 visible matches","80 profile views","80 contact unlocks","Match assistance","4 spotlight boosts"]}
    ],
    "memberPlanEntitlements": {
      "bronze": {"planId":"bronze","label":"Bronze","visibleMatches":80,"profileViews":10,"contactDetails":0,"engagePlus":false,"shortlist":5,"interests":5,"matchAssistance":false,"chat":false,"spotlightBoosts":0},
      "silver": {"planId":"silver","label":"Silver","visibleMatches":80,"profileViews":30,"contactDetails":15,"engagePlus":true,"shortlist":20,"interests":20,"matchAssistance":false,"chat":true,"spotlightBoosts":0},
      "gold": {"planId":"gold","label":"Gold","visibleMatches":80,"profileViews":50,"contactDetails":30,"engagePlus":true,"shortlist":40,"interests":40,"matchAssistance":true,"chat":true,"spotlightBoosts":2},
      "platinum": {"planId":"platinum","label":"Platinum","visibleMatches":80,"profileViews":80,"contactDetails":80,"engagePlus":true,"shortlist":80,"interests":80,"matchAssistance":true,"chat":true,"spotlightBoosts":4}
    },
    "membershipFeatureMatrix": [
      {"featureKey":"visible_matches","label":"Visible Matches","description":"Recommended profiles visible in discovery.","bronze":"Access up to 80","silver":"Access up to 80","gold":"Access up to 80","platinum":"Access up to 80"},
      {"featureKey":"profile_views","label":"Profile Views","description":"Detailed profile views per 30-day cycle.","bronze":"10","silver":"30","gold":"50","platinum":"80"},
      {"featureKey":"contact_details","label":"Contact Details","description":"Eligible contact unlocks per 30-day cycle.","bronze":"Not available","silver":"15","gold":"30","platinum":"80"},
      {"featureKey":"engage_plus","label":"Engage+","description":"Engagement and intent insights.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"shortlist","label":"Shortlist","description":"Profiles saved per 30-day cycle.","bronze":"5","silver":"20","gold":"40","platinum":"80"},
      {"featureKey":"interests","label":"Interests","description":"Interests sent per 30-day cycle.","bronze":"5","silver":"20","gold":"40","platinum":"80"},
      {"featureKey":"match_assistance","label":"Match Assistance","description":"SoulMatch assisted matching access.","bronze":false,"silver":false,"gold":true,"platinum":true},
      {"featureKey":"chat","label":"Chat","description":"Chat after mutual acceptance.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"spotlight_boost","label":"Spotlight Boost","description":"Monthly spotlight credits.","bronze":"No","silver":"No","gold":"2 / month","platinum":"4 / month"}
    ]
  }'::jsonb,
  TRUE,
  'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
  config_value = app_config.config_value
    || jsonb_build_object(
      'plans', EXCLUDED.config_value->'plans',
      'memberPlanEntitlements', EXCLUDED.config_value->'memberPlanEntitlements',
      'membershipFeatureMatrix', EXCLUDED.config_value->'membershipFeatureMatrix'
    ),
  is_public = TRUE,
  updated_at = NOW(),
  updated_by = 'migration';
