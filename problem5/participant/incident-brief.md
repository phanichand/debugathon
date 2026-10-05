# Incident: The Price That Came Back From the Dead

Support has customer observations resembling:

**₹820 → catalogue update to ₹950 → ₹950 → ₹820 → ₹950**

The catalogue update returned success. Engineers inspecting the product later
found ₹950 in PostgreSQL and Redis. Cache-operation logs reported successful
operations. Error rates and infrastructure dashboards did not explain the
customer reports. Ordinary one-at-a-time requests appear healthy.

You have the pricing source, local runtime, metrics, logs and data stores.
The service uses an in-process cache, Redis and PostgreSQL. Treat the initial
report as a lead to reproduce, not proof of a particular cause.

## Your task

1. Capture a customer-visible occurrence using reproducible traffic.
2. Explain which requests, values and state changes produced it.
3. Build a reliable regression test and implement a permanent correction.
4. Show normal reads/updates still work and explain concurrency, throughput and
   availability implications.
5. Provide a concise incident report with evidence and remaining limitations.

Competing investigation leads include expiration/TTL, propagation or replication
delay, local cache state, read-replica routing, invalidation outcomes and concurrent
request ordering. Establish which leads the running system actually supports.

LLMs, documentation and source search are allowed. Naming a plausible bug or
editing a suspicious line is not sufficient: demonstrate cause and effect.
