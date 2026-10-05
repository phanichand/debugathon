#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Checking gateway..."
curl -fsS "${BASE_URL}/healthz" >/dev/null

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
HTTP_CODE=$(curl -sS -o /tmp/debugathon-problem2-smoke.json -w "%{http_code}"   "${BASE_URL}/me"   -H "Authorization: Bearer ${TOKEN}")

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "FAIL: expected HTTP 200 after replication settled; got ${HTTP_CODE}"
  cat /tmp/debugathon-problem2-smoke.json
  exit 1
fi

echo "PASS: login and authenticated request are healthy after replication settles."
