INSERT INTO app_config (config_key, config_value, is_public, updated_by)
VALUES (
    'content',
    '{
      "home": {
        "bestMatchMinimumProfiles": 8,
        "bestMatchHighCompatibilityThreshold": 80
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
