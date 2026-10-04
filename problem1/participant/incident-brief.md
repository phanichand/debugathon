# Incident Brief: SEV-2 — Booking Conversion Degradation

**Severity:** SEV-2

## Summary

Booking success has fallen from its usual level by approximately 1–2 percentage
points. Payment success appears stable. Operator request latency has
increased, although operator HTTP error rate remains low. Customer Support
has begun receiving booking-failure complaints. There has been no
application deployment immediately preceding the incident.

## Your Mandate

Your team owns the incident.

Determine impact, mitigate safely, establish root cause, reproduce the
failure, and implement a permanent fix.

You may use all available engineering tools, including LLM assistants.

Any claimed root cause must be supported with runtime evidence.

No further detail will be provided up front — investigate using the system
itself: its public API, its dashboards, and its logs.

## What to Do Next

- Access instructions and allowed tools: [`README.md`](./README.md)
- System overview: [`../docs/architecture.md`](../docs/architecture.md)
- Submit your findings using: [`submission-template.md`](./submission-template.md)
