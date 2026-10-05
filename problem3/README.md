# Problem 3 — The One Bad Pod

Customers intermittently fail to retrieve availability following routine infrastructure work. Retrying often succeeds. Investigate the running system and deliver a durable repair supported by evidence.

**Work inside `problem3/` for every command below.** This environment is independent of the other problems.


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


## Start here

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon/problem3
# If already at the repository root, just run: cd problem3
```

Read [the incident brief](participant/incident-brief.md) before investigating.

Prerequisites: Docker Engine/Desktop with Compose v2.20+, Python 3.10+ for host scripts, approximately 4 GB available memory, and network access for the first image build. A first build can take several minutes; the incident timing starts after readiness, not during downloads.

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
