#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-docker/production.env}"
BACKUP_DIR="${BACKUP_DIR:-/home/azureuser/backups/soulmatch}"
OPS_DIR="${OPS_DIR:-/home/azureuser/soulmatch-ops}"
DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-85}"
MEMORY_WARN_PERCENT="${MEMORY_WARN_PERCENT:-90}"
BACKUP_MAX_AGE_HOURS="${BACKUP_MAX_AGE_HOURS:-36}"
ALERT_WEBHOOK_URL="${ALERT_WEBHOOK_URL:-}"

mkdir -p "$OPS_DIR"
STATE_FILE="$OPS_DIR/monitor-state.txt"
REPORT_FILE="$OPS_DIR/monitor-latest.json"

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

add_failure() {
  failures+=("$1")
  log "FAIL: $1"
}

json_string_array() {
  python3 - "$@" <<'PY'
import json
import sys
print(json.dumps(sys.argv[1:]))
PY
}

send_alert() {
  local status="$1"
  local details="$2"

  [ -n "$ALERT_WEBHOOK_URL" ] || return 0

  python3 - "$status" "$details" <<'PY' | curl -fsS -X POST -H 'Content-Type: application/json' --data-binary @- "$ALERT_WEBHOOK_URL" >/dev/null || true
import json
import socket
import sys
payload = {
    "text": f"SoulMatch production monitor: {sys.argv[1]}\nHost: {socket.gethostname()}\n{sys.argv[2]}"
}
print(json.dumps(payload))
PY
}

cd "$APP_DIR"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

failures=()
expected_services=(
  postgres
  mongodb
  redis
  auth-service
  profile-service
  matching-service
  search-service
  chat-service
  notification-service
  payment-service
  admin-service
  admin-web
)

health_urls=(
  "auth-service=http://127.0.0.1:3001/health"
  "profile-service=http://127.0.0.1:3002/health"
  "matching-service=http://127.0.0.1:3003/health"
  "search-service=http://127.0.0.1:3004/health"
  "chat-service=http://127.0.0.1:3005/health"
  "notification-service=http://127.0.0.1:3006/health"
  "payment-service=http://127.0.0.1:3007/health"
  "admin-service=http://127.0.0.1:3011/health"
  "admin-web=http://127.0.0.1:3000"
  "nginx=http://127.0.0.1"
)

log "Starting SoulMatch production monitor."

if ! command -v docker >/dev/null 2>&1; then
  add_failure "docker command is missing"
elif ! docker compose version >/dev/null 2>&1; then
  add_failure "docker compose is unavailable"
else
  for service in "${expected_services[@]}"; do
    container_id="$(compose ps -q "$service" 2>/dev/null || true)"
    if [ -z "$container_id" ]; then
      add_failure "$service container not found"
      continue
    fi

    state="$(docker inspect "$container_id" --format '{{.State.Status}}' 2>/dev/null || true)"
    health="$(docker inspect "$container_id" --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' 2>/dev/null || true)"
    if [ "$state" != "running" ]; then
      add_failure "$service is $state"
    elif [ "$health" = "unhealthy" ]; then
      add_failure "$service healthcheck is unhealthy"
    fi
  done
fi

for item in "${health_urls[@]}"; do
  name="${item%%=*}"
  url="${item#*=}"
  if ! curl -fsS --max-time 5 "$url" >/dev/null; then
    add_failure "$name health endpoint failed: $url"
  fi
done

disk_percent="$(df -P / | awk 'NR == 2 { gsub("%", "", $5); print $5 }')"
if [ -n "$disk_percent" ] && [ "$disk_percent" -ge "$DISK_WARN_PERCENT" ]; then
  add_failure "root disk usage is ${disk_percent}% (threshold ${DISK_WARN_PERCENT}%)"
fi

memory_percent="$(free | awk '/Mem:/ { printf "%d", ($3 / $2) * 100 }')"
if [ -n "$memory_percent" ] && [ "$memory_percent" -ge "$MEMORY_WARN_PERCENT" ]; then
  add_failure "memory usage is ${memory_percent}% (threshold ${MEMORY_WARN_PERCENT}%)"
fi

latest_backup="$(find "$BACKUP_DIR" -maxdepth 1 -mindepth 1 -type d -name '20*' -printf '%T@ %p\n' 2>/dev/null | sort -nr | awk 'NR == 1 { print $2 }')"
latest_backup_name=""
if [ -z "$latest_backup" ]; then
  add_failure "no local production backup found in $BACKUP_DIR"
else
  latest_backup_name="$(basename "$latest_backup")"
  latest_epoch="$(stat -c %Y "$latest_backup")"
  now_epoch="$(date +%s)"
  age_hours="$(( (now_epoch - latest_epoch) / 3600 ))"
  if [ "$age_hours" -gt "$BACKUP_MAX_AGE_HOURS" ]; then
    add_failure "latest local backup is ${age_hours}h old (threshold ${BACKUP_MAX_AGE_HOURS}h)"
  fi

  if [ -f "$latest_backup/manifest.json" ] && command -v python3 >/dev/null 2>&1; then
    if ! python3 - "$latest_backup/manifest.json" "$latest_backup" <<'PY'
import hashlib
import json
import pathlib
import sys

manifest = pathlib.Path(sys.argv[1])
backup_dir = pathlib.Path(sys.argv[2])
data = json.loads(manifest.read_text())
for item in data.get("files", []):
    if item.get("name") == "manifest.json":
        continue
    path = backup_dir / item["name"]
    if not path.exists():
        raise SystemExit(f"missing {path}")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest.lower() != item["sha256"].lower():
        raise SystemExit(f"checksum mismatch {path}")
PY
    then
      add_failure "latest local backup manifest verification failed: $latest_backup"
    fi
  else
    add_failure "latest local backup is missing manifest.json: $latest_backup"
  fi
fi

if [ -n "${AZURE_STORAGE_CONNECTION_STRING:-}" ] && [ -n "${AZURE_BACKUP_CONTAINER:-}" ] && [ -n "$latest_backup_name" ]; then
  if command -v az >/dev/null 2>&1; then
    exists="$(az storage blob exists \
      --connection-string "$AZURE_STORAGE_CONNECTION_STRING" \
      --container-name "$AZURE_BACKUP_CONTAINER" \
      --name "$latest_backup_name/manifest.json" \
      --query exists \
      -o tsv 2>/dev/null || true)"
    if [ "$exists" != "true" ]; then
      add_failure "latest backup is not present in Azure Blob: $latest_backup_name/manifest.json"
    fi
  else
    add_failure "Azure backup upload is configured but az CLI is missing"
  fi
fi

status="OK"
if [ "${#failures[@]}" -gt 0 ]; then
  status="FAIL"
fi

failures_json="$(json_string_array "${failures[@]}")"
cat >"$REPORT_FILE" <<EOF
{
  "checkedAt": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
  "status": "$status",
  "diskPercent": ${disk_percent:-0},
  "memoryPercent": ${memory_percent:-0},
  "latestBackup": "${latest_backup_name:-}",
  "failures": $failures_json
}
EOF

details="$(printf '%s\n' "${failures[@]:-All checks passed.}")"
previous_status="$(cat "$STATE_FILE" 2>/dev/null || true)"
current_status="$status|$details"
if [ "$current_status" != "$previous_status" ]; then
  send_alert "$status" "$details"
  printf '%s\n' "$current_status" >"$STATE_FILE"
fi

if [ "$status" = "OK" ]; then
  log "OK: all production checks passed."
  exit 0
fi

log "ERROR: ${#failures[@]} production check(s) failed."
exit 1
