INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'content',
    '{
      "home": {
        "bestMatchMinimumProfiles": 5,
        "bestMatchInsertFrequency": 2,
        "showBestMatchInsertCards": true,
        "showBestMatchUpgradeCards": true,
        "showBestMatchAdCards": true,
        "bestMatchAdCards": [
          {
            "id": "wedding-services",
            "type": "marriage",
            "title": "Wedding services nearby",
            "body": "Shortlist decorators, photographers, and venues once both families are ready.",
            "cta": "View ideas",
            "destination": "search"
          },
          {
            "id": "family-horoscope",
            "type": "astrology",
            "title": "Family horoscope support",
            "body": "Add horoscope details and compare compatibility before the first family call.",
            "cta": "Open astrology",
            "destination": "astrology_services"
          },
          {
            "id": "verified-profiles",
            "type": "profiles",
            "title": "Verified profiles first",
            "body": "Focus on members with stronger trust signals and active intent.",
            "cta": "Browse profiles",
            "destination": "search"
          }
        ]
      }
    }'::JSONB,
    TRUE,
    'migration'
)
ON CONFLICT (config_key)
DO UPDATE SET
    config_value = jsonb_set(
        app_config.config_value,
        '{home}',
        COALESCE(app_config.config_value->'home', '{}'::JSONB) || (EXCLUDED.config_value->'home'),
        TRUE
    ),
    is_public = TRUE,
    updated_at = NOW(),
    updated_by = 'migration';
