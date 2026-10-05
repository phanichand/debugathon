# Problem 3 submission

## Impact and timeline
Record the baseline commit, environment readiness time, first visible error, request count, initial-request failure rate and retry success separately.

## Runtime evidence
Include exact commands, timestamps and representative request IDs. Break results down by replica/revision. Correlate gateway responses, service logs, downstream results and metrics. Attach evidence rather than only screenshots of source.

## Competing explanations
Which observations support or rule out application changes, saturation, dependency failures, routing, and differences between instances? State what remains uncertain.

## Causal explanation
Explain why only some requests fail, why retry helps, why recycling might temporarily help, and how the infrastructure change entered the failure path. Trace the relevant runtime state back to its sources and deployment artifacts; distinguish declared settings from what requests actually used.

## Controlled experiment
Repeat the same request on different replicas and compare results. Change one relevant condition, repeat the request, and show whether the predicted behavior follows. Include a counterexample or negative control.

## Mitigation and durable repair
Provide your patch and explain why it fixes the causal mechanism. Evaluate at least two alternative mitigations and their limitations. Describe rollback and effects on other workloads.

## Verification
Repeat the original traffic workload after repair. Show initial-request and retry results, per-replica observations, dependency health and relevant runtime state. Verify after configuration refresh, restart and recreation; cover existing and newly created replicas. Add regression coverage for the failure path without disabling the incident workload or masking errors.
