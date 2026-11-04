#!/usr/bin/env python3
"""Sanity check of the bots' class kits (L2Autobots data/autobots/equipment.xml) against the aCis item data.

Usage: python3 scripts/check_equipment.py <equipment.xml> <aCis_datapack dir>
Reports, per row: unknown items, items above the grade allowed at minLevel, a shield next to a two-handed or
dual weapon, a mage kit with heavy body armor or a fighter kit in a robe, and level gaps 1..80 per class line
(a first profession's rows must reach 80 while later professions are not scripted for the population).
Exit code 1 when anything is found.
"""
import glob
import re
import sys

GRADES = ['NONE', 'D', 'C', 'B', 'A', 'S']
MAGES = {'HUMAN_MYSTIC', 'HUMAN_WIZARD', 'CLERIC', 'ELVEN_MYSTIC', 'ELVEN_WIZARD', 'ELVEN_ORACLE', 'DARK_MYSTIC', 'DARK_WIZARD',
         'SHILLIEN_ORACLE', 'ORC_MYSTIC', 'ORC_SHAMAN', 'SORCERER', 'NECROMANCER', 'WARLOCK', 'BISHOP', 'PROPHET', 'SPELLSINGER',
         'ELEMENTAL_SUMMONER', 'ELVEN_ELDER', 'SPELLHOWLER', 'PHANTOM_SUMMONER', 'SHILLIEN_ELDER', 'OVERLORD', 'WARCRYER',
         'ARCHMAGE', 'SOULTAKER', 'ARCANA_LORD', 'CARDINAL', 'HIEROPHANT', 'MYSTIC_MUSE', 'ELEMENTAL_MASTER', 'EVAS_SAINT',
         'STORM_SCREAMER', 'SPECTRAL_MASTER', 'SHILLIEN_SAINT', 'DOMINATOR', 'DOOMCRYER'}
FIRST = {'WARRIOR', 'KNIGHT', 'ROGUE', 'HUMAN_WIZARD', 'CLERIC', 'ELVEN_KNIGHT', 'ELVEN_SCOUT', 'ELVEN_WIZARD', 'ELVEN_ORACLE',
         'PALUS_KNIGHT', 'ASSASSIN', 'DARK_WIZARD', 'SHILLIEN_ORACLE', 'ORC_RAIDER', 'MONK', 'ORC_SHAMAN', 'ARTISAN', 'SCAVENGER'}


def grade_for(level):
    return 5 if level >= 76 else 4 if level >= 61 else 3 if level >= 52 else 2 if level >= 40 else 1 if level >= 20 else 0


def main():
    kits, datapack = sys.argv[1], sys.argv[2]
    items = {}
    for path in glob.glob(datapack + '/data/xml/items/*.xml'):
        for block in re.findall(r'<item id="\d+".*?</item>', open(path, encoding='utf-8').read(), re.S):
            get = lambda key: (re.search(r'name="%s" val="([^"]*)"' % key, block) or [None, ''])[1]
            items[int(re.search(r'id="(\d+)"', block).group(1))] = dict(
                name=re.search(r'name="([^"]*)"', block).group(1), grade=get('crystal_type') or 'NONE',
                armor=get('armor_type'), part=get('bodypart'), weapon=get('weapon_type'))
    problems, levels = [], {}
    for m in re.finditer(r'<equipment classId="(\w+)" minLevel="(\d+)" maxLevel="(\d+)"([^/]*)/>', open(kits).read()):
        cls, lo, hi = m.group(1), int(m.group(2)), int(m.group(3))
        slots = {k: int(v) for k, v in re.findall(r'(\w+)="(\d+)"', m.group(4))}
        levels.setdefault(cls, []).append((lo, hi))
        where = f'{cls} {lo}-{hi}'
        for slot, item in slots.items():
            if item not in items:
                problems.append(f'{where}: {slot} {item} unknown')
            elif GRADES.index(items[item]['grade']) > grade_for(lo):
                problems.append(f"{where}: {slot} {items[item]['name']} grade {items[item]['grade']} above level")
        weapon = items.get(slots.get('rhand'))
        if weapon and slots.get('lhand') and weapon['part'] in ('lrhand',):
            problems.append(f"{where}: shield {items[slots['lhand']]['name']} with two-handed {weapon['name']}")
        chest = items.get(slots.get('chest'))
        if chest and chest['armor'] == ('HEAVY' if cls in MAGES else 'MAGIC'):
            problems.append(f"{where}: {chest['name']} ({chest['armor']}) on a {'mage' if cls in MAGES else 'fighter'}")
    for cls in FIRST:
        covered = set()
        for lo, hi in levels.get(cls, []):
            covered.update(range(lo, hi + 1))
        missing = sorted(set(range(20, 81)) - covered)
        if missing:
            problems.append(f'{cls}: no kit for levels {missing[0]}..{missing[-1]} ({len(missing)} levels)')
    print('\n'.join(problems) or 'EQUIPMENT OK')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
