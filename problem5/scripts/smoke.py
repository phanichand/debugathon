import time
from client import ready, request


def main():
    ready()
    path = "/prices/SMOKE-001"
    status, current, *_ = request(path)
    assert status == 200, current
    for amount in (820, 950, 975):
        status, updated, *_ = request(path, "PUT", {"amount": amount, "expectedVersion": current["version"]})
        assert status == 200, updated
        assert updated["version"] == current["version"] + 1
        assert updated["amount"] == amount
        for _ in range(5):
            status, read, *_ = request(path)
            assert status == 200 and read == updated, read
        status, _, *_ = request(path, "PUT", {"amount": 500, "expectedVersion": current["version"]})
        assert status == 409, status
        current = updated
    for invalid in (0, -1, 1.234, 1000001):
        assert request(path, "PUT", {"amount": invalid, "expectedVersion": current["version"]})[0] == 400
    assert request("/prices/does-not-exist")[0] == 404
    # Exercise reads beyond both configured cache lifetimes.
    time.sleep(9)
    status, after_expiry, *_ = request(path)
    assert status == 200 and after_expiry == current
    print("PASS: readiness, sequential reads/updates, version conflicts, validation, cache expiry")


if __name__ == "__main__":
    main()
