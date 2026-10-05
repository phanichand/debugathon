"""Small HTTP client shared by local operational scripts (Python standard library)."""
import json
import os
import time
import uuid
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8085").rstrip("/")


def request(path, method="GET", body=None, request_id=None):
    request_id = request_id or str(uuid.uuid4())
    headers = {"Content-Type": "application/json", "X-Request-ID": request_id}
    req = urllib.request.Request(BASE + path, method=method, headers=headers,
                                 data=None if body is None else json.dumps(body).encode())
    start = time.monotonic_ns()
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            payload = response.read().decode()
            return response.status, json.loads(payload), request_id, start, time.monotonic_ns()
    except urllib.error.HTTPError as error:
        return error.code, {"error": error.read().decode()}, request_id, start, time.monotonic_ns()


def ready(timeout=180):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            if request("/actuator/health/readiness")[0] == 200:
                return
        except (OSError, ValueError):
            pass
        time.sleep(1)
    raise RuntimeError(f"Pricing readiness did not pass within {timeout}s at {BASE}")
