import asyncio
from collections import deque

from .session_index import SessionIndex
from .shared_session_repository import SharedSessionRepository


class ReplicationBuffer:
    def __init__(self) -> None:
        self._pending: deque[tuple[str, str]] = deque()

    def stage(self, session_id: str, nonce: str) -> None:
        self._pending.append((session_id, nonce))

    def drain(self) -> list[tuple[str, str]]:
        values = list(self._pending)
        self._pending.clear()
        return values


class SessionReplicator:
    def __init__(
        self,
        session_index: SessionIndex,
        shared_repository: SharedSessionRepository,
        buffer: ReplicationBuffer,
        flush_ms: int,
        hydration_interval_ms: int,
    ) -> None:
        self._session_index = session_index
        self._shared_repository = shared_repository
        self._buffer = buffer
        self._flush_seconds = flush_ms / 1000
        self._hydration_seconds = hydration_interval_ms / 1000
        self._tasks: list[asyncio.Task] = []

    async def start(self) -> None:
        self._tasks = [
            asyncio.create_task(self._flush_loop()),
            asyncio.create_task(self._hydrate_loop()),
        ]

    async def stop(self) -> None:
        for task in self._tasks:
            task.cancel()
        for task in self._tasks:
            try:
                await task
            except asyncio.CancelledError:
                pass

    async def _flush_loop(self) -> None:
        while True:
            await asyncio.sleep(self._flush_seconds)
            for session_id, nonce in self._buffer.drain():
                await self._shared_repository.save(session_id, nonce)

    async def _hydrate_loop(self) -> None:
        while True:
            await asyncio.sleep(self._hydration_seconds)
            for session_id, nonce in await self._shared_repository.recent():
                self._session_index.put(session_id, nonce)
