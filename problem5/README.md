# Problem 5 — The Price That Came Back From the Dead

Customers report that a successfully updated price sometimes changes back before
recovering. Your team owns the pricing service. Investigate, reproduce, repair,
and demonstrate that your change is safe.

Run every command below from `problem5/`.


## Local setup checklist

Use a Bash-compatible terminal on macOS/Linux. On Windows, use a WSL2 Linux
terminal with Docker integration enabled; do not paste these commands into
PowerShell or Command Prompt. Install Git and a running Docker engine with
Compose v2.20+ before the event. Install the additional tools listed above/below
in the same terminal environment.

```bash
git --version
docker info
docker compose version
curl --version
```

If this problem lists Python or jq, also check `python3 --version` or `jq --version`.
A missing command is a setup issue; install it before continuing. Container images
include application runtimes; a local Java/Python development environment is only
needed for tests run outside containers.

Run one problem at a time on each team machine. Stop the previous problem from
its own directory before switching: some exercises share host ports. Each team
needs its own checkout and runtime. Initial image/dependency downloads may take
several minutes and need network access; they are outside investigation time.
No organizer action is required to activate the incident.


## Prerequisites

- Docker Engine/Desktop with Docker Compose v2.
- Python 3.10+ (`python3`); scripts use only its standard library.
- `curl`; `jq` is optional for filtering logs.
- Allow approximately 4 GB memory for the complete stack and free disk for images.
- The first build downloads Maven dependencies and container images. The incident
  observation window starts **after readiness**, not during the first build.

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem5
```

Already cloned? Pull the latest changes, then `cd problem5` from the repo root.
Read [the incident brief](participant/incident-brief.md) before opening the source.

## Start and verify

```bash
docker compose -f docker/docker-compose.yml up -d --build
docker compose -f docker/docker-compose.yml ps
./scripts/smoke-test.sh
```

The smoke test waits up to 180 seconds for the API, database and Redis to be ready.
It verifies normal reads/updates and prints `PASS`. It modifies only `SMOKE-001`.
It intentionally does not certify concurrent correctness.

## Generate customer activity

```bash
./scripts/generate-traffic.sh --seconds 180
```

The generator browses products, makes normal catalogue updates, and records
subsequent customer responses. Watch for a reported customer sequence. The target
is to encounter the incident within roughly 2–3 minutes of healthy traffic;
timing depends on the host. `--seconds` limits when another batch starts; a current
batch is allowed to finish, so total runtime can be slightly longer.

Every HTTP observation is saved to `artifacts/traffic.jsonl`, including request IDs,
start/end times and response versions. Copy this file before rerunning, or use
`--output artifacts/second-run.jsonl`. Traffic updates each selected product once;
reruns skip products already updated. Use `--start-sku 1001` for a fresh range.
Do not run two generators against the same product range simultaneously.

If there is no signal, check script errors, readiness, resource pressure and
whether another workload is running. Retain the output, then try another fresh
range. An explanation must account for runtime evidence, not just host speed.

## Inspect the system

| Surface | Address |
|---|---|
| Pricing API | http://localhost:8085 |
| Grafana, Pricing overview dashboard | http://localhost:3005/d/pricing-overview |
| Prometheus | http://localhost:9095 |
| PostgreSQL | localhost:5545, database/user/password `pricing` |
| Redis | localhost:6395 |

Grafana permits anonymous read access. Local admin login is `admin` /
`local-debugathon`. All published ports bind to loopback; these defaults are for
an isolated local exercise.

```bash
curl -s http://localhost:8085/prices/DEMO-001
docker compose -f docker/docker-compose.yml logs -f pricing-api
docker compose -f docker/docker-compose.yml exec postgres \
  psql -U pricing -d pricing -c "SELECT * FROM product_prices WHERE product_id='DEMO-001';"
docker compose -f docker/docker-compose.yml exec redis \
  redis-cli GET pricing:product:DEMO-001
```

Substitute a product ID from your traffic evidence when inspecting stored state.
A later snapshot is not a historical record.

Update a product using the `version` from a recent GET:

```bash
curl -s -X PUT http://localhost:8085/prices/DEMO-001 \
  -H 'Content-Type: application/json' -H 'X-Request-ID: manual-update-1' \
  -d '{"amount":950,"expectedVersion":1}'
```

An outdated `expectedVersion` returns HTTP 409. The update response includes the
committed version. Never assume that every rerun starts at version 1.

See [architecture and API](docs/architecture.md) and
[submission requirements](participant/submission-template.md).

## Tests and cleanup

Java 21 + Maven 3.9+ are needed only for running Java tests outside Docker:

```bash
cd services/pricing-api
mvn test
cd ../..
./scripts/smoke-test.sh
```

Stop while preserving the database:

```bash
docker compose -f docker/docker-compose.yml down
```

Reset **this exercise's** database, prices and versions:

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```

Reset destroys this exercise's local database. Export evidence before resetting.
Its Compose project name is separate. Port 8085 overlaps with Problem 4;
stop Problem 4 before starting this exercise.


## Save evidence, apply a repair, and submit

Use a second terminal in this same problem directory for inspection while traffic
runs. Ctrl+C stops a foreground log view or traffic script; it does not stop the
Docker environment.

Before editing or recreating containers, save a baseline:
```bash
mkdir -p evidence
docker compose -f docker/docker-compose.yml logs --no-color > evidence/baseline-compose.log
git rev-parse HEAD > evidence/baseline-commit.txt
```
Keep generated traffic artifacts too; copy them before a rerun overwrites them.
Use different filenames for before/after evidence.

After changing source or configuration, rebuild and recreate the stack:
```bash
docker compose -f docker/docker-compose.yml up -d --build --force-recreate
```
Then rerun the readiness/smoke command above and the original workload. A restart
alone does not compile source changes. Preserve the workload and diagnostic
interfaces; demonstrate your repair with before/after evidence and regression
tests. Public checks that assert the original incident may fail after a correct
repair; they are not a grading suite.

Complete `participant/submission-template.md`. Submit your tested code changes
(or a commit), reproduction steps, before/after evidence, environment/version
details, and remaining limitations. Include new files as well as modified files;
`git diff` alone omits untracked files. Use the event's announced private submission
destination and deadline; do not publish solutions in this repository. For solo
practice, keep this package locally. Organizer hints are optional.

## If setup fails

```bash
docker compose -f docker/docker-compose.yml ps -a
docker compose -f docker/docker-compose.yml logs --tail 100
```

- Docker connection error: start the Docker engine and retry `docker info`.
- Port already in use: stop the previous exercise or the conflicting local service.
- Image/build download failure: check network/proxy access, then retry startup.
- Container exited or readiness timeout: inspect that service's logs and Docker
  memory/disk allocation. Resolve setup failure before interpreting incident symptoms.
- Dashboard empty: allow a few monitoring scrapes, check Prometheus target health,
  and use application logs in the meantime.
- Script permission error: use `bash scripts/script-name.sh` with the actual filename.
- Still blocked: share the failing command, terminal output and container status
  with event support. Do not send only “it does not work.”
