"""Canonical google-activation-v1 vectors. Synthetic values only; no verifier bypass."""
import argparse
import base64
import hashlib
import json
from copy import deepcopy
from pathlib import Path

HERE = Path(__file__).resolve().parent
REV = 'google-activation-v1'
REQUEST = '11111111-1111-4111-8111-111111111111'
CHALLENGE_ID = '22222222-2222-4222-8222-222222222222'
BONUS_ID = '33333333-3333-4333-8333-333333333333'
INTENT = '44444444-4444-4444-8444-444444444444'
BASE_ID = '55555555-5555-4555-8555-555555555555'
AT = '2026-10-17T00:00:00Z'
OBSERVED = '2026-10-17T00:01:00Z'
SIGNAL = '0123456789abcdef'
PROOF = base64.urlsafe_b64encode(bytes(range(32, 64))).decode().rstrip('=')
CHALLENGE = base64.urlsafe_b64encode(bytes(range(32))).decode().rstrip('=')


def canonical(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def digest(raw):
    return base64.urlsafe_b64encode(hashlib.sha256(raw.encode('utf-8')).digest()).decode().rstrip('=')


def vector(name, value):
    raw = canonical(value)
    return dict(name=name, value=value, utf8=raw, hex=raw.encode('utf-8').hex(), sha256Base64url=digest(raw))


def wallet(free=0, purchased=0):
    return dict(credits=free + purchased, freeCredits=free, purchasedCredits=purchased,
                analysesLeftToday=20, freeTailoringsLeftToday=0, day='2026-10-17',
                resetsAt='2026-10-17T18:30:00Z', unlockedApplicationIds=[])


def receipt(kind='GOOGLE_SIGNUP'):
    return dict(id=BONUS_ID if kind == 'GOOGLE_SIGNUP' else BASE_ID, kind=kind,
                amount=5 if kind == 'GOOGLE_SIGNUP' else 2, createdAt=OBSERVED,
                policyVersion='signup-local-v1' if kind == 'GOOGLE_SIGNUP' else 'guest-local-v1')


def activation(source='GUEST_LINK', eligibility='ELIGIBLE', reason='ELIGIBLE'):
    return dict(source=source, eligibility=eligibility, reason=reason,
                eligibilityAnchorAt=AT, firstGoogleObservedAt=OBSERVED,
                referralDeadline='2026-10-18T00:00:00Z' if eligibility == 'ELIGIBLE' else None)


def outcome(free=6, source='GUEST_LINK', base_status='NOT_ATTEMPTED', base_reason='GUEST_LINK'):
    return dict(wallet=wallet(free), activation=activation(source), grants=[receipt()],
                signupBonus=dict(status='GRANTED', reason='ELIGIBLE'),
                baseTrial=dict(status=base_status, reason=base_reason),
                referralEligibility=dict(status='ELIGIBLE', deadline='2026-10-18T00:00:00Z'), replayed=False)


def generate():
    link = vector('guest-link', ['TMR_GOOGLE_ACTIVATION_PAYLOAD_V1', 'GUEST_LINK', INTENT, 'SIGNUP_ONLY', '', ''])
    recovery = vector('same-uid-no-secret-recovery', ['TMR_GOOGLE_ACTIVATION_PAYLOAD_V1', 'GUEST_LINK', '', 'SIGNUP_ONLY', '', ''])
    direct = vector('direct-base', ['TMR_GOOGLE_ACTIVATION_PAYLOAD_V1', 'DIRECT_GOOGLE', '', 'SIGNUP_AND_BASE', 'ANDROID_ID', SIGNAL])
    direct_no_base = vector('direct-no-base', ['TMR_GOOGLE_ACTIVATION_PAYLOAD_V1', 'DIRECT_GOOGLE', '', 'SIGNUP_ONLY', '', ''])
    proof = vector('intent-proof', ['TMR_UPGRADE_PROOF_V1', 'tailormyresume', 'fixture', PROOF])
    create = vector('intent-create', ['TMR_UPGRADE_INTENT_V1', proof['sha256Base64url']])
    conflict = vector('conflict-complete', ['TMR_EXISTING_ACCOUNT_HANDOFF_V1', INTENT, 'PRESERVE_LOCAL_WORK'])
    base_only = vector('direct-base-signup-campaign-absent', direct['value'])
    bindings = []
    for payload, pair in [(link, ['signup-local-v1', None]), (direct, ['signup-local-v1', 'guest-local-v1']),
                          (base_only, [None, 'guest-local-v1'])]:
        bindings.append(vector(payload['name'] + '-binding', ['TMR_PROMOTION_BINDING_V1', 'tailormyresume',
                        'fixture', 'synthetic-uid-google-01', 'GOOGLE_ACTIVATION', REQUEST,
                        payload['sha256Base64url'], canonical(pair), CHALLENGE_ID, CHALLENGE]))
    hashes = dict(revision=REV, syntheticOnly=True, proof=PROOF, challenge=CHALLENGE,
                  vectors=[link, recovery, direct, direct_no_base, proof, create, conflict] + bindings,
                  negativeMutations=[dict(name='different-action', position=4, replacement='GUEST_TRIAL'),
                                     dict(name='different-environment', position=2, replacement='production'),
                                     dict(name='different-signup-revision', position=7,
                                          replacement=canonical(['signup-local-v2', 'guest-local-v1'])),
                                     dict(name='different-base-revision', position=7,
                                          replacement=canonical(['signup-local-v1', 'guest-local-v2'])),
                                     dict(name='wrong-revision-encoding', position=7,
                                          replacement='signup-local-v1')])
    link_request = dict(requestId=REQUEST, policyVersion='signup-local-v1', trialPolicyVersion=None,
                        path='GUEST_LINK', upgradeIntent=dict(intentId=INTENT, proof=PROOF), continuity=None,
                        challengeId=CHALLENGE_ID, integrityToken='synthetic-integrity-not-valid-in-any-environment')
    direct_request = dict(link_request, path='DIRECT_GOOGLE', upgradeIntent=None,
                          trialPolicyVersion='guest-local-v1', continuity=dict(type='ANDROID_ID', value=SIGNAL))
    recovery_request = dict(link_request, upgradeIntent=None)
    requests = dict(revision=REV, syntheticOnly=True, valid=dict(
        prepare=dict(requestId=REQUEST, proof=PROOF), status=dict(intentId=INTENT, proof=PROOF),
        observation=dict(requestId=REQUEST), guestLink=link_request, sameUidRecovery=recovery_request,
        directGoogle=direct_request, directBaseSignupAbsent=dict(direct_request, policyVersion=None), directNoBase=dict(direct_request, continuity=None, trialPolicyVersion=None),
        challenge=dict(operation='GOOGLE_ACTIVATION', requestId=REQUEST, policyVersion='signup-local-v1',
                       trialPolicyVersion='guest-local-v1', payloadDigest=direct['sha256Base64url']),
        complete=dict(requestId=REQUEST, intentId=INTENT, proof=PROOF, disposition='PRESERVE_LOCAL_WORK')),
        invalid=[dict(name='unknown-client-uid', request=dict(link_request, userId='forged')),
                 dict(name='amount-is-server-owned', request=dict(link_request, amount=500)),
                 dict(name='google-link-cannot-refill-base', request=dict(link_request, continuity=direct_request['continuity'])),
                 dict(name='direct-cannot-claim-intent', request=dict(direct_request, upgradeIntent=link_request['upgradeIntent'])),
                 dict(name='trial-revision-without-signal', request=dict(direct_request, continuity=None)),
                 dict(name='uppercase-request-id', request=dict(link_request, requestId='AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA')),
                 dict(name='proof-not-32-bytes', request=dict(requestId=REQUEST, proof='short'))])
    direct_full = outcome(7, 'DIRECT_GOOGLE', 'GRANTED', 'ELIGIBLE')
    direct_full['grants'].insert(0, receipt('GUEST_TRIAL'))
    used = outcome(5, 'DIRECT_GOOGLE', 'NOT_ELIGIBLE', 'SIGN_IN_REQUIRED')
    base_retry = outcome(5, 'DIRECT_GOOGLE', 'GRANTED', 'ELIGIBLE')
    base_retry['signupBonus'] = dict(status='ALREADY_GRANTED', reason='ALREADY_GRANTED')
    base_retry['grants'].insert(0, receipt('GUEST_TRIAL'))
    base_first = outcome(2, 'DIRECT_GOOGLE', 'GRANTED', 'ELIGIBLE')
    base_first['signupBonus'] = dict(status='UNAVAILABLE', reason='POLICY_UNAVAILABLE')
    base_first['grants'] = [receipt('GUEST_TRIAL')]
    signup_later = outcome(7, 'DIRECT_GOOGLE', 'ALREADY_GRANTED', 'ALREADY_GRANTED')
    signup_later['grants'].insert(0, receipt('GUEST_TRIAL'))
    signup_policy = dict(status='AVAILABLE', reason='ELIGIBLE', amount=5, version='signup-local-v1',
                         grantPolicyVersion=None, effectiveAt=AT, expiresAt='2026-10-19T00:00:00Z',
                         operation='GOOGLE_ACTIVATION', bindingVersion=1, integrity='PLAY_INTEGRITY_STANDARD',
                         referralWindowSeconds=86400,
                         baseTrial=dict(status='AVAILABLE', reason='ELIGIBLE', amount=2, version='guest-local-v1', grantPolicyVersion=None))
    retry_policy = dict(signup_policy, status='ALREADY_GRANTED', reason='ALREADY_GRANTED', amount=None,
                        version=None, grantPolicyVersion='signup-local-v1', effectiveAt=None, expiresAt=None)

    replay = outcome(4)
    replay.update(replayed=True)
    existing = outcome(9, 'EXISTING_GOOGLE')
    existing.update(activation=activation('EXISTING_GOOGLE', 'EXISTING', 'EXISTING_ACCOUNT'), grants=[],
                    signupBonus=dict(status='NOT_ELIGIBLE', reason='EXISTING_ACCOUNT'),
                    baseTrial=dict(status='NOT_ATTEMPTED', reason='EVIDENCE_NOT_PROVIDED'),
                    referralEligibility=dict(status='NOT_ELIGIBLE', deadline=None))
    base_limited = outcome(5, 'DIRECT_GOOGLE', 'UNAVAILABLE', 'RATE_LIMITED')
    signup_limited = deepcopy(base_first)
    signup_limited['signupBonus']['reason'] = 'RATE_LIMITED'
    limited_replay = deepcopy(base_limited)
    limited_replay.update(wallet=wallet(4), replayed=True)
    limited_headers = {'Cache-Control': 'no-store', 'Retry-After': '60'}
    http_examples = dict(
        baseRateLimited=dict(status=200, headers=limited_headers, body=base_limited),
        signupRateLimited=dict(status=200, headers=limited_headers, body=signup_limited),
        partialRateLimitedReplayAfterSpend=dict(status=200, headers=limited_headers, body=limited_replay),
        noNewGrantRateLimited=dict(status=429, headers=limited_headers,
            body=dict(error=dict(code='RATE_LIMITED', message='Synthetic fixed message.'))))
    responses = dict(revision=REV, syntheticOnly=True, examples=dict(
        prepared=dict(intentId=INTENT, createdAt=AT, expiresAt='2026-10-17T00:05:00Z', state='PREPARED'),
        statusPrepared=dict(intent=dict(id=INTENT, state='PREPARED', createdAt=AT,
                                       expiresAt='2026-10-17T00:05:00Z'), completion=None),
        challenge=dict(challengeId=CHALLENGE_ID, challenge=CHALLENGE, expiresAt='2026-10-17T00:05:00Z',
                       bindingVersion=1, requestHash=bindings[1]['sha256Base64url']),
        guestOneToSix=outcome(), guestTwoToSeven=outcome(7), directUnused=direct_full, directUsedSignal=used,
        directBaseRetryAfterSignupPause=base_retry, directBaseSignupAbsent=base_first, signupRetryAfterBaseFirst=signup_later,
        signupPolicyNewDirect=signup_policy, signupPolicyBaseRetry=retry_policy,
        observation=dict(wallet=wallet(1), activation=activation(), grants=[],
                         referralEligibility=dict(status='ELIGIBLE', deadline='2026-10-18T00:00:00Z')),
        committedReplayAfterSpendAndPause=replay, existingGoogle=existing,
        conflict=dict(wallet=wallet(9, 3), completion=dict(kind='EXISTING_ACCOUNT', disposition='PRESERVE_LOCAL_WORK',
                      creditsTransferred=0, purchasesTransferred=False, runningJobsTransferred=False), replayed=False)),
        httpExamples=http_examples,
        errors=[dict(status=status, body=dict(error=dict(code=code, message='Synthetic fixed message.')))
                for status, code in [(403, 'GOOGLE_REQUIRED'), (403, 'GUEST_REQUIRED'),
                                     (403, 'GOOGLE_IDENTITY_REJECTED'), (403, 'RECENT_GOOGLE_AUTH_REQUIRED'),
                                     (409, 'ACCOUNT_LINK_CONFLICT'), (409, 'UPGRADE_INTENT_EXPIRED'),
                                     (409, 'UPGRADE_INTENT_REQUIRED'), (409, 'POLICY_CHANGED'),
                                     (409, 'IDEMPOTENCY_CONFLICT'), (503, 'IDENTITY_UNAVAILABLE'),
                                     (503, 'PROMOTION_UNAVAILABLE'), (404, 'NOT_FOUND')]])
    fixture_policy = dict(revision=REV, fixtureOnly=True, liveAdmissionDefault=False,
        trial=dict(version='guest-local-v1', amount=2, capacityCreditUnits=100, entitlement='guest-trial',
                   scope=['tailormyresume', 'fixture']),
        signup=dict(version='signup-local-v1', amount=5, capacityCreditUnits=500, entitlement='google-signup',
                    scope=['tailormyresume', 'fixture', 'google-signup']),
        eligibilityCutoff=AT, effectiveAt=AT, expiresAt='2026-10-19T00:00:00Z',
        secretTtlSeconds=300, prelinkRecoverySeconds=2592000, operationCacheSeconds=2592000,
        providerAliasSeconds=15552000, postDeletionAliasSeconds=0, referralWindowSeconds=86400,
        freshGoogleAuthSeconds=300, metadataSnapshotMaxAgeSeconds=30, directObservationSeconds=600, clockSkewSeconds=30,
        actorAttempts=5, networkAttempts=100, campaignAttempts=1000, counterWindowSeconds=60,
        decisionRetentionSeconds=604800, networkCounterRetentionSeconds=3600,
        localHandoffPolicy='Explicit unresolved production retention; fixture journal not auto-erased',
        productionUnresolved=['cutoff', 'amount/config publication', 'capacities', 'freshness bounds',
                              'referral window', 'retention/disclosure', 'real verifier/package/certificate',
                              'local unresolved-handoff retention'])
    return {'hash-vectors.json': hashes, 'requests.json': requests,
            'responses.json': responses, 'policy.json': fixture_policy}


def validate(files):
    vectors = files['hash-vectors.json']['vectors']
    for v in vectors:
        assert canonical(v['value']) == v['utf8']
        assert bytes.fromhex(v['hex']) == v['utf8'].encode('utf-8')
        assert digest(v['utf8']) == v['sha256Base64url']
        assert len(v['sha256Base64url']) == 43
    binding = vectors[-2]
    for mutation in files['hash-vectors.json']['negativeMutations']:
        changed = list(binding['value'])
        changed[mutation['position']] = mutation['replacement']
        assert digest(canonical(changed)) != binding['sha256Base64url']
    examples = files['responses.json']['examples']
    assert examples['guestOneToSix']['wallet']['credits'] == 1 + 5
    assert examples['guestTwoToSeven']['wallet']['credits'] == 2 + 5
    assert examples['directUnused']['wallet']['credits'] == 2 + 5
    assert examples['directUsedSignal']['wallet']['credits'] == 5
    http_examples = files['responses.json']['httpExamples']
    for sample in http_examples.values():
        assert int(sample['headers']['Retry-After']) > 0
        assert sample['headers']['Cache-Control'] == 'no-store'
    base_limited = http_examples['baseRateLimited']['body']
    signup_limited = http_examples['signupRateLimited']['body']
    limited_replay = http_examples['partialRateLimitedReplayAfterSpend']['body']
    assert base_limited['wallet']['credits'] == 5
    assert [g['kind'] for g in base_limited['grants']] == ['GOOGLE_SIGNUP']
    assert base_limited['baseTrial'] == dict(status='UNAVAILABLE', reason='RATE_LIMITED')
    assert signup_limited['wallet']['credits'] == 2
    assert [g['kind'] for g in signup_limited['grants']] == ['GUEST_TRIAL']
    assert signup_limited['signupBonus'] == dict(status='UNAVAILABLE', reason='RATE_LIMITED')
    assert limited_replay['replayed'] is True and limited_replay['wallet']['credits'] == 4
    for field in ['activation', 'grants', 'signupBonus', 'baseTrial', 'referralEligibility']:
        assert limited_replay[field] == base_limited[field]
    assert http_examples['noNewGrantRateLimited']['status'] == 429
    assert http_examples['noNewGrantRateLimited']['body']['error']['code'] == 'RATE_LIMITED'
    for sample in examples.values():
        if 'wallet' in sample:
            w = sample['wallet']
            assert w['credits'] == w['freeCredits'] + w['purchasedCredits']
    assert files['policy.json']['postDeletionAliasSeconds'] == 0


parser = argparse.ArgumentParser()
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
files = generate()
validate(files)
for name, data in files.items():
    rendered = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    path = HERE / name
    if args.check:
        assert path.read_text() == rendered, f'{name} is stale'
    else:
        path.write_text(rendered)
print(('Verified' if args.check else 'Generated') + f' {len(files)} {REV} canonical fixture files')
