# System Architecture

This describes the Debugathon Incident 1 platform at a system level: what
each service does and how they fit together. It does not describe internal
implementation details — those are in the service source code, which
you're free to read.

## Overview

```text
Customer
   │
   ▼
Booking API
   │
   ▼
Booking Orchestrator
   │         │         │
   │         ▼         ▼
   │      Payment    Operator
   │      Simulator  Simulator
   │         │         │
   ▼         ▼         ▼
         PostgreSQL
```

Four services, fronted by a single customer-facing API. Three of the four
— booking-orchestrator, payment-simulator, and operator-simulator — each
have their own PostgreSQL database; booking-api is stateless.

## Services

### booking-api (port 8080)

The only service customers talk to directly.

- Validates incoming booking requests.
- Generates or receives a correlation ID for request tracing.
- Delegates to booking-orchestrator and returns its result.
- Exposes `POST /api/bookings` and `GET /api/bookings/{bookingId}`.

### booking-orchestrator (port 8081)

Coordinates a booking end-to-end. Not called directly by customers.

- Maintains booking state.
- Calls payment-simulator to charge the customer.
- Calls operator-simulator to create the operator-side reservation.
- Performs retry logic on the operator call.
- Persists a record of each booking attempt.

### payment-simulator (port 8082)

Simulates the customer-payment step of a booking. Not called directly by
customers.

- Accepts a payment request and returns a payment reference once the charge
  completes.

### operator-simulator (port 8083)

Represents an external operator/bus booking system that
booking-orchestrator calls into. Not called directly by customers.

- Creates and persists operator-side reservations from orchestrator requests.
- Processes each accepted request to completion independently of whether the
  calling service is still waiting for a response.

## Observability

All four services expose:

- `/actuator/health` — liveness/health
- `/actuator/prometheus` — Prometheus-format metrics

Metrics are scraped into Prometheus (port 9090) and visualized in Grafana
(port 3000, anonymous viewer access), covering request rates,
success/failure rates, and latency percentiles per service. Structured JSON
logs from every service are collected and queryable from Grafana's Explore
view.

## Data

booking-orchestrator, payment-simulator, and operator-simulator each own
their own PostgreSQL database — there is no shared schema between them.
booking-api has no database of its own.
