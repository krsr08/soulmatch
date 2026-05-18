# SoulMatch Production Runbook

## Targets

- RTO: 4 hours for single-VM recovery.
- RPO: 24 hours with daily database backups. Reduce to 1 hour before paid launch.
- Backup retention: 14 daily local/VM backups plus one offline encrypted copy.

## Daily Backup

```bash
BACKUP_DIR=/secure-backups/postgres COMPOSE_FILE=docker/docker-compose.prod.yml ./recovery-kit/scripts/postgres-backup.sh
BACKUP_DIR=/secure-backups/mongo COMPOSE_FILE=docker/docker-compose.prod.yml ./recovery-kit/scripts/mongo-backup.sh
```

Copy backup files and `.sha256` manifests to offline storage after each run.

## Restore Drill

1. Provision a clean VM with Docker and Docker Compose.
2. Clone the repository and checkout the intended release tag.
3. Restore `docker/production.env` from encrypted secret storage.
4. Start only data services: `docker compose -f docker/docker-compose.prod.yml up -d postgres mongodb redis`.
5. Restore Postgres:
   ```bash
   CONFIRM_RESTORE=yes ./recovery-kit/scripts/postgres-restore.sh /path/soulmatch-postgres.dump
   ```
6. Restore Mongo:
   ```bash
   CONFIRM_RESTORE=yes ./recovery-kit/scripts/mongo-restore.sh /path/soulmatch-mongo.archive.gz
   ```
7. Start application services: `docker compose -f docker/docker-compose.prod.yml up -d --build`.
8. Run smoke tests against the gateway:
   ```bash
   node test_folder/auth-flow-smoke.js
   node test_folder/api-smoke.js
   ```

## Incident Checklist

1. Declare severity and owner.
2. Capture current git SHA, container image IDs, and deployment timestamp.
3. Check service health, Prometheus alerts, gateway logs, and database health.
4. If user data is at risk, pause public writes with maintenance mode from CMS config.
5. Preserve logs before restart if investigating security or payment issues.
6. Roll back to the previous known-good tag if the current release caused the issue.
7. Record root cause, remediation, and follow-up actions in the admin audit log and release notes.

## Payment Webhook Failure

1. Check `payment-service` 5xx logs using request ID.
2. Verify Razorpay webhook secret in the secret store.
3. Replay failed webhooks only after idempotency check.
4. Confirm subscriptions were not double-created.

## Notification Outbox Failure

1. Check `soulmatch_notification_dlq_depth`.
2. Inspect `notification_dlq` for provider response.
3. Rotate invalid FCM tokens if many permanent failures occur.
4. Requeue DLQ items only after fixing the root provider/config issue.

## Security Incident

1. Disable affected admin user/session.
2. Rotate relevant secrets.
3. Export admin audit logs, access logs, and affected profile IDs.
4. Notify users/regulators according to DPDP incident obligations.
5. Patch and redeploy through the release checklist.
