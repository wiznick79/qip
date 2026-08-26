#!/usr/bin/env sh
set -eu

ROOT_DIR="${QIP_HOSTED_ROOT:-/opt/qip}"
BACKUP_DIR="${QIP_BACKUP_DIRECTORY:-$ROOT_DIR/backups}"
RETENTION_DAYS="${QIP_BACKUP_RETENTION_DAYS:-14}"
COMPOSE_FILE="$ROOT_DIR/deploy/compose.hosted.yaml"
SECRETS_FILE="$ROOT_DIR/deploy/.env"
DEPLOYMENT_FILE="$ROOT_DIR/deploy/.deployment.env"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TARGET="$BACKUP_DIR/$STAMP"

compose() {
  docker compose --env-file "$SECRETS_FILE" --env-file "$DEPLOYMENT_FILE" -f "$COMPOSE_FILE" "$@"
}

mkdir -p "$TARGET"
chmod 700 "$BACKUP_DIR" "$TARGET"

# Stop writes so the database and uploaded-document snapshots describe one bounded point in time.
compose stop qip
trap 'compose start qip >/dev/null 2>&1 || true' EXIT

compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' \
  > "$TARGET/qip-database.dump"
docker run --rm \
  -v qip-hosted-document-data:/source:ro \
  -v "$TARGET:/backup" \
  alpine:3.22.1 tar -C /source -czf /backup/qip-documents.tar.gz .

sha256sum "$TARGET/qip-database.dump" "$TARGET/qip-documents.tar.gz" > "$TARGET/SHA256SUMS"
compose start qip
trap - EXIT

find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -mtime "+$RETENTION_DAYS" -exec rm -rf -- {} +
echo "Created hosted backup $TARGET"
