# Team submission

## Customer impact and reproduction
- Exact commit, environment and commands:
- Product IDs and saved request/response evidence:
- Time to first observed incident, traffic duration and request count:
- Which observations were separate requests started after update success?

## Evidence timeline
| Request ID | Start/end or event sequence | Operation | Observed price/version | Store or response |
|---|---|---|---|---|

Attach the relevant structured log lines and traffic records. Explain how you
ordered events and any limits of clock precision or snapshots.

## Hypotheses
| Hypothesis | Test/evidence | Supported or rejected, and why |
|---|---|---|

## Root cause
Explain how individually successful operations produced the observed behavior.
Account for the database/Redis snapshots and the later recovery.

## Reproduction test and fix
- Reliable test before the change and the result after it:
- Changed code and the invariant it enforces:
- Why overlapping operations cannot violate that invariant:
- Behavior when entries expire, requests fail or successive updates overlap:

## Safety and performance
- Sequential read/update regression results:
- Concurrent workload results and comparison against the baseline:
- Cache effectiveness, independent-product progress and resource usage:
- Throughput/availability trade-offs, mitigations and remaining risks:

## Incident communication
Customer impact, cause, mitigation, permanent fix and follow-up observability.
