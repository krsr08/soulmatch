#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-docker/production.env}"
BACKUP_PATH="${1:-}"
CONFIRM_RESTORE="${CONFIRM_RESTORE:-}"

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

fail() {
  log "ERROR: $*"
  exit 1
}

if [ -z "$BACKUP_PATH" ]; then
  fail "Usage: CONFIRM_RESTORE=yes bash tools/restore-production.sh /home/azureuser/backups/soulmatch/<timestamp>"
fi

if [ "$CONFIRM_RESTORE" != "yes" ]; then
  fail "Refusing restore. Re-run with CONFIRM_RESTORE=yes after verifying the backup path."
fi

cd "$APP_DIR"

if [ ! -d "$BACKUP_PATH" ]; then
  fail "Backup directory not found: $BACKUP_PATH"
fi
[ -f "$BACKUP_PATH/postgres.dump" ] || fail "Missing $BACKUP_PATH/postgres.dump"
[ -f "$BACKUP_PATH/mongodb.archive.gz" ] || fail "Missing $BACKUP_PATH/mongodb.archive.gz"

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

log "Stopping application services before restore."
compose stop admin-web admin-service payment-service notification-service chat-service search-service matching-service profile-service auth-service || true
compose up -d postgres mongodb

postgres_container="$(compose ps -q postgres)"
mongo_container="$(compose ps -q mongodb)"
[ -n "$postgres_container" ] || fail "Postgres container not found."
[ -n "$mongo_container" ] || fail "MongoDB container not found."

log "Waiting for Postgres readiness."
for attempt in $(seq 1 30); do
  if docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    break
  fi
  [ "$attempt" -lt 30 ] || fail "Postgres did not become ready."
  sleep 2
done

log "Restoring Postgres database $POSTGRES_DB."
docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" \
  psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${POSTGRES_DB}' AND pid <> pg_backend_pid();" >/dev/null
docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-acl \
  <"$BACKUP_PATH/postgres.dump"

log "Restoring MongoDB."
docker exec -i "$mongo_container" mongorestore --archive --gzip --drop <"$BACKUP_PATH/mongodb.archive.gz"

if [ -f "$BACKUP_PATH/profile_uploads.tar.gz" ]; then
  profile_upload_volume="$(
    docker volume ls --format '{{.Name}}' | grep '_profile_uploads$' | head -n 1 || true
  )"
  if [ -n "$profile_upload_volume" ]; then
    log "Restoring profile uploads volume $profile_upload_volume."
    docker run --rm \
      -v "$profile_upload_volume:/data" \
      -v "$BACKUP_PATH:/backup:ro" \
      alpine:3.20 \
      sh -c 'rm -rf /data/* /data/.[!.]* /data/..?* 2>/dev/null || true; tar xzf /backup/profile_uploads.tar.gz -C /data'
  else
    log "Profile uploads volume not found; skipping upload restore."
  fi
fi

log "Starting full application stack."
compose up -d
log "Restore complete."
