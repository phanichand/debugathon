# Debugathon — Problem 1

A production-incident simulation involving a booking flow across four services.

All commands below are intended to be run from the `problem1/` directory.

## 1. Enter Problem 1

If you have not cloned the repository yet:

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem1
```

If you are already at the repository root:

```bash
cd problem1
```

Confirm you are in the right place:

```bash
pwd
ls
```

You should see directories such as `docker/`, `services/`, `scripts/`,
`participant/`, and `docs/`.

## 2. Read the Incident Brief

Start with:

[participant/incident-brief.md](participant/incident-brief.md)

Your team owns the incident. Do not look for an organizer answer key; none is
included in the participant repository.

## 3. Prerequisites

You need:

- Docker with Docker Compose
- `curl`
- `jq`

Check Docker Compose:

```bash
docker compose version
```

If your machine only provides the older `docker-compose` command, see the
troubleshooting section below.

## 4. Start the Environment

From `problem1/`:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Check container status:

```bash
docker compose -f docker/docker-compose.yml ps
```

The environment includes:

| Service | Port | Purpose |
|---|---:|---|
| booking-api | 8080 | Customer-facing booking API |
| booking-orchestrator | 8081 | Coordinates payment and operator booking |
| payment-simulator | 8082 | Simulated payment service |
| operator-simulator | 8083 | Simulated external operator |
| PostgreSQL | 5432 | Service databases |
| Grafana | 3000 | Dashboards and log exploration |
| Prometheus | 9090 | Raw metrics |

## 5. Verify the Environment

Before investigating the incident, verify that the stack itself is healthy:

```bash
./scripts/smoke-test.sh
```

A successful run ends with `PASS`.

This step is important: it distinguishes an environment/setup problem from the
incident you are expected to debug.

## 6. Start Production-Like Traffic

Keep this running in a terminal:

```bash
./scripts/generate-traffic.sh
```

It sends requests only through the public booking API.

The generator prints progress and reports any failed booking IDs it observes.
Once the stack is healthy, incident signals should begin appearing within the
first few minutes.

Do not treat the first failed booking as the answer. Use it as one starting point
for investigation.

You can generate more traffic if useful:

```bash
COUNT=500 ./scripts/generate-traffic.sh
```

## 7. Open Observability

Grafana:

```text
http://localhost:3000
```

Prometheus:

```text
http://localhost:9090
```

Use the provided dashboards, Grafana Explore/logs, service metrics, source code,
and databases as you would during a production incident.

For the system overview, read:

[docs/architecture.md](docs/architecture.md)

## 8. Optional Manual Booking

Create one booking manually:

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"tripId":"TRIP-100","customerId":"CUSTOMER-21","passengers":[{"name":"Arun Kumar"}],"amount":1240.00}'
```

Check a known booking:

```bash
curl http://localhost:8080/api/bookings/{bookingId}
```

## 9. Investigation Expectations

You may use any engineering tools available to you, including IDEs, shells,
documentation, internet search, and LLM assistants.

A plausible explanation is not enough.

Your submission must support the claimed root cause with runtime evidence and
include:

- impact assessment;
- investigation path and hypotheses;
- causal evidence;
- reproduction;
- safe mitigation;
- permanent fix;
- validation;
- observability improvements;
- incident communication.

## 10. Submit Your Findings

Use:

[participant/submission-template.md](participant/submission-template.md)

## Troubleshooting

### `docker compose` is unavailable

If:

```bash
docker compose version
```

does not work but the older standalone command exists, substitute
`docker-compose` for `docker compose`.

For the smoke test, use:

```bash
./scripts/smoke-test-docker-compose-v1.sh
```

### Docker daemon is not running

If you see an error similar to:

```text
failed to connect to the docker API at unix:///.../docker.sock
```

start your container runtime.

For Colima:

```bash
colima start
```

If needed on macOS:

```bash
brew install colima
```

You should not need `sudo` for the normal Debugathon commands.
