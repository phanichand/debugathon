#!/usr/bin/env python3
"""Validate the published incident symptoms, without prescribing a diagnosis."""
import json
import sys

payload = json.load(open(sys.argv[1]))
summary, events = payload['summary'], payload['events']
assert summary['requests'] >= 600, summary
assert summary['transport_errors'] == 0, summary
assert summary['unexpected_statuses'] == 0, summary
assert summary['first_error_seconds'] is not None and summary['first_error_seconds'] <= 120, summary
# Discard the first 35 seconds of startup observations for the steady-state rate.
steady = [event for event in events if event['elapsed_seconds'] >= 35]
assert len(steady) >= 300, len(steady)
failed = [event for event in steady if event['initial']['status']==504]
rate = len(failed)/len(steady)
assert .02 <= rate <= .09, f'Unexpected steady-state failure rate: {rate}'
replicas = {event['initial']['replica'] for event in steady}
assert len(replicas) == 4 and None not in replicas, replicas
counts = {pod: sum(event['initial']['replica']==pod for event in failed) for pod in replicas}
assert len(failed) >= 10 and max(counts.values())/len(failed) >= .90, counts
recovered = sum(event.get('retry',{}).get('status')==200 for event in failed)
assert recovered/len(failed) >= .60, (recovered, len(failed))
print(f'PASS: incident visible, steady-state failures={rate:.2%}, retry recovery={recovered/len(failed):.2%}')
