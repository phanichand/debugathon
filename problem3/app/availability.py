import asyncio
from contextlib import asynccontextmanager, suppress
import os
import time
import uuid

import httpx
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, Response
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST

from app.configuration import baseline, digest, read_json, resolve
from app.telemetry import log

POD = os.getenv('POD_NAME', 'availability-local')
REVISION = os.getenv('DEPLOYMENT_REVISION', 'r41')
LABELS = {'workload': os.getenv('WORKLOAD', 'availability'), 'region': os.getenv('REGION', 'ap-south')}
SUPPLIER = os.getenv('SUPPLIER_URL', 'http://supplier:8000')
BUNDLE = os.getenv('POLICY_BUNDLE', '/runtime/bundle.json')
REQUESTS = Counter('availability_requests_total', 'Availability responses', ['pod', 'revision', 'status'])
LATENCY = Histogram('availability_duration_seconds', 'Availability latency', ['pod'], buckets=(.1, .25, .5, 1, 2, 5))


async def refresh(app):
    while True:
        await asyncio.sleep(float(os.getenv('POLICY_REFRESH_SECONDS', '30')))
        try:
            bundle = read_json(BUNDLE)
            values, sources = resolve(*app.state.base, bundle, LABELS)
            snapshot = {'values': values, 'provenance': sources, 'bundle_revision': bundle['revision'],
                        'fingerprint': digest(values), 'loaded_at': time.time()}
            previous = app.state.snapshot
            app.state.snapshot = snapshot
            if previous['fingerprint'] != snapshot['fingerprint'] or previous['bundle_revision'] != bundle['revision']:
                log('configuration_snapshot_loaded', pod=POD, revision=REVISION,
                    bundle_revision=bundle['revision'], fingerprint=snapshot['fingerprint'])
        except Exception as exc:
            log('configuration_refresh_failed', pod=POD, revision=REVISION, error=type(exc).__name__)


@asynccontextmanager
async def lifespan(app):
    app.state.base = baseline()
    values, sources = app.state.base
    app.state.snapshot = {'values': values, 'provenance': sources, 'bundle_revision': None,
                          'fingerprint': digest(values), 'loaded_at': time.time()}
    app.state.started = time.time()
    app.state.client = httpx.AsyncClient(timeout=3, trust_env=False, limits=httpx.Limits(max_connections=100))
    task = asyncio.create_task(refresh(app))
    log('service_started', pod=POD, revision=REVISION, application_version='1.0.0')
    yield
    task.cancel()
    with suppress(asyncio.CancelledError):
        await task
    await app.state.client.aclose()


app = FastAPI(title='Availability Service', lifespan=lifespan)


@app.get('/healthz')
async def health():
    return {'status': 'ok', 'pod': POD, 'revision': REVISION, 'application_version': '1.0.0'}


@app.get('/readyz')
async def ready():
    try:
        result = await app.state.client.get(SUPPLIER + '/healthz')
        result.raise_for_status()
        return {'status': 'ready', 'pod': POD, 'dependency': result.json()}
    except httpx.HTTPError:
        return JSONResponse({'status': 'not-ready', 'pod': POD}, status_code=503)


@app.get('/ops/runtime')
async def runtime():
    return {'pod': POD, 'revision': REVISION, 'labels': LABELS, 'application_version': '1.0.0',
            'uptime_seconds': round(time.time() - app.state.started, 2),
            'configuration': app.state.snapshot}


@app.get('/metrics')
async def metrics():
    return Response(generate_latest(), headers={'Content-Type': CONTENT_TYPE_LATEST})


@app.get('/availability')
async def availability(request: Request, route: str = 'BLR-MAA'):
    request_id = request.headers.get('X-Request-ID', str(uuid.uuid4()))[:128]
    snapshot = app.state.snapshot
    started = time.monotonic()
    status, error = 200, None
    try:
        response = await app.state.client.get(SUPPLIER + '/inventory', params={'route': route},
            headers={'X-Request-ID': request_id, 'X-Caller-Pod': POD},
            timeout=snapshot['values']['DOWNSTREAM_TIMEOUT_MS'] / 1000)
        response.raise_for_status()
        body = response.json()
    except httpx.TimeoutException:
        status, error = 504, 'supplier_deadline_exceeded'
        body = {'error': 'availability_temporarily_unavailable', 'request_id': request_id}
    except httpx.HTTPError:
        status, error = 502, 'supplier_transport_error'
        body = {'error': 'availability_temporarily_unavailable', 'request_id': request_id}
    elapsed = time.monotonic() - started
    REQUESTS.labels(POD, REVISION, str(status)).inc()
    LATENCY.labels(POD).observe(elapsed)
    log('availability_completed', request_id=request_id, pod=POD, revision=REVISION,
        route=route, status=status, error=error, duration_ms=round(elapsed * 1000, 2),
        config_fingerprint=snapshot['fingerprint'])
    return JSONResponse(body, status_code=status, headers={'X-Replica': POD,
        'X-Revision': REVISION, 'X-Request-ID': request_id})
