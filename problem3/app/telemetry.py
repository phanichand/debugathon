import json
from datetime import datetime, timezone


def log(event, **fields):
    print(json.dumps({'timestamp': datetime.now(timezone.utc).isoformat(),
                      'event': event, **fields}), flush=True)
