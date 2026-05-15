INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
  'content',
  '{
    "notificationPrompt": {
      "enabled": true,
      "title": "Don''t miss important match updates",
      "subtitle": "Turn on alerts for interests, acceptances, and family messages.",
      "bullets": [
        "Get alerts when a new interest or recommendation arrives",
        "Know when a family accepts your interest",
        "Be notified when a profile sends you a message"
      ],
      "allowCta": "Allow notifications",
      "laterCta": "Maybe later"
    },
    "safetyCenter": {
      "title": "Safety Center",
      "subtitle": "Explore practical tools and guidance that help you stay safe while matching.",
      "tiles": [
        {
          "id": "online_personal_tips",
          "title": "Online / Personal Tips",
          "subtitle": "Safe habits before sharing personal details.",
          "icon": "tips",
          "tone": "gold",
          "destination": "article:online_personal_tips"
        },
        {
          "id": "privacy_settings",
          "title": "Privacy Settings",
          "subtitle": "Control photos, alerts, and visibility.",
          "icon": "shield",
          "tone": "green",
          "destination": "article:privacy_settings"
        },
        {
          "id": "report_block",
          "title": "Report / Block Profile",
          "subtitle": "Act quickly on suspicious behavior.",
          "icon": "report",
          "tone": "rose",
          "destination": "article:report_block"
        },
        {
          "id": "mental_wellbeing",
          "title": "Mental Wellbeing",
          "subtitle": "Move at a pace that feels right.",
          "icon": "heart",
          "tone": "purple",
          "destination": "article:mental_wellbeing"
        }
      ],
      "verificationCard": {
        "title": "Help us make SoulMatch safe and authentic",
        "body": "Verified profiles help families move forward with more confidence. Complete your trust details when you are ready.",
        "cta": "Verify yourself",
        "destination": "my_profile"
      },
      "resourcesTitle": "We''re here for you",
      "resources": [
        {
          "id": "cyber_crime",
          "title": "Other resources",
          "subtitle": "Cyber cell contacts to help you take action.",
          "icon": "help",
          "destination": "article:cyber_crime"
        }
      ],
      "articles": [
        {
          "id": "online_personal_tips",
          "title": "Online / Personal Tips",
          "subtitle": "Use simple checks before sharing contact details or meeting offline.",
          "bullets": [
            "Keep early conversations inside SoulMatch until both families are comfortable.",
            "Do not send money, gifts, documents, or OTPs to a new contact.",
            "Speak with family members before moving to private calls or meetings."
          ]
        },
        {
          "id": "privacy_settings",
          "title": "Privacy Settings",
          "subtitle": "Choose how much of your profile is visible while you are still deciding.",
          "bullets": [
            "Use private photos when you want matches to request access first.",
            "Pause push notifications when you do not want alerts on this device.",
            "Hide or block profiles when you do not want further interaction."
          ],
          "primaryCta": "Open privacy settings",
          "destination": "settings"
        },
        {
          "id": "report_block",
          "title": "Report / Block Profile",
          "subtitle": "Report suspicious requests, abusive messages, fake profiles, or money demands.",
          "bullets": [
            "Block a member immediately when the interaction feels unsafe.",
            "Report profiles that ask for payments, gifts, loans, visas, or emergency help.",
            "SoulMatch reviews safety reports and may restrict accounts."
          ]
        },
        {
          "id": "mental_wellbeing",
          "title": "Mental Wellbeing",
          "subtitle": "Matchmaking can be emotional. Keep the process steady and family-supported.",
          "bullets": [
            "Take breaks from discovery when conversations feel overwhelming.",
            "Avoid pressure to decide quickly or share details before you are ready.",
            "Use trusted family support for important decisions."
          ]
        },
        {
          "id": "cyber_crime",
          "title": "Take action against cyber crime",
          "subtitle": "Use official channels to report illegal online activity.",
          "body": "If a profile threatens, blackmails, impersonates, or asks for money, preserve screenshots and report through official cyber crime channels.",
          "contacts": [
            { "label": "National cyber crime helpline", "value": "1930", "type": "phone" },
            { "label": "Cyber crime website", "value": "https://cybercrime.gov.in/", "type": "url" }
          ]
        }
      ]
    }
  }'::jsonb,
  TRUE,
  'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
  config_value = app_config.config_value || jsonb_build_object(
    'notificationPrompt', EXCLUDED.config_value -> 'notificationPrompt',
    'safetyCenter', EXCLUDED.config_value -> 'safetyCenter'
  ),
  is_public = TRUE,
  updated_at = NOW(),
  updated_by = 'migration';
