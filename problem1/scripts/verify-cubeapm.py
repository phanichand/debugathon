#!/usr/bin/env python3
"""Read back real application telemetry from a fresh local CubeAPM instance."""
import base64
import json
import time
import urllib.parse
import urllib.request
import uuid

QUERY = 'http://cubeapm:3140'
SERVICES = {'booking-api', 'booking-orchestrator', 'payment-simulator', 'operator-simulator'}
start = int(time.time()) - 900


def request(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers=headers or {})
    with urllib.request.urlopen(req, timeout=15) as response:
        return response.read().decode()


def query(path, values, post=False):
    encoded = urllib.parse.urlencode(values)
    return request(QUERY + path + ('' if post else '?' + encoded),
                   encoded.encode() if post else None)


def eventually(check, description):
    deadline = time.monotonic() + 180
    last = 'no result'
    while time.monotonic() < deadline:
        try:
            result = check()
            if result:
                print('PASS:', description, flush=True)
                return result
        except Exception as exc:
            last = type(exc).__name__  # Never print credentials/response bodies.
        time.sleep(5)
    raise RuntimeError(description + ': timed out (' + last + ')')

# Use a known W3C trace ID so readiness/scrape traces cannot satisfy the test.
trace_id = uuid.uuid4().hex
parent_id = uuid.uuid4().hex[:16]
body = json.dumps({'tripId': 'TRIP-100', 'customerId': 'CUBEAPM-VERIFY',
                   'passengers': [{'name': 'Telemetry Test'}], 'amount': 1240.00}).encode()
result = json.loads(request('http://booking-api:8080/api/bookings', body,
    {'Content-Type': 'application/json', 'traceparent': f'00-{trace_id}-{parent_id}-01'}))
assert result['status'] in ('CONFIRMED', 'FAILED'), result['status']


def trace():
    data = json.loads(query('/api/traces/api/v1/traces/' + trace_id,
                           {'start': start, 'end': int(time.time())}))
    spans = data.get('spans', [])
    names = {s.get('process', {}).get('service_name') for s in spans}
    if not SERVICES <= names:
        return False
    expected = base64.b64encode(bytes.fromhex(trace_id)).decode()
    assert all(s['trace_id'] == expected for s in spans)
    ids = {s['span_id'] for s in spans}
    linked = {s.get('process', {}).get('service_name') for s in spans
              if any(r.get('span_id') in ids for r in s.get('references') or [])}
    assert SERVICES - {'booking-api'} <= linked
    return True


eventually(trace, 'one booking trace connects all four application services')


def metrics():
    data = json.loads(query('/api/metrics/api/v1/query',
        {'query': 'booking_requests_total', 'time': int(time.time())}, post=True))
    return data.get('status') == 'success' and any(
        float(row['value'][1]) > 0 for row in data.get('data', {}).get('result', []))


eventually(metrics, 'CubeAPM returns nonzero application booking counter')
for service in sorted(SERVICES):
    def logs(service=service):
        raw = query('/api/logs/select/logsql/query',
                    {'query': '{service.name="' + service + '"}',
                     'start': start, 'end': int(time.time()), 'limit': 1}, post=True)
        return any(json.loads(line).get('_msg') for line in raw.splitlines() if line.strip())
    eventually(logs, 'CubeAPM returns logs for ' + service)
print('PASS: CubeAPM trace, metric and log readback verified; trace=' + trace_id)
