# Problem 3 — The One Bad Pod

Customers intermittently fail to retrieve availability following routine infrastructure work. Retrying often succeeds. Investigate the running system and deliver a durable repair supported by evidence.

**Work inside `problem3/` for every command below.** This environment is independent of the other problems.


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
| jq | Not required | — |

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


## Start here

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem3
# If already at the repository root, just run: cd problem3
```

Read [the incident brief](participant/incident-brief.md) before investigating.

Complete the local setup checklist above. Initial builds may take several minutes;
incident timing starts after readiness, not during downloads.

```bash
docker compose -f docker/docker-compose.yml up -d --build --wait --wait-timeout 180
python3 scripts/smoke_test.py
python3 scripts/generate_traffic.py
```

The smoke test must print `PASS`. It checks infrastructure, dependencies, all replicas, Prometheus scrapes and Grafana independently of customer errors. The traffic script runs about 90 seconds, prints failed initial attempts and their retries, and writes `artifacts/traffic.json`. Keep the environment running. Expect observable errors within roughly 2–3 minutes after readiness; generate another batch if you need a longer window.

## Service access

| Component | Local address |
|---|---|
| Customer gateway | http://localhost:8300 |
| Availability replicas A–D | http://localhost:8301 through http://localhost:8304 |
| Supplier | http://localhost:8310 |
| Grafana | http://localhost:3300/d/availability-overview |
| Prometheus | http://localhost:9390 |

Ports bind to loopback. Grafana permits anonymous viewing; local editing credentials are `admin` / `local-debugathon`. Do not expose this training environment directly to the internet.

```bash
curl -i 'http://localhost:8300/availability?route=BLR-MAA'
docker compose -f docker/docker-compose.yml logs -f availability-a availability-b availability-c availability-d supplier
```

`X-Replica`, `X-Revision` and `X-Request-ID` identify the responding instance. Use direct replica ports for controlled investigations. Request IDs appear in both application and supplier logs. HTTP API documentation is available at `/docs` on each application service. See [architecture and operating notes](docs/architecture.md) for the service map and operational interfaces.

## Investigation and submission

LLMs, source inspection and external documentation are allowed. A suspicious line or plausible diagnosis alone is insufficient. Collect timestamped runtime evidence, reproduce the failure, test competing explanations, and demonstrate a durable fix with the original traffic workload.

Use [the submission template](participant/submission-template.md). Keep your baseline evidence before editing. Do not change the supplier latency distribution, traffic mix, error classification, observability or acceptance criteria to hide failures.

## Development and troubleshooting

Run application tests in an isolated environment:

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
python -m pytest -q
```

After source or deployment edits, rebuild/recreate the relevant services using Compose. A container restart does not rebuild its image. If service addresses change after recreation, recreate the gateway as well so upstream DNS is refreshed.

```bash
docker compose -f docker/docker-compose.yml config --quiet
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml logs --tail 100
```

If smoke fails, resolve that failure before interpreting customer errors. Check Docker resources, occupied ports, image downloads and dependency logs. The default traffic output distinguishes HTTP errors from transport errors; transport errors are not an expected incident signal.

To stop while retaining database state:

```bash
docker compose -f docker/docker-compose.yml down
```

For a completely fresh local database, use `down -v` instead. This deletes this problem's catalog volume. All runtime scripts, configuration, tests and documentation live here; the repository-level workflow only invokes them.


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
