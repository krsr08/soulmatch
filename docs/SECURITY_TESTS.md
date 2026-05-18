# SoulMatch Security Test Plan

## Automated IDOR Suite

Run these in CI and before release:

```bash
node --test backend/profile-service/test/blockVisibility.test.js
node --test backend/chat-service/test/chatRouteSecurity.test.js
node --test backend/payment-service/test/paymentController.test.js
node --test backend/admin-service/test/adminSecurity.test.js
```

Coverage:

- Profile: blocked users cannot see each other in detail/search/matches.
- Chat: user C cannot read user A-B messages or send into the conversation.
- Payment: user/order/amount/plan reconciliation and webhook idempotency.
- Admin: member/agent JWTs fail admin issuer/audience checks; support roles cannot execute super-admin operations.

## Manual Security Checks

- Profile view anti-scrape: exceed `PROFILE_VIEW_RATE_LIMIT` in `PROFILE_VIEW_RATE_WINDOW_MS` and expect HTTP 429.
- Admin CSRF: cookie-authenticated unsafe request without `x-csrf-token` must return 403.
- XSS: save `<script>alert(1)</script>` in admin-editable text and confirm script tags are stripped.
- Mock OTP: `NODE_ENV=production MOCK_OTP=true` must fail auth service boot.
- Mock payments: production API must reject `gateway=mock`.
- Production compose: Postgres, MongoDB, and Redis must not publish public ports.

## Load Tests

```bash
k6 run api-testing/k6/search-100rps-5m.js
k6 run api-testing/k6/matches-load.js
k6 run api-testing/k6/chat-200-connections.js
```

Use staging tokens via `ACCESS_TOKEN` and gateway URL via `BASE_URL`. The chat socket script also accepts `SOCKET_URL`.

## Secret Scan

```bash
gitleaks detect --source . --redact
```

Any confirmed live secret must be rotated in the provider, removed from history only through an approved repository-cleanup window, and documented in `docs/PRODUCTION_HARDENING_LOG.md`.
