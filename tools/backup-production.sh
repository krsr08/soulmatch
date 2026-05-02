#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-docker/production.env}"
BACKUP_DIR="${BACKUP_DIR:-/home/azureuser/backups/soulmatch}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
AZURE_BACKUP_CONTAINER="${AZURE_BACKUP_CONTAINER:-}"
AZURE_STORAGE_CONNECTION_STRING="${AZURE_STORAGE_CONNECTION_STRING:-}"

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

fail() {
  log "ERROR: $*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

cd "$APP_DIR"
require_command docker

if ! docker compose version >/dev/null 2>&1; then
  fail "Docker Compose is not available through 'docker compose'."
fi

if [ ! -f "$ENV_FILE" ]; then
  fail "Missing production env file: $APP_DIR/$ENV_FILE"
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

if [ -z "${POSTGRES_USER:-}" ] || [ -z "${POSTGRES_PASSWORD:-}" ] || [ -z "${POSTGRES_DB:-}" ]; then
  fail "POSTGRES_USER, POSTGRES_PASSWORD, and POSTGRES_DB must be set in $ENV_FILE."
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

timestamp="$(date -u '+%Y%m%dT%H%M%SZ')"
run_dir="$BACKUP_DIR/$timestamp"
mkdir -p "$run_dir"

postgres_container="$(compose ps -q postgres)"
mongo_container="$(compose ps -q mongodb)"
profile_container="$(compose ps -q profile-service || true)"

[ -n "$postgres_container" ] || fail "Postgres container not found."
[ -n "$mongo_container" ] || fail "MongoDB container not found."

log "Starting SoulMatch production backup: $run_dir"

log "Backing up Postgres database $POSTGRES_DB."
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner --no-acl \
  >"$run_dir/postgres.dump"

log "Backing up MongoDB."
docker exec "$mongo_container" \
  mongodump --archive --gzip \
  >"$run_dir/mongodb.archive.gz"

if [ -n "$profile_container" ]; then
  profile_upload_volume="$(
    docker inspect "$profile_container" \
      --format '{{range .Mounts}}{{if eq .Destination "/app/uploads"}}{{.Name}}{{end}}{{end}}'
  )"
  if [ -n "$profile_upload_volume" ]; then
    log "Backing up profile uploads volume $profile_upload_volume."
    docker run --rm \
      -v "$profile_upload_volume:/data:ro" \
      -v "$run_dir:/backup" \
      alpine:3.20 \
      tar czf /backup/profile_uploads.tar.gz -C /data .
  else
    log "Profile uploads volume not found; skipping uploads backup."
  fi
else
  log "Profile service container not found; skipping uploads backup."
fi

log "Writing backup manifest."
manifest_tmp="$run_dir/manifest.json.tmp"
{
  printf '{\n'
  printf '  "timestamp": "%s",\n' "$timestamp"
  printf '  "appDir": "%s",\n' "$APP_DIR"
  printf '  "postgresDatabase": "%s",\n' "$POSTGRES_DB"
  printf '  "files": [\n'
  first=1
  for file in "$run_dir"/*; do
    [ -f "$file" ] || continue
    name="$(basename "$file")"
    if [ "$name" = "manifest.json" ] || [ "$name" = "manifest.json.tmp" ]; then
      continue
    fi
    size="$(wc -c <"$file" | tr -d '[:space:]')"
    sha="$(sha256sum "$file" | awk '{print $1}')"
    if [ "$first" -eq 0 ]; then
      printf ',\n'
    fi
    first=0
    printf '    { "name": "%s", "bytes": %s, "sha256": "%s" }' "$name" "$size" "$sha"
  done
  printf '\n  ]\n'
  printf '}\n'
} >"$manifest_tmp"
mv "$manifest_tmp" "$run_dir/manifest.json"

if [ -n "$AZURE_STORAGE_CONNECTION_STRING" ] && [ -n "$AZURE_BACKUP_CONTAINER" ]; then
  if command -v az >/dev/null 2>&1; then
    log "Uploading backup to Azure Blob container $AZURE_BACKUP_CONTAINER."
    az storage blob upload-batch \
      --connection-string "$AZURE_STORAGE_CONNECTION_STRING" \
      --destination "$AZURE_BACKUP_CONTAINER" \
      --destination-path "$timestamp" \
      --source "$run_dir" \
      --overwrite true >/dev/null
  else
    log "Azure upload skipped: az CLI is not installed."
  fi
else
  log "Azure upload skipped: AZURE_STORAGE_CONNECTION_STRING/AZURE_BACKUP_CONTAINER not configured."
fi

log "Applying local retention: deleting backups older than $RETENTION_DAYS days."
find "$BACKUP_DIR" -maxdepth 1 -mindepth 1 -type d -name '20*' -mtime +"$RETENTION_DAYS" -exec rm -rf {} +

log "Backup complete: $run_dir"
