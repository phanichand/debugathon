# Problem 4 — Kafka Says Everything Is Fine

Investigate missing and duplicate settlements while infrastructure dashboards look healthy.
This directory is a complete, independent environment. All commands below run inside `problem4/`.


## Local setup checklist

| Tool | Why you need it | Check |
|---|---|---|
| Git | Download and track code | `git --version` |
| Docker + Compose v2.20 or newer | Run the complete local stack | `docker info`, `docker compose version` |
| Bash-compatible terminal | Run the supplied commands | `bash --version` |
| curl | Send HTTP requests | `curl --version` |
| Web browser | View dashboards at the listed localhost URLs | Open your browser |
| Your preferred code editor | Read/edit source and the submission template | Open the problem folder |
| jq | Required by shell scripts | `jq --version` |
| Python 3 | Optional: needed for `scripts/verify.py` | `python3 --version` |
| Host Java / Gradle | Not required for Docker startup | Optional for host Java tests |

Allocate at least **4 CPUs and 6 GB RAM** to Docker.
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


## 1. Requirements

Complete the local setup checklist above. The 2–3 minute observation window begins
after readiness. This local stack uses sample credentials and loopback-only ports.

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem4
```

Read [the incident brief](participant/incident-brief.md) before investigating.

## 2. Start and verify

```bash
docker compose -f docker/docker-compose.yml up -d --build
./scripts/smoke-test.sh
```

| Surface | Location |
|---|---|
| Producer and operations API | http://localhost:8084 |
| Consumer health/metrics | http://localhost:8085/actuator/health |
| Grafana | http://localhost:3004/d/settlement-overview |
| Prometheus | http://localhost:9094 |
| PostgreSQL | localhost:5444; database/user/password `settlements`/`settlement`/`settlement` |
| Kafka (host clients) | localhost:19094 |

If startup fails, inspect `docker compose -f docker/docker-compose.yml ps -a` and `logs`.
Check memory, image download access, and occupied ports. Kafka topic initialization must exit with code 0.

## 3. Generate the incident

```bash
./scripts/generate-traffic.sh
```

The default workload runs for three minutes and replaces the consumer process twice while requests continue.
Expect roughly 0.1% missing settlements over sufficiently large runs, with occasional duplicates; exact rates vary.
Open Grafana during the workload. You should see useful evidence around the first process replacement, within
roughly two minutes of starting traffic. The final reconciliation waits for ordinary in-flight processing to drain.

To inspect while traffic runs, copy the printed run ID:

```bash
curl -s 'http://localhost:8084/operations/reconciliation?runId=YOUR_RUN_ID' | jq .
curl -s http://localhost:8084/operations/offsets | jq .
docker compose -f docker/docker-compose.yml logs -f consumer
```

Reconciliation during active traffic includes work still in progress. Do not classify every temporary difference as loss.
If a small run has no discrepancy, repeat with a different `SEED` or increase `DURATION`.
`RATE=200 DURATION=240 SEED=71 ./scripts/generate-traffic.sh` is an example.
The workload script performs at most two replacements per invocation.

## 4. Investigate

See [architecture and API reference](docs/architecture.md). Investigate an actual event:

```bash
curl -s http://localhost:8084/operations/events/YOUR_EVENT_ID | jq .
docker compose -f docker/docker-compose.yml exec postgres psql -U settlement -d settlements
```

Kafka group inspection:

```bash
docker compose -f docker/docker-compose.yml exec kafka   /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092   --group settlement-workers --describe
```

Optional scale-out/rebalance:

```bash
docker compose -f docker/docker-compose.yml --profile scale up -d consumer-b
docker compose -f docker/docker-compose.yml stop consumer-b
```

Stop consumer-b before running the default workload or public verification.
Logs persist across `stop`/`start`; save logs before recreating a container.

## 5. Public environment verification

On a **fresh local stack**, the following checks ordinary processing and reproduces the incident symptoms.
It changes only this training stack, terminates/recreates its consumer, and saves evidence under `evidence/`.
Run without concurrent traffic. These checks describe the supplied incident build, so symptom assertions may
stop passing after you fix it. They are not the grading suite.

```bash
RECEIPT_TIMEOUT_RATE=0 docker compose -f docker/docker-compose.yml up -d --build
python3 scripts/verify.py
```

## 6. Submit

LLMs and internet documentation are allowed. A suspicious code line or plausible theory is insufficient.
Use [the submission template](participant/submission-template.md): supply runtime artifacts, a reproduction,
a tested patch, safe recovery instructions, and the consequences of replaying historical data.

## 7. Stop or reset

```bash
# Preserve all data:
docker compose -f docker/docker-compose.yml --profile scale down
# Delete only this problem's training data and Kafka offsets:
docker compose -f docker/docker-compose.yml --profile scale down -v
```

After reset, rerun startup and smoke checks. Do not reset data before collecting evidence.


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
