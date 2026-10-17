from datetime import datetime
from hashlib import sha256
import ipaddress
import json
from pathlib import Path
import re
from urllib.parse import urlencode

from jsonschema import Draft202012Validator, FormatChecker

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[5]
load = lambda name: json.loads((ROOT / name).read_text())
schema = load('schemas.json')
Draft202012Validator.check_schema(schema)

def validate(kind, value):
    local = {**schema, '$ref': '#/$defs/' + kind}
    Draft202012Validator(local, format_checker=FormatChecker()).validate(value)

def stamp(value):
    return datetime.fromisoformat(value.replace('Z', '+00:00'))

def semantic(kind, value):
    validate(kind, value)
    if kind == 'linkConfig' and value['enabled']:
        assert value['origin'] in value['allowedOrigins']
        for origin in [value['origin'], *value['allowedOrigins']]:
            host = origin.removeprefix('https://')
            try:
                ipaddress.ip_address(host)
            except ValueError:
                pass
            else:
                raise AssertionError('IP origin')
            assert len(host) <= 253 and all(len(label) <= 63 for label in host.split('.'))
            assert not any(host == suffix or host.endswith('.' + suffix) for suffix in ('localhost', 'local', 'internal', 'flycast'))
        expected = 'com.tailormyresume.app' if value['environment'] == 'production' else 'com.tailormyresume.app.staging'
        assert value['packageName'] == expected
        offer = value['publicOffer']
        if offer['mode'] == 'SNAPSHOT':
            assert 0 < offer['maxAgeSeconds'] <= offer['maxValiditySeconds'] <= 9007199254740991
    if kind == 'offerSnapshot':
        assert stamp(value['effectiveAt']) < stamp(value['expiresAt'])
        assert value['availability'] != 'AVAILABLE' or value['rewardAmount'] is not None

examples = load('examples.json')
by_name = {item['name']: item['value'] for item in examples['examples']}
for item in examples['examples']:
    semantic(item['schema'], item['value'])
negatives = load('negative-shapes.json')
for item in negatives['cases'] + negatives['semanticCases']:
    try:
        semantic(item['schema'], item['value'])
    except Exception:
        pass
    else:
        raise AssertionError('accepted negative ' + item['name'])

vectors = load('transport-vectors.json')
code_pattern = re.compile(r'[a-z][a-z0-9-]{0,31}\.[a-z][a-z0-9-]{0,31}\.[a-z2-7]{32}')
def code(raw):
    if not raw.isascii():
        return None
    normalized = raw.lower()
    return normalized if len(normalized) <= 98 and code_pattern.fullmatch(normalized) else None

def link(raw):
    prefix = vectors['origin'] + '/r/'
    if not raw.isascii() or len(raw) > 512 or not raw.startswith(prefix):
        return None
    return code(raw[len(prefix):])

def referrer(raw):
    prefix = 'tmr_referral='
    if not raw.isascii() or len(raw) > 1024 or not raw.startswith(prefix):
        return None
    return code(raw[len(prefix):])

for group, parse in [('url', link), ('referrer', referrer)]:
    for item in vectors[group + 'Valid']:
        assert parse(item['raw']) == item['code'], item['name']
        assert item['source'] == 'LINK'
    for item in vectors[group + 'Invalid']:
        assert parse(item['raw']) is None, item['name']
assert examples['play']['storeUrl'] == 'https://play.google.com/store/apps/details?' + urlencode({'id': vectors['packageName'], 'referrer': examples['play']['innerReferrer']})
assert referrer(examples['play']['innerReferrer']) is not None

for item in examples['snapshotRendering']:
    snapshot = by_name.get(item['snapshot'])
    current = snapshot is not None and snapshot['appId'] == 'tailormyresume' and snapshot['environment'] == 'staging' and snapshot['availability'] == 'AVAILABLE' and snapshot['rewardAmount'] is not None and stamp(snapshot['effectiveAt']) <= stamp(item['now']) < stamp(snapshot['expiresAt'])
    assert item['expected'] == ('CONDITIONAL_NUMERIC' if current else 'NEUTRAL')

matrix = load('acceptance-matrix.json')
assert len(matrix['cases']) == 24 and len({case['id'] for case in matrix['cases']}) == 24
assert sum(case['status'] == 'BLOCKED_EXTERNAL' for case in matrix['cases']) == 2
assert all(case['status'] in ('PENDING', 'BLOCKED_EXTERNAL') for case in matrix['cases'])
freeze = load('freeze.json')
contract = (REPO / 'docs/BACKEND_CONTRACT.md').read_bytes()
assert sha256(contract[:freeze['predecessorContractBytes']]).hexdigest() == freeze['predecessorContractSha256']
for group in ('files', 'unchangedPredecessorFixtures'):
    for path, expected in freeze[group].items():
        assert sha256((REPO / path).read_bytes()).hexdigest() == expected, path
print('PASS: 6 schemas/examples, 14 negative cases, 6 valid/31 invalid transport vectors, Play encoding, 5 snapshot boundaries, 24 acceptance requirements, unchanged prior contract/fixtures and frozen hashes')
