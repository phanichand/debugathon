# Participant Guide — Debugathon Incident 1

## Start Here

Read the incident brief first: [`incident-brief.md`](./incident-brief.md)

## Access Instructions

1. Start the full stack:

   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```

2. Public services:

   | Service | Port | What it's for |
   |---|---|---|
   | booking-api | 8080 | The only API you call directly — see below |
   | Grafana | 3000 | Dashboards (anonymous viewer access, no login needed) |
   | Prometheus | 9090 | Raw metrics, if you want to query directly |

   (`booking-orchestrator`, `payment-simulator`, and `operator-simulator`
   also run, each with its own port, but you don't call them directly —
   `booking-api` is your entry point.)

3. Create a booking:

   ```bash
   curl -X POST http://localhost:8080/api/bookings \
     -H "Content-Type: application/json" \
     -d '{"tripId":"TRIP-100","customerId":"CUSTOMER-21","passengers":[{"name":"Arun Kumar"}],"amount":1240.00}'
   ```

4. Check a booking's status:

   ```bash
   curl http://localhost:8080/api/bookings/{bookingId}
   ```

5. Every service also exposes `/actuator/health` and `/actuator/prometheus`
   on its own port, and ships structured JSON logs that are queryable from
   Grafana's Explore view (Loki datasource).

6. Generate traffic to see the system under load (it hits only the public
   booking API, nothing else):

   ```bash
   ./scripts/generate-traffic.sh              # 200 bookings by default
   COUNT=500 ./scripts/generate-traffic.sh    # or a different amount
   ```

   A single booking succeeds the vast majority of the time — you may need
   a few hundred before you see a failure. That's expected, not a bug in
   the script.

See [`../docs/architecture.md`](../docs/architecture.md) for how the
services fit together.

### If `docker compose` doesn't work on your machine

Some setups only have the older standalone `docker-compose` (with a
hyphen) rather than the `docker compose` plugin. If so:

- Substitute `docker-compose` for `docker compose` in any command above.
- Use [`../scripts/smoke-test-docker-compose-v1.sh`](../scripts/smoke-test-docker-compose-v1.sh)
  instead of `scripts/smoke-test.sh` — it's identical except for that one
  difference.

## Allowed Tools

You may use any engineering tool available to you, including:

- Your IDE, shell, and standard debugging tools
- Documentation and internet search
- LLM assistants (e.g. Claude, ChatGPT, Copilot, Cursor)

There is no tool restriction. The constraint is evidentiary: any claimed
root cause must be supported by runtime evidence you can show — logs,
metrics, or a reproduction — not just a plausible-sounding theory.

## Submitting Your Findings

Use the template at [`submission-template.md`](./submission-template.md).
Fill in every section.
