# Debugathon

A collection of production-incident simulations for hands-on debugging practice.

Each problem is self-contained. Participant setup, incident context, architecture,
scripts, and submission instructions live inside that problem's directory.

## Install tools before cloning

All five exercises run locally. Prepare the tools below before the event.

| Tool | Purpose | Check |
|---|---|---|
| Git | Download and track code | `git --version` |
| Docker + Compose v2.20 or newer | Run the complete local stack | `docker info`, `docker compose version` |
| Bash-compatible terminal | Run the supplied commands | `bash --version` |
| curl | Send HTTP requests | `curl --version` |
| Web browser | View dashboards at the listed localhost URLs | Open your browser |
| Your preferred code editor | Read/edit source and the submission template | Open the problem folder |

| Additional tool | Problem 1 | Problem 2 | Problem 3 | Problem 4 | Problem 5 |
|---|---|---|---|---|---|
| jq (`jq --version`) | Required | Required | Not required | Required | Optional |
| Host Python (`python3 --version`) | Not required for startup | Not required for startup | 3.10+ required | Python 3 for optional verification | 3.10+ required |


### Install on your operating system

Use your company's approved Docker runtime if one is already provided. Do not
install a second runtime just for this exercise.

**macOS**
1. Install [Docker Desktop for Mac](https://docs.docker.com/desktop/setup/install/mac-install/),
   choosing Apple silicon or Intel as appropriate, then open Docker and wait for it to start.
2. Open Terminal. If using [Homebrew](https://brew.sh/), install the command-line tools
   listed in the table with `brew install git jq python` (omit jq/Python if not needed).
   macOS supplies curl. Without Homebrew, use the official
   [Git](https://git-scm.com/downloads/), [jq](https://jqlang.org/download/) and
   [Python](https://www.python.org/downloads/) installation pages.
3. Run `bash` in Terminal, then run the checks below.

**Windows**
1. Follow [Microsoft's WSL installation instructions](https://learn.microsoft.com/en-us/windows/wsl/install)
   to install a WSL2 Ubuntu distribution. That one-time installation uses an
   administrator PowerShell window; restart Windows if requested.
2. Install and start [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/).
   Enable its [WSL2 backend and Ubuntu integration](https://docs.docker.com/desktop/features/wsl/).
3. Open **Ubuntu** from the Start menu. Install the tools inside Ubuntu using the
   Ubuntu commands below. Run all exercise commands there, not in PowerShell.
   Clone the repository inside your Linux home directory.

**Ubuntu Linux (and tools inside WSL Ubuntu)**
Install command-line tools in the Ubuntu terminal:
```bash
sudo apt update
sudo apt install -y git curl jq python3 python3-venv
```
This installs the combined toolset for all five problems; jq/Python can be omitted
when the table says they are not needed. On native Ubuntu, follow the official
[Docker Engine and Compose plugin instructions](https://docs.docker.com/engine/install/ubuntu/).
For WSL using Docker Desktop, use its integration instead of installing another
Docker Engine. Other Linux distributions should use their own package manager
and the [Docker installation guide](https://docs.docker.com/engine/install/).

Installation may need administrator assistance. Before continuing, `docker info`
must work from the same terminal you will use for the exercise.


### Before starting the incident

Checks should print versions, and `docker info` should show a **Server** section
without a connection error. If a command is missing, return to the installation
steps. For Python, check that `python3 --version` meets the table's requirement.

The application runtimes and required PostgreSQL, Redis, Kafka, Grafana and
Prometheus services are supplied by Docker as applicable. Do not install these
servers separately. Local Java/build tools are only needed if you choose to run
Java tests outside Docker. **Host Python is required for the supplied scripts in
Problems 3 and 5**, even though the applications run in containers.

Build/download dependencies before the event; initial startup needs network access.
Run one problem at a time per machine and stop the previous exercise before
switching because some ports overlap. Each team uses its own checkout/runtime.
Organizer action is not required to activate an incident.

### Machine resources

These are resources available to Docker, not the laptop's total RAM.
Leave capacity for your operating system, browser and editor.

| Problem | Docker resource guidance |
|---|---|
| 1 | Suggested starting allocation: 4 CPUs / 6 GB RAM; not a benchmarked minimum |
| 2 | Suggested starting allocation: 2 CPUs / 4 GB RAM; not a benchmarked minimum |
| 3 | Approximately 4 GB RAM |
| 4 | At least 4 CPUs / 6 GB RAM |
| 5 | Approximately 4 GB RAM |

Leave free disk space for images, dependencies and evidence. These exercises were
verified on Linux CI; macOS/Windows participants should rehearse setup beforehand.
Each problem README repeats the tools and setup instructions for standalone use.

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

From that point onward, use the instructions inside that problem. Run one problem
at a time per machine; stop the previous environment before switching. Each team
uses its own local environment. Setup, incident generation and investigation are
self-service; the organizer supplies event rules and the submission destination.

## Problems

| Problem | Incident | Stack | Status |
|---|---|---|---|
| [Problem 1](problem1/) | Booking conversion degradation | Java / Spring Boot | Available |
| [Problem 2](problem2/) | Authentication failures after scale-out | Python / FastAPI | Available |
| [Problem 3](problem3/) | The One Bad Pod | Python / FastAPI | Available |
| [Problem 4](problem4/) | Settlement processing discrepancies | Java / Spring Boot / Kafka | Available |
| [Problem 5](problem5/) | The Price That Came Back From the Dead | Java / Spring Boot | Available |

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
