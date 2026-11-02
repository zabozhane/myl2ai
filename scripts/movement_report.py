#!/usr/bin/env python3
"""Read-only movement audit of timestamped game output; no database or server writes.

Usage: docker compose logs --no-color --timestamps --since 1h game |
       python3 scripts/movement_report.py --json
Raw console files are also supported. Do not combine overlapping copies of a log.
"""
import argparse
from collections import Counter, defaultdict
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys

ISO = re.compile(r"(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)(?:\.(\d+))?Z")
CONSOLE = re.compile(r"\[(\d\d-\d\d-\d{4} \d\d:\d\d:\d\d)\]")
ACTOR = re.compile(r"(?:Autobots (?:route|movement|economy|session|water|state):|Offline farm(?: status)?:) (\S+) id=(\d+)")
DEST = re.compile(r"(?:unsafe leg to |can't reach |walks to a hunting spot at |to=)(-?\d+),(-?\d+)(?:,(-?\d+))?")
STATUS = re.compile(r"(?:Offline farm status|Autobots state):.*? exp (\d+).*? at (-?\d+),(-?\d+)")


def timestamp(line):
    match = ISO.search(line)
    if match:
        return datetime.fromisoformat(match[1]).replace(tzinfo=timezone.utc).timestamp() + float('0.' + (match[2] or '0'))
    match = CONSOLE.search(line)
    if match:
        # The Java console uses UTC in these containers, despite the host timezone.
        return datetime.strptime(match[1], '%d-%m-%Y %H:%M:%S').replace(tzinfo=timezone.utc).timestamp()
    return None


def event_kind(line):
    reason = re.search(r'failed reason=(\w+)', line)
    if reason:
        return 'route_' + reason[1]
    for needle, kind in (
        ('no non-excluded hunting spot', 'no_spot'),
        ('planned to=', 'planned'),
        ('route planned trip=', 'planned'),
        ('arrived to=', 'arrived'),
        ('refused unsafe leg', 'unsafe_leg'),
        ("can't reach ", 'unreachable'),
        ('exhausted reachable hunting spots', 'no_spot'),
        ('left an unproductive hunting anchor', 'empty_anchor'),
        ("can't afford the gatekeeper hop", 'unaffordable_ride'),
        ('heads to town for errands', 'town_trip'),
        ('walks to a hunting spot', 'hunt_trip'),
        ('reached the spot, hunting', 'arrived'),
    ):
        if needle in line:
            return kind
    return None


def audit(lines):
    actors, groups, counts, timelines = {}, {}, Counter(), defaultdict(list)
    times, missing_time = [], 0
    # A burst is repeated reports by one actor at one destination, at most 60s apart.
    # It is not proof of a single failed journey; old logs do not have journey IDs.
    bursts = {}
    statuses = defaultdict(list)
    for line in lines:
        at = timestamp(line)
        if at is not None:
            times.append(at)
        if 'Server build:' in line:
            # A restart separates observations; it must not look like continuous stalling.
            for rows in statuses.values():
                rows.append(None)
            bursts.clear()
        actor = ACTOR.search(line)
        if not actor:
            continue
        name, actor_id = actor.groups()
        actors[actor_id] = name
        if at is None:
            missing_time += 1
        kind = event_kind(line)
        if kind:
            counts[kind] += 1
            dest = DEST.search(line)
            xyz = tuple(int(n) if n else None for n in dest.groups()) if dest else None
            key = (kind, xyz)
            group = groups.setdefault(key, {'kind': kind, 'destination': xyz, 'lines': 0,
                                           'bursts': 0, 'actors': set(), 'first': at, 'last': at})
            group['lines'] += 1
            group['actors'].add(actor_id)
            if at is not None:
                group['first'] = min(group['first'] or at, at)
                group['last'] = max(group['last'] or at, at)
            burst_key = (actor_id, kind, xyz)
            previous = bursts.get(burst_key)
            if at is None or previous is None or at - previous > 60 or at < previous:
                group['bursts'] += 1
            if at is not None:
                bursts[burst_key] = at
                timelines[actor_id].append({'at': at, 'kind': kind, 'destination': xyz})
        match = STATUS.search(line)
        if match and at is not None:
            values = dict(re.findall(r'(\w+)=([^\s]+)', line))
            statuses[actor_id].append({'at': at, 'exp': int(match[1]),
                                      'xy': (int(match[2]), int(match[3])), **values})

    observations = []
    for actor_id, rows in statuses.items():
        run = []
        def flush():
            if len(run) < 2 or run[-1]['at'] - run[0]['at'] < 60:
                return
            observations.append({'actor': actor_id, 'name': actors[actor_id],
                                 'kind': 'stationary_travel' if run[0].get('traveling') == 'true' else 'idle_without_action',
                                 'seconds': round(run[-1]['at'] - run[0]['at']),
                                 'xy': run[0]['xy'], 'first': run[0]['at'], 'last': run[-1]['at']})
        for row in rows:
            if row is None:
                flush()
                run = []
                continue
            # Sitting, death, chores and party/quest ownership can explain an idle tick.
            candidate = row.get('dead') == 'false' and row.get('sitting') == 'false' and row.get('store') != 'true' and row.get('activity') not in ('Afk', 'Home', 'Trade') and (
                row.get('traveling') == 'true' or (
                    row.get('chores') == 'false' and row.get('life') == 'false'
                    and row.get('intention') == 'IDLE' and (row.get('autofarming') == 'false' or
                         row.get('activity') == 'Hunt' and row.get('sequence') == '-' and row.get('target') == '-')))
            same = not run or (row['xy'] == run[0]['xy'] and row['exp'] == run[0]['exp']
                               and row.get('traveling') == run[0].get('traveling')
                               and 0 <= row['at'] - run[-1]['at'] <= 90)
            if not candidate or not same:
                flush()
                run = []
            if candidate:
                run.append(row)
        flush()
    result_groups = []
    for group in groups.values():
        group = dict(group)
        group['actors'] = [{'id': i, 'name': actors[i]} for i in sorted(group['actors'])]
        result_groups.append(group)
    return {'coverage': {'first_utc': min(times) if times else None,
                         'last_utc': max(times) if times else None,
                         'actors_with_supported_logs': len(actors), 'actor_lines_without_time': missing_time},
            'counts': dict(counts),
            'groups': sorted(result_groups, key=lambda g: g['lines'], reverse=True),
            'idle_observations': sorted(observations, key=lambda g: g['seconds'], reverse=True),
            'timelines': dict(timelines),
            'limitations': ['Only logged actors are covered; population status is periodic on builds with movement-status.',
                            'Bursts are log repetition, not distinct trips. Idle observations require investigation.',
                            'Stops between snapshots, small loops and progressing detours cannot be classified reliably.']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('log', nargs='?', help='Log file, or stdin when omitted')
    parser.add_argument('--json', action='store_true', help='Include full groups and actor timelines')
    args = parser.parse_args()
    if args.log:
        with Path(args.log).open(errors='replace') as stream:
            result = audit(stream)
    else:
        result = audit(sys.stdin)
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return
    print('Logged actors:', result['coverage']['actors_with_supported_logs'])
    print('Events:', json.dumps(result['counts'], ensure_ascii=False))
    print('Most repeated destinations (lines / bursts / actors):')
    for group in result['groups'][:15]:
        names = ', '.join(a['name'] for a in group['actors'][:8])
        print(f"  {group['kind']} {group['destination']}: {group['lines']} / {group['bursts']} / {len(group['actors'])}; {names}")
    print('Stationary observations (not confirmed causes):')
    for row in result['idle_observations'][:15]:
        print(f"  {row['name']} id={row['actor']} {row['kind']} {row['seconds']}s at {row['xy']}")
    for note in result['limitations']:
        print('Coverage:', note)


if __name__ == '__main__':
    main()
