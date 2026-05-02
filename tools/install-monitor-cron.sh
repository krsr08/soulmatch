#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
OPS_DIR="${OPS_DIR:-/home/azureuser/soulmatch-ops}"
MONITOR_SCHEDULE="${MONITOR_SCHEDULE:-*/5 * * * *}"
CRON_MARKER="# SoulMatch production monitor"

mkdir -p "$OPS_DIR"

tmp_cron="$(mktemp)"
existing_cron="$(mktemp)"
crontab -l >"$existing_cron" 2>/dev/null || true
grep -v -E "SoulMatch production monitor|tools/monitor-production-health\\.sh" "$existing_cron" >"$tmp_cron" || true
{
  printf '\n'
  printf '%s\n' "$CRON_MARKER"
  printf '%s cd %s && OPS_DIR=%s bash tools/monitor-production-health.sh >> %s/monitor.log 2>&1 # SoulMatch production monitor\n' \
    "$MONITOR_SCHEDULE" "$APP_DIR" "$OPS_DIR" "$OPS_DIR"
} >>"$tmp_cron"

crontab "$tmp_cron"
rm -f "$tmp_cron" "$existing_cron"

printf 'Installed SoulMatch production monitor cron schedule: %s\n' "$MONITOR_SCHEDULE"
printf 'Monitor directory: %s\n' "$OPS_DIR"
printf 'Log file: %s/monitor.log\n' "$OPS_DIR"
