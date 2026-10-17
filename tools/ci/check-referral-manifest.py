#!/usr/bin/env python3
"""Assert actual merged variant output; does not build or change configuration."""

import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("manifest", type=Path)
parser.add_argument("--variant", choices=["debug", "staging", "stagingPlay"], required=True)
parser.add_argument("--origin", required=True, help="Exact synthetic origin, or empty for disabled")
args = parser.parse_args()
android = "{http://schemas.android.com/apk/res/android}"
root = ET.fromstring(args.manifest.read_bytes())
assert root.get("package") == "com.tailormyresume.app.staging"
application = root.find("application")
assert application is not None
assert application.get(android + "networkSecurityConfig") == "@xml/staging_network_security_config"
activities = [a for a in application.findall("activity") if a.get(android + "name") == "com.tailormyresume.app.MainActivity"]
assert len(activities) == 1
assert activities[0].get(android + "launchMode") == "singleTop"
views = [f for f in activities[0].findall("intent-filter") if any(a.get(android + "name") == "android.intent.action.VIEW" for a in f.findall("action"))]
if args.origin:
    assert args.origin.startswith("https://") and args.origin.count("/") == 2
    assert len(views) == 1
    assert views[0].get(android + "autoVerify") == "true"
    assert {c.get(android + "name") for c in views[0].findall("category")} == {"android.intent.category.DEFAULT", "android.intent.category.BROWSABLE"}
    data = views[0].findall("data")
    assert len(data) == 1
    assert data[0].attrib == {android + "scheme": "https", android + "host": args.origin.removeprefix("https://"), android + "pathPrefix": "/r/"}
else:
    assert not views
if args.variant == "debug":
    assert any(m.get(android + "name") == "android.app.shortcuts" for m in activities[0].findall("meta-data"))
    assert any(a.get(android + "name") == "com.tailormyresume.app.debug.DebugScenarioActivity" for a in application.findall("activity"))
assert "${" not in args.manifest.read_text()
print(json.dumps(dict(variant=args.variant, origin=args.origin, package=root.get("package"), mainActivityViewFilters=len(views), preservedVariantMetadata=True)))
