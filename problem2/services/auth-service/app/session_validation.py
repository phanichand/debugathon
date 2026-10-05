import hashlib

from .replication import ReplicationBuffer
from .session_index import SessionIndex


class RegistryPolicy:
    def __init__(self, rollout_percent: int) -> None:
        self._threshold = max(0, min(100, rollout_percent))

    def requires_registry(self, session_id: str) -> bool:
        digest = hashlib.sha256(session_id.encode("utf-8")).digest()
        bucket = int.from_bytes(digest[:4], "big") % 100
        return bucket < self._threshold


class SessionRegistry:
    def __init__(
        self,
        session_index: SessionIndex,
        replication_buffer: ReplicationBuffer,
        policy: RegistryPolicy,
    ) -> None:
        self._session_index = session_index
        self._replication_buffer = replication_buffer
        self._policy = policy

    def register(self, session_id: str, nonce: str) -> None:
        if not self._policy.requires_registry(session_id):
            return
        self._session_index.put(session_id, nonce)
        self._replication_buffer.stage(session_id, nonce)


class SessionValidator:
    def __init__(self, session_index: SessionIndex, policy: RegistryPolicy) -> None:
        self._session_index = session_index
        self._policy = policy

    def valid(self, session_id: str, nonce: str) -> bool:
        if not self._policy.requires_registry(session_id):
            return True
        return self._session_index.matches(session_id, nonce)
