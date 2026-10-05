import json
import httpx
from fastapi.testclient import TestClient
from app import availability


class Supplier:
    def __init__(self, outcome):
        self.outcome = outcome
        self.timeout = None
    async def get(self, url, **kwargs):
        self.timeout = kwargs.get('timeout')
        if self.outcome == 'timeout':
            raise httpx.ReadTimeout('deadline')
        if self.outcome == 'error':
            raise httpx.ConnectError('connection')
        return httpx.Response(200, json={'seats': 42}, request=httpx.Request('GET',url))
    async def aclose(self):
        pass


def setup(monkeypatch):
    monkeypatch.setattr(availability, 'baseline', lambda: ({'DOWNSTREAM_TIMEOUT_MS': 1234}, {}))


def test_request_uses_snapshot_deadline_and_returns_replica_evidence(monkeypatch):
    setup(monkeypatch)
    with TestClient(availability.app) as client:
        supplier = Supplier('ok')
        availability.app.state.client = supplier
        result = client.get('/availability', headers={'X-Request-ID':'case-1'})
        assert result.status_code == 200 and result.json()['seats']==42
        assert supplier.timeout == 1.234
        assert result.headers['X-Request-ID'] == 'case-1'
        assert result.headers['X-Replica']


def test_timeout_is_504_not_readiness_failure(monkeypatch):
    setup(monkeypatch)
    with TestClient(availability.app) as client:
        availability.app.state.client = Supplier('timeout')
        assert client.get('/availability').status_code == 504
        assert client.get('/healthz').status_code == 200


def test_transport_failure_is_distinct(monkeypatch):
    setup(monkeypatch)
    with TestClient(availability.app) as client:
        availability.app.state.client = Supplier('error')
        assert client.get('/availability').status_code == 502
        assert client.get('/readyz').status_code == 503


def test_refresh_replaces_snapshot_and_retains_last_valid_on_error(monkeypatch, tmp_path):
    setup(monkeypatch)
    bundle = tmp_path/'bundle.json'
    rule = {'id':'test','priority':1,'selector':{},'settings':{'DOWNSTREAM_TIMEOUT_MS':765},'origin':{'policy':'test'}}
    bundle.write_text(json.dumps({'revision':'test','rules':[rule]}))
    monkeypatch.setattr(availability, 'BUNDLE', str(bundle))
    monkeypatch.setenv('POLICY_REFRESH_SECONDS', '.01')
    import time
    with TestClient(availability.app) as client:
        deadline = time.monotonic()+2
        while client.get('/ops/runtime').json()['configuration']['bundle_revision'] is None:
            assert time.monotonic()<deadline
            time.sleep(.02)
        snapshot = client.get('/ops/runtime').json()['configuration']
        assert snapshot['values']['DOWNSTREAM_TIMEOUT_MS']==765
        assert snapshot['provenance']['DOWNSTREAM_TIMEOUT_MS']['origin']=={'policy':'test'}
        bundle.write_text('invalid')
        time.sleep(.04)
        assert client.get('/ops/runtime').json()['configuration']==snapshot
