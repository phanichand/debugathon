# Incident 4 — Kafka Says Everything Is Fine

Operations reports that approximately 0.1% of booking events have no settlement entry. Some bookings instead
have duplicate settlement entries. Reports correlate somewhat with consumer replacements and rebalances.

The producer is publishing bookings, Kafka brokers appear healthy, consumer throughput is excellent,
consumer lag is approximately zero, database health looks normal, and application error rates are low.
Operators have seen “received” messages in logs for some disputed events.

A recent performance optimization increased asynchronous processing. There has been no confirmed broker outage.

Your task is to establish what happened to specific business events, explain both outcomes with one consistent
model, implement a durable correction, and propose safe recovery of affected settlements. Preserve throughput
and distinguish temporary in-flight work from persistent loss. Do not assume a duplicate DB row represents a
legitimate second business event, or that equal aggregate counts imply correctness.

Provide evidence for event identity, partition/offset, worker execution, Kafka checkpoint, process lifetime and
final database state. Explain which facts support your conclusion and which hypotheses your experiments reject.
