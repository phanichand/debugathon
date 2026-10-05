"""Browse and catalogue-update workload; writes only through the public pricing API."""
import argparse
import concurrent.futures as cf
import datetime as dt
import json
import os
from pathlib import Path
import random
import threading
import time

from client import ready, request


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seconds", type=int, default=int(os.environ.get("DURATION", "180")))
    parser.add_argument("--batch", type=int, default=24)
    parser.add_argument("--start-sku", type=int, default=1)
    parser.add_argument("--seed", type=int, default=71)
    parser.add_argument("--output", default="artifacts/traffic.jsonl")
    parser.add_argument("--require-signal", action="store_true",
                        help="Return nonzero unless a complete reported customer sequence is observed")
    args = parser.parse_args()
    if args.seconds < 1 or not 1 <= args.batch <= 64 or not 1 <= args.start_sku <= 20000:
        parser.error("seconds must be positive, batch 1..64, start-sku 1..20000")
    ready()
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    lock = threading.Lock()
    stats = {"products": 0, "sequences": 0, "post_update_older": 0, "errors": 0}
    started = time.monotonic()
    deadline = started + args.seconds
    rng = random.Random(args.seed)
    sku = args.start_sku

    with open(args.output, "w") as output, cf.ThreadPoolExecutor(max_workers=args.batch) as flows, \
            cf.ThreadPoolExecutor(max_workers=args.batch) as browsers:
        def emit(record):
            with lock:
                output.write(json.dumps({"at": dt.datetime.now(dt.timezone.utc).isoformat(), **record}) + "\n")
                output.flush()

        def call(product, operation, method="GET", body=None):
            try:
                status, value, rid, begin, end = request("/prices/" + product, method, body)
                emit({"productId": product, "operation": operation, "requestId": rid,
                      "status": status, "startNanos": begin, "endNanos": end, "response": value})
                if status != 200:
                    raise RuntimeError(f"{operation}: HTTP {status}: {value}")
                return value
            except Exception:
                with lock:
                    stats["errors"] += 1
                raise

        def exercise(product, initial, delay):
            old_read = browsers.submit(call, product, "browse")
            time.sleep(delay)
            updated = call(product, "catalogue-update", "PUT",
                           {"amount": 950, "expectedVersion": initial["version"]})
            observed_new = observed_old = complete = False
            until = time.monotonic() + 9
            while time.monotonic() < until:
                value = call(product, "customer")
                if value["version"] == updated["version"]:
                    if observed_old:
                        complete = True
                        break
                    observed_new = True
                elif value["version"] < updated["version"]:
                    with lock:
                        stats["post_update_older"] += 1
                    if observed_new:
                        observed_old = True
                time.sleep(0.08)
            old_read.result(timeout=30)
            with lock:
                stats["products"] += 1
                stats["sequences"] += int(complete)
            if complete:
                print(f"{product}: ₹820 → update acknowledged → ₹950 → ₹820 → ₹950 "
                      f"({time.monotonic() - started:.1f}s)", flush=True)

        while time.monotonic() < deadline and sku <= 20000:
            ids = [f"SKU-{i:05d}" for i in range(sku, min(sku + args.batch, 20001))]
            sku += len(ids)
            warmed = list(flows.map(lambda p: (p, call(p, "browse-initial")), ids))
            selected = [(p, v) for p, v in warmed if v["version"] == 1 and v["amount"] == 820]
            if not selected:
                continue
            # A customer think interval between browsing and catalogue activity.
            time.sleep(9)
            futures = [flows.submit(exercise, p, value, rng.uniform(0.02, 1.0)) for p, value in selected]
            for future in futures:
                future.result()
            print(json.dumps({"elapsedSeconds": round(time.monotonic() - started, 1), **stats}), flush=True)
        stats["elapsedSeconds"] = round(time.monotonic() - started, 1)
        emit({"summary": stats})
        print("SUMMARY " + json.dumps(stats), flush=True)
    if stats["products"] == 0:
        raise SystemExit("No untouched products found; use a fresh --start-sku range or reset this exercise")
    if stats["errors"] or (args.require_signal and not stats["sequences"]):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
