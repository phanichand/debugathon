# System Architecture — Problem 2

## Overview

```text
Client
  |
  v
Nginx Gateway :8080
  |
  +------------------------+
  |           |            |
  v           v            v
auth-a      auth-b       auth-c
:8081       :8082        :8083
  \           |           /
   \          |          /
              Redis
```

The gateway distributes requests across three identical Python/FastAPI auth
service replicas.

Each auth replica performs JWT verification and participates in the session
validation subsystem, which also uses Redis.

## Public API

### POST /login

Accepts a username and returns a signed JWT.

### GET /me

Requires:

```http
Authorization: Bearer <token>
```

Returns the authenticated user when validation succeeds.

Every response includes:

```http
X-Auth-Pod: <replica>
```

which identifies the serving replica.

## Runtime components

### gateway

Nginx load-balances requests across the auth replicas.

### auth replicas

All replicas run the same Python application and configuration other than their
replica name.

### Redis

Shared infrastructure used by the authentication system.

### Prometheus / Grafana

Prometheus scrapes each auth replica independently. Grafana provides aggregate
and per-replica authentication metrics.

## Useful investigation surfaces

- gateway: http://localhost:8080
- auth-a: http://localhost:8081
- auth-b: http://localhost:8082
- auth-c: http://localhost:8083
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Redis: localhost:6379

You are free to inspect source, container logs, metrics, Redis state, and direct
replica behavior.
