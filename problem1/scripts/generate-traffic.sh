#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COUNT="${COUNT:-200}"

TRIP_IDS=()
for i in $(seq 1 20); do TRIP_IDS+=("TRIP-$(printf '%03d' "$i")"); done
CUSTOMER_IDS=()
for i in $(seq 1 50); do CUSTOMER_IDS+=("CUSTOMER-$(printf '%02d' "$i")"); done
PASSENGER_NAMES=("Arun Kumar" "Priya Sharma" "Rahul Verma" "Sneha Iyer" "Vikram Singh" \
  "Ananya Rao" "Karthik Nair" "Divya Menon" "Rohan Gupta" "Meera Pillai")

CONFIRMED=0
FAILED=0

echo "Generating ${COUNT} bookings against ${BASE_URL}/api/bookings ..."
echo "(Only hits the public booking API — no admin access, no docker involved.)"
echo ""

for i in $(seq 1 "${COUNT}"); do
  TRIP_ID="${TRIP_IDS[$((RANDOM % ${#TRIP_IDS[@]}))]}"
  CUSTOMER_ID="${CUSTOMER_IDS[$((RANDOM % ${#CUSTOMER_IDS[@]}))]}"
  PASSENGER_NAME="${PASSENGER_NAMES[$((RANDOM % ${#PASSENGER_NAMES[@]}))]}"
  CENTS=$(( (RANDOM % 450000) + 50000 ))
  AMOUNT=$(awk "BEGIN { printf \"%.2f\", ${CENTS}/100 }")

  RESPONSE=$(curl -sS -X POST "${BASE_URL}/api/bookings" \
    -H "Content-Type: application/json" \
    -d "{\"tripId\":\"${TRIP_ID}\",\"customerId\":\"${CUSTOMER_ID}\",\"passengers\":[{\"name\":\"${PASSENGER_NAME}\"}],\"amount\":${AMOUNT}}")

  STATUS=$(echo "${RESPONSE}" | jq -r '.status // "UNKNOWN"')

  case "${STATUS}" in
    CONFIRMED)
      CONFIRMED=$((CONFIRMED + 1))
      ;;
    FAILED)
      FAILED=$((FAILED + 1))
      echo "[${i}/${COUNT}] FAILED: ${RESPONSE}"
      ;;
    *)
      echo "[${i}/${COUNT}] Unexpected response: ${RESPONSE}"
      ;;
  esac

  if (( i % 25 == 0 )); then
    echo "  ...${i}/${COUNT} done so far (confirmed=${CONFIRMED}, failed=${FAILED})"
  fi
done

echo ""
echo "Done: ${CONFIRMED} confirmed, ${FAILED} failed, out of ${COUNT} total."
if (( FAILED > 0 )); then
  echo "Found ${FAILED} failed booking(s) above — use their bookingId to start investigating."
else
  echo "No failures yet — try a larger COUNT (e.g. COUNT=500 ./scripts/generate-traffic.sh)."
fi
