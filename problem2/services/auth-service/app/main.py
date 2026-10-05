import json
import logging
import secrets
import uuid
from contextlib import asynccontextmanager

import jwt
from fastapi import FastAPI, Header, HTTPException, Request, Response
from prometheus_client import make_asgi_app
from redis.asyncio import Redis

from .config import Settings
from .jwt_service import JwtService
from .local_session_store import LocalSessionStore
from .metrics import LOGIN_TOTAL, LOCAL_REGISTRY_SIZE, VALIDATION_TOTAL
from .models import LoginRequest, LoginResponse, UserResponse
from .replication import ReplicationBuffer, SessionReplicator
from .session_validation import RegistryPolicy, SessionRegistry, SessionValidator
from .shared_session_repository import SharedSessionRepository

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger("auth-service")

settings = Settings.from_env()
local_store = LocalSessionStore()
replication_buffer = ReplicationBuffer()
policy = RegistryPolicy(settings.registry_rollout_percent)
registry = SessionRegistry(local_store, replication_buffer, policy)
validator = SessionValidator(local_store, policy)
jwt_service = JwtService(settings.jwt_secret, settings.jwt_ttl_seconds)
redis_client = Redis.from_url(settings.redis_url)
shared_repository = SharedSessionRepository(redis_client, settings.session_ttl_seconds)
replicator = SessionReplicator(
    local_store,
    shared_repository,
    replication_buffer,
    settings.replication_flush_ms,
    settings.hydration_interval_ms,
)


def log_event(event: str, **fields: object) -> None:
    logger.info(json.dumps({"event": event, "pod": settings.pod_name, **fields}))


@asynccontextmanager
async def lifespan(_: FastAPI):
    await replicator.start()
    try:
        yield
    finally:
        await replicator.stop()
        await redis_client.aclose()


app = FastAPI(title="Debugathon Auth Service", lifespan=lifespan)
app.mount("/metrics", make_asgi_app())


@app.middleware("http")
async def add_pod_header(request: Request, call_next):
    response: Response = await call_next(request)
    response.headers["X-Auth-Pod"] = settings.pod_name
    LOCAL_REGISTRY_SIZE.labels(settings.pod_name).set(local_store.size())
    return response


@app.get("/healthz")
async def healthz() -> dict:
    try:
        redis_ok = await shared_repository.ping()
    except Exception:
        redis_ok = False
    if not redis_ok:
        raise HTTPException(status_code=503, detail="dependency unavailable")
    return {"status": "UP", "pod": settings.pod_name}


@app.post("/login", response_model=LoginResponse)
async def login(request: LoginRequest) -> LoginResponse:
    session_id = str(uuid.uuid4())
    nonce = secrets.token_urlsafe(18)
    registry.register(session_id, nonce)
    token = jwt_service.issue(request.username, session_id, nonce)
    LOGIN_TOTAL.labels(settings.pod_name).inc()
    log_event("login_succeeded", username=request.username, session_id=session_id)
    return LoginResponse(access_token=token)


@app.get("/me", response_model=UserResponse)
async def me(authorization: str = Header(default="")) -> UserResponse:
    if not authorization.startswith("Bearer "):
        VALIDATION_TOTAL.labels(settings.pod_name, "missing_token").inc()
        raise HTTPException(status_code=401, detail="invalid session")

    token = authorization.removeprefix("Bearer ").strip()

    try:
        claims = jwt_service.decode(token)
    except jwt.PyJWTError:
        VALIDATION_TOTAL.labels(settings.pod_name, "jwt_rejected").inc()
        raise HTTPException(status_code=401, detail="invalid session")

    session_id = str(claims.get("sid", ""))
    nonce = str(claims.get("nonce", ""))
    username = str(claims.get("sub", ""))

    if not session_id or not nonce or not username or not validator.valid(session_id, nonce):
        VALIDATION_TOTAL.labels(settings.pod_name, "invalid_session").inc()
        log_event("session_validation_failed", session_id=session_id)
        raise HTTPException(status_code=401, detail="invalid session")

    VALIDATION_TOTAL.labels(settings.pod_name, "success").inc()
    log_event("session_validation_succeeded", username=username, session_id=session_id)
    return UserResponse(username=username, session_id=session_id)
