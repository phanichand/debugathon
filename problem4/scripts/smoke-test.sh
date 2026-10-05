#!/usr/bin/env bash
set -euo pipefail
for i in $(seq 1 90); do
  if curl -fsS http://localhost:8084/actuator/health | jq -e '.status=="UP"' >/dev/null &&
     curl -fsS http://localhost:8085/actuator/health | jq -e '.status=="UP"' >/dev/null &&
     curl -fsS http://localhost:8084/operations/offsets | jq -e 'length==3' >/dev/null; then
    echo 'PASS: producer, consumer, DB and three Kafka partitions are reachable'; exit 0
  fi
  sleep 2
done
echo 'FAIL: environment did not become healthy' >&2
exit 1
