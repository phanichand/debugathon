#!/usr/bin/env python3
"""Check infrastructure readiness independently of customer incident behavior."""
import argparse
import json
import time
import urllib.request


def get(url):
    with urllib.request.urlopen(url, timeout=5) as response:
        return json.load(response)


def check_once():
    assert get('http://localhost:8300/healthz')['status'] == 'ok'
    assert get('http://localhost:8310/healthz')['database'] == 'ok'
    pods = set()
    for port in range(8301,8305):
        health = get(f'http://localhost:{port}/readyz')
        assert health['status'] == 'ready'
        assert health['dependency']['database'] == 'ok'
        pods.add(health['pod'])
    assert len(pods) == 4
    targets = get('http://localhost:9390/api/v1/targets')['data']['activeTargets']
    required = [target for target in targets if target['labels'].get('job') in ('availability', 'supplier')]
    assert len(required) == 5 and all(target['health']=='up' for target in required)
    assert get('http://localhost:3300/api/health')['database'] == 'ok'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--timeout', type=float, default=180)
    args = parser.parse_args()
    deadline = time.monotonic()+args.timeout
    while True:
        try:
            check_once()
            print('PASS: gateway, four replicas, supplier, database, five scrape targets and Grafana are healthy')
            break
        except Exception as exc:
            if time.monotonic() >= deadline:
                raise SystemExit(f'FAIL: environment not healthy: {type(exc).__name__}: {exc}')
            time.sleep(2)
