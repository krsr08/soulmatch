# SoulMatch Extension Points

This document lists the architecture pieces that are intentionally designed as placeholders for later. They should not be activated during development unless the owner explicitly approves the cost and rollout plan.

## Budget Guardrail

Current development target: keep Azure cost below the agreed low monthly budget.

Default rule:

- Use the existing single VM and Docker stack.
- Prefer local code structure, adapters, and documentation first.
- Do not add paid Azure services just to prepare for scale.
- Keep every future service behind a small interface so it can be enabled later without rewriting app screens.

## Extension Matrix

| Capability | Current State | When To Activate | Cost Impact | Owner |
| --- | --- | --- | --- | --- |
| Mobile BFF | Not implemented | Android starts making too many service calls per screen or needs app-specific aggregation | Low if hosted in current VM | Backend platform |
| Queue/workers | Partial outbox foundation exists | Notifications, trust scoring, indexing, or webhooks become slow or unreliable in request path | Low on current Redis/VM | Backend platform |
| Blob storage | Local/profile upload volume today | Uploaded photos/docs grow, or backup/restore needs simpler portable media storage | Low/medium depending on storage volume | Profile service |
| OpenSearch | PostgreSQL search/filtering today | Search p95 is slow after indexes and realistic seed volume | Medium/high | Search service |
| WAF rules | Nginx edge today | Public launch with domain and SSL | Low if simple Nginx rules; higher if managed WAF | DevOps |
| Managed database | Container Postgres today | Public production reliability requires managed backups, HA, or point-in-time restore | Medium/high | DevOps/database |
| Key Vault/secret manager | Environment files/GitHub secrets today | Public launch or multiple operators | Low/medium | DevOps/security |
| Observability stack | Metrics endpoints and docs ready | Before beta users or recurring QA cycles | Low on current VM; higher with managed monitoring | DevOps |

## Mobile BFF Placeholder

Purpose: provide Android-specific responses without forcing the Android app to call many backend services.

Reserved location:

```text
backend/mobile-bff/
```

Initial responsibilities, when activated:

- Aggregate home feed data.
- Aggregate profile detail data.
- Normalize runtime config for Android.
- Keep no independent business truth. It should call existing services and return app-shaped DTOs.

Do not put these in the BFF:

- Payment verification.
- OTP decisions.
- Profile privacy enforcement.
- Admin verification logic.

## Queue And Worker Placeholder

Purpose: move slow side effects out of user-facing requests.

Reserved location:

```text
backend/workers/
  notification-worker/
  trust-score-worker/
  search-index-worker/
  payment-webhook-worker/
```

Recommended first worker:

- `notification-worker`, because push/email delivery is a classic side effect and can retry safely.

Queue provider:

- Start with Redis Streams or BullMQ on the existing Redis container.
- Move to managed queue only when the app becomes live and volume justifies it.

Developer rule:

- The API must first write the main database transaction.
- The worker should process an outbox event after the transaction is committed.
- Never make a user-visible state depend only on an unconfirmed push notification.

## Blob Storage Placeholder

Purpose: move profile photos and KYC documents out of local container volumes later.

Adapter location:

```text
backend/profile-service/src/services/storageAdapter.js
```

Expected interface:

```js
async function putObject({ key, contentType, buffer, visibility })
async function getSignedReadUrl({ key, expiresInSeconds })
async function deleteObject({ key })
```

Activation trigger:

- Local upload backup becomes hard.
- KYC/photo storage needs private signed URLs.
- Media volume grows beyond simple VM storage.

Privacy rule:

- KYC documents must never be public.
- Profile photos must respect profile privacy and photo approval status.

## Search Engine Placeholder

Purpose: add OpenSearch or another search engine only after PostgreSQL indexes are not enough.

Adapter location:

```text
backend/search-service/src/services/searchIndexAdapter.js
```

Current preferred path:

- Keep PostgreSQL filters and indexes.
- Use `pg_trgm` for basic text search.
- Measure with realistic seed data before adding a paid search service.

Activation trigger:

- Search p95 remains high after indexes.
- Search relevance needs synonyms, fuzzy ranking, or large filter combinations.

## WAF And Edge Placeholder

Purpose: protect public traffic while keeping the current VM simple.

Current:

- Nginx reverse proxy.

Future low-cost steps:

- TLS with a real domain.
- Nginx rate limits for OTP, login, admin, payment webhook routes.
- Basic deny rules for suspicious paths.
- Request size limits.

Managed WAF should wait until:

- Public production traffic begins.
- The app has real users and business risk justifies the monthly cost.

## Secret Manager Placeholder

Purpose: remove long-lived secrets from VM files and GitHub Actions secrets when the app moves closer to production.

Acceptable development state:

- `.env` files local only.
- GitHub Actions secrets for deployment.
- No secrets committed to git.

Future options:

- Azure Key Vault.
- 1Password Secrets Automation.
- Bitwarden/Vaultwarden.
- HashiCorp Vault.

Activation trigger:

- More than one operator manages production.
- Public launch begins.
- Secret rotation needs audit history.

## Observability Placeholder

Purpose: give developers and operators visibility without adding heavy cost too early.

Current base:

- Service metrics endpoints.
- Structured logs plan.
- Runbook and alert docs.

Low-cost activation:

- Prometheus and Grafana containers on the same VM.
- Small retention window.
- Alerts for service down, OTP errors, payment webhook failures, DB down, and 5xx rate.

Do not add high-volume managed log ingestion until:

- The app has real production traffic.
- The team agrees on retention and monthly budget.

## Developer Checklist Before Activating Any Extension

1. Update this document with the selected provider.
2. Add environment variables to `.env.example` only, not real secrets.
3. Add adapter tests.
4. Add rollback steps to `docs/RUNBOOK.md`.
5. Update `docs/DEPLOYMENT.md`.
6. Confirm monthly cost impact with the owner.
7. Deploy to staging or local Docker first.

