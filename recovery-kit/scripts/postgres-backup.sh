#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups/postgres}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-${PGUSER:-soulmatch_user}}"
POSTGRES_DB="${POSTGRES_DB:-${PGDATABASE:-soulmatch_db}}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output="${BACKUP_DIR}/soulmatch-postgres-${timestamp}.dump"
manifest="${output}.sha256"

echo "Creating Postgres backup: ${output}"
docker compose -f "$COMPOSE_FILE" exec -T "$POSTGRES_SERVICE" \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --no-owner --no-acl > "$output"

sha256sum "$output" > "$manifest"
find "$BACKUP_DIR" -name 'soulmatch-postgres-*.dump' -type f -mtime +"$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -name 'soulmatch-postgres-*.dump.sha256' -type f -mtime +"$RETENTION_DAYS" -delete

echo "Postgres backup complete."
echo "Backup: ${output}"
echo "Checksum: ${manifest}"
