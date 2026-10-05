# Problem 3 — The One Bad Pod

Customers intermittently fail to retrieve availability following routine infrastructure work. Retrying often succeeds. Investigate the running system and deliver a durable repair supported by evidence.

**Work inside `problem3/` for every command below.** This environment is independent of the other problems.

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
