-- Runtime CMS controls for best-match inserts, scam awareness, and member tier gates.

WITH content_payload AS (
  SELECT '{
    "home": {
      "bestMatchInsertFrequency": 2,
      "showBestMatchInsertCards": true,
      "showBestMatchUpgradeCards": true,
      "showBestMatchAdCards": true,
      "bestMatchAdCards": [
        {"id":"upgrade-bronze-to-silver","type":"upgrade","enabled":true,"targetPlans":["free","bronze"],"minPlan":"free","maxPlan":"free","theme":"rose","badge":"Starter upgrade","title":"Move from Bronze to Silver","body":"Unlock more daily interests, better visibility, and contact access for serious conversations.","bullets":["20 contact views","More visible matches","Profile visitor insights","Priority support"],"cta":"Upgrade to Silver","discountLabel":"SILVER BENEFITS","destination":"membership"},
        {"id":"upgrade-silver-to-gold","type":"upgrade","enabled":true,"targetPlans":["free","bronze","silver"],"minPlan":"free","maxPlan":"silver","theme":"gold","badge":"Recommended","title":"The Gold Tier Experience","body":"Get stronger reach, unlimited discovery, and richer match actions for your family shortlist.","bullets":["Priority reach","More contact views","Profile boost tools","Advanced filters"],"cta":"Explore Gold","discountLabel":"MOST CHOSEN","destination":"membership"},
        {"id":"upgrade-gold-to-platinum","type":"upgrade","enabled":true,"targetPlans":["free","bronze","silver","gold"],"minPlan":"free","maxPlan":"gold","theme":"dark","badge":"Elite access","title":"Platinum for families who want full access","body":"Unlock unlimited contact discovery, premium visibility, and high-touch assisted benefits.","bullets":["Unlimited contacts","Featured placement","Concierge support","Deep match insights"],"cta":"Go Platinum","discountLabel":"PLATINUM","destination":"membership"},
        {"id":"spotlight-day-pass","type":"spotlight","enabled":true,"targetPlans":["free","bronze","silver","gold"],"minPlan":"free","maxPlan":"gold","theme":"sunrise","badge":"Spotlight","title":"Be the first profile others see for an entire day","body":"Appear on top of recommendations and increase your chances of receiving more interests.","cta":"Get Spotlight","destination":"membership"},
        {"id":"contact-unlock","type":"membership","enabled":true,"targetPlans":["free","bronze","silver"],"minPlan":"free","maxPlan":"silver","theme":"blue","badge":"Contact access","title":"Ready to speak with the right family?","body":"Upgrade to unlock eligible contact views after privacy and trust checks.","bullets":["Verified phone access","Privacy-first contact rules","Safer introductions"],"cta":"Unlock contacts","destination":"membership"},
        {"id":"profile-boost","type":"membership","enabled":true,"targetPlans":["free","bronze","silver"],"minPlan":"free","maxPlan":"silver","theme":"rose","badge":"Boost","title":"Get noticed by more compatible families","body":"Boosted profiles receive higher placement in compatible recommendations.","bullets":["Higher listing priority","More profile views","Better response chances"],"cta":"Boost my profile","destination":"membership"},
        {"id":"horoscope-family-match","type":"astrology","enabled":true,"targetPlans":["free","bronze","silver","gold","platinum"],"theme":"purple","badge":"Horoscope","title":"Add horoscope details for family compatibility","body":"Help families compare birth details, rashi, nakshatra, and kundli expectations.","cta":"Open astrology","destination":"astrology_services"},
        {"id":"verified-trust-profile","type":"trust","enabled":true,"targetPlans":["free","bronze","silver","gold","platinum"],"theme":"green","badge":"Trust profile","title":"Verified profiles receive more confident responses","body":"Complete phone, email, photo, document, education, income, and family trust checks.","cta":"Improve trust","destination":"my_profile"},
        {"id":"private-photo-control","type":"privacy","enabled":true,"targetPlans":["free","bronze","silver","gold","platinum"],"theme":"ivory","badge":"Privacy","title":"Keep photos private until you are ready","body":"Use request-based photo access so families can review visibility safely.","cta":"Manage photos","destination":"my_profile"},
        {"id":"assisted-discovery","type":"assist","enabled":true,"targetPlans":["silver","gold","platinum"],"minPlan":"silver","theme":"peach","badge":"SoulMatch Assist","title":"Need offline help from a local agent?","body":"Share your profile with selected registered agents for offline introductions.","cta":"Open Assist","destination":"soulmatch_assist"},
        {"id":"wedding-readiness","type":"marriage","enabled":true,"targetPlans":["free","bronze","silver","gold","platinum"],"theme":"maroon","badge":"Family planning","title":"Shortlist services after both families connect","body":"Keep venues, photography, and ceremony planning separate from discovery until you are ready.","cta":"View ideas","destination":"search"},
        {"id":"success-stories","type":"story","enabled":true,"targetPlans":["free","bronze","silver","gold","platinum"],"theme":"cream","badge":"Success stories","title":"See how families used SoulMatch safely","body":"Browse real journey patterns and learn what details create better responses.","cta":"View stories","destination":"success_stories"}
      ],
      "scamAwarenessCards": [
        {"id":"gift-cod","enabled":true,"title":"Never make payments for unsolicited gifts","body":"Scammers may send gifts by cash-on-delivery and pressure you to pay."},
        {"id":"import-duty","enabled":true,"title":"Do not pay import duty or custom fees","body":"Fraudsters may pose as officials and demand fees for gifts or parcels."},
        {"id":"video-call","enabled":true,"title":"Be cautious during video calls","body":"Avoid explicit calls and report anyone who blackmails or asks for money."},
        {"id":"emergency-cash","enabled":true,"title":"Validate emergency cash requests","body":"Never transfer money because of sudden medical, travel, or family emergencies."},
        {"id":"advance-fee","enabled":true,"title":"Agents must not demand advance fees","body":"Use SoulMatch-listed agents carefully and report anyone asking for unofficial payments."},
        {"id":"bank-transfer","enabled":true,"title":"Avoid direct bank transfers to new contacts","body":"Do not send money for visas, tickets, gifts, loans, medical stories, or emergencies."}
      ]
    },
    "navigation": {
      "upgrade": "Upgrade"
    }
  }'::jsonb AS payload
)
INSERT INTO app_config (config_key, config_value, is_public, updated_by)
SELECT 'content', payload, TRUE, 'migration'
FROM content_payload
ON CONFLICT (config_key)
DO UPDATE SET
  config_value = app_config.config_value
    || jsonb_build_object(
      'home',
      COALESCE(app_config.config_value->'home', '{}'::jsonb) || (EXCLUDED.config_value->'home'),
      'navigation',
      COALESCE(app_config.config_value->'navigation', '{}'::jsonb) || (EXCLUDED.config_value->'navigation')
    ),
  is_public = TRUE,
  updated_at = NOW(),
  updated_by = 'migration';

WITH monetization_payload AS (
  SELECT '{
    "plans": [
      {"planId":"free","name":"Bronze","displayName":"Bronze (Free)","price":0,"duration":"lifetime","durationDays":0,"tierRank":0,"features":["10 visible matches","5 interests/day","Basic discovery","Private photo requests"]},
      {"planId":"silver","name":"Silver","displayName":"Silver","price":999,"duration":"monthly","durationDays":30,"tierRank":1,"features":["50 visible matches","20 contact views","Advanced filters","SoulMatch Assist opt-in"]},
      {"planId":"gold","name":"Gold","displayName":"Gold","price":2499,"duration":"monthly","durationDays":30,"tierRank":2,"features":["Unlimited visible matches","100 contact views","Priority listing","Spotlight credits"]},
      {"planId":"platinum","name":"Platinum","displayName":"Platinum","price":4999,"duration":"monthly","durationDays":30,"tierRank":3,"features":["Unlimited contacts","Featured placement","Concierge support","Unlimited boosts"]}
    ],
    "membershipFeatureMatrix": [
      {"featureKey":"visible_matches","label":"Visible Matches","description":"Number of recommended profiles visible in discovery.","bronze":"10","silver":"50","gold":"Unlimited","platinum":"Unlimited"},
      {"featureKey":"contact_views","label":"Contact Views","description":"Eligible verified contact views per month.","bronze":"0","silver":"20","gold":"100","platinum":"Unlimited"},
      {"featureKey":"daily_interests","label":"Daily Interests","description":"Interest requests a member can send each day.","bronze":"5","silver":"20","gold":"Unlimited","platinum":"Unlimited"},
      {"featureKey":"advanced_filters","label":"Advanced Filters","description":"Filter by work, family, activity, horoscope, trust and privacy attributes.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"chat_after_match","label":"Chat After Match","description":"Message after mutual interest acceptance.","bronze":true,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"photo_request","label":"Private Photo Requests","description":"Request access to private photos.","bronze":true,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"spotlight","label":"Spotlight Boosts","description":"Profile appears higher in compatible recommendations.","bronze":false,"silver":"Paid add-on","gold":"3 / month","platinum":"Unlimited"},
      {"featureKey":"anonymous_browsing","label":"Anonymous Browsing","description":"Browse profiles without showing visitor identity.","bronze":false,"silver":false,"gold":true,"platinum":true},
      {"featureKey":"soulmatch_assist","label":"SoulMatch Assist","description":"Share profile with selected registered agents for offline support.","bronze":false,"silver":true,"gold":true,"platinum":true},
      {"featureKey":"priority_listing","label":"Priority Listing","description":"Higher visibility in best-match and recently active feeds.","bronze":false,"silver":false,"gold":true,"platinum":true},
      {"featureKey":"concierge_support","label":"Concierge Support","description":"Dedicated help for high-intent families.","bronze":false,"silver":false,"gold":false,"platinum":true},
      {"featureKey":"trust_badge_boost","label":"Trust Badge Boost","description":"Trust-complete profiles receive visual priority and better response prompts.","bronze":true,"silver":true,"gold":true,"platinum":true}
    ]
  }'::jsonb AS payload
)
INSERT INTO app_config (config_key, config_value, is_public, updated_by)
SELECT 'monetization', payload, TRUE, 'migration'
FROM monetization_payload
ON CONFLICT (config_key)
DO UPDATE SET
  config_value = app_config.config_value
    || jsonb_build_object(
      'plans', EXCLUDED.config_value->'plans',
      'membershipFeatureMatrix', EXCLUDED.config_value->'membershipFeatureMatrix'
    ),
  is_public = TRUE,
  updated_at = NOW(),
  updated_by = 'migration';
