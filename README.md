# Debugathon

A collection of production-incident simulations for hands-on debugging practice.

Each problem is self-contained. Participant setup, incident context, architecture,
scripts, and submission instructions live inside that problem's directory.

## Getting Started

Clone the repository once:

```bash
git clone https://github.com/phanichand/debugathon.git
cd debugathon
```

Then choose a problem and follow the README inside that directory.

For example:

```bash
cd problem1
```

From that point onward, use the instructions inside that problem.

## Problems

| Problem | Incident | Stack | Status |
|---|---|---|---|
| [Problem 1](problem1/) | Booking conversion degradation | Java / Spring Boot | Available |
| [Problem 2](problem2/) | Authentication failures after scale-out | Python / FastAPI | Available |
| Problem 3 | Production incident simulation | TBD | Planned |
| Problem 4 | Production incident simulation | TBD | Planned |
| Problem 5 | Production incident simulation | TBD | Planned |

## Participant Model

The repository is intended to contain participant-facing material only.

For each problem:

1. enter the problem directory;
2. read that problem's README and incident brief;
3. start the environment using that problem's instructions;
4. investigate using the available source, APIs, logs, metrics, dashboards, and databases;
5. submit findings using the problem's submission template.

Organizer runbooks, answer keys, hidden validation, and event-control material are
kept separately from this repository.
