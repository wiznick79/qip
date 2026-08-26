#!/usr/bin/env sh
set -eu

ROOT_DIR="${QIP_HOSTED_ROOT:-/opt/qip}"
COMPOSE_FILE="$ROOT_DIR/deploy/compose.hosted.yaml"
SECRETS_FILE="$ROOT_DIR/deploy/.env"
DEPLOYMENT_FILE="$ROOT_DIR/deploy/.deployment.env"
STATE_FILE="$ROOT_DIR/deploy/.previous-image"

fail() {
  echo "Hosted deployment failed: $*" >&2
  exit 1
}

compose() {
  docker compose --env-file "$SECRETS_FILE" --env-file "$DEPLOYMENT_FILE" -f "$COMPOSE_FILE" "$@"
}

write_image() {
  image="$1"
  temporary="$DEPLOYMENT_FILE.tmp"
  printf 'QIP_IMAGE=%s\n' "$image" > "$temporary"
  chmod 600 "$temporary"
  mv "$temporary" "$DEPLOYMENT_FILE"
}

wait_until_healthy() {
  attempts=0
  while [ "$attempts" -lt 24 ]; do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' qip-hosted-qip 2>/dev/null || true)"
    [ "$status" = "healthy" ] && return 0
    [ "$status" = "unhealthy" ] && return 1
    attempts=$((attempts + 1))
    sleep 5
  done
  return 1
}

deploy() {
  next_image="$1"
  echo "$next_image" | grep -Eq '^ghcr\.io/[a-z0-9_.-]+/[a-z0-9_.-]+:[0-9]+\.[0-9]+\.[0-9]+$' \
    || fail "image must be a semantic-versioned GHCR reference"

  previous_image=""
  if [ -f "$DEPLOYMENT_FILE" ]; then
    previous_image="$(sed -n 's/^QIP_IMAGE=//p' "$DEPLOYMENT_FILE")"
  fi

  write_image "$next_image"
  compose pull
  compose up -d --remove-orphans

  if wait_until_healthy; then
    [ -z "$previous_image" ] || printf '%s\n' "$previous_image" > "$STATE_FILE"
    echo "QIP is healthy on $next_image"
    return 0
  fi

  if [ -n "$previous_image" ]; then
    echo "New image did not become healthy; restoring $previous_image" >&2
    write_image "$previous_image"
    compose up -d --remove-orphans
    wait_until_healthy || fail "automatic rollback also failed"
  fi
  fail "new image did not become healthy"
}

rollback() {
  [ -s "$STATE_FILE" ] || fail "no previous healthy image is recorded"
  rollback_image="$(cat "$STATE_FILE")"
  current_image="$(sed -n 's/^QIP_IMAGE=//p' "$DEPLOYMENT_FILE")"
  write_image "$rollback_image"
  compose up -d --remove-orphans
  wait_until_healthy || fail "rollback image did not become healthy"
  printf '%s\n' "$current_image" > "$STATE_FILE"
  echo "QIP rolled back to $rollback_image"
}

[ -f "$COMPOSE_FILE" ] || fail "missing $COMPOSE_FILE"
[ -f "$SECRETS_FILE" ] || fail "missing $SECRETS_FILE; create it from .env.hosted.example"
[ "$(stat -c '%a' "$SECRETS_FILE")" = "600" ] || fail "$SECRETS_FILE must have mode 600"

case "${1:-}" in
  deploy)
    [ "$#" -eq 2 ] || fail "usage: deploy-hosted.sh deploy <ghcr-image:semver>"
    deploy "$2"
    ;;
  rollback)
    [ "$#" -eq 1 ] || fail "usage: deploy-hosted.sh rollback"
    rollback
    ;;
  *) fail "usage: deploy-hosted.sh deploy <image> | rollback" ;;
esac
