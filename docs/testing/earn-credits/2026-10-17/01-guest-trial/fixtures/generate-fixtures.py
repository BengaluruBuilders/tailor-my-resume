import argparse
import base64
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
REQUEST_ID = '11111111-1111-4111-8111-111111111111'
CHALLENGE_ID = '22222222-2222-4222-8222-222222222222'
GRANT_ID = '33333333-3333-4333-8333-333333333333'
POLICY_VERSION = 'guest-local-v1'
AT = '2026-10-17T00:00:00Z'
SIGNAL = '0123456789abcdef'
CHALLENGE = base64.urlsafe_b64encode(bytes(range(32))).decode().rstrip('=')


def canonical(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def digest(value):
    return base64.urlsafe_b64encode(hashlib.sha256(value.encode('utf-8')).digest()).decode().rstrip('=')


def wallet(free=0, purchased=0):
    return dict(credits=free + purchased, freeCredits=free, purchasedCredits=purchased,
                analysesLeftToday=20, freeTailoringsLeftToday=0, day='2026-10-17',
                resetsAt='2026-10-17T18:30:00Z', unlockedApplicationIds=[])


def policy(status='AVAILABLE', reason='ELIGIBLE', mode='ANONYMOUS', configured=True):
    return dict(policy=dict(version=POLICY_VERSION if configured else None,
        effectiveAt=AT if configured else None,
        expiresAt='2026-10-18T00:00:00Z' if configured else None, accountMode=mode,
        guestTrial=dict(status=status, reason=reason, amount=2 if configured else None,
            operation='GUEST_TRIAL', integrity='PLAY_INTEGRITY_STANDARD',
            continuity='ANDROID_ID', bindingVersion=1),
        signupBonus=dict(status='UNAVAILABLE'), referrals=dict(status='UNAVAILABLE')))


def payload(signal=SIGNAL):
    return ['TMR_GUEST_TRIAL_PAYLOAD_V1', 'ANDROID_ID', signal]


def binding(signal=SIGNAL, env='local-fixture', uid='guest-fixture-uid',
            action='GUEST_TRIAL', version=POLICY_VERSION, request=REQUEST_ID,
            challenge=CHALLENGE):
    return ['TMR_PROMOTION_BINDING_V1', 'tailormyresume', env, uid, action,
            request, digest(canonical(payload(signal))), version, CHALLENGE_ID, challenge]


def generate():
    vectors = []
    for name, signal, scope in [
        ('baseline', SIGNAL, {}),
        ('second-signal', 'fedcba9876543210', {}),
        ('unicode-and-escape-uid', SIGNAL, dict(uid='guest-भारत-"\\')),
    ]:
        p = canonical(payload(signal))
        b = canonical(binding(signal=signal, **scope))
        vectors.append(dict(name=name, businessPayload=payload(signal),
            canonicalPayload=p, payloadUtf8Hex=p.encode().hex(), payloadDigest=digest(p),
            binding=binding(signal=signal, **scope), canonicalBinding=b,
            bindingUtf8Hex=b.encode().hex(), requestHash=digest(b)))
    baseline = vectors[0]
    mutations = []
    for name, kwargs in [
        ('wrong-environment', dict(env='production')),
        ('wrong-uid', dict(uid='another-guest')),
        ('wrong-action', dict(action='GOOGLE_SIGNUP')),
        ('wrong-request-id', dict(request='44444444-4444-4444-8444-444444444444')),
        ('changed-signal', dict(signal='fedcba9876543210')),
        ('changed-policy', dict(version='guest-local-v2')),
        ('changed-challenge', dict(challenge='A' * 43)),
    ]:
        value = binding(**kwargs)
        h = digest(canonical(value))
        assert h != baseline['requestHash']
        mutations.append(dict(name=name, binding=value, requestHash=h,
                              mustNotMatch=baseline['requestHash']))
    challenge_request = dict(operation='GUEST_TRIAL', requestId=REQUEST_ID,
                            policyVersion=POLICY_VERSION, payloadDigest=baseline['payloadDigest'])
    challenge_response = dict(challengeId=CHALLENGE_ID,
        expiresAt='2026-10-17T00:05:00Z', bindingVersion=1, requestHash=baseline['requestHash'])
    initialize_request = dict(requestId=REQUEST_ID, policyVersion=POLICY_VERSION,
        continuity=dict(type='ANDROID_ID', value=SIGNAL), challengeId=CHALLENGE_ID,
        integrityToken='synthetic-token-not-valid-in-any-environment')
    receipt = dict(id=GRANT_ID, kind='GUEST_TRIAL', amount=2, createdAt=AT, policyVersion=POLICY_VERSION)
    def outcome(status, reason, credits=0, grants=None, replayed=False):
        return dict(wallet=wallet(credits), grants=grants or [],
                    trial=dict(status=status, reason=reason), replayed=replayed)
    success = outcome('GRANTED', 'ELIGIBLE', 2, [receipt])
    replay = outcome('ALREADY_GRANTED', 'ALREADY_GRANTED', 0, [receipt], True)
    cases = [
        dict(name='policy-eligible', method='GET', path='/credits/policy', status=200, response=policy()),
        dict(name='policy-missing', method='GET', path='/credits/policy', status=200,
             response=policy('UNAVAILABLE', 'POLICY_UNAVAILABLE', configured=False)),
        dict(name='policy-paused', method='GET', path='/credits/policy', status=200,
             response=policy('UNAVAILABLE', 'ADMISSIONS_DISABLED')),
        dict(name='policy-google', method='GET', path='/credits/policy', status=200,
             response=policy('NOT_ELIGIBLE', 'ACCOUNT_MODE_INELIGIBLE', mode='GOOGLE')),
        dict(name='challenge-created', method='POST', path='/credits/grant-challenges', status=201,
             request=challenge_request, response=challenge_response),
        dict(name='initialize-granted', method='POST', path='/credits/initialize', status=200,
             request=initialize_request, response=success),
        dict(name='initialize-replay-after-two-spends-paused-policy-expired-token', method='POST',
             path='/credits/initialize', status=200, request=initialize_request, response=replay),
        dict(name='initialize-another-key-existing-trial', method='POST', path='/credits/initialize', status=200,
             request={**initialize_request, 'requestId':'44444444-4444-4444-8444-444444444444'},
             response=outcome('ALREADY_GRANTED','ALREADY_GRANTED',0,[receipt])),
        dict(name='initialize-allocated-signal-other-owner', method='POST', path='/credits/initialize', status=200,
             request=initialize_request, response=outcome('NOT_ELIGIBLE','SIGN_IN_REQUIRED')),
        dict(name='initialize-historical-account', method='POST', path='/credits/initialize', status=200,
             request=initialize_request, response={**outcome('NOT_ELIGIBLE','LEGACY_ACCOUNT'),'wallet':wallet(1,5)}),
        dict(name='wallet-absent-no-grant', method='GET', path='/wallet',status=200,response=dict(wallet=wallet())),
        dict(name='wallet-after-grant',method='GET',path='/wallet',status=200,response=dict(wallet=wallet(2))),
        dict(name='wallet-after-successful-tailoring',method='GET',path='/wallet',status=200,response=dict(wallet=wallet(1))),
        dict(name='wallet-after-failed-tailoring',method='GET',path='/wallet',status=200,response=dict(wallet=wallet(2))),
        dict(name='trial-ledger', method='GET', path='/credits', status=200,
             response=dict(credits=dict(balance=2,entries=[dict(id='55555555-5555-4555-8555-555555555555',
                 kind='GUEST_TRIAL',amount=2,applicationId=None,productId=None,createdAt=AT)]))),
        dict(name='legacy-ledger-preserved',method='GET',path='/credits',status=200,
             response=dict(credits=dict(balance=6,entries=[
                 dict(id='66666666-6666-4666-8666-666666666666',kind='PURCHASE',amount=5,
                      applicationId=None,productId='application_pack_5',createdAt='2026-10-15T00:00:00Z'),
                 dict(id='77777777-7777-4777-8777-777777777777',kind='WELCOME',amount=1,
                      applicationId=None,productId=None,createdAt='2026-10-14T00:00:00Z')]))),
    ]
    errors=[]
    for status, code in [
        (400,'INVALID_INPUT'),(401,'UNAUTHENTICATED'),(401,'INVALID_TOKEN'),(403,'CROSS_APP_TOKEN'),
        (403,'GUEST_REQUIRED'),(403,'PROVIDER_NOT_SUPPORTED'),(403,'GOOGLE_REQUIRED'),(403,'FORBIDDEN'),
        (404,'NOT_FOUND'),(409,'ACCOUNT_DELETED'),(409,'IDEMPOTENCY_CONFLICT'),(409,'POLICY_CHANGED'),
        (409,'INTEGRITY_CHALLENGE_INVALID'),(403,'INTEGRITY_REJECTED'),(503,'INTEGRITY_UNAVAILABLE'),
        (503,'PROMOTION_UNAVAILABLE'),(429,'RATE_LIMITED'),(413,'PAYLOAD_TOO_LARGE'),(500,'INTERNAL_ERROR'),
    ]:
        headers={'Cache-Control':'no-store'}
        if status in (429,503): headers['Retry-After']='60'
        errors.append(dict(status=status,code=code,headers=headers,
            response=dict(error=dict(code=code,message='Synthetic safe recovery message.'))))
    invalid=[
        dict(name='client-amount', request={**initialize_request,'amount':2}, status=400,code='INVALID_INPUT'),
        dict(name='uppercase-signal', request={**initialize_request,'continuity':dict(type='ANDROID_ID',value='0123456789ABCDEF')},status=400,code='INVALID_INPUT'),
        dict(name='empty-signal', request={**initialize_request,'continuity':dict(type='ANDROID_ID',value='')},status=400,code='INVALID_INPUT'),
        dict(name='all-zero-signal', request={**initialize_request,'continuity':dict(type='ANDROID_ID',value='0'*16)},status=400,code='INVALID_INPUT'),
        dict(name='known-unusable-signal', request={**initialize_request,'continuity':dict(type='ANDROID_ID',value='9774d56d682e549c')},status=400,code='INVALID_INPUT'),
        dict(name='client-provider',request={**initialize_request,'accountMode':'ANONYMOUS'},status=400,code='INVALID_INPUT'),
        dict(name='client-environment',request={**initialize_request,'environment':'production'},status=400,code='INVALID_INPUT'),
        dict(name='missing-proof',request={k:v for k,v in initialize_request.items() if k!='integrityToken'},status=400,code='INVALID_INPUT'),
    ]
    schemas={
        '$schema':'https://json-schema.org/draft/2020-12/schema',
        '$id':'urn:tailormyresume:guest-trial-v1:requests',
        '$defs':{
            'requestId':{'type':'string','pattern':'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'},
            'policyVersion':{'type':'string','pattern':'^[a-z0-9][a-z0-9._-]{0,63}$'},
            'digest':{'type':'string','pattern':'^[A-Za-z0-9_-]{43}$'},
            'continuity':{'type':'object','additionalProperties':False,'required':['type','value'],
                'properties':{'type':{'const':'ANDROID_ID'},'value':{'type':'string','pattern':'^[0-9a-f]{16}$',
                    'not':{'enum':['0000000000000000','9774d56d682e549c']}}}},
            'challengeRequest':{'type':'object','additionalProperties':False,
                'required':['operation','requestId','policyVersion','payloadDigest'],'properties':{
                    'operation':{'const':'GUEST_TRIAL'},'requestId':{'$ref':'#/$defs/requestId'},
                    'policyVersion':{'$ref':'#/$defs/policyVersion'},'payloadDigest':{'$ref':'#/$defs/digest'}}},
            'initializeRequest':{'type':'object','additionalProperties':False,
                'required':['requestId','policyVersion','continuity','challengeId','integrityToken'],'properties':{
                    'requestId':{'$ref':'#/$defs/requestId'},'policyVersion':{'$ref':'#/$defs/policyVersion'},
                    'continuity':{'$ref':'#/$defs/continuity'},'challengeId':{'$ref':'#/$defs/requestId'},
                    'integrityToken':{'type':'string','minLength':1,'maxLength':12000}}}
        }
    }
    files={
        'hash-vectors.json':dict(revision='guest-trial-v1',positive=vectors,negativeBinding=mutations),
        'http-cases.json':dict(revision='guest-trial-v1',prefix='/v1/tailormyresume',
            syntheticOnly=True,cases=cases,errors=errors,invalidRequests=invalid),
        'request-schemas.json':schemas,
    }
    validate(files)
    return files


def validate(files):
    import re
    from copy import deepcopy
    schemas=files['request-schemas.json']['$defs']
    def matches(value, schema):
        if '$ref' in schema: return matches(value,schemas[schema['$ref'].split('/')[-1]])
        if 'const' in schema and value != schema['const']: return False
        if 'enum' in schema and value not in schema['enum']: return False
        if 'not' in schema and matches(value,schema['not']): return False
        kind=schema.get('type')
        if kind=='string':
            if not isinstance(value,str): return False
            if len(value)<schema.get('minLength',0) or len(value)>schema.get('maxLength',10**9): return False
            if 'pattern' in schema and not re.fullmatch(schema['pattern'],value): return False
        if kind=='object':
            if not isinstance(value,dict) or not set(schema.get('required',[])) <= value.keys(): return False
            if schema.get('additionalProperties') is False and not value.keys() <= schema['properties'].keys(): return False
            return all(matches(v,schema['properties'][k]) for k,v in value.items() if k in schema.get('properties',{}))
        return True
    http=files['http-cases.json']
    for case in http['cases']:
        request=case.get('request')
        if request:
            schema=schemas['challengeRequest' if case['path'].endswith('grant-challenges') else 'initializeRequest']
            assert matches(request,schema),case['name']
        response=case['response']
        if 'wallet' in response:
            w=response['wallet']
            assert w['credits']==w['freeCredits']+w['purchasedCredits']>=0
        if 'grants' in response:
            assert len(response['grants']) <= 1
            assert all(g['amount']==2 and g['kind']=='GUEST_TRIAL' for g in response['grants'])
    for case in http['invalidRequests']:
        assert not matches(case['request'],schemas['initializeRequest']),case['name']
    positive=files['hash-vectors.json']['positive']
    assert len({v['requestHash'] for v in positive})==len(positive)
    for vector in positive:
        assert digest(vector['canonicalPayload'])==vector['payloadDigest']
        assert digest(vector['canonicalBinding'])==vector['requestHash']
        assert len(vector['requestHash'])==len(vector['payloadDigest'])==43
    baseline=positive[0]
    reissue=deepcopy(http['cases'][5]['request'])
    reissue['policyVersion']='guest-local-v2'
    reissue['challengeId']='88888888-8888-4888-8888-888888888888'
    reissue['integrityToken']='replacement-synthetic-token'
    assert digest(canonical(payload(reissue['continuity']['value'])))==baseline['payloadDigest']
    assert digest(canonical(binding(version='guest-local-v2')))!=baseline['requestHash']


parser=argparse.ArgumentParser()
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
for name,value in generate().items():
    expected=json.dumps(value,indent=2,ensure_ascii=False)+'\n'
    target=HERE/name
    if args.check:
        assert target.read_text()==expected, f'{name} differs; regenerate deliberately'
    else:
        target.write_text(expected)
print('guest-trial-v1: 3 fixture files reproduced; schemas, balance invariants, hash binding, and negative cases passed')
