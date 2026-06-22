INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
  'monetization',
  '{
    "currency": "INR",
    "accessMode": "subscription",
    "subscriptionModelEnabled": true,
    "premiumLimits": {
      "dailySwipes": {"free": 25, "silver": 80, "gold": 200, "platinum": 500},
      "dailyInterests": {"free": 5, "silver": 0, "gold": 50, "platinum": 80},
      "videoCallsPerMonth": {"free": 0, "silver": 1, "gold": 8, "platinum": 30}
    },
    "plans": [
      {"planId":"free","name":"Bronze","displayName":"Bronze (Free)","price":0,"duration":"lifetime","durationDays":0,"tierRank":0,"features":["Access up to 80 matches","10 profile views","5 shortlists","5 interests"]},
      {"planId":"silver","name":"Pro","displayName":"Pro","price":199,"duration":"monthly","durationDays":30,"tierRank":1,"features":["Contact sharing","Engage+","25 contact details","Gold badge"]},
      {"planId":"gold","name":"Pro Max","displayName":"Pro Max","price":399,"duration":"monthly","durationDays":30,"tierRank":2,"features":["Contact sharing","Engage+","50 contact details","50 Super Interests","1 spotlight","Gold badge"]},
      {"planId":"platinum","name":"Pro Supreme","displayName":"Pro Supreme","price":599,"duration":"monthly","durationDays":30,"tierRank":3,"features":["Contact sharing","Engage+","80 contact details","80 Super Interests","3 spotlights","Gold badge"]}
    ],
    "memberPlanEntitlements": {
      "bronze": {"planId":"bronze","label":"Bronze","visibleMatches":80,"profileViews":10,"contactDetails":0,"engagePlus":false,"shortlist":5,"interests":5,"matchAssistance":false,"chat":false,"spotlightBoosts":0,"verifiedOnly":true},
      "silver": {"planId":"silver","label":"Pro","visibleMatches":80,"profileViews":30,"contactDetails":25,"engagePlus":true,"shortlist":20,"interests":0,"matchAssistance":false,"chat":true,"spotlightBoosts":0,"verifiedOnly":true},
      "gold": {"planId":"gold","label":"Pro Max","visibleMatches":80,"profileViews":50,"contactDetails":50,"engagePlus":true,"shortlist":40,"interests":50,"matchAssistance":true,"chat":true,"spotlightBoosts":1,"verifiedOnly":true},
      "platinum": {"planId":"platinum","label":"Pro Supreme","visibleMatches":80,"profileViews":80,"contactDetails":80,"engagePlus":true,"shortlist":80,"interests":80,"matchAssistance":true,"chat":true,"spotlightBoosts":3,"verifiedOnly":true}
    },
    "membershipFeatureMatrix": [
      {"featureKey":"contact_sharing","label":"Contact Sharing","description":"Share contact after trust and privacy checks.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"engage_plus","label":"Engage+","description":"Engagement and intent insights.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"contact_details","label":"Contact Details","description":"Eligible verified contact unlocks per 30-day cycle.","bronze":"Not available","silver":"25","gold":"50","platinum":"80"},
      {"featureKey":"super_interest","label":"Super Interest","description":"High-intent interest requests available each month.","bronze":"0","silver":"0","gold":"50","platinum":"80"},
      {"featureKey":"spotlight","label":"Spotlights","description":"Profile appears higher in compatible recommendations.","bronze":"0","silver":"0","gold":"1","platinum":"3"},
      {"featureKey":"gold_badge","label":"Gold Badge","description":"Premium badge displayed on the member profile.","bronze":false,"silver":true,"gold":true,"platinum":true}
    ],
    "upgradePackageGroups": [
      {
        "tabKey":"silver",
        "tabTitle":"Pro",
        "bannerTitle":"Start with Pro discovery",
        "bannerText":"A focused one-month Pro plan for members who want contact sharing and safer introductions.",
        "assistiveContent":"Useful when you already have a shortlist and want faster introductions.",
        "packages":[{"pkgId":101,"planId":"silver","pkgName":"Pro 1 Month","pkgActualRate":199,"pkgDiscountedRate":199,"pkgRate":199,"pkgDuration":"30 days","pkgDurationDays":30,"pkgPhoneCount":25,"pkgBenefit":"Unlock contact sharing, Engage+, 25 contact details, and a gold badge.","buyerChoice":true,"badge":"Pro","features":["Contact sharing","Engage+","25 contact details","Gold badge"]}]
      },
      {
        "tabKey":"gold",
        "tabTitle":"Pro Max",
        "bannerTitle":"Most families start here",
        "bannerText":"Pro Max adds stronger contact limits, Super Interests, and one monthly spotlight.",
        "assistiveContent":"Best for families comparing matches seriously with guided support.",
        "packages":[{"pkgId":201,"planId":"gold","pkgName":"Pro Max 1 Month","pkgActualRate":399,"pkgDiscountedRate":399,"pkgRate":399,"pkgDuration":"30 days","pkgDurationDays":30,"pkgPhoneCount":50,"pkgBenefit":"Adds 50 contact details, 50 Super Interests, one spotlight, and a gold badge.","buyerChoice":true,"badge":"Recommended","features":["Contact sharing","Engage+","50 contact details","50 Super Interests","1 spotlight","Gold badge"]}]
      },
      {
        "tabKey":"platinum",
        "tabTitle":"Pro Supreme",
        "bannerTitle":"High-touch assisted matching",
        "bannerText":"Pro Supreme gives the highest monthly limits and strongest discovery reach.",
        "assistiveContent":"Best when the family wants guided support and maximum access.",
        "packages":[{"pkgId":301,"planId":"platinum","pkgName":"Pro Supreme 1 Month","pkgActualRate":599,"pkgDiscountedRate":599,"pkgRate":599,"pkgDuration":"30 days","pkgDurationDays":30,"pkgPhoneCount":80,"pkgBenefit":"Includes 80 contact details, 80 Super Interests, three spotlights, and a gold badge.","buyerChoice":false,"badge":"Top Seller","features":["Contact sharing","Engage+","80 contact details","80 Super Interests","3 spotlights","Gold badge"]}]
      }
    ]
  }'::jsonb,
  TRUE,
  'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
  config_value = app_config.config_value || EXCLUDED.config_value,
  is_public = TRUE,
  updated_at = NOW(),
  updated_by = 'migration';
