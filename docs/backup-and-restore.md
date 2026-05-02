# SoulMatch Backup and Restore

SoulMatch production backups are handled by `tools/backup-production.sh`.

## What Is Backed Up

- Postgres database as `postgres.dump`
- MongoDB data as `mongodb.archive.gz`
- Profile upload volume as `profile_uploads.tar.gz`
- A `manifest.json` with file sizes and SHA-256 checksums

Default backup location:

```bash
/home/azureuser/backups/soulmatch/<timestamp>
```

Default retention:

```bash
14 days
```

## Manual Backup

Run on the Azure VM:

```bash
cd /home/azureuser/soulmatch
bash tools/backup-production.sh
```

## Install Daily Schedule

Run on the Azure VM:

```bash
cd /home/azureuser/soulmatch
bash tools/install-backup-cron.sh
```

Default schedule is daily at `02:17 UTC`.

Check the schedule:

```bash
crontab -l
```

Check logs:

```bash
tail -n 100 /home/azureuser/backups/soulmatch/backup.log
```

## Optional Azure Blob Upload

If the VM has Azure CLI installed, set these in the backup runtime environment before the script runs:

```bash
AZURE_STORAGE_CONNECTION_STRING="<storage-connection-string>"
AZURE_BACKUP_CONTAINER="soulmatch-backups"
```

Without those variables, backups stay local on the VM.

## Restore

Restores are destructive. Stop and think before running.

```bash
cd /home/azureuser/soulmatch
CONFIRM_RESTORE=yes bash tools/restore-production.sh /home/azureuser/backups/soulmatch/<timestamp>
```

The restore script:

- Stops app services.
- Keeps Postgres and Mongo running.
- Restores Postgres with `pg_restore --clean --if-exists`.
- Restores Mongo with `mongorestore --drop`.
- Restores profile uploads if present.
- Starts the full Docker Compose stack again.

## Current Limitation

Local VM backups protect against accidental app/database corruption but not VM loss. Before real production, add Azure Blob Storage upload plus a monthly restore drill.
