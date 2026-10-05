# Incident: intermittent availability failures

A routine capacity change introduced another availability-service replica and restarted some infrastructure. The application version did not change. Customer support now reports intermittent failures when checking bus availability; a second attempt often works.

Production observations supplied by the incident commander:

- Initial-request success is approximately 94–97%.
- Aggregate CPU and memory are unremarkable.
- Supplier availability and latency remain within its normal contract (responses within 1 second).
- Catalog database checks are healthy.
- Some operators report a temporary improvement after recycling a replica.

These reports are starting hypotheses, not a substitute for measuring your environment. Compose containers represent production replicas/revisions; Kubernetes is not required for this exercise.

Your task is to establish the causal chain, assess mitigation risks, implement the smallest durable repair, and verify it under the original workload. Explain the infrastructure-change connection and any temporary recovery you observe.

Deliver a concise incident report plus before/after evidence and a patch. Use the submission template. Preserve the diagnostic interfaces and workload. The public CI signal check validates the distributed incident baseline; after a repair, its expectation of failures will intentionally no longer hold. Supply your own before/after repair validation without weakening the baseline check.
