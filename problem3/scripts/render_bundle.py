#!/usr/bin/env python3
"""Compile a release manifest and its regional policy overlays."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def compile_release(manifest, root=ROOT):
    release = json.loads(Path(manifest).read_text())
    rules = []
    for binding in release['bindings']:
        policy = json.loads((root / binding['policy']).read_text())
        overlay = binding.get('overlay', {})
        rule = {**policy, **overlay}
        rule['origin'] = {'policy': binding['policy'], 'release': release['revision'],
                          'overlay': overlay}
        rules.append(rule)
    return {'revision': release['revision'], 'rules': rules}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('manifest')
    parser.add_argument('output')
    args = parser.parse_args()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix('.tmp')
    temporary.write_text(json.dumps(compile_release(args.manifest), indent=2) + '\n')
    temporary.replace(output)
