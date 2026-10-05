import asyncio
from contextlib import asynccontextmanager
import hashlib
import os
import time

import psycopg
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, Response
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from app.telemetry import log

DSN = os.getenv('DATABASE_URL', 'postgresql://catalog:catalog@db:5432/catalog')
REQUESTS = Counter('supplier_requests_total', 'Supplier responses', ['status'])
LATENCY = Histogram('supplier_duration_seconds', 'Supplier response time', buckets=(.1, .25, .5, 1, 2))
DB_LATENCY = Histogram('catalog_query_duration_seconds', 'Catalog query latency')


def route_latency_ms(route):
    return 600 if int.from_bytes(hashlib.sha256(route.encode()).digest()[:4], 'big') % 5 == 0 else 60


@asynccontextmanager
async def lifespan(app):
    connection = await psycopg.AsyncConnection.connect(DSN, autocommit=True)
    async with connection.cursor() as cursor:
        await cursor.execute('CREATE TABLE IF NOT EXISTS inventory (route TEXT PRIMARY KEY, seats INTEGER NOT NULL)')
        await cursor.execute("INSERT INTO inventory VALUES ('BLR-MAA',42) ON CONFLICT DO NOTHING")
    await connection.close()
    yield


app = FastAPI(title='Supplier Inventory', lifespan=lifespan)


async def query():
    started = time.monotonic()
    async with await psycopg.AsyncConnection.connect(DSN) as connection:
        async with connection.cursor() as cursor:
            await cursor.execute('SELECT seats FROM inventory WHERE route=%s', ('BLR-MAA',))
            result = await cursor.fetchone()
    DB_LATENCY.observe(time.monotonic() - started)
    return result[0]


@app.get('/healthz')
async def health():
    try:
        await query()
        return {'status': 'ok', 'database': 'ok'}
    except psycopg.Error:
        return JSONResponse({'status': 'unavailable', 'database': 'unavailable'}, status_code=503)


@app.get('/metrics')
async def metrics():
    return Response(generate_latest(), headers={'Content-Type': CONTENT_TYPE_LATEST})


@app.get('/inventory')
async def inventory(request: Request, route: str = 'BLR-MAA'):
    started = time.monotonic()
    try:
        seats = await query()
    except psycopg.Error:
        REQUESTS.labels('503').inc()
        return JSONResponse({'error': 'catalog_unavailable'}, status_code=503)
    await asyncio.sleep(route_latency_ms(route) / 1000)
    elapsed = time.monotonic() - started
    REQUESTS.labels('200').inc()
    LATENCY.observe(elapsed)
    log('inventory_completed', request_id=request.headers.get('X-Request-ID'),
        caller_pod=request.headers.get('X-Caller-Pod'), route=route,
        status=200, duration_ms=round(elapsed * 1000, 2))
    return {'route': route, 'seats': seats, 'currency': 'INR'}
