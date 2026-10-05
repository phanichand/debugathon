# Participant Guide — Debugathon Incident 1

## Start Here

Run the exercise from the `problem1/` directory.

The complete startup and investigation flow is documented in:

[../README.md](../README.md)

Read the incident brief before starting your investigation:

[incident-brief.md](./incident-brief.md)

For a system-level overview:

[../docs/architecture.md](../docs/architecture.md)

## Participant Access

Once the environment is running, the main participant-facing surfaces are:

| Service | Port | What it's for |
|---|---:|---|
| booking-api | 8080 | Customer-facing API |
| Grafana | 3000 | Dashboards and log exploration |
| Prometheus | 9090 | Raw metrics |

The other application services are also reachable locally and their source code,
health endpoints, metrics, logs, and databases are available for investigation.

## Allowed Tools

You may use any engineering tool available to you, including:

- your IDE, shell, and standard debugging tools;
- documentation and internet search;
- LLM assistants such as Claude, ChatGPT, Copilot, or Cursor.

There is no tool restriction.

The constraint is evidentiary: any claimed root cause must be supported by
runtime evidence you can show, such as logs, metrics, database state, traces, or
a reliable reproduction. A plausible theory by itself is not sufficient.

## Submitting Your Findings

Use:

[submission-template.md](./submission-template.md)

Fill in every section.
