from redis.asyncio import Redis


class SharedSessionRepository:
    def __init__(self, redis_client: Redis, ttl_seconds: int) -> None:
        self._redis = redis_client
        self._ttl_seconds = ttl_seconds

    async def save(self, session_id: str, nonce: str) -> None:
        await self._redis.set(
            f"session:{session_id}",
            nonce,
            ex=self._ttl_seconds,
        )

    async def recent(self) -> list[tuple[str, str]]:
        values: list[tuple[str, str]] = []
        async for key in self._redis.scan_iter(match="session:*", count=200):
            nonce = await self._redis.get(key)
            if nonce is None:
                continue
            key_text = key.decode() if isinstance(key, bytes) else key
            nonce_text = nonce.decode() if isinstance(nonce, bytes) else nonce
            values.append((key_text.removeprefix("session:"), nonce_text))
        return values

    async def ping(self) -> bool:
        return bool(await self._redis.ping())
