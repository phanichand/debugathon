# Problem 4 — Kafka Says Everything Is Fine

Investigate missing and duplicate settlements while infrastructure dashboards look healthy.
This directory is a complete, independent environment. All commands below run inside `problem4/`.

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
