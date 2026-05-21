# SoulMatch Backend Service Map

This folder contains the backend service tier. Each service owns a specific business boundary. Keep changes inside the owning service unless a shared rule is intentionally required.

## Services

| Folder | Owns | Common Change Types |
| --- | --- | --- |
| `auth-service` | OTP, login, Google/Firebase auth, JWT, refresh token rotation, duplicate account signals | OTP delivery, role selection auth decisions, logout/session changes |
| `profile-service` | Member profiles, agent profiles, KYC, documents, photos, privacy, partner preferences, trust inputs | Profile wizard, agent onboarding, photo privacy, contact masking, document uploads |
| `matching-service` | Compatibility, recommendations, interest acceptance, match explanations | Match score changes, interest-to-chat flow, recommendation ordering |
| `search-service` | Search/filter APIs and filter option discovery | Refine matches, filter tabs, search result constraints |
| `chat-service` | Conversations, messages, socket events, chat eligibility | Messenger screens, chat permissions, socket behavior |
| `notification-service` | Push/inbox templates, device registration, notification delivery | FCM registration, notification inbox, template changes |
| `payment-service` | Razorpay orders, webhooks, subscriptions, invoices, expiry | Upgrade page, contact unlock limits, plan lifecycle |
| `admin-service` | Admin login, RBAC, moderation, CMS/config, verification, audit logs | Admin dashboard, CMS, member/agent verification, system pages |
| `shared` | Cross-service rules and config helpers | Privacy redaction, entitlements, runtime config, observability |

## Standard Node Service Shape

Most Node services use:

```text
src/
  app.js              Express app and service-level middleware
  config/             database, redis, environment setup
  controllers/        HTTP request/response orchestration
  middleware/         auth, validation, service auth, errors
  repositories/       database reads/writes
  routes/             endpoint declarations
  services/           domain helpers and external integrations
  utils/              small pure helpers
test/                 service-level tests
```

## Backend Rules

1. Backend must enforce privacy, contact masking, block visibility, verification, and plan entitlements.
2. Android/Admin UI can guide a user, but cannot be the source of truth for paid or private behavior.
3. Put reusable logic in `backend/shared/` only when at least two services need it.
4. Add tests in the owning service for every behavior change.
5. Keep database schema changes in `database/migrations/`; avoid runtime DDL in service startup.

## Useful References

- `docs/MODULE_OWNERSHIP_MAP.md`
- `docs/DEVELOPER_BOUNDARIES.md`
- `docs/RUNTIME_CONFIG_CONTROL_PLANE.md`
- `docs/ARCHITECTURE_FOUNDATION.md`

