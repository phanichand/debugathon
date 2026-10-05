#!/usr/bin/env bash
set -euo pipefail
RUN_ID="${RUN_ID:-traffic-$(date +%s)}"
SECONDS_TO_RUN="${DURATION:-180}"
RATE="${RATE:-200}"
mkdir -p evidence
curl -fsS -X POST "http://localhost:8084/traffic?runId=${RUN_ID}&rate=${RATE}&seconds=${SECONDS_TO_RUN}&seed=${SEED:-42}" | jq .
echo "Run: $RUN_ID. This local workload includes two consumer process replacements."
start=$(date +%s)
for target in 80 160; do
  if (( target >= SECONDS_TO_RUN )); then break; fi
  # Jitter keeps the normal workload probabilistic; deterministic checks are separate.
  target=$((target + RANDOM % 7 - 3))
  while (( $(date +%s) - start < target )); do sleep 1; done
  date -u '+Consumer termination %FT%TZ' | tee -a "evidence/${RUN_ID}-lifecycle.log"
  docker compose -f docker/docker-compose.yml kill -s SIGKILL consumer
  docker compose -f docker/docker-compose.yml start consumer
done
while curl -fsS http://localhost:8084/traffic | jq -e '.running' >/dev/null; do sleep 2; done
# Give outstanding business work time to finish before assessing reconciliation.
sleep 12
curl -fsS "http://localhost:8084/operations/reconciliation?runId=${RUN_ID}" | tee "evidence/${RUN_ID}-reconciliation.json" | jq .
curl -fsS http://localhost:8084/operations/offsets | tee "evidence/${RUN_ID}-offsets.json" | jq .
docker compose -f docker/docker-compose.yml logs --no-color consumer > "evidence/${RUN_ID}-consumer.log"
echo "Evidence saved under evidence/. Use event IDs to investigate discrepancies."
