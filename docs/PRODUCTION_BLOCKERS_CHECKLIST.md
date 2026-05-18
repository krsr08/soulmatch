# Phase 3 Production Blockers Checklist

| Blocker | Status | Evidence |
| --- | --- | --- |
| Subscription expiry cron deactivates plans | Implemented | `backend/payment-service/src/services/subscriptionExpiryService.js` runs on boot and hourly. |
| Rate limit profile views | Implemented | `PROFILE_VIEW_RATE_LIMIT` and `PROFILE_VIEW_RATE_WINDOW_MS` protect profile detail/view routes. |
| Duplicate account detection | Implemented | `phone_hash`, `device_id_hash`, `duplicate_signal`; Android sends `x-device-id`. |
| Admin CSRF for cookie sessions | Implemented | Cookie-authenticated unsafe admin requests require `x-csrf-token`. |
| Android WebSocket reconnect on token refresh | Implemented | `ChatThreadViewModel` reconnects when the auth token changes. |
| PgBouncer documented | Implemented | `docs/DEPLOYMENT.md`. |
| Production compose never auto-seeds demo users | Implemented | `docker/docker-compose.prod.yml` mounts schema only. |
| GST/invoice or Razorpay invoice doc | Implemented | `docs/DEPLOYMENT.md`. |
| Play Store data safety alignment | Implemented | `docs/PLAY_STORE_DATA_SAFETY.md`. |
| Agent JWT cannot hit admin routes | Verified by design | Admin issuer/audience checks require `soulmatch-admin` tokens. |
| Input sanitization on free text | Implemented | Admin request sanitizer strips script tags, event handlers, and `javascript:` URLs. |
| JWT clock skew leeway | Implemented | Access, refresh, admin, socket, and matching JWT verification use 30s default leeway. |
| README setup steps accurate | Implemented | README points to Docker setup and release docs. |
