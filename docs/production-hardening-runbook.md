# SoulMatch Production Hardening Runbook

Last updated: 10 May 2026

## Crashlytics and Runtime Monitoring

Android release builds include Firebase Crashlytics through the Firebase BoM and Crashlytics Gradle plugin. Crash collection is disabled for debug builds and enabled for release builds. The app records non-sensitive breadcrumbs for startup, route resolution, FCM delivery, network 5xx responses, payment callbacks, and FCM token registration failures.

Production VM monitoring runs from cron every five minutes:

```bash
cd /home/azureuser/soulmatch
OPS_DIR=/home/azureuser/soulmatch-ops bash tools/monitor-production-health.sh
```

The monitor checks Docker service state, service health URLs, disk, memory, local backup age, backup manifest checksums, and optional Azure Blob backup presence. When a failure state changes, it can send:

- a webhook alert through `ALERT_WEBHOOK_URL`
- an admin dashboard alert through `ADMIN_ALERT_API_URL` with `INTERNAL_SERVICE_SECRET`

Required production env values:

- `INTERNAL_SERVICE_SECRET`
- `ADMIN_ALERT_API_URL=http://127.0.0.1:3011/api/v1/admin/system/alerts`
- `ALERT_WEBHOOK_URL` if Slack/Teams/custom webhook alerting is used

## Secret and Dependency Scans

Local audit:

```powershell
powershell -ExecutionPolicy Bypass -File tools/run-security-audit.ps1 -FailOnFindings
```

Rotation rule:

1. Treat any committed private key, service-account JSON, DB password, Razorpay secret, Twilio token, Firebase private key, AWS secret, Azure connection string, JWT secret, or internal service secret as compromised.
2. Rotate at the provider first.
3. Update `docker/production.env` and any deployment secrets store in use.
4. Deploy.
5. Revoke the old value.
6. Re-run `tools/run-security-audit.ps1 -FailOnFindings`.

The stale `android/app/google-services_old.json` placeholder was removed from source control. The active `google-services.json` should contain only client configuration and must be restricted in Google Cloud/Firebase by package name and SHA certificate.

## DPDP Consent Operations

The app now records consent events in `consent_events` for:

- `photo_upload`
- `kyc_upload`
- `agent_kyc_upload`
- `agent_profile_share`
- `soulmatch_assistance`

Consent events include user/profile, purpose, status, notice version, metadata, source, IP, user agent, and timestamp. This supports audit review for photos, KYC documents, selected-agent sharing, and SoulMatch Assistance opt-in/withdrawal.

Operational checks:

```sql
SELECT consent_type, status, COUNT(*)
FROM consent_events
GROUP BY consent_type, status
ORDER BY consent_type, status;
```

```sql
SELECT *
FROM consent_events
WHERE profile_id = '<profile-id>'
ORDER BY created_at DESC;
```

## Release Gate

Before public launch:

1. Android compile and unit tests pass.
2. Backend syntax/tests pass.
3. Local security audit passes with `tools/run-security-audit.ps1 -FailOnFindings`.
4. Production monitor is green.
5. Crashlytics receives a non-production test crash from Firebase App Distribution.
6. DPDP consent events are being created for profile photos, KYC, and SoulMatch Assistance.
7. Offsite backup upload and restore verification pass.
