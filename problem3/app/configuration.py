"""Runtime configuration snapshots and source attribution."""
import hashlib
import json
import os
from pathlib import Path

ROOT = Path(os.getenv('CONFIG_ROOT', '/srv/config'))


def read_json(path):
    return json.loads(Path(path).read_text())


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()[:16]


def baseline(root=ROOT, environ=None):
    environ = os.environ if environ is None else environ
    values = read_json(root / 'application.json')
    sources = {key: {'source': 'application.json', 'value': value} for key, value in values.items()}
    for key in values:
        if key in environ:
            values[key] = int(environ[key])
            sources[key] = {'source': 'environment', 'key': key, 'value': values[key]}
    return values, sources


def resolve(values, sources, bundle, labels):
    values, sources = dict(values), dict(sources)
    for rule in sorted(bundle['rules'], key=lambda rule: rule['priority']):
        if all(labels.get(key) == value for key, value in rule['selector'].items()):
            for key, value in rule['settings'].items():
                if key not in values or not isinstance(value, int) or value <= 0:
                    raise ValueError(f'Invalid configuration setting: {key}')
                values[key] = value
                sources[key] = {'source': 'runtime-policy', 'rule': rule['id'],
                                'bundle': bundle['revision'], 'selector': rule['selector'],
                                'origin': rule['origin'], 'value': value}
    return values, sources
