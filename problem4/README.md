# Problem 4 — Kafka Says Everything Is Fine

Investigate missing and duplicate settlements while infrastructure dashboards look healthy.
This directory is a complete, independent environment. All commands below run inside `problem4/`.


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


## 1. Requirements

Docker Engine/Desktop with Compose v2, `curl`, `jq`, and Python 3 for optional environment verification.
Allocate at least 4 CPU cores and 6 GB of Docker memory. First build downloads Java/Gradle dependencies and images;
the 2–3 minute incident window starts **after the environment is healthy**. No local Java installation is needed.
This is a local training stack with sample credentials and loopback-only published ports.

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
