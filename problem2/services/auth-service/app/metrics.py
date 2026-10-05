from prometheus_client import Counter, Gauge

LOGIN_TOTAL = Counter(
    "auth_login_total",
    "Number of login requests",
    ["pod"],
)

VALIDATION_TOTAL = Counter(
    "auth_validation_total",
    "Authenticated request validation outcomes",
    ["pod", "result"],
)

LOCAL_REGISTRY_SIZE = Gauge(
    "auth_local_registry_size",
    "Number of session entries currently held by this replica",
    ["pod"],
)
