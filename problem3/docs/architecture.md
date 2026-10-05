# Availability platform

The gateway distributes each incoming lookup across four availability-service replicas. The service calls the supplier, which reads its PostgreSQL catalog and returns inventory. Supplier route timings are stable so repeated requests for the same route can be compared. The gateway does not transparently retry upstream errors; the supplied client records initial requests and one customer retry separately.

## Deployment assets

All replicas use application version `1.0.0` and the same Docker image. Deployment revisions identify infrastructure releases independently of application code. The checked-in release manifests are compiled at container creation into `/runtime/bundle.json` by `scripts/render_bundle.py`. This models a release pipeline producing a mounted runtime artifact.

Policy definitions are shared by availability and suggestions workloads. The local environment runs availability only. Regional release bindings are maintained alongside the infrastructure manifests. Application defaults, the shared environment, policy definitions, compiled artifacts and live snapshots can all be inspected.

Configuration is initialized during process startup and refreshed periodically. Invalid refresh input retains the previous usable snapshot and logs a refresh failure. `/ops/runtime` is a read-only operational endpoint on each replica; it includes process identity, runtime state and source attribution. It is not exposed through the customer gateway. `/healthz` reports liveness, `/readyz` checks dependency readiness, and `/metrics` exposes service/process telemetry. These endpoints are operational interfaces, not scenario controls.

## Observability

The default Grafana dashboard shows aggregate customer status, latency, supplier health, database-query latency and process resource usage. It does not identify a root cause. In Prometheus, `availability_requests_total` has `pod`, `revision` and `status` labels. Supplier metrics track its completed responses even if a caller has already stopped waiting.

Application JSON logs include request IDs, replica, revision, duration, result and configuration fingerprint. Supplier logs carry the caller's request ID. Configuration lifecycle logs record snapshot changes. Prometheus process metrics expose CPU and resident memory per target.

Useful operational commands, from `problem3/`:

```bash
docker compose -f docker/docker-compose.yml exec availability-a cat /runtime/bundle.json
docker compose -f docker/docker-compose.yml exec db psql -U catalog -d catalog -c 'SELECT * FROM inventory;'
```

Only the dependency catalog is persisted. Recreating a replica recompiles its release assets; restarting reuses its existing container filesystem. The first build and monitoring warm-up are infrastructure time, separate from incident reproduction time.
