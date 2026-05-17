# Production Hardening Log
- Backup branch: backup/pre-production-20260517-200142
- Backup tag: pre-production-backup-20260517
- Working branch: feat/production-hardening
- Started: 2026-05-17 20:01 IST

## Status
| ID | Item | Status | Commit | Notes |
|----|------|--------|--------|-------|
| P1-01 | Secrets & repo hygiene | Done | 78eaa84 | Added GitHub Actions gitleaks scan, verified ignore coverage for env/build artifacts, and scanned tracked files. `android/app/google-services.json` contains a Firebase mobile API key; it is public client config but should be restricted/rotated in Firebase/Google Console before launch. |
| P1-02 | Docker production network | Done | c03066a | Verified Postgres, MongoDB, and Redis are internal-only. Backend service ports are bound to `127.0.0.1` for the VM-local gateway and health checks; only Nginx should be public. Documented this in `docs/DEPLOYMENT.md`. |
| P1-03 | API gateway / reverse proxy | Done | c03066a | Added CORS allowlist support across backend services and matching service, documented single public API base URL, and added OTP IP/phone rate limits. |
| P1-04 | Auth service hardening | Done | 5697a49 | Added issuer/audience/jti JWT claims, refresh rotation revocation markers, production MOCK_OTP boot guard, OTP IP/phone throttles, and verified Google-email linking. Tested with `node --test backend/auth-service/test/tokenService.test.js`; route limiters enforce 3 OTP/15m per phone and 10/15m per IP. |
| P1-05 | Android release security | Done | pending | Confirmed release backup/cleartext/logging guards, added encrypted token storage using Android Keystore-backed encrypted preferences, removed blocking token lookup from the OkHttp auth interceptor, and confirmed FCM token registration/channel/permission path. |
| P1-06 | Remove release mocks | Done | pending | Kept demo fixtures debug-only through `AppEnvironment.allowDemoFallback`, blocked mock checkout completion in release builds, and left release API failures as error/empty state instead of fake data. |
| P1-07 | Profile privacy | Done | pending | Added shared profile visibility/redaction helpers, fixed null admin-status visibility handling, enforced `matches_only` profile visibility, redacted photo/contact/last-seen fields centrally on profile detail, and changed detail-screen hide to call server match feedback. Tested with `node --test backend/profile-service/test/profileVisibility.test.js`. |
