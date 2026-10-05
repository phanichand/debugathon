#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080"

echo "Waiting for booking API readiness (up to 180 seconds)..."
deadline=$((SECONDS + 180))
until curl -fsS --connect-timeout 2 --max-time 3 "${BASE_URL}/actuator/health" | jq -e '.status == "UP"' >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "FAIL: API not ready; inspect docker compose -f docker/docker-compose.yml ps -a and logs" >&2
    exit 1
  fi
  sleep 2
done

# Exercise normal behavior without changing the intentionally degraded profile.
STATUS=""
for attempt in 1 2 3 4 5; do
  echo "Creating independent smoke booking ${attempt}/5..."
  CREATE_RESPONSE=$(curl -fsS --connect-timeout 3 --max-time 30 -X POST "${BASE_URL}/api/bookings" \
    -H "Content-Type: application/json" \
    -d '{"tripId":"TRIP-100","customerId":"CUSTOMER-21","passengers":[{"name":"Arun Kumar"}],"amount":1240.00}')
  echo "Response: ${CREATE_RESPONSE}"
  STATUS=$(echo "${CREATE_RESPONSE}" | jq -r '.status')
  BOOKING_ID=$(echo "${CREATE_RESPONSE}" | jq -r '.bookingId')
  OPERATOR_BOOKING_ID=$(echo "${CREATE_RESPONSE}" | jq -r '.operatorBookingId')
  if [[ "${STATUS}" == "CONFIRMED" ]]; then break; fi
  if [[ "${STATUS}" != "FAILED" ]]; then
    echo "FAIL: unexpected booking status ${STATUS}" >&2
    exit 1
  fi
  echo "Observed incident booking ${BOOKING_ID}; keeping it for investigation."
done
if [[ "${STATUS}" != "CONFIRMED" ]]; then
  echo "FAIL: no normal booking succeeded in five attempts; inspect service logs." >&2
  exit 1
fi

echo "Fetching booking ${BOOKING_ID}..."
GET_RESPONSE=$(curl -sS "${BASE_URL}/api/bookings/${BOOKING_ID}")
echo "Response: ${GET_RESPONSE}"

GET_STATUS=$(echo "${GET_RESPONSE}" | jq -r '.status')
if [[ "${GET_STATUS}" != "CONFIRMED" ]]; then
  echo "FAIL: GET returned status ${GET_STATUS}, expected CONFIRMED"
  exit 1
fi

echo "Verifying exactly one operator booking exists for source booking ${BOOKING_ID}..."
OPERATOR_COUNT=$(docker compose -f docker/docker-compose.yml exec -T postgres \
  psql -U debugathon -d operatordb -tAc \
  "SELECT count(*) FROM operator_bookings WHERE source_booking_id = '${BOOKING_ID}'")

if [[ "$(echo "${OPERATOR_COUNT}" | tr -d '[:space:]')" != "1" ]]; then
  echo "FAIL: expected exactly one operator booking for source booking ${BOOKING_ID}, found ${OPERATOR_COUNT}"
  exit 1
fi

echo "Verifying payment amount persisted exactly as submitted..."
PAYMENT_AMOUNT=$(docker compose -f docker/docker-compose.yml exec -T postgres \
  psql -U debugathon -d paymentdb -tAc \
  "SELECT amount::text FROM payment_transactions WHERE booking_reference = '${BOOKING_ID}'")
PAYMENT_AMOUNT="$(echo "${PAYMENT_AMOUNT}" | tr -d '[:space:]')"

if [[ "${PAYMENT_AMOUNT}" != "1240.00" ]]; then
  echo "FAIL: expected payment amount 1240.00, found ${PAYMENT_AMOUNT}"
  exit 1
fi

echo "Verifying operator booking amount persisted exactly as submitted..."
OPERATOR_AMOUNT=$(docker compose -f docker/docker-compose.yml exec -T postgres \
  psql -U debugathon -d operatordb -tAc \
  "SELECT amount::text FROM operator_bookings WHERE source_booking_id = '${BOOKING_ID}'")
OPERATOR_AMOUNT="$(echo "${OPERATOR_AMOUNT}" | tr -d '[:space:]')"

if [[ "${OPERATOR_AMOUNT}" != "1240.00" ]]; then
  echo "FAIL: expected operator booking amount 1240.00, found ${OPERATOR_AMOUNT}"
  exit 1
fi

echo "PASS: booking ${BOOKING_ID} is CONFIRMED with exactly one operator booking ${OPERATOR_BOOKING_ID}, payment amount ${PAYMENT_AMOUNT}, operator amount ${OPERATOR_AMOUNT}"
