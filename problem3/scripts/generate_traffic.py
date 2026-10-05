#!/usr/bin/env python3
"""Exercise customer availability lookups; retain first attempts and retries separately."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import random
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


def request(base, route, request_id):
    started = time.monotonic()
    req = urllib.request.Request(base.rstrip('/') + '/availability?' + urllib.parse.urlencode({'route': route}),
                                 headers={'X-Request-ID': request_id})
    try:
        response = urllib.request.urlopen(req, timeout=6)
    except urllib.error.HTTPError as exc:
        response = exc
    except (OSError, TimeoutError) as exc:
        return {'status': 0, 'replica': None, 'error': str(exc), 'duration_ms': round((time.monotonic()-started)*1000, 2)}
    with response:
        response.read()
        return {'status': response.status, 'replica': response.headers.get('X-Replica'),
                'revision': response.headers.get('X-Revision'),
                'duration_ms': round((time.monotonic()-started)*1000, 2)}


def run(base, count, rps, concurrency, seed, output):
    rng = random.Random(seed)
    run_id = uuid.uuid4().hex[:12]
    events = []
    started = time.monotonic()
    def customer(index, route):
        request_id = f'{run_id}-{index}'
        initial = request(base, route, request_id)
        event = {'request_id': request_id, 'route': route, 'initial': initial,
                 'elapsed_seconds': round(time.monotonic()-started, 3)}
        if initial['status'] >= 500:
            time.sleep(random.Random(seed + index).uniform(.05, .45))
            event['retry'] = request(base, route, request_id + '-retry')
            print(json.dumps(event), flush=True)
        return event
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = []
        for index in range(count):
            time.sleep(max(0, started + index/rps - time.monotonic()))
            futures.append(pool.submit(customer, index, f'route-{rng.randrange(100000)}'))
        events = [future.result() for future in futures]
    failures = [e for e in events if e['initial']['status'] >= 500]
    summary = {'requests': count, 'initial_failures': len(failures),
               'initial_success_rate': sum(e['initial']['status']==200 for e in events)/count,
               'transport_errors': sum(e['initial']['status']==0 for e in events),
               'unexpected_statuses': sum(e['initial']['status'] not in (200,504) for e in events),
               'retry_recovered': sum(e.get('retry',{}).get('status')==200 for e in failures),
               'first_error_seconds': min((e['elapsed_seconds'] for e in failures), default=None),
               'duration_seconds': round(time.monotonic()-started, 2)}
    payload = {'summary': summary, 'events': events}
    Path(output).parent.mkdir(parents=True, exist_ok=True)
    Path(output).write_text(json.dumps(payload, indent=2)+'\n')
    print(json.dumps({'summary': summary}), flush=True)
    return payload


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://localhost:8300')
    parser.add_argument('--count', type=int, default=900)
    parser.add_argument('--rps', type=float, default=10)
    parser.add_argument('--concurrency', type=int, default=16)
    parser.add_argument('--seed', type=int, default=37)
    parser.add_argument('--output', default='artifacts/traffic.json')
    args = parser.parse_args()
    if args.count <= 0 or args.rps <= 0 or args.concurrency <= 0:
        parser.error('count, rps and concurrency must be positive')
    run(args.base_url, args.count, args.rps, args.concurrency, args.seed, args.output)
