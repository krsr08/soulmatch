#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
BACKUP_DIR="${BACKUP_DIR:-/home/azureuser/backups/soulmatch}"
BACKUP_SCHEDULE="${BACKUP_SCHEDULE:-17 2 * * *}"
CRON_MARKER="# SoulMatch production backup"

mkdir -p "$BACKUP_DIR"

tmp_cron="$(mktemp)"
existing_cron="$(mktemp)"
crontab -l >"$existing_cron" 2>/dev/null || true
grep -v -E "SoulMatch production backup|tools/backup-production\\.sh" "$existing_cron" >"$tmp_cron" || true
{
  printf '\n'
  printf '%s\n' "$CRON_MARKER"
  printf '%s cd %s && BACKUP_DIR=%s bash tools/backup-production.sh >> %s/backup.log 2>&1 # SoulMatch production backup\n' \
    "$BACKUP_SCHEDULE" "$APP_DIR" "$BACKUP_DIR" "$BACKUP_DIR"
} >>"$tmp_cron"

crontab "$tmp_cron"
rm -f "$tmp_cron" "$existing_cron"

printf 'Installed SoulMatch production backup cron schedule: %s\n' "$BACKUP_SCHEDULE"
printf 'Backup directory: %s\n' "$BACKUP_DIR"
printf 'Log file: %s/backup.log\n' "$BACKUP_DIR"
