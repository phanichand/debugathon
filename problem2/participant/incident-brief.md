# Incident Brief: SEV-2 — Authentication Failures After Scale-Out

**Severity:** SEV-2

## Summary

Authentication success has degraded after traffic increased and the auth service
was scaled horizontally.

Current observations:

- login itself succeeds;
- approximately 6–10% of immediately authenticated follow-up requests return
  `401 Invalid session`;
- retrying the same request shortly afterward often succeeds;
- JWT signature/expiry validation appears healthy;
- Redis is reachable and has no meaningful error spike;
- CPU and memory remain normal;
- there was no application code deployment immediately before the incident.

The service previously ran with one replica. It now runs with three.

## Your Mandate

Your team owns the incident.

Determine impact, mitigate safely, establish the root cause, reproduce the
failure, and implement a permanent fix.

You may use all available engineering tools, including LLM assistants.

Any claimed root cause must be supported with runtime evidence.

## What to Do Next

- Follow the startup and investigation steps in [../README.md](../README.md).
- Review the system overview in [../docs/architecture.md](../docs/architecture.md).
- Submit findings using [submission-template.md](submission-template.md).
