CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_razorpay_payment_id
    ON transactions(razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL;

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'monetization',
    '{
      "upgradePackageGroups": [
        {
          "tabKey": "one_month",
          "tabTitle": "1 Month",
          "bannerTitle": "Start with verified discovery",
          "bannerText": "A focused one-month plan for members who want verified contact access and better visibility.",
          "assistiveContent": "Useful when you already have a shortlist and want faster introductions.",
          "packages": [
            {
              "pkgId": 101,
              "planId": "silver",
              "pkgName": "Verified Plus 1 Month",
              "pkgActualRate": 799,
              "pkgDiscountedRate": 499,
              "pkgRate": 499,
              "pkgDuration": "30 days",
              "pkgDurationDays": 30,
              "pkgPhoneCount": 20,
              "pkgBenefit": "Unlock verified contact views, advanced search, and visitor insights for one focused month.",
              "buyerChoice": true,
              "badge": "Starter",
              "features": ["20 interests/day", "Advanced search", "See viewers"]
            }
          ]
        },
        {
          "tabKey": "three_months",
          "tabTitle": "3 Months",
          "bannerTitle": "Most families start here",
          "bannerText": "Three months gives enough time to shortlist, speak, and involve both families without rushing.",
          "assistiveContent": "Best for families comparing matches seriously over a full quarter.",
          "packages": [
            {
              "pkgId": 201,
              "planId": "gold",
              "pkgName": "Family Assist 3 Months",
              "pkgActualRate": 1499,
              "pkgDiscountedRate": 999,
              "pkgRate": 999,
              "pkgDuration": "90 days",
              "pkgDurationDays": 90,
              "pkgPhoneCount": 60,
              "pkgBenefit": "Adds unlimited interests, video calling, priority search, and family decision support.",
              "buyerChoice": true,
              "badge": "Recommended",
              "features": ["Unlimited interests", "Video calling", "Priority search"]
            }
          ]
        },
        {
          "tabKey": "elite",
          "tabTitle": "Elite",
          "bannerTitle": "High-touch assisted matching",
          "bannerText": "A premium plan for members who want deeper visibility, anonymous browsing, and advisor-assisted discovery.",
          "assistiveContent": "Best when the family wants guided support and a longer membership window.",
          "packages": [
            {
              "pkgId": 301,
              "planId": "platinum",
              "pkgName": "Advisor Assisted Elite",
              "pkgActualRate": 2499,
              "pkgDiscountedRate": 1499,
              "pkgRate": 1499,
              "pkgDuration": "365 days",
              "pkgDurationDays": 365,
              "pkgPhoneCount": 150,
              "pkgBenefit": "Includes all Gold features, anonymous browsing, premium boosts, and advisor-assisted discovery.",
              "buyerChoice": false,
              "badge": "Elite",
              "features": ["All Gold features", "Anonymous browsing", "Unlimited boosts"]
            }
          ]
        }
      ]
    }'::JSONB,
    TRUE,
    'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
    config_value = jsonb_set(
        app_config.config_value,
        '{upgradePackageGroups}',
        EXCLUDED.config_value->'upgradePackageGroups',
        true
    ),
    is_public = TRUE,
    updated_at = NOW(),
    updated_by = 'migration';

INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'client_integrations',
    '{"supportEmail": "support@soulmatch.app"}'::JSONB,
    TRUE,
    'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
    config_value = app_config.config_value || EXCLUDED.config_value,
    is_public = TRUE,
    updated_at = NOW(),
    updated_by = 'migration';
