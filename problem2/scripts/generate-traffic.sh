#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COUNT="${COUNT:-150}"
RETRY_DELAY="${RETRY_DELAY:-1.3}"

SUCCESS=0
INITIAL_401=0
RECOVERED=0
STILL_FAILED=0

echo "Generating ${COUNT} login + authenticated-request flows against ${BASE_URL}"
echo ""

for i in $(seq 1 "${COUNT}"); do
  USERNAME="user-$(printf '%04d' "${i}")"

  LOGIN_HEADERS=$(mktemp)
  LOGIN_BODY=$(mktemp)
  curl -sS -D "${LOGIN_HEADERS}" -o "${LOGIN_BODY}" -X POST "${BASE_URL}/login"     -H "Content-Type: application/json"     -d "{\"username\":\"${USERNAME}\"}"

  TOKEN=$(jq -r '.access_token' "${LOGIN_BODY}")
  LOGIN_POD=$(awk 'tolower($1) == "x-auth-pod:" {gsub("\r","",$2); print $2}' "${LOGIN_HEADERS}" | tail -1)

  ME_HEADERS=$(mktemp)
  ME_BODY=$(mktemp)
  HTTP_CODE=$(curl -sS -D "${ME_HEADERS}" -o "${ME_BODY}" -w "%{http_code}"     "${BASE_URL}/me"     -H "Authorization: Bearer ${TOKEN}")
  ME_POD=$(awk 'tolower($1) == "x-auth-pod:" {gsub("\r","",$2); print $2}' "${ME_HEADERS}" | tail -1)

  if [[ "${HTTP_CODE}" == "200" ]]; then
    SUCCESS=$((SUCCESS + 1))
  elif [[ "${HTTP_CODE}" == "401" ]]; then
    INITIAL_401=$((INITIAL_401 + 1))
    echo "[${i}/${COUNT}] 401 after login: loginPod=${LOGIN_POD:-?} requestPod=${ME_POD:-?} user=${USERNAME}"

    sleep "${RETRY_DELAY}"

    RETRY_HEADERS=$(mktemp)
    RETRY_BODY=$(mktemp)
    RETRY_CODE=$(curl -sS -D "${RETRY_HEADERS}" -o "${RETRY_BODY}" -w "%{http_code}"       "${BASE_URL}/me"       -H "Authorization: Bearer ${TOKEN}")
    RETRY_POD=$(awk 'tolower($1) == "x-auth-pod:" {gsub("\r","",$2); print $2}' "${RETRY_HEADERS}" | tail -1)

    if [[ "${RETRY_CODE}" == "200" ]]; then
      RECOVERED=$((RECOVERED + 1))
      echo "           retry recovered on pod=${RETRY_POD:-?}"
    else
      STILL_FAILED=$((STILL_FAILED + 1))
      echo "           retry still failed: HTTP ${RETRY_CODE} pod=${RETRY_POD:-?}"
    fi

    rm -f "${RETRY_HEADERS}" "${RETRY_BODY}"
  else
    echo "[${i}/${COUNT}] unexpected HTTP ${HTTP_CODE}: $(cat "${ME_BODY}")"
  fi

  rm -f "${LOGIN_HEADERS}" "${LOGIN_BODY}" "${ME_HEADERS}" "${ME_BODY}"

  if (( i % 25 == 0 )); then
    echo "  ...${i}/${COUNT} flows (success=${SUCCESS}, initial401=${INITIAL_401}, recovered=${RECOVERED})"
  fi
done

echo ""
echo "Done."
echo "Immediate success: ${SUCCESS}"
echo "Initial 401:       ${INITIAL_401}"
echo "Recovered retry:  ${RECOVERED}"
echo "Still failed:     ${STILL_FAILED}"
