# Problem 1 with CubeAPM

This optional pilot adds CubeAPM alongside the existing Grafana/Prometheus/Loki
stack. It instruments all four Java services with a pinned, checksum-verified
OpenTelemetry agent. Application behaviour is unchanged. Runtime acceptance is
pending: do not treat configuration validation as proof of telemetry ingestion.

## Prerequisites

Use the normal Problem 1 prerequisites. Additionally obtain a CubeAPM account
token approved for local exercise instances and a CubeAPM administrator email.
Confirm participant login with your administrator before the event. A Docker image
alone does not supply a licence or working login. Allow additional Docker memory
for CubeAPM and the collector; the pilot has not been resource-benchmarked.

All commands below run from `problem1/` in Bash. Use one problem per machine.

```bash
cp docker/cubeapm/.env.example docker/cubeapm/.env
# Edit .env: token, administrator email and a fresh 32-character session key.
# To generate the session key: openssl rand -hex 16
```

The local `.env` is excluded from Git AND Docker build contexts. Do not paste it
into a ticket or commit it. Avoid sharing `docker compose config` output, which
resolves credentials. Use `config --quiet` for validation.

```bash
cube() {
  docker compose --env-file docker/cubeapm/.env \
    -f docker/docker-compose.yml -f docker/docker-compose.cubeapm.yml "$@"
}
cube config --quiet
cube up -d --build
cube ps -a
./scripts/smoke-test.sh
COUNT=300 ./scripts/generate-traffic.sh
```

Open http://localhost:3125 and sign in using your approved CubeAPM account.
Select environment `debugathon-problem1`. Only the browser port is published;
telemetry travels on the local Compose network. Built-in demo traces are disabled
so they cannot be mistaken for exercise traffic.

## Verify before using this pilot

Wait up to two minutes after traffic for export/ingestion. Record these results:

1. All four services appear: `booking-api`, `booking-orchestrator`,
   `payment-simulator`, `operator-simulator`.
2. Open a booking trace and confirm connected spans across the API,
   orchestrator and downstream services. Check successful and slow requests.
3. Confirm fresh application logs for each service in CubeAPM. Compare a request
   with `cube logs booking-orchestrator` and inspect trace correlation where available.
4. Confirm the metrics explorer returns fresh `booking_requests_total` and
   `http_server_requests_seconds_count` series. Compare rates/latency over the
   same time window with the existing Grafana dashboard at http://localhost:3000.
   Existing custom metric names/labels are collected from all four original
   `/actuator/prometheus` endpoints. Auto-generated APM metrics do not replace
   these business metrics. Grafana dashboard JSON is not imported automatically.
5. Confirm the normal smoke passes and the traffic run still produces the
   incident signal described in the participant brief. Retain traffic output.
6. Check `cube logs cubeapm cubeapm-collector` and Java service logs for rejected
   exports, authentication errors or restart loops. A working UI alone is not
   evidence that ingestion works.

Existing Grafana views and SQL/log investigation instructions remain applicable.
This pilot adds APM; it does not yet replace the curated Grafana dashboard.

## Troubleshooting and cleanup

- Missing token/email/key: populate `.env`. The session key must be 32 characters.
- CubeAPM login/startup rejected: ask the account administrator to confirm token
  validity and local-instance entitlement. Never substitute a production token
  without approval for this use.
- Empty telemetry: check all containers, exporter errors and the selected time
  window/environment. Metrics are scraped every 15 seconds. Java agent metrics
  are disabled deliberately to avoid collecting the same metrics twice through
  that agent and the collector.
- Exporter connection errors immediately at startup can be transient while
  CubeAPM starts. Persistent errors mean verification has failed.
- Rebuild after application changes with `cube up -d --build`.

```bash
cube down                    # Stop; retain CubeAPM telemetry volume.
# cube down -v               # Optional full reset; deletes CubeAPM telemetry.
```

To return to the uninstrumented default setup, stop the overlay first, then run:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Reference: https://docs.cubeapm.com/instrumentation/opentelemetry/java
Configuration: https://docs.cubeapm.com/configure
