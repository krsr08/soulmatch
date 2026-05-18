#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <backup.dump>"
  exit 64
fi

BACKUP_FILE="$1"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-${PGUSER:-soulmatch_user}}"
POSTGRES_DB="${POSTGRES_DB:-${PGDATABASE:-soulmatch_db}}"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: ${BACKUP_FILE}"
  exit 66
fi

if [[ -f "${BACKUP_FILE}.sha256" ]]; then
  sha256sum -c "${BACKUP_FILE}.sha256"
fi

echo "Restoring Postgres backup into ${POSTGRES_DB}. This is destructive for existing tables."
echo "Set CONFIRM_RESTORE=yes to proceed."
if [[ "${CONFIRM_RESTORE:-no}" != "yes" ]]; then
  exit 65
fi

docker compose -f "$COMPOSE_FILE" exec -T "$POSTGRES_SERVICE" \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

docker compose -f "$COMPOSE_FILE" exec -T "$POSTGRES_SERVICE" \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-acl < "$BACKUP_FILE"

echo "Postgres restore complete."
