# Architecture and API reference

The booking producer publishes keyed JSON events to `booking-events` (three partitions). A settlement consumer
group processes them using a bounded executor and writes PostgreSQL settlement entries. The same Spring Boot
artifact supports producer and consumer roles; each runs in its own container. PostgreSQL stores publication
receipts and business settlements separately. Monetary amounts are integer paise.

The producer records an event before sending, then records the broker-assigned partition/offset after send succeeds.
Reconciliation counts only source events with delivery receipts. A failed request without a receipt is not proof
that Kafka never received it; retain producer responses when investigating uncertain publication outcomes.
Producer idempotence is enabled for Kafka transport retries. Application retries may publish the same business event.
Reuse of an event ID with changed payload is rejected.

| Endpoint | Purpose |
|---|---|
| `POST /events` | Publish one event and return event ID, partition and offset |
| `POST /traffic?runId=demo&rate=200&seconds=180&seed=42` | Start a bounded workload; concurrent runs are rejected |
| `GET /traffic` | Current run progress and publication errors |
| `GET /operations/reconciliation?runId=demo` | Expected distinct events, settlement rows, missing IDs and duplicates |
| `GET /operations/events/{id}` | Source, delivery receipts and settlement rows for an event |
| `GET /operations/offsets` | Broker-reported committed next offsets, end offsets and lag per partition |
| `GET /actuator/health` | Service and database health |
| `GET /actuator/prometheus` | Metrics |

Example event (send with `Content-Type: application/json`):

```json
{"eventId":"e-1","bookingId":"b-1","merchantId":"standard","amountPaise":82000,"currency":"INR","runId":"manual"}
```

The `review` merchant follows a slower enrichment path. Merchant processing coordinates with PostgreSQL advisory
locks; a concurrent merchant maintenance transaction can delay enrichment. Workload options `reviews` and
`retries` control the proportion of review bookings and repeated deliveries. `CHECKPOINT_MS` is the consumer
checkpoint interval. `RECEIPT_TIMEOUT_RATE` models ambiguous persistence receipts in the local training environment.
The workload generator makes normal API calls; it never edits settlements or Kafka offsets.

Grafana shows records received/sec, completed business processing/sec, errors, group lag and infrastructure
health. Group lag uses committed offsets, not merely the consumer's fetch position. No-data is not equivalent
to zero lag. Event-level logs and SQL are available for deeper investigation. Kafka replication factor 1 is a
laptop-resource choice; broker durability faults are outside this incident.
