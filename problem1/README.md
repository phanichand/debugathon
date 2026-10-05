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

## Troubleshooting

**`unknown shorthand flag: 'f' in -f`** — your Docker CLI doesn't have the
Compose V2 plugin installed (common if Docker wasn't installed via Docker
Desktop). Check with `docker compose version`. Fix: install
[Docker Desktop](https://www.docker.com/products/docker-desktop/) (bundles
the plugin), or if you have the older standalone `docker-compose` (with a
hyphen) available, substitute it for every `docker compose` command below.

**`failed to connect to the docker API at unix:///.../docker.sock` (using
Colima)** — your container runtime isn't running. Start it:

```bash
colima start
```

(install first with `brew install colima` if you don't have it). You should
not need `sudo` for any of these commands — if a command only works with
`sudo`, that usually means a different, misconfigured Docker install is
being picked up; fix the underlying setup rather than using `sudo`.
