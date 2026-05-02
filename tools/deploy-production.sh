#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/home/azureuser/soulmatch}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-docker/production.env}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_SLEEP_SECONDS="${HEALTH_SLEEP_SECONDS:-3}"

log() {
  printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "Missing required command: $1"
    exit 1
  fi
}

cd "$APP_DIR"

require_command docker
require_command curl

if ! docker compose version >/dev/null 2>&1; then
  log "Docker Compose is not available through 'docker compose'."
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  log "Missing production env file: $APP_DIR/$ENV_FILE"
  log "Create it from docker/production.env.example and keep real secrets only on the VM."
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

if [ -z "${POSTGRES_USER:-}" ] || [ -z "${POSTGRES_PASSWORD:-}" ] || [ -z "${POSTGRES_DB:-}" ]; then
  log "POSTGRES_USER, POSTGRES_PASSWORD, and POSTGRES_DB must be set in $ENV_FILE."
  exit 1
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

application_services=(
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

log "Validating Compose configuration."
compose config >/tmp/soulmatch-compose.resolved.yml

log "Pulling base images."
compose pull postgres mongodb redis || true

log "Building application images."
compose build --pull

log "Stopping application services for a migration-safe rollout."
compose stop "${application_services[@]}" >/dev/null 2>&1 || true

log "Starting infrastructure services."
compose up -d postgres mongodb redis

postgres_container="$(compose ps -q postgres)"
if [ -z "$postgres_container" ]; then
  log "Postgres container was not found after compose up."
  compose ps
  exit 1
fi

log "Waiting for Postgres readiness."
for attempt in $(seq 1 "$HEALTH_RETRIES"); do
  if docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq "$HEALTH_RETRIES" ]; then
    log "Postgres did not become ready."
    compose logs --tail=80 postgres
    exit 1
  fi
  sleep "$HEALTH_SLEEP_SECONDS"
done

psql_exec() {
  docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" -i "$postgres_container" \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 "$@"
}

log "Ensuring migration tracking table exists."
psql_exec -c "CREATE TABLE IF NOT EXISTS schema_migrations (filename TEXT PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT NOW());" >/dev/null

if [ -d database/migrations ]; then
  log "Applying pending database migrations."
  while IFS= read -r migration; do
    filename="$(basename "$migration")"
    applied="$(psql_exec -t -A -c "SELECT 1 FROM schema_migrations WHERE filename='${filename//\'/\'\'}';" | tr -d '[:space:]')"
    if [ "$applied" = "1" ]; then
      log "Skipping already applied migration: $filename"
      continue
    fi

    log "Applying migration: $filename"
    psql_exec <"$migration"
    psql_exec -c "INSERT INTO schema_migrations (filename) VALUES ('${filename//\'/\'\'}') ON CONFLICT DO NOTHING;" >/dev/null
  done < <(find database/migrations -maxdepth 1 -type f -name '*.sql' | sort)
fi

log "Starting application services."
compose up -d --remove-orphans "${application_services[@]}"

log "Waiting for application health checks."
health_check() {
  local name="$1"
  local url="$2"

  for attempt in $(seq 1 "$HEALTH_RETRIES"); do
    if curl -fsS --max-time 5 "$url" >/dev/null; then
      log "Healthy: $name"
      return 0
    fi
    sleep "$HEALTH_SLEEP_SECONDS"
  done

  log "Unhealthy after deploy: $name ($url)"
  compose ps
  compose logs --tail=120 "$name" || true
  return 1
}

health_check auth-service "http://127.0.0.1:3001/health"
health_check profile-service "http://127.0.0.1:3002/health"
health_check matching-service "http://127.0.0.1:3003/health"
health_check search-service "http://127.0.0.1:3004/health"
health_check chat-service "http://127.0.0.1:3005/health"
health_check notification-service "http://127.0.0.1:3006/health"
health_check payment-service "http://127.0.0.1:3007/health"
health_check admin-service "http://127.0.0.1:3011/health"
health_check admin-web "http://127.0.0.1:3000"

log "Current service status:"
compose ps

log "Pruning unused Docker build cache."
docker builder prune -f >/dev/null || true

log "SoulMatch production deployment completed successfully."
