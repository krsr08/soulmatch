#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups/mongo}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
MONGO_SERVICE="${MONGO_SERVICE:-mongodb}"
MONGO_DB="${MONGO_DB:-soulmatch_chat}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

mkdir -p "$BACKUP_DIR"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output="${BACKUP_DIR}/soulmatch-mongo-${timestamp}.archive.gz"
manifest="${output}.sha256"

echo "Creating MongoDB backup: ${output}"
docker compose -f "$COMPOSE_FILE" exec -T "$MONGO_SERVICE" \
  mongodump --db "$MONGO_DB" --archive --gzip > "$output"

sha256sum "$output" > "$manifest"
find "$BACKUP_DIR" -name 'soulmatch-mongo-*.archive.gz' -type f -mtime +"$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -name 'soulmatch-mongo-*.archive.gz.sha256' -type f -mtime +"$RETENTION_DAYS" -delete

echo "MongoDB backup complete."
echo "Backup: ${output}"
echo "Checksum: ${manifest}"
