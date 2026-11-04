#!/usr/bin/env python3
"""Hunting zones of the server by monster level, for docs/leveling-guides.md.

For every named gatekeeper destination (teleporter HTML + teleportLocations.xml) it counts the
monster spawns of spawnlist.sql within RADIUS on the same floor and prints the level spread
(p10-p90, median), the share of aggressive monsters and the most common monsters.

Usage: python3 scripts/leveling_zones.py <aCis_datapack dir> [radius]
The datapack is the one of the pinned ACIS_COMMIT (.env).
"""
import glob
import math
import re
import sys


def load(datapack):
    names = {}
    for path in glob.glob(datapack + '/data/html/teleporter/*.htm'):
        text = open(path, encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'_goto (\d+)"\s*msg="811;([^"]*)"', text):
            names.setdefault(int(m.group(1)), m.group(2))
    teleports = []
    for m in re.finditer(r'<teleport id="(\d+)" x="(-?\d+)" y="(-?\d+)" z="(-?\d+)" price="(\d+)" isNoble="(\w+)"',
                         open(datapack + '/data/xml/teleportLocations.xml').read()):
        tid = int(m.group(1))
        if names.get(tid) and m.group(6) == 'false' and int(m.group(5)) > 0:
            teleports.append((tid, names[tid], int(m.group(2)), int(m.group(3)), int(m.group(4))))
    npcs = {}
    for path in glob.glob(datapack + '/data/xml/npcs/*.xml'):
        for block in re.findall(r'<npc id="\d+".*?</npc>', open(path, encoding='utf-8', errors='ignore').read(), re.S):
            kind = re.search(r'name="type" val="(\w+)"', block)
            if not kind or kind.group(1) != 'Monster':
                continue
            level = re.search(r'name="level" val="(\d+)"', block)
            aggro = re.search(r' aggro="(\d+)"', block)
            npcs[int(re.search(r'id="(\d+)"', block).group(1))] = (
                re.search(r'name="([^"]*)"', block).group(1), int(level.group(1)) if level else 0,
                int(aggro.group(1)) > 0 if aggro else False)
    spawns = [(int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4)))
              for m in re.finditer(r"\('(\d+)', '(-?\d+)', '(-?\d+)', '(-?\d+)'", open(datapack + '/sql/spawnlist.sql').read())
              if int(m.group(1)) in npcs]
    return teleports, npcs, spawns


def main():
    datapack = sys.argv[1]
    radius = int(sys.argv[2]) if len(sys.argv) > 2 else 4000
    teleports, npcs, spawns = load(datapack)
    rows, seen = [], set()
    for tid, name, x, y, z in teleports:
        near = [npcs[i] for i, sx, sy, sz in spawns if abs(sz - z) < 1200 and math.hypot(sx - x, sy - y) < radius]
        if len(near) < 8 or (name, x, y) in seen:
            continue
        seen.add((name, x, y))
        levels = sorted(n[1] for n in near)
        pick = lambda q: levels[min(len(levels) - 1, int(q * len(levels)))]
        counts = {}
        for n in near:
            counts[n] = counts.get(n, 0) + 1
        top = '; '.join(f'{n[0]}/{n[1]}' for n, _ in sorted(counts.items(), key=lambda a: -a[1])[:4])
        aggressive = round(100 * sum(1 for n in near if n[2]) / len(near))
        rows.append((pick(0.5), tid, name, pick(0.1), pick(0.9), aggressive, len(near), top))
    print('| median | tp | zone | p10-p90 | aggressive % | spawns | main monsters |')
    for r in sorted(rows):
        print(f'| {r[0]} | {r[1]} | {r[2]} | {r[3]}-{r[4]} | {r[5]} | {r[6]} | {r[7]} |')


if __name__ == '__main__':
    main()
