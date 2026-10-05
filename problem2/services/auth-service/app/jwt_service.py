from datetime import datetime, timedelta, timezone

import jwt


class JwtService:
    def __init__(self, secret: str, ttl_seconds: int) -> None:
        self._secret = secret
        self._ttl_seconds = ttl_seconds

    def issue(self, username: str, session_id: str, nonce: str) -> str:
        now = datetime.now(timezone.utc)
        payload = {
            "sub": username,
            "sid": session_id,
            "nonce": nonce,
            "iat": now,
            "exp": now + timedelta(seconds=self._ttl_seconds),
        }
        return jwt.encode(payload, self._secret, algorithm="HS256")

    def decode(self, token: str) -> dict:
        return jwt.decode(token, self._secret, algorithms=["HS256"])
