# Problem 5 — The Price That Came Back From the Dead

Customers report that a successfully updated price sometimes changes back before
recovering. Your team owns the pricing service. Investigate, reproduce, repair,
and demonstrate that your change is safe.

Run every command below from `problem5/`.


## Local setup checklist

| Tool | Why you need it | Check |
|---|---|---|
| Git | Download and track code | `git --version` |
| Docker + Compose v2.20 or newer | Run the complete local stack | `docker info`, `docker compose version` |
| Bash-compatible terminal | Run the supplied commands | `bash --version` |
| curl | Send HTTP requests | `curl --version` |
| Web browser | View dashboards at the listed localhost URLs | Open your browser |
| Your preferred code editor | Read/edit source and the submission template | Open the problem folder |
| Python 3.10+ | Required on your machine for smoke and traffic scripts | `python3 --version` |
| jq | Optional for filtering JSON | `jq --version` |
| Host Java / Maven | Not required for Docker startup | Java 21 + Maven 3.9+ for optional host tests |

Allow approximately **4 GB RAM** for Docker.
Leave additional memory for your editor/browser and free disk for container images.
Linux CI is verified; macOS/Windows setup should be rehearsed on your own machine.


### Install on your operating system

Use your company's approved Docker runtime if one is already provided. Do not
install a second runtime just for this exercise.

**macOS**
1. Install [Docker Desktop for Mac](https://docs.docker.com/desktop/setup/install/mac-install/),
   choosing Apple silicon or Intel as appropriate, then open Docker and wait for it to start.
2. Open Terminal. If using [Homebrew](https://brew.sh/), install the command-line tools
   listed in the table with `brew install git jq python` (omit jq/Python if not needed).
   macOS supplies curl. Without Homebrew, use the official
   [Git](https://git-scm.com/downloads/), [jq](https://jqlang.org/download/) and
   [Python](https://www.python.org/downloads/) installation pages.
3. Run `bash` in Terminal, then run the checks below.

**Windows**
1. Follow [Microsoft's WSL installation instructions](https://learn.microsoft.com/en-us/windows/wsl/install)
   to install a WSL2 Ubuntu distribution. That one-time installation uses an
   administrator PowerShell window; restart Windows if requested.
2. Install and start [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/).
   Enable its [WSL2 backend and Ubuntu integration](https://docs.docker.com/desktop/features/wsl/).
3. Open **Ubuntu** from the Start menu. Install the tools inside Ubuntu using the
   Ubuntu commands below. Run all exercise commands there, not in PowerShell.
   Clone the repository inside your Linux home directory.

**Ubuntu Linux (and tools inside WSL Ubuntu)**
Install command-line tools in the Ubuntu terminal:
```bash
sudo apt update
sudo apt install -y git curl jq python3 python3-venv
```
This installs the combined toolset for all five problems; jq/Python can be omitted
when the table says they are not needed. On native Ubuntu, follow the official
[Docker Engine and Compose plugin instructions](https://docs.docker.com/engine/install/ubuntu/).
For WSL using Docker Desktop, use its integration instead of installing another
Docker Engine. Other Linux distributions should use their own package manager
and the [Docker installation guide](https://docs.docker.com/engine/install/).

Installation may need administrator assistance. Before continuing, `docker info`
must work from the same terminal you will use for the exercise.


### Before starting the incident

Checks should print versions, and `docker info` should show a **Server** section
without a connection error. If a command is missing, return to the installation
steps. For Python, check that `python3 --version` meets the table's requirement.

The application runtimes and required PostgreSQL, Redis, Kafka, Grafana and
Prometheus services are supplied by Docker as applicable. Do not install these
servers separately. Local Java/build tools are only needed if you choose to run
Java tests outside Docker. **Host Python is required for the supplied scripts in
Problems 3 and 5**, even though the applications run in containers.

Build/download dependencies before the event; initial startup needs network access.
Run one problem at a time per machine and stop the previous exercise before
switching because some ports overlap. Each team uses its own checkout/runtime.
Organizer action is not required to activate an incident.


## Get the code

Complete the local setup checklist above. Initial build/download time is separate
from the incident observation window.

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
