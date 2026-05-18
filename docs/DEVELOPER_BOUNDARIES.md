# SoulMatch Developer Boundaries

This document defines where future developers should make changes. Keep ownership clear so a change in one feature does not quietly break another flow.

## Golden Rules

1. Keep user-facing behavior changes separate from structure-only changes.
2. Put reusable backend code in `backend/shared/` only when at least two services need it.
3. Do not let Android decide privacy, entitlement, or verification truth. The backend must enforce it.
4. Keep payment, OTP, KYC, and admin actions auditable.
5. Prefer adapters for future infrastructure such as Blob storage, OpenSearch, workers, and BFF.

## Service Ownership

| Service | Owns | Should Not Own |
| --- | --- | --- |
| `auth-service` | OTP, login, Google/Firebase auth, JWT, refresh tokens, duplicate account signals | Profile details, matching rules, payments |
| `profile-service` | Member profiles, agent profiles, documents, photos, privacy, partner preferences, trust inputs | Payment charging, chat messages |
| `matching-service` | Compatibility, recommendations, interest acceptance transaction, match explanations | Raw profile editing, OTP |
| `search-service` | Search/filter APIs and filter option discovery | Match scoring, profile mutations |
| `chat-service` | Conversations, messages, socket events, chat eligibility checks | Profile KYC, payment orders |
| `notification-service` | Push/inbox templates, outbox delivery, device registration | Business decisions that create notifications |
| `payment-service` | Plans, orders, Razorpay verify/webhook, subscription expiry | UI merchandising cards |
| `admin-service` | Admin login, RBAC, moderation, verification operations, CMS/config, audit logs | Member app screen rendering |
| `backend/shared` | Config schemas, entitlements, profile visibility, observability helpers | Service-specific business workflows |

## Database Ownership

PostgreSQL is currently shared. Treat table ownership as logical ownership until schemas are split later.

| Table Area | Logical Owner |
| --- | --- |
| `users`, token/session/consent rows | `auth-service` |
| `profiles`, physical/education/family/lifestyle details | `profile-service` |
| `profile_photos`, documents, verifications | `profile-service` and admin moderation |
| `partner_preferences`, match feedback | `profile-service` writes, `matching-service` reads |
| `interests`, match events | `matching-service` |
| `subscriptions`, `payment_orders`, transactions | `payment-service` |
| `notifications`, outbox/DLQ/templates | `notification-service` |
| `admin_users`, roles, permissions, audit logs | `admin-service` |
| `app_config`, CMS/runtime config | `admin-service` writes, all services read through shared control plane |
| Chat message body | `chat-service` in MongoDB |

## API Responsibility

| API Group | Service |
| --- | --- |
| `/api/v1/auth/*` | `auth-service` |
| `/api/v1/profile/*` | `profile-service` |
| `/api/v1/matches/*` | `matching-service` |
| `/api/v1/interests/*` | `matching-service` |
| `/api/v1/search/*` | `search-service` |
| `/api/v1/chat/*` and `/socket.io/*` | `chat-service` |
| `/api/v1/notifications/*` | `notification-service` |
| `/api/v1/payment/*` | `payment-service` |
| `/api/v1/admin/*` | `admin-service` |
| `/api/v1/public/config` | `admin-service` public runtime config |

## Android Screen Mapping

| Android Area | Folder | Primary Backend |
| --- | --- | --- |
| Login, OTP, role selection | `ui/screens/auth` | `auth-service`, `profile-service` |
| Member home and best matches | `ui/screens/home` | `matching-service`, `profile-service`, `admin-service` config |
| Profile detail and my profile | `ui/screens/profile` | `profile-service`, `matching-service` |
| Agent dashboard and agent profile flows | `ui/screens/agent` | `profile-service`, `admin-service` |
| Chat list/thread | `ui/screens/chat` | `chat-service`, `notification-service` |
| Activity/interests | `ui/screens/interests` | `matching-service` |
| Search/discover/refine filters | `ui/screens/search` | `search-service` |
| Subscription/upgrade | `ui/screens/subscription` | `payment-service`, `admin-service` config |
| Settings/safety/privacy | `ui/screens/settings` | `profile-service`, `notification-service`, `admin-service` config |
| Launch/maintenance | `ui/screens/system` | `admin-service` runtime config |

## Admin Page Mapping

| Admin Area | Primary Backend Responsibility |
| --- | --- |
| Overview/dashboard | `admin-service` aggregates Postgres/service health |
| Members | `admin-service` reads/writes profile records through admin routes |
| Agents | `admin-service` + `profile-service` agent records |
| Verification | `admin-service` actions on profile/doc verification queues |
| Payments/subscriptions | `payment-service` data surfaced through `admin-service` |
| Moderation | `admin-service` with profile/chat/report queues |
| CMS/runtime config | `admin-service` writes `app_config` |
| Analytics | `admin-service` SQL funnel and analytics events |
| System/health | `admin-service` polls services and exposes deployment/version audit |
| Role master/RBAC | `admin-service` admin user, role, permission tables |

## Shared Backend Boundary

Use `backend/shared/` for:

- `controlPlane.js`: runtime config read/normalize helpers.
- `architectureFlags.js`: cost-safe infrastructure mode placeholders for future adapters.
- `memberEntitlements.js`: plan/feature access rules.
- `profileVisibility.js`: privacy and redaction rules.
- `observability.js`: Express metrics/log middleware.
- `configSchemas/`: schema contracts for CMS/runtime config.

Do not put service-specific controller logic in `backend/shared/`.

## Future Extension Placement

These folders are reserved as future extension points. Do not create paid infrastructure just because these are documented.

```text
backend/
  mobile-bff/                  # Optional future API aggregator for Android
  workers/
    notification-worker/        # Async FCM/email/SMS delivery
    trust-score-worker/         # Async trust score recalculation
    search-index-worker/        # Future OpenSearch indexing
    payment-webhook-worker/     # Async webhook reconciliation/retry
```

Adapter placement:

| Future Capability | Adapter Location |
| --- | --- |
| Blob/S3 storage | `backend/profile-service/src/services/mediaService.js` or `storageAdapter.js` |
| OpenSearch | `backend/search-service/src/services/searchIndexAdapter.js` |
| Queue provider | `backend/shared/queueAdapter.js` after at least two services need queues |
| WAF mode | Nginx config and deployment docs first, then env config if runtime needs it |
| Mobile BFF | `backend/mobile-bff/` only when Android calls become too chatty |

## Commenting Rule

Add comments only for:

- Security-sensitive logic.
- Privacy/redaction decisions.
- Payment or webhook idempotency.
- Queue/outbox behavior.
- Non-obvious compatibility or matching formulas.

Avoid comments that repeat the code.
