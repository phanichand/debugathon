# Debugathon — Problem 2

A production-incident simulation involving intermittent authentication failures
after a horizontally scaled service rollout.

All commands below are intended to be run from the `problem2/` directory.


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

- Docker with Docker Compose v2.20+
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

A successful run ends with `PASS`. The script waits for gateway readiness first.
This checks normal operation after settling; immediate-request failures are
investigated separately with the traffic script.

## 6. Start Traffic

Keep this running in one terminal:

```bash
mkdir -p evidence
set -o pipefail
./scripts/generate-traffic.sh 2>&1 | tee evidence/baseline-traffic.log
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

If the default workload shows no initial 401s, save its output, confirm all three
auth replicas are running, and retry with `COUNT=500 ./scripts/generate-traffic.sh`.
Connection errors and HTTP 502 responses are setup problems, not the intended incident.


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
