#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Waiting for gateway readiness (up to 180 seconds)..."
deadline=$((SECONDS + 180))
until curl -fsS --connect-timeout 2 --max-time 3 "${BASE_URL}/healthz" | jq -e '.status == "UP"' >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "FAIL: gateway not ready; inspect docker compose -f docker/docker-compose.yml ps -a and logs" >&2
    exit 1
  fi
  sleep 2
done
SMOKE_BODY=$(mktemp)
trap 'rm -f "$SMOKE_BODY"' EXIT

echo "Logging in..."
LOGIN_RESPONSE=$(curl -fsS -X POST "${BASE_URL}/login"   -H "Content-Type: application/json"   -d '{"username":"smoke-user"}')

TOKEN=$(echo "${LOGIN_RESPONSE}" | jq -r '.access_token')
if [[ -z "${TOKEN}" || "${TOKEN}" == "null" ]]; then
  echo "FAIL: login did not return a token"
  exit 1
fi

echo "Allowing normal replication to settle..."
sleep 1.5

echo "Calling authenticated endpoint..."
HTTP_CODE=$(curl -sS -o "${SMOKE_BODY}" -w "%{http_code}"   "${BASE_URL}/me"   -H "Authorization: Bearer ${TOKEN}")

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "FAIL: expected HTTP 200 after replication settled; got ${HTTP_CODE}"
  cat "${SMOKE_BODY}"
  exit 1
fi

echo "PASS: login and authenticated request are healthy after replication settles."
