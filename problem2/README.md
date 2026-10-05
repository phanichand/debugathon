# Debugathon — Problem 2

A production-incident simulation involving intermittent authentication failures
after a horizontally scaled service rollout.

All commands below are intended to be run from the `problem2/` directory.

## 1. Enter Problem 2

If you have not cloned the repository yet:

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem2
```

If you are already at the repository root:

```bash
cd problem2
```

You should see `docker/`, `services/`, `scripts/`, `participant/`, and
`docs/`.

## 2. Read the Incident Brief

Start with:

[participant/incident-brief.md](participant/incident-brief.md)

## 3. Prerequisites

You need:

- Docker with Docker Compose
- `curl`
- `jq`

## 4. Start the Environment

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Check status:

```bash
docker compose -f docker/docker-compose.yml ps
```

The environment includes:

| Service | Port | Purpose |
|---|---:|---|
| gateway | 8080 | Public authentication endpoint |
| auth-a | 8081 | Auth service replica A |
| auth-b | 8082 | Auth service replica B |
| auth-c | 8083 | Auth service replica C |
| Redis | 6379 | Shared session infrastructure |
| Grafana | 3000 | Metrics dashboard |
| Prometheus | 9090 | Raw metrics |

## 5. Verify the Environment

```bash
./scripts/smoke-test.sh
```

A successful run ends with `PASS`.

## 6. Start Traffic

Keep this running in one terminal:

```bash
./scripts/generate-traffic.sh
```

The script performs realistic login followed immediately by an authenticated
request. It reports intermittent authentication failures and retries failed
requests after a short delay.

Once the stack is healthy, the incident signal should be visible within the
first couple of minutes.

Do not treat retry success as the root cause. Use it as evidence.

Generate more traffic if useful:

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

Logs:

```bash
docker compose -f docker/docker-compose.yml logs -f gateway auth-a auth-b auth-c
```

Architecture overview:

[docs/architecture.md](docs/architecture.md)

## 8. Manual Requests

Login:

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"participant"}' | jq -r '.access_token')
```

Use the token:

```bash
curl -i http://localhost:8080/me \
  -H "Authorization: Bearer $TOKEN"
```

The `X-Auth-Pod` response header identifies the replica that served a request.

## 9. Investigation Expectations

You may use source code, logs, metrics, Redis, direct replica ports, Docker,
internet documentation, and LLM assistants.

A plausible explanation is not enough. Your root cause must be supported with
runtime evidence and a reliable reproduction.

## 10. Submit Your Findings

Use:

[participant/submission-template.md](participant/submission-template.md)
