#!/usr/bin/env python3
"""Collect an isolated Java scenario result; exit codes alone are unreliable in aCis."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time


def docker(*args):
    return subprocess.check_output(['docker', *args], stderr=subprocess.STDOUT, text=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('container')
    parser.add_argument('marker')
    parser.add_argument('--timeout', type=float, default=180)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    info = json.loads(docker('inspect', args.container))[0]
    if ('L2_MOVEMENT_ISOLATED=yes' not in info['Config']['Env']
            or not info['Name'].startswith('/l2-')):
        parser.error('Only explicitly isolated l2-* scenario containers are allowed')
    container_id = info['Id']
    args.output.parent.mkdir(parents=True, exist_ok=True)
    started = time.monotonic()
    completed_at = None
    outcome = None
    reason = None
    last_report = 0
    while True:
        log = docker('logs', container_id)
        state = json.loads(docker('inspect', container_id))[0]['State']
        elapsed = time.monotonic() - started
        if outcome is None:
            if re.search(r'^java\.lang\.[\w$]*(?:Exception|Error)(?::|$)|Exception in thread "main"', log, re.M):
                outcome, reason = 'failed', 'scenario exception'
            elif args.marker in log:
                outcome, reason = 'passed', 'success marker'
            elif not state['Running']:
                outcome, reason = 'failed', 'container ended without success marker'
            elif elapsed >= args.timeout:
                outcome, reason = 'failed', 'scenario timeout'
            if outcome is not None:
                completed_at = time.monotonic()
                print(f'{outcome.upper()}: {reason}', flush=True)
        if not state['Running']:
            break
        if outcome is not None and time.monotonic() - completed_at >= 5:
            # A scenario already reached its result; do not wait on a hung shutdown hook.
            docker('stop', '--time', '5', container_id)
            log = docker('logs', container_id)
            break
        if outcome is None and elapsed - last_report >= 20:
            last_report = elapsed
            progress = [line for line in log.splitlines()
                        if 'MOVEMENT PASS:' in line or ' INFO ' in line]
            print(f'RUNNING {elapsed:.0f}s: {progress[-1] if progress else "server startup"}', flush=True)
        time.sleep(1)
    args.output.with_suffix('.log').write_text(log)
    result = dict(outcome=outcome, reason=reason, marker=args.marker, container=container_id,
                  image=info['Image'], elapsed_seconds=round(time.monotonic() - started, 2),
                  exit_code=json.loads(docker('inspect', container_id))[0]['State']['ExitCode'])
    args.output.with_suffix('.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result), flush=True)
    return 0 if outcome == 'passed' else 1


if __name__ == '__main__':
    raise SystemExit(main())
