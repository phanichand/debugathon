from app.replication import ReplicationBuffer
from app.session_index import SessionIndex
from app.session_validation import SessionRegistry, SessionValidator


class AlwaysRegistryPolicy:
    def requires_registry(self, session_id: str) -> bool:
        return True


def test_registered_session_is_valid_in_same_index():
    index = SessionIndex()
    buffer = ReplicationBuffer()
    policy = AlwaysRegistryPolicy()
    registry = SessionRegistry(index, buffer, policy)
    validator = SessionValidator(index, policy)

    registry.register("session-1", "nonce-1")

    assert validator.valid("session-1", "nonce-1")


def test_wrong_nonce_is_rejected():
    index = SessionIndex()
    buffer = ReplicationBuffer()
    policy = AlwaysRegistryPolicy()
    registry = SessionRegistry(index, buffer, policy)
    validator = SessionValidator(index, policy)

    registry.register("session-1", "nonce-1")

    assert not validator.valid("session-1", "nonce-2")
