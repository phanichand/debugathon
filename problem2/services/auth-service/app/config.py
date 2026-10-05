import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    pod_name: str
    redis_url: str
    jwt_secret: str
    jwt_ttl_seconds: int
    registry_rollout_percent: int
    replication_flush_ms: int
    hydration_interval_ms: int
    session_ttl_seconds: int

    @classmethod
    def from_env(cls) -> "Settings":
        return cls(
            pod_name=os.getenv("POD_NAME", "auth-local"),
            redis_url=os.getenv("REDIS_URL", "redis://localhost:6379/0"),
            jwt_secret=os.getenv("JWT_SECRET", "debugathon-local-secret"),
            jwt_ttl_seconds=int(os.getenv("JWT_TTL_SECONDS", "900")),
            registry_rollout_percent=int(os.getenv("REGISTRY_ROLLOUT_PERCENT", "12")),
            replication_flush_ms=int(os.getenv("REPLICATION_FLUSH_MS", "800")),
            hydration_interval_ms=int(os.getenv("HYDRATION_INTERVAL_MS", "200")),
            session_ttl_seconds=int(os.getenv("SESSION_TTL_SECONDS", "300")),
        )
