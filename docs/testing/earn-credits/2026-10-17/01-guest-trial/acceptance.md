# Guest-trial slice 01 acceptance

Revision: `guest-trial-v1`. Authority: `docs/BACKEND_CONTRACT.md` section 8.
This is the first complete guest journey, not production activation approval. Implementations must
agree on this contract and the fixture bytes before source writes. No Google bonus/link, referral,
new payment-retirement behavior, merge, or production deployment is included.

| Case | Required observable result | Deterministic proof |
|---|---|---|
| Fresh guest | Anonymous Firebase session, current policy, protected initialization, exactly +2 in server wallet/history | Verified-provider fake + fixture challenge/hash + one grant/ledger/capacity transaction |
| Two successful tailorings | Each consumes one credit once; third new job gets existing NO_CREDIT | Existing reserve/settle tests extended to guest |
| Duplicate tap/lost response/process restart | Persisted requestId/signal recovers same receipt, current wallet; no second award | Same-key sequential and simultaneous initialize; spent-credit replay fixture |
| Different request keys | Same lifetime entitlement, stable original grant ID, no top-up | Race across keys/Uids and unique grant + ledger link |
| Failure/interrupted tailoring | Reservation released, no debit; existing running/completed job recovered before zero gate | Provider failure, process boot recovery and repeated poll/retry tests |
| Last credit/capacity | No overdraft; only one complete last campaign award commits | Concurrent transaction tests with isolated local DB |
| Reads/old call sites | Wallet/credits/export/unlock/purchase/tailoring do not create WELCOME or promotional credits | Fresh user each route, no ledger/grant row; zero projection fixture |
| Stable request mismatch | Same key/different signal returns409 IDEMPOTENCY_CONFLICT, original untouched | Changed-signal request; same key across policy/evidence refresh remains same business digest |
| Action binding | Wrong UID/app/env/action/key/digest/policy/challenge/timestamp/package/cert/hash cannot grant | Golden negative vectors + injected verifier failure cases |
| Temporary policy/config/provider failure | Typed503/409, retry guidance, existing balance retained; no insecure local/debug fallback | Missing policy, paused admissions, expiry, stale revision, missing key/DB revision and verifier timeout |
| Committed replay during outage | Auth/live-user pass, original receipt + current wallet before provider/policy checks | Paused policy + expired token after original credits spent |
| Reused signal | New UID gets NOT_ELIGIBLE/SIGN_IN_REQUIRED with only own wallet | Same signal independent of changed network; no other-owner ID/data |
| Legacy classification | Historical UID/wallet/trusted old Firebase creation gets no new grant; counters/ledger unchanged | Seed historical WELCOME/PURCHASE/TAILORING/MIGRATION history; unknown creation fails closed |
| Identity and API boundary | Forged provider/body amount rejected; anonymous cannot use generic AI or earn referrals | Missing/invalid/cross-project token, OTHER claim, other registered app routing and unsupported routes |
| Counter dimensions | Actor/signal durable separately; shared NAT/model do not prove same person; bounded network load shedding | Controlled clock/windows, IP canonicalization, trusted ingress and spoofed forwarded-header cases |
| Rotation/rollout | Policy/key changes do not reset entitlement; active+retiring keys find same allocation | Existing allocation under previous key and new policy; absent key ring blocks |
| Export/deletion | Own grant receipts included; no raw evidence/other UID; all attributable new rows erased; aggregate capacity stays consumed | DELETE partial failure/recovery/concurrent initialize and other-user/app preservation |
| Session switch | Old wallet/policy/grant responses cannot overwrite another account/environment | Android generation guards, persisted pending-key scope and cache-clearing tests |
| Local data flow | Import/manual profile/job review/tailor/export use guest session normally | Focused model/ViewModel tests and existing screen flow |
| Unknown policy UI | Neutral unavailable text, nullable fields respected; no hardcoded +2/Google/referral promise | Fixture DTO/mapping/ViewModel checks |

Fixtures: `fixtures/http-cases.json`, `fixtures/request-schemas.json`, `fixtures/hash-vectors.json`.
Run `python3 docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/generate-fixtures.py --check`
from the frontend checkout. The generator checks compact UTF-8 hashes, schema/body constraints,
balance arithmetic, same-business retry behavior, and negative binding differences. Synthetic integrity
tokens are deliberately unusable; fixtures prove serialization/application behavior, not Google trust.
No external fetch, database, third-party Python package, key, or real Android identifier is needed.

For backend implementation use Node24.21.0 and the separate disposable local Postgres described in
`../decisions-environment.md`; never print connection values. Run required `npm run check` after
focused tests. Generate migrations through the prescribed tool, do not edit them. Frontend runs its
required `tools/ci/verify-local.sh` after focused tests. Contract-only changes need fixture reproduction
and document/diff checks, not either implementation suite.

Actual staging validation uses the user-authorized GitHub push → Ubuntu workstation pull candidate
flow. Inspect the remote checkout/rules and keep its candidate host/ports/database isolated from the
existing server; no production deploy or merge. Pixel7Pro validation must preserve app data: do not
uninstall, clear data, reset device, or force a destructive account/profile transition. Use a disposable
emulator for reset/reinstall/second-profile cases. A fake-integrity local/emulator result must never be
reported as real Play Integrity success.

Still external activation evidence: Firebase anonymous provider enablement in the correct project;
real Standard Integrity cloud/package/cert/version/license verdicts; disclosure/retention/Data Safety
alignment; trusted candidate staging ingress; explicit finite production capacity/thresholds; and a
compatible client/history projection. Default-off admissions remain until those requirements are
satisfied for that environment. Missing external credentials do not prevent code or deterministic tests.
