# Slice03 backend corrected candidate handoff

Final checked/pushed candidate **06bcccf5625b6d92e5c016a5ffd11ebad87a49c0**, branch `codex/earn-03-manual-referrals`, worktree `/tmp/tmr-earn-20261017-backend03`. Feature push24710 exited0 and remote readback95565 confirms the exact full SHA. It is a normal fast-forward correction from provisional `2a20be12598cb268bd57e4ac029b984d8fbcbea4`, based on predecessor `4e954057173fd6a25576708450c864fd31d17c19` (draft PR125). No PR, force/default push, merge, deployment, workstation restart or remote migration was performed. Root's staging owner must exact-pull this new SHA before any authorized isolated staging update;2a20 is superseded.

Contract `manual-referrals-v1`, clarified commit `453a5dbb74856f554b979ae99783fc0486b43a0a`, SHA256 **66db09b4ccb06e8224a4c039c491a4e90dec434f56ee78aab478c0a5bb293add**, freeze SHA256 **0d4878c86324d02a328f999c264c0bd18a73cb30b29a16953ab89cf12bac1ff8**. Both writers explicitly agreed before root released the correction. No backend writer edited the frontend contract/fixtures. Section8/9, wire schemas, examples, canonical byte strings/digests/request hashes/fingerprints are unchanged. The BE copied freeze metadata now points at the clarified owner manifest; its README explains existing formatter-only whitespace normalization of copied schema/vector JSON.

## Correction and exact ownership

The provisional candidate had a coding defect: TMR offer/admission checked retained namespace mappings, but new code issuance did not enforce it atomically, and codeIssuance capability could report AVAILABLE on conflict. The generic service now returns existing own codes first, then takes the existing promotion-scope lock after its caller's actor lock. It checks all retained same-scope code prefixes and inserts under that same lock. Concurrent instances with different configs cannot establish mixed mappings in an empty scope. The TMR offer now projects `codeIssuance:UNAVAILABLE` when a retained mapping conflicts. Projection is informational; the transaction is authoritative. No registry/schema/migration, rotation API, new field or live namespace decision was added.

Only four executable inputs changed relative to2a20's manifest:

- `src/core/referrals/service.ts`: actor→scope atomic retained-mapping fence before new issuance; own recovery retains precedence.
- `src/apps/tailormyresume/referrals.ts`: coherent mapping compatibility drives both offer and codeIssuance capability.
- `test/tailormyresume-referrals-namespace.test.ts`: four required regression groups using real TMR routes/local Postgres plus generic concurrency.
- `test/fixtures/manual-referrals/freeze.json`: clarified frontend-owned metadata only; canonical fixtures unchanged.

`docs/MANUAL_REFERRALS.md`, current fixture README, acceptance/provenance and backend namespace evidence were synchronized. Initial implementation files, generated migrations0015/0016, lifecycle/atomic reward/counters/export/ledger behavior and governing proposals remain as described in the historical handoff archived at `backend-handoff-before-namespace.md`. No historical/applied migration was edited.

## Actual regression and required results

Focused handle90113 terminal exit0: **7/7 PASS,0skips**. Four new groups prove:

1. Real TMR routes configured with both non-fixture tokens `resume-demo.preview-2` issue and manually attach with exactly one full5-credit inviter reward, one grant/ledger/capacity5, invitee unchanged.
2. Wrong app token, wrong namespace, wrong deployment and a real stored code under another scope cannot resolve/award; attachment/grant/ledger/wallet/capacity snapshots remain exactly unchanged. API app/environment come from authenticated context, never the code tokens.
3. Existing namespaceA plus configB rejects new-owner issuance, reports unavailable issuance/offer, rejects new attachment and preserves original own-code/own-receipt recovery without fresh Admin/Integrity calls or rewrites.
4. Concurrent first A/B issuance behind two distinct actor locks admits exactly one owner/mapping and returns503 for the conflicting contender; existing-own recovery still precedes new configuration/qualification checks. The preexisting generic non-Google sink/collision suite also passes.

Unchanged mandatory `npm run check` handle30518 terminal exit0: **749/749 PASS,0failed/0skipped/0canceled**; typecheck, lint, formatting, boundaries all PASS. Log `/tmp/tmr-be03-namespace-required-check.log`, committed as backend `docs/testing/earn-credits/2026-10-17/03-manual-referrals/logs/namespace-required-check.log`. This includes all prior grants/payments/lifecycle/restore/concurrency/ledger tests and the new namespace cases.

Final **226-input** executable/config/schema/test manifest `docs/testing/earn-credits/2026-10-17/03-manual-referrals/source-input-manifest.json` SHA256 **0dd03db8e1bae5ee01183caeae6a2d072a5e4193b8acdbb29ac154b3b4d41ba0** matched fresh post-check and post-commit recomputation byte-for-byte. Prior225-input manifest is retained as source-input-manifest-before-namespace.json. `git diff --check` passed. No source changes or further checks followed PASS; only docs/evidence and normal Git publication completed. Current correction batch has **zero failed checks/zero repairs**. Earlier failures/repairs and exact logs are preserved in historical validation.md; they were not hidden/reset.

Authoritative current evidence is backend `docs/testing/earn-credits/2026-10-17/03-manual-referrals/namespace-validation.md`. The prior745-test result in validation.md is explicitly historical for2a20. Full log/manifest identities distinguish both candidate versions. No independent or genuine provider acceptance is claimed from fixture results.

## Runtime, caps and stop state

Local Node24.21.0/npm11.19.0/PG dump+restore18.6; unchanged locked predecessor dependencies and package-lock SHA256a797a75aafe3381243771cf3364434571d11d73c7dccfe8708f3abf91dc9779b. Private local environment was sourced without printing values; all tests use disposable localhost databases on55437, guarded against nonlocal URLs. PG18 loaders remain `/tmp/tmr-slice02-publication-pg-bin/{pg_dump,pg_restore}` targeting `/tmp/tmr-pr117-postgres/root/usr/lib/postgresql/18/bin`.

Caps unchanged: NODE_OPTIONS=--max-old-space-size=2048, UV_THREADPOOL_SIZE=2, package testconcurrency2; focused1worker. One heavy job at a time. Required30518 has ended and local heavy lane was explicitly released to frontend/root. All test/push/readback handles are terminal; **no pending heavy process** remains. Tracked source is clean; only untracked path is the existing local node_modules dependency symlink. Backend writer is idle for independent validation; no future slice or runtime mutation will start autonomously.

## Matching Android and remaining gates

Android now saves full syntax-valid candidates without hardcoded namespace assumptions; it must preserve full canonical bytes and fixed API/app/environment/session ownership, never derive scope from tokens, and distinguish saving from actual referral validity. Authoritative server rejection remains mandatory. Existing optional/null continuity, explicit attempt-only evidence, GET-first uncertain recovery, detached409, original deadline/provider binding, stable ledger IDs/history and signup/work preservation rules remain unchanged. Own issued codes display/copy verbatim. Missing/conflicting backend config must retain pending candidate and advertise no reward.

The new test namespace tokens are fixtures, not approved live values. Admissions/config remain unset/off. Genuine matching Google/Integrity/package/certificate/licensing/signed-device admitted referral reward+spend and live capacity/window/retention/namespace/ingress/disclosure decisions remain **BLOCKED**; candidate is **not ready for merge**. Backend rule3 resolved beneficiary/rule9 survivor ownership and frontend three-action Integrity/finite signal-purpose proposals still require human review. No live cap/retention exception was chosen. Root owns exact-pair staging, independent validation and later PR publication. App Links/domain/install referrer and remaining payment retirement remain slices04/05; no whole issue closes here.
