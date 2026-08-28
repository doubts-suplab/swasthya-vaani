#!/usr/bin/env bash
# Start the SwasthyaVaani backend and frontend together for local development.
# Requires: Java 21, Node 20+, pnpm. Set SARVAM_API_KEY in your environment first.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -z "${SARVAM_API_KEY:-}" ]]; then
  echo "⚠️  SARVAM_API_KEY is not set — the app will run but Sarvam calls will fail."
  echo "    export SARVAM_API_KEY=... (see docs/sarvam-integration.md)"
fi

cleanup() {
  echo "Shutting down..."
  kill 0
}
trap cleanup EXIT INT TERM

echo "▶ Backend  : http://localhost:8080  (health: /actuator/health, ping: /api/v1/ping)"
( cd "$ROOT_DIR/backend" && ./mvnw -q -pl swasthyavaani-api -am spring-boot:run ) &

echo "▶ Frontend : http://localhost:5173"
( cd "$ROOT_DIR/frontend" && pnpm install --silent && pnpm dev ) &

wait
