#!/usr/bin/env python3
"""Static wiring checks for PrivBrowse. No Android SDK required."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
main = (ROOT / "app/src/main/java/com/privbrowse/app/ui/MainActivity.kt").read_text()
feature = (ROOT / "app/src/main/java/com/privbrowse/app/ui/FeatureCenterActivity.kt").read_text()

ACTIONS_DEF = re.compile(r'const\s+val\s+(ACTION_[A-Z0-9_]+)\s*=\s*"')
KEYS_DEF = re.compile(r'const\s+val\s+(KEY_[A-Z0-9_]+)\s*=\s*"')
ACTION_REF = re.compile(r'MainActivity\.(ACTION_[A-Z0-9_]+)')
KEY_REF = re.compile(r'MainActivity\.(KEY_[A-Z0-9_]+)')
BRANCH = re.compile(r'\n\s*(ACTION_[A-Z0-9_]+)\s*->')
ACTIVITY_REF = re.compile(r'Intent\(this,\s*([A-Za-z0-9_]+)::class\.java\)')


actions = set(ACTIONS_DEF.findall(main))
refs = set(ACTION_REF.findall(feature))
keys = set(KEYS_DEF.findall(main))
key_refs = set(KEY_REF.findall(feature))
branches = set(BRANCH.findall(main))
missing_actions = sorted(refs - actions)
missing_keys = sorted(key_refs - keys)
missing_branches = sorted(refs - branches)
activity_refs = sorted(set(ACTIVITY_REF.findall(feature)))
missing_activities = [
    a for a in activity_refs
    if not (ROOT / 'app/src/main/java/com/privbrowse/app/ui' / f'{a}.kt').exists()
]

# Simple source sanity checks for the reported CI failure class.
forbidden_broken = []
for p in (ROOT / 'app/src/main/java').rglob('*.kt'):
    text = p.read_text()
    if '.singleLine =' in text:
        forbidden_broken.append(f'{p.relative_to(ROOT)}: uses property assignment for singleLine')

print(f"Feature actions referenced: {len(refs)}")
print(f"Feature keys referenced: {len(key_refs)}")
print(f"MainActivity action constants: {len(actions)}")
print(f"MainActivity key constants: {len(keys)}")

if missing_actions:
    print("Missing actions:", ', '.join(missing_actions))
if missing_keys:
    print("Missing keys:", ', '.join(missing_keys))
if missing_branches:
    print("Unhandled actions:", ', '.join(missing_branches))
if missing_activities:
    print("Missing activities:", ', '.join(missing_activities))
if forbidden_broken:
    print("Broken singleLine usages:", '; '.join(forbidden_broken))

if missing_actions or missing_keys or missing_branches or missing_activities or forbidden_broken:
    sys.exit(1)
print("AUDIT PASS")
