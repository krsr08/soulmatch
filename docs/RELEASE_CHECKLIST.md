# SoulMatch Release Checklist

## Pre-launch

- [ ] Rotate all secrets since the backup tag.
- [ ] `MOCK_OTP=false` in production.
- [ ] Razorpay live keys are stored in a secret manager only.
- [ ] SSL is enabled on the gateway.
- [ ] Backup restore drill completed within the last 30 days.
- [ ] Smoke tests pass on staging.
- [ ] k6 search, matches, and chat tests pass on staging.
- [ ] Android release APK tested: signup -> publish -> interest -> chat -> pay.
- [ ] Admin tested: verify, ban, refund, maintenance mode.
- [ ] Legal URLs live and consent version logged.
- [ ] Production compose does not seed demo users.
- [ ] Postgres/Mongo/Redis have no public ports.
- [ ] Admin users and roles reviewed.
- [ ] Firebase/Google/Twilio/Razorpay keys restricted by environment.
- [ ] Play Store data safety matches `docs/ANALYTICS_EVENTS.md`.

## Release

- [ ] Create release branch/tag from `main`.
- [ ] Confirm `.github/workflows/ci.yml` is green.
- [ ] Deploy to staging and run smoke tests.
- [ ] Deploy to production through the approved workflow.
- [ ] Verify `/health` and `/metrics` for every backend service.
- [ ] Verify login, role selection, member home, profile detail, interest, chat, payment, and admin login.
- [ ] Confirm dashboard Deployment / Version Audit entry was created.

## Post-release

- [ ] Watch 5xx rate, p95 latency, payment webhooks, OTP failures, notification DLQ for 60 minutes.
- [ ] Confirm backup job completes after release.
- [ ] Update release notes with commit SHA and migration list.
- [ ] Close or roll back if any critical alert remains open for more than 10 minutes.
