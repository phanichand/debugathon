# Debugathon — Problem 1

A production-incident simulation: four services, a real booking flow, and
an incident to investigate.

## Start Here

Read the incident brief first: [`participant/incident-brief.md`](participant/incident-brief.md)

Then see [`participant/README.md`](participant/README.md) for access
instructions and allowed tools, and
[`docs/architecture.md`](docs/architecture.md) for a system overview.

## Local Run

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./gradlew build
docker compose -f docker/docker-compose.yml up -d --build
./scripts/smoke-test.sh
```

Services: booking-api (8080), booking-orchestrator (8081), payment-simulator
(8082), operator-simulator (8083), postgres (5432), Prometheus (9090),
Grafana (3000).

Happy path: `POST /api/bookings` → payment succeeds → operator creates
exactly one booking → booking becomes `CONFIRMED` →
`GET /api/bookings/{id}` returns it.

## Submitting Your Findings

Use [`participant/submission-template.md`](participant/submission-template.md).
