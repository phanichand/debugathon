# Incident 4 submission

## Impact and reproduction
- Commit tested, machine/resources, exact startup commands and workload seed/run ID:
- Distinct expected events, missing events, duplicate business effects, affected amounts and time window:
- How did you exclude normal in-flight processing and failed publications?

## Evidence for a missing settlement
- Event ID, business identity, Kafka partition and offset:
- Worker start/end timestamps and consumer instance:
- Committed next offset, evidence timestamp and source:
- Restart/crash/rebalance timestamp:
- Final DB query/results and supporting logs:

## Evidence for a duplicate settlement
- Event ID, all delivery offsets, execution IDs and settlement rows:
- Was this the same Kafka record replayed or a repeated business event at another offset? Prove it.
- Worker, DB transaction, checkpoint and lifecycle timeline:

## Causal model
Explain partitions, within-partition processing concurrency, acknowledgement and commit timing, delivery semantics,
business idempotency, and why healthy infrastructure/low lag are consistent with the observed impact.
Separate what “received”, “completed”, and “committed” mean in this application.

## Patch and validation
- Patch/commit and rationale:
- Before/after evidence with multiple partitions and out-of-order completion:
- Crash before DB commit; crash after DB commit; consumer rebalance; worker failure:
- Same business event redelivered at a different Kafka offset; distinct legitimate events for one booking:
- Throughput/backpressure measurements and trade-offs:
- Remaining failure windows and limitations:

## Recovery and operational response
- Immediate containment without increasing impact:
- How affected IDs/offset ranges are identified:
- Bounded replay procedure, authorization checks, audit trail and stop/rollback criteria:
- How existing duplicates and financial totals are handled:
- Why your recovery does not generate another duplicate settlement:
- Prevention metrics/alerts and a concise incident update:

## Alternatives tested
Record evidence for rejected alternatives. Explain why capacity changes, restarts, retention changes,
blind offset resets, a single configuration toggle, or a delivery-semantics slogan do or do not solve the incident.
