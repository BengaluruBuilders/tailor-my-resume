import base64
import hashlib
import json
import re
from pathlib import Path

import jsonschema

ROOT = Path(__file__).resolve().parent
VERSION = 'manual-referrals-v1'
PREFIX_SHA = '2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a'


def read(name):
    return json.loads((ROOT / name).read_text())


def compact(value):
    return json.dumps(value, separators=(',', ':'), ensure_ascii=False)


def digest(value):
    return base64.urlsafe_b64encode(hashlib.sha256(value.encode()).digest()).decode().rstrip('=')


def normalize(raw):
    if not 1 <= len(raw) <= 256 or not raw.isascii():
        raise ValueError('raw code bounds')
    code = raw.strip(' \t\r\n').lower()
    if not re.fullmatch(r'tailormyresume\.staging\.[a-z2-7]{32}', code):
        raise ValueError('code syntax or scope')
    return code


schema = read('schemas.json')
jsonschema.Draft202012Validator.check_schema(schema)
checker = jsonschema.FormatChecker()
positive_count = 0
for example in read('examples.json')['examples']:
    validator = jsonschema.Draft202012Validator(
        {**schema, '$ref': '#/$defs/' + example['schema']}, format_checker=checker,
    )
    validator.validate(example['body'])
    if example['schema'] == 'attachResponse':
        assert example['body']['reward'] == example['body']['attachment']['reward']
        assert example['httpStatus'] == (200 if example['body']['replayed'] else 201)
    positive_count += 1
for example in read('negative-shapes.json')['cases']:
    validator = jsonschema.Draft202012Validator(
        {**schema, '$ref': '#/$defs/' + example['schema']}, format_checker=checker,
    )
    assert not validator.is_valid(example['body']), example
vectors = read('vectors.json')
by_name = {}
for vector in vectors['valid']:
    code = normalize(vector['rawCode'])
    assert code == vector['canonicalCode']
    signal = vector['continuity']
    payload = compact(['TMR_REFERRAL_ATTACH_PAYLOAD_V1', code, vector['source'], signal['type'] if signal else '', signal['value'] if signal else ''])
    assert payload == vector['payloadCanonical']
    assert digest(payload) == vector['payloadDigest']
    binding = ['TMR_PROMOTION_BINDING_V1', 'tailormyresume', 'staging', 'synthetic-invitee', 'REFERRAL_ATTACH', vectors['requestId'], digest(payload), 'referral-local-v1', vectors['challengeId'], vectors['challenge']]
    assert compact(binding) == vector['bindingCanonical']
    assert digest(compact(binding)) == vector['requestHash']
    for index in range(1, len(binding)):
        changed = list(binding)
        changed[index] += '-changed'
        assert digest(compact(changed)) != vector['requestHash']
    fingerprint = compact(['TMR_OPERATION_FINGERPRINT_V1', vectors['scope'], 'synthetic-invitee', digest(payload)])
    assert fingerprint == vector['fingerprintCanonical']
    assert digest(fingerprint) == vector['fingerprint']
    by_name[vector['name']] = vector
assert by_name['manual-null']['payloadDigest'] == by_name['manual-case-space']['payloadDigest']
assert len({by_name[key]['payloadDigest'] for key in ['manual-null', 'manual-signal', 'link-null']}) == 3
for raw in vectors['invalidCodes']:
    try:
        normalize(raw)
    except ValueError:
        continue
    raise AssertionError('invalid code accepted: ' + repr(raw))
for file in ROOT.glob('*.json'):
    data = read(file.name)
    if file.name != 'schemas.json':
        assert data['contractVersion'] == VERSION
matrix = read('acceptance-matrix.json')
assert len({case['id'] for case in matrix['cases']}) == len(matrix['cases'])
assert matrix['implementationStatus'] == 'PENDING_NOT_TEST_RESULTS'
policy = read('test-policy.json')
assert policy['fixtureOnly'] is True
assert policy['productionAdmissions'] == 'UNSET_OFF'
assert policy['scope'] == vectors['scope'] == compact(['tailormyresume', 'staging', 'referral-reward'])
contract_path = ROOT.parents[4] / 'BACKEND_CONTRACT.md'
contract = contract_path.read_bytes()
prefix = contract.split(b'\n## 10. Manual referrals', 1)[0]
assert hashlib.sha256(prefix).hexdigest() == PREFIX_SHA, 'section8/9 or prior bytes changed'
assert b'INVALID_INPUT / REFERRAL_CODE_INVALID' in contract
assert b'detachment precedence pending' not in contract
print(f'PASS: {positive_count} schema examples, {len(read("negative-shapes.json")["cases"])} negative shapes, {len(vectors["valid"])} hash vectors, {len(vectors["invalidCodes"])} invalid codes, {len(matrix["cases"])} acceptance cases; predecessor prefix unchanged')
