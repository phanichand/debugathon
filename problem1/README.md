# Debugathon — Problem 1

A production-incident simulation involving a booking flow across four services.

All commands below are intended to be run from the `problem1/` directory.


## Local setup checklist

| Tool | Why you need it | Check |
|---|---|---|
| Git | Download and track code | `git --version` |
| Docker + Compose v2.20 or newer | Run the complete local stack | `docker info`, `docker compose version` |
| Bash-compatible terminal | Run the supplied commands | `bash --version` |
| curl | Send HTTP requests | `curl --version` |
| Web browser | View dashboards at the listed localhost URLs | Open your browser |
| Your preferred code editor | Read/edit source and the submission template | Open the problem folder |
| jq | Required: format/process JSON | `jq --version` |
| Host Python / Java | Not required for Docker startup | Optional only for host development/tests |

Suggested starting allocation: **4 CPUs and 6 GB RAM** for Docker. This is planning guidance, not a benchmarked minimum.
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

## 3. Check tools

Complete the local setup checklist above before startup. Docker Compose v1 is not supported.

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

A successful run ends with `PASS`. The check waits for API readiness and makes
up to five independent bookings to verify one normal transaction. An incident
booking is reported and retained; it is not mistaken for a startup failure.
If no normal booking succeeds, the check fails with diagnostics.

This step is important: it distinguishes an environment/setup problem from the
incident you are expected to debug.

## 6. Start Production-Like Traffic

Keep this running in a terminal:

```bash
mkdir -p evidence
set -o pipefail
./scripts/generate-traffic.sh 2>&1 | tee evidence/baseline-traffic.log
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

Install or enable the Docker Compose v2 plugin, then repeat the version check.

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

## Stop, reset, and switch problems

Save evidence first. To pause without deleting containers/data:
```bash
docker compose -f docker/docker-compose.yml stop
# Resume:
docker compose -f docker/docker-compose.yml start
```
To remove this exercise's containers and local data before a fresh run:
```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```
Treat container removal/recreation as potentially destructive to this exercise's
local data. Save evidence and code first. Stop/remove this stack before starting
another exercise that shares its ports.

If you used an older release with the default project name `docker`, preserve its
evidence, then run `docker compose -p docker -f docker/docker-compose.yml down`
from that old problem's directory before starting the newly named stack. This
removes the legacy exercise containers; do not use it for unrelated projects.


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
