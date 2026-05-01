# SoulMatch Owner Control Plane

## What is now database-driven

- `app_config.branding`: app title, logos, preview art, share base URL.
- `app_config.theme`: primary, secondary, accent, background, surface colors.
- `app_config.feature_flags`: chat, video calling, maintenance mode toggle.
- `app_config.payment_gateways`: gateway-level enable/disable state.
- `app_config.maintenance`: title, message, and optional schedule window.
- `app_config.monetization`: plans, pricing, duration days, premium limits.
- `app_config.notification_templates`: editable push notification copy.
- `app_config.seo_defaults`: default social and OpenGraph metadata.

## New platform tables

- `app_config`
- `landing_pages`
- `referral_codes`
- `referral_redemptions`
- `analytics_events`

## Admin API surfaces

- `GET /api/v1/admin/config`
- `PUT /api/v1/admin/config/:key`
- `GET /api/v1/admin/landing-pages`
- `POST /api/v1/admin/landing-pages`
- `PUT /api/v1/admin/landing-pages/:slug`
- `GET /api/v1/admin/referrals`
- `POST /api/v1/admin/referrals/codes`
- `GET /api/v1/admin/analytics/funnel`
- `GET /api/v1/admin/analytics/events`
- `GET /api/v1/admin/service-health`

## Public runtime surfaces

- `GET /api/v1/public/config`
- `GET /api/v1/public/profiles/:profileId`
- `GET /api/v1/public/landing-pages/:slug`
- `GET /share/profile/:profileId`
- `GET /share/landing/:slug`

## Deployment notes

- Fresh database bootstrap uses `database/schema.sql` and `database/seeds/seed_data.sql`.
- Existing databases should apply `database/migrations/001_control_plane.sql`.
- Production compose path: `docker/docker-compose.prod.yml`
- Shared runtime env file: `docker/production.env`
- Recommended startup:
  `docker compose --env-file docker/production.env -f docker/docker-compose.prod.yml up --build`
