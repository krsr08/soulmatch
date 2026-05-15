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

read_env_value() {
  local key="$1"
  local line value
  line="$(grep -m 1 -E "^${key}=" "$ENV_FILE" || true)"
  value="${line#*=}"
  value="${value%$'\r'}"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "$value"
}

POSTGRES_USER="$(read_env_value POSTGRES_USER)"
POSTGRES_PASSWORD="$(read_env_value POSTGRES_PASSWORD)"
POSTGRES_DB="$(read_env_value POSTGRES_DB)"

if [ -z "${POSTGRES_USER:-}" ] || [ -z "${POSTGRES_PASSWORD:-}" ] || [ -z "${POSTGRES_DB:-}" ]; then
  log "POSTGRES_USER, POSTGRES_PASSWORD, and POSTGRES_DB must be set in $ENV_FILE."
  exit 1
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

configure_nginx() {
  if ! command -v nginx >/dev/null 2>&1; then
    log "Nginx is not installed; skipping reverse proxy configuration."
    return 0
  fi
  if ! sudo -n true >/dev/null 2>&1; then
    log "Passwordless sudo is not available; skipping reverse proxy configuration."
    return 0
  fi

  log "Configuring Nginx reverse proxy."
  cat >/tmp/soulmatch-nginx.conf <<'NGINX'
server {
    listen 80 default_server;
    server_name _;
    client_max_body_size 25m;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    location /uploads/ {
        proxy_pass http://127.0.0.1:3002/uploads/;
    }

    location /api/v1/admin/ {
        proxy_pass http://127.0.0.1:3011/api/v1/admin/;
    }

    location /api/v1/public/ {
        proxy_pass http://127.0.0.1:3011/api/v1/public/;
    }

    location /api/v1/auth/ {
        proxy_pass http://127.0.0.1:3001/api/v1/auth/;
    }

    location /api/v1/profile/ {
        proxy_pass http://127.0.0.1:3002/api/v1/profile/;
    }

    location /api/v1/matches/ {
        proxy_pass http://127.0.0.1:3003/api/v1/matches/;
    }

    location /api/v1/interests/ {
        proxy_pass http://127.0.0.1:3003/api/v1/interests/;
    }

    location /api/v1/matching/ {
        proxy_pass http://127.0.0.1:3003/api/v1/matching/;
    }

    location /api/v1/search/ {
        proxy_pass http://127.0.0.1:3004/api/v1/search/;
    }

    location /api/v1/chat/ {
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_pass http://127.0.0.1:3005/api/v1/chat/;
    }

    location /socket.io/ {
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_pass http://127.0.0.1:3005/socket.io/;
    }

    location /api/v1/notifications/ {
        proxy_pass http://127.0.0.1:3006/api/v1/notifications/;
    }

    location /api/v1/payment/ {
        proxy_pass http://127.0.0.1:3007/api/v1/payment/;
    }

    location / {
        proxy_pass http://127.0.0.1:3000;
    }
}
NGINX
  sudo mv /tmp/soulmatch-nginx.conf /etc/nginx/sites-available/soulmatch
  sudo rm -f /etc/nginx/sites-enabled/default
  sudo ln -sf /etc/nginx/sites-available/soulmatch /etc/nginx/sites-enabled/soulmatch
  sudo nginx -t
  sudo systemctl reload nginx || sudo systemctl restart nginx
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

psql_cmd() {
  docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$postgres_container" \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 "$@"
}

psql_file() {
  docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" -i "$postgres_container" \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1
}

sql_escape() {
  printf "%s" "${1:-}" | sed "s/'/''/g"
}

write_deployment_version() {
  local source_commit="${DEPLOYED_SOURCE_COMMIT:-}"
  local source_branch="${DEPLOYED_SOURCE_BRANCH:-}"
  local deployed_at

  deployed_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

  if [ -z "$source_commit" ] && command -v git >/dev/null 2>&1 && [ -d .git ]; then
    source_commit="$(git rev-parse HEAD 2>/dev/null || true)"
  fi
  if [ -z "$source_branch" ] && command -v git >/dev/null 2>&1 && [ -d .git ]; then
    source_branch="$(git branch --show-current 2>/dev/null || true)"
  fi

  cat >.soulmatch-deployed-version.json <<EOF
{
  "sourceCommit": "${source_commit:-unknown}",
  "sourceBranch": "${source_branch:-unknown}",
  "deployedAt": "$deployed_at",
  "appDir": "$APP_DIR",
  "composeFile": "$COMPOSE_FILE"
}
EOF
}

record_deployment_audit() {
  local source_commit="${DEPLOYED_SOURCE_COMMIT:-}"
  local source_branch="${DEPLOYED_SOURCE_BRANCH:-}"
  local release_version="${RELEASE_VERSION:-}"
  local release_description="${RELEASE_DESCRIPTION:-Automated production deploy from main}"
  local release_changes="${RELEASE_CHANGE_DETAILS:-Code synchronized, migrations applied, services restarted and health checks passed.}"
  local change_type="${RELEASE_CHANGE_TYPE:-Both}"

  if [ -z "$source_commit" ] && command -v git >/dev/null 2>&1 && [ -d .git ]; then
    source_commit="$(git rev-parse HEAD 2>/dev/null || true)"
  fi
  if [ -z "$source_branch" ] && command -v git >/dev/null 2>&1 && [ -d .git ]; then
    source_branch="$(git branch --show-current 2>/dev/null || true)"
  fi
  if [ -z "$release_version" ]; then
    release_version="${source_commit:0:8}"
  fi

  cat <<SQL | psql_file >/dev/null
INSERT INTO deployment_audit_logs (
  admin_actor,
  release_description,
  change_details,
  release_version,
  deployment_status,
  change_type,
  source_commit,
  source_branch
) VALUES (
  'github-actions',
  '$(sql_escape "$release_description")',
  '$(sql_escape "$release_changes")',
  '$(sql_escape "${release_version:-unknown}")',
  'success',
  '$(sql_escape "$change_type")',
  '$(sql_escape "${source_commit:-unknown}")',
  '$(sql_escape "${source_branch:-unknown}")'
);
SQL
}

log "Ensuring migration tracking table exists."
psql_cmd -c "CREATE TABLE IF NOT EXISTS schema_migrations (filename TEXT PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT NOW());" >/dev/null

if [ -d database/migrations ]; then
  log "Applying pending database migrations."
  while IFS= read -r migration; do
    filename="$(basename "$migration")"
    applied="$(psql_cmd -t -A -c "SELECT 1 FROM schema_migrations WHERE filename='${filename//\'/\'\'}';" | tr -d '[:space:]')"
    if [ "$applied" = "1" ]; then
      log "Skipping already applied migration: $filename"
      continue
    fi

    log "Applying migration: $filename"
    psql_file <"$migration"
    psql_cmd -c "INSERT INTO schema_migrations (filename) VALUES ('${filename//\'/\'\'}') ON CONFLICT DO NOTHING;" >/dev/null
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

configure_nginx
health_check nginx-public-config "http://127.0.0.1/api/v1/public/config"

log "Current service status:"
compose ps

log "Pruning unused Docker build cache."
docker builder prune -f >/dev/null || true

write_deployment_version
record_deployment_audit || log "Deployment audit write skipped."
log "SoulMatch production deployment completed successfully."
