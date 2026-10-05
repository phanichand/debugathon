# Pricing runtime

One Spring Boot instance handles reads and catalogue writes. Read lookup order is
the process-local Caffeine cache, shared Redis, then PostgreSQL. PostgreSQL holds
the authoritative product amount, currency, version and update timestamp.

Cold catalogue reads also prepare a quote breakdown using market tax rules on a
bounded worker pool. All components run ordinary production code paths; there
are no scenario-control endpoints.

## API contract

- `GET /prices/{productId}` returns `productId`, `amount`, `currency`, `version`
  and `updatedAt`. Unknown products return 404.
- `PUT /prices/{productId}` accepts `amount` and `expectedVersion`. Successful
  updates increment the database version and return the committed price.
- Invalid amounts return 400; version conflicts return 409.
- `X-Request-ID` is accepted or generated and echoed. `X-Pricing-Instance`
  identifies the serving process. Responses prohibit HTTP caching.
- `/actuator/health/readiness` checks application, database and Redis readiness.
- `/actuator/prometheus` exposes operational metrics.

Amounts use decimal arithmetic; versions belong to individual products.
This local exercise runs a single PostgreSQL primary and a single Redis server.
Deployment configuration is in `docker/docker-compose.yml`; application settings
are in `services/pricing-api/src/main/resources/application.yml`.

## Evidence

The API emits structured JSON to stdout. Each business event carries a request ID;
price events carry a product, amount and version. Process boot ID, monotonic time
and event sequence help correlate logs from one run. Event sequence denotes log
emission order, not a distributed transaction boundary. Use request start/end
times and operation semantics alongside log events.

Prometheus/Grafana show request rates, latency, errors, cache hits, read sources,
update rates, CPU and heap. Metrics are aggregated and are not a per-request
history. Raw structured logs remain available through Docker Compose.

There is no durable update-event replay system in this exercise. Redis and local
cache contents are transient; PostgreSQL persists in the named Compose volume.
