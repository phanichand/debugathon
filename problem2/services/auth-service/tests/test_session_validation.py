from app.local_session_store import LocalSessionStore
from app.replication import ReplicationBuffer
from app.session_validation import SessionRegistry, SessionValidator


class AlwaysRegistryPolicy:
    def requires_registry(self, session_id: str) -> bool:
        return True


def test_registered_session_is_valid_on_same_replica():
    store = LocalSessionStore()
    buffer = ReplicationBuffer()
    policy = AlwaysRegistryPolicy()
    registry = SessionRegistry(store, buffer, policy)
    validator = SessionValidator(store, policy)

    registry.register("session-1", "nonce-1")

    assert validator.valid("session-1", "nonce-1")


def test_wrong_nonce_is_rejected():
    store = LocalSessionStore()
    buffer = ReplicationBuffer()
    policy = AlwaysRegistryPolicy()
    registry = SessionRegistry(store, buffer, policy)
    validator = SessionValidator(store, policy)

    registry.register("session-1", "nonce-1")

    assert not validator.valid("session-1", "nonce-2")
