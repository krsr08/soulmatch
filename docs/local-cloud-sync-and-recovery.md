# SoulMatch Local and Cloud Sync

This runbook keeps your laptop build and cloud build aligned, and explains how to rebuild the cloud VM from your laptop if the VM is lost.

## Simple Rule

Use this rule always:

1. Code lives in GitHub and on your laptop.
2. Production runtime secrets live in `docker/production.env`, never in Git.
3. Production data lives in Postgres, MongoDB, and the profile upload volume.
4. Daily backups must be copied to your laptop or external drive.
5. A recovery package combines code plus the latest backup so you can rebuild the VM.

## What "In Sync" Means

| Area | Source of truth | Sync method |
|---|---|---|
| App source code | GitHub `main` and local Git | Commit and push |
| Android build config | `android/local.properties` on laptop | Local file, not Git |
| Production secrets | VM `docker/production.env` plus your private local copy | Never commit |
| Production data | Postgres, MongoDB, profile uploads | Backup and offline sync |
| Deployed version | VM `.soulmatch-deployed-version.json` | Written by deploy script |

## Daily/Weekly Operator Checklist

Run this on your laptop:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\check-local-cloud-sync.ps1
```

Expected result:

- `Code sync: OK`
- `Cloud health: OK`
- `Laptop backup:` shows a recent backup folder

If laptop backup is missing or old, sync backups:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\sync-azure-backups-to-windows.ps1 -Verify
```

## Create a Laptop Recovery Package

Run after a successful production deployment and backup sync:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\create-local-recovery-package.ps1
```

Default output:

```text
C:\Users\ANIRUDH\Documents\soulmatch-recovery\<timestamp>-<commit>
```

This package contains:

- Clean source archive from the current Git commit
- Recovery manifest with commit and checksum
- Latest local DB backup, if one exists

## Include Production Secrets in a Private Recovery Package

For full disaster recovery, you also need production secrets. Only do this on your own laptop or external drive.

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\create-local-recovery-package.ps1 -IncludeProductionEnv
```

Important:

- Do not upload this package to GitHub.
- Do not send it in WhatsApp/email.
- Store it on your laptop plus external drive.
- Anyone with this package can access production systems.

## Rebuild Cloud from Laptop Package

Use this when the VM is new, corrupted, or lost.

First prepare a new VM with SSH access for `azureuser`. Then run:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\rebuild-cloud-from-local.ps1 `
  -RecoveryPackage "C:\Users\ANIRUDH\Documents\soulmatch-recovery\<timestamp>-<commit>" `
  -VmHost "20.204.142.19" `
  -InstallPrerequisites `
  -UseProductionEnvFromPackage `
  -RestoreData
```

What this does:

1. Installs VM prerequisites when `-InstallPrerequisites` is used.
2. Uploads the source archive to the VM.
3. Restores `docker/production.env` if the package contains it.
4. Runs production Docker deploy.
5. Restores Postgres, MongoDB, and profile uploads when `-RestoreData` is used.
6. Runs health checks.

If your new VM has a different IP, pass the new IP in `-VmHost` and later update Android/API base URLs and GitHub secret `AZURE_VM_HOST`.

## If You Only Want to Redeploy Code

Use the same rebuild script without data restore:

```powershell
cd C:\Users\ANIRUDH\Documents\soulmatch
powershell -ExecutionPolicy Bypass -File .\tools\rebuild-cloud-from-local.ps1 `
  -RecoveryPackage "C:\Users\ANIRUDH\Documents\soulmatch-recovery\<timestamp>-<commit>" `
  -VmHost "20.204.142.19"
```

This keeps the existing production database and uploads.

## What Must Never Be Committed

These files must stay out of Git:

- `docker/production.env`
- `android/local.properties`
- `*.jks`, `*.keystore`, `*.p12`
- Firebase service account JSON
- Razorpay secrets
- SSH private keys
- Backup folders
- Recovery packages

## Minimum Disaster Recovery Drill

Do this once before real public launch:

1. Create a recovery package with `-IncludeProductionEnv`.
2. Create a temporary test VM.
3. Run `rebuild-cloud-from-local.ps1` against the test VM.
4. Confirm admin web opens.
5. Confirm mobile login works against the test VM.
6. Confirm profiles, photos, matches, notifications, and admin dashboard data restored.
7. Delete the test VM after validation.

This proves that laptop plus backup can rebuild SoulMatch.
