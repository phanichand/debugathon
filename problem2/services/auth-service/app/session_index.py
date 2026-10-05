from threading import RLock


class SessionIndex:
    def __init__(self) -> None:
        self._values: dict[str, str] = {}
        self._lock = RLock()

    def put(self, session_id: str, nonce: str) -> None:
        with self._lock:
            self._values[session_id] = nonce

    def matches(self, session_id: str, nonce: str) -> bool:
        with self._lock:
            return self._values.get(session_id) == nonce

    def size(self) -> int:
        with self._lock:
            return len(self._values)
