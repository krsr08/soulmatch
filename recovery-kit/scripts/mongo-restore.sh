#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <backup.archive.gz>"
  exit 64
fi

BACKUP_FILE="$1"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.prod.yml}"
MONGO_SERVICE="${MONGO_SERVICE:-mongodb}"
MONGO_DB="${MONGO_DB:-soulmatch_chat}"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup file not found: ${BACKUP_FILE}"
  exit 66
fi

if [[ -f "${BACKUP_FILE}.sha256" ]]; then
  sha256sum -c "${BACKUP_FILE}.sha256"
fi

echo "Restoring MongoDB archive into ${MONGO_DB}. Existing collections will be dropped."
echo "Set CONFIRM_RESTORE=yes to proceed."
if [[ "${CONFIRM_RESTORE:-no}" != "yes" ]]; then
  exit 65
fi

docker compose -f "$COMPOSE_FILE" exec -T "$MONGO_SERVICE" \
  mongorestore --db "$MONGO_DB" --archive --gzip --drop < "$BACKUP_FILE"

echo "MongoDB restore complete."
