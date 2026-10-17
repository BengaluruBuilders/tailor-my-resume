# Slice 01 backend handoff

Candidate: `69797ec759f3493b6d4f5d6912a82b1429e30fdb` on `codex/earn-01-guest-trial`, worktree `/tmp/tmr-earn-20261017-backend`, base `3abd46cafb45abaee4f0806f0efa32a02f992375`. Local candidate committed; this agent did not push, open a PR, merge, deploy, migrate remote data or operate a device. The pre-existing copied/untracked `docs/REFERRALS_AND_CREDITS_IMPLEMENTATION_PLAN.md` remains preserved and unstaged. Tracked candidate files are committed.

## Frozen contract agreement

Backend explicitly agreed before application writes to frontend commit `3c9ff77f82fc7a1c9a394c8431bc07a40f1af8df`, `BACKEND_CONTRACT.md` SHA-256 `b76d2202bf1e76f25dfec6191e2b3e2d04cb9dbb419deccfa33979444071a2a6`, section `guest-trial-v1`. NO initialize challenge echo: the server computes the authoritative binding at challenge issuance, persists its expected requestHash and a hash of the challenge secret, then compares the decoded hash to that binding. Exact frozen hash vectors are copied to backend `test/fixtures/guest-trial/hash-vectors.json` and reproduced by Node tests.

## Implemented behavior

Shared opt-in app/environment policy/grant/capacity/idempotency/challenge/continuity/counter/decision tables and services provide immutable revisions, authoritative locked policy publication, stable lifetime entitlement uniqueness, atomic full-award campaign liability and transaction-aware app sinks. A second test-only app uses the same core service with amount 9, a different qualifying event and isolated database without TMR tables. No signup, upgrade, referral or payment-retirement slice is implemented.

TMR derives anonymous/Google/other mode from verified Firebase claims, obtains trusted Firebase creation metadata outside SQL locks, and conservatively excludes legacy/unknown creation histories. New wallets are zero; wallet/credits/export reads create no wallet or ledger reward. Existing balances/history/purchases and spending order remain. Typed provider access, Google-only legacy purchase submission, generic-AI bypass denial and deleted-account guards apply.

Authenticated policy/challenge/initialize routes match frozen schemas/outcomes, strict unknown-key/UTF-8 size validation, stale-policy conflicts and retry/no-store headers. Real Standard Integrity OAuth/decode adapter validates exact request binding, package, certificates, version, freshness, baseline device recognition and licensing. It has no production fake or HTTP bypass. Android-ID continuity uses app/environment/purpose/key-version HMAC aliases with retained-key checks, independent actor/signal/network/campaign counters, finite campaign capacity and bounded evidence expiry. Admissions remain off without complete configuration.

The grant transaction couples capacity, signal allocation, grant, wallet, unique grant-linked ledger and operation receipt; source or sink failure rolls it all back. Original receipts recover current balances before policy/evidence checks; policy/key changes do not reset entitlement. Account export adds own promotion receipts, user-owned rows cascade through deletion/recovery, and aggregate capacity remains consumed without an identity edge. A configured namespace gets a startup/five-minute promotion purge; short counter windows are bounded to 60 seconds. The existing hourly purge also covers expired evidence. Tests cover deletion during verifier work, partial failure/retry and bounded cleanup.

Actual guest +2 → existing tailoring service → two successful debits → zero → repeated completed job recovery → third-new-start refusal is exercised against real local PostgreSQL and injected AI. Existing failure/interruption/concurrent-last-credit tests remain, with explicit historical fixture funding instead of implicit welcome assumptions.

## Schema and operations

Migration `drizzle/tailormyresume/0012_fantastic_warhawk.sql`, its snapshot and journal were generated with `npm run db:generate`; generated SQL was not hand-edited. It adds eight shared promotion tables and an additive nullable/unique ledger grant reference plus GUEST_TRIAL kind. No opening-balance backfill or existing ledger rewrite is added. Core-schema composition and both table/snapshot expectations were updated.

Read candidate `docs/GUEST_TRIAL.md` for exact policy/key/Integrity configuration, lock order, lifecycle and publication commands. Normal authorized publication uses a reviewed JSON document and `PROMOTION_PUBLICATION_ID=<reviewed-release-id> node scripts/publish-promotions.ts <reviewed-policy.json>` with private pooled app configuration. It does not run at startup/check time. A policy rollback requires a new revision, preserves consumed campaign liability and never revokes committed grants. Keep the stable environment namespace when disabling admissions so old receipts remain recoverable.

## Actual validation and repair accounting

Exact Node 24.21.0 via `/tmp/node-v24.21.0-linux-x64/bin`; `npm ci --ignore-scripts` succeeded. Local database setup was sourced privately from `/tmp/tmr-earn-credits-20261017/local-postgres.env`; no credential values were printed. All tests used disposable local PostgreSQL and injected outside services.

- Focused development diagnostics initially found/fixed a duplicate preflight/challenge timing race and obsolete tests depending on implicit welcome funding. The latest pre-gate selected set passed 42/42, including NAT/actor throttling, sink rollback, deletion race and historical summary/shutdown coverage. See `backend-focused-final.log`.
- Required `npm run check` attempt 1 failed: 690 tests, 688 passed, 1 failed, 1 existing skip. The failing existing test was `the sync deadline starts before the first database transaction` in `test/tailormyresume-resume-parse.test.ts`. A redundant new global live-user query ran before the sync AI timeout began. See `backend-check-attempt1.log`.
- **Repair 1 of maximum 2** removed that redundant global query, retained each route's existing own live guard, and guarded only static `/packs` separately. Challenge requests explicitly guard deleted accounts before provider-specific refusal. No test/quality/security assertion was weakened. Focused deadline/trial regression: **35/35 passed** (`backend-repair1-focused.log`).
- Required `npm run check` attempt 2 **PASS, exit 0**: types, lint, formatting, import boundaries, **690 tests / 689 passed / 0 failed / 1 skipped**, test phase 88.625 seconds. See `backend-check-attempt2.log`. Required-check repair count used: **1 of 2**. No further heavy checks were run after the sole slot was released.
- The one existing skip is **`a real pg_dump produces a custom-format archive`** (`test/backup.test.ts`), reason **`pg_dump is not on PATH`**. No new skip was added. The available extracted binary `/tmp/tmr-pr117-postgres/root/usr/lib/postgresql/18/bin/pg_dump` reports PostgreSQL 18.6 when given `LD_LIBRARY_PATH=/tmp/tmr-pr117-postgres/root/usr/lib/x86_64-linux-gnu`. No installation is necessary; adding those private process paths later enables the existing test. That existing test only dumps the admin URL and checks PGDMP magic. It does **not** prove the new schema/data restore. A meaningful later backup rehearsal should dump a migrated disposable candidate database, restore into another disposable database, assert grant/ledger/capacity contents and reapply deletion under existing backup policy. This agent did not run extra backup tests after slot release.
- Memory was monitored for both required attempts. Attempt 2 minimum MemAvailable was **10,010 MiB**, memory PSI avg10 remained 0; no pressure stop was needed. Logs: `backend-check-attempt{1,2}-memory.log`. The earlier OOM interruption was diagnosed separately in `../memory-incident.md`; no check count was reset. Backend heavy slot was explicitly released to the parent after attempt 2 exited.

## Workstation candidate startup

Production Neon connection validation is unchanged. The committed **test-only** `test/guest-trial-local-server.ts` composes actual TMR routes/services with real staging Firebase verification and real Integrity when configured; it contains no HTTP evidence bypass and listens only on loopback. Its environment guard has a passing unit test. It has not yet been started on the workstation by this agent.

1. Parent/deployment owner must **push this exact candidate, pull it over `ssh ubuntu-workstation`, and verify the pulled Git SHA** before runtime/device evidence. Do not transfer an uncommitted archive. Preserve the existing staging service and database.
2. Use a new task-private loopback database named **`tmr_guest_trial_<suffix>`**; the prepared `tmr_earn_candidate` name will be rejected and must not cause weakening of this guard. Set `GUEST_TRIAL_LOCAL_DATABASE_URL` privately and `GUEST_TRIAL_LOCAL_PORT=20000` (the allocated candidate API port). This script rejects remote hosts, unrelated database names, production Firebase or any namespace except local-fixture.
3. Require `TAILORMYRESUME_FIREBASE_PROJECT_ID=tailormy-resume-staging`, `TAILORMYRESUME_PROMOTION_ENVIRONMENT=local-fixture`, normal private staging Firebase/storage/model-price settings, and reviewed `GUEST_TRIAL_LOCAL_POLICY` JSON if publication is requested. Source the permissioned candidate env without printing it. Actual AI is mounted only if the real ANTHROPIC_API_KEY is configured, with unchanged quota/cost caps. No fake AI/provider is automatically substituted.
4. From the **pulled candidate** with exact Node and locked dependencies, run `node test/guest-trial-local-server.ts --serve-local-guest --migrate-local --publish-local-policy`. Omit publication flag if no reviewed policy is set; omit migration flag after migrations are installed. Those flags only affect the guarded private local test DB. The existing candidate Caddy endpoint `https://abhishek-dubey-MS-7B16.local:20002/` should proxy loopback 20000; parent owns actual host/port confirmation. Keep Android TLS trust intact.
5. The test composition deliberately refuses real Firebase account deletion and has no real file deletion port: local rows may reach deletion-pending, while genuine destructive cleanup remains unavailable there. Full lifecycle logic is verified with injected disposable tests; genuine platform deletion needs a deliberately selected disposable identity and authorized composition. No real user's Firebase/files may be deleted through this harness.
6. Record supervised PID/log locations and narrowly scoped stop instructions in the remote task runtime/notes. Do not log credentials, tokens, raw signals or resume data.

## Unresolved activation/validation gates

Genuine Firebase anonymous-provider enablement, actual signed/Play-distributed package/certificate/version, linked Integrity project/service account and accepted verdict, real candidate HTTPS/device pairing, trusted ingress, coherent limited-signal disclosure/Terms/Data Safety, production cutoff/threshold/capacity values and coordinated compatible client rollout remain **unproven**. Missing keys keep real admissions closed; local fake results prove application logic only. No device install, real Google/Integrity result, workstation startup, production activation, CI run or PR is claimed. Publishing/pulling the candidate and independent validation belong to the parent/validator next.

## Changed files in candidate

```text
docs/GUEST_TRIAL.md
docs/architecture.md
docs/compliance.md
docs/stack.md
drizzle.config.ts
drizzle/tailormyresume/0012_fantastic_warhawk.sql
drizzle/tailormyresume/meta/0012_snapshot.json
drizzle/tailormyresume/meta/_journal.json
scripts/publish-promotions.ts
src/apps/tailormyresume/config.ts
src/apps/tailormyresume/export.ts
src/apps/tailormyresume/ledger.ts
src/apps/tailormyresume/module.ts
src/apps/tailormyresume/purge.ts
src/apps/tailormyresume/routes.ts
src/apps/tailormyresume/schema.ts
src/apps/tailormyresume/trial-config.ts
src/apps/tailormyresume/trial-routes.ts
src/apps/tailormyresume/trial-security.ts
src/apps/tailormyresume/trial.ts
src/apps/tailormyresume/wallet.ts
src/core/app-module.ts
src/core/auth/middleware.ts
src/core/auth/service.ts
src/core/errors.ts
src/core/promotions/schema.ts
src/core/promotions/service.ts
src/index.ts
src/server.ts
test/fixtures/guest-trial/hash-vectors.json
test/guest-trial-local-server.test.ts
test/guest-trial-local-server.ts
test/promotions-shared.test.ts
test/schema-drift.test.ts
test/schema-migration.test.ts
test/tailormyresume-ai-harness.ts
test/tailormyresume-job-imports.test.ts
test/tailormyresume-ledger.test.ts
test/tailormyresume-module.test.ts
test/tailormyresume-purchases.test.ts
test/tailormyresume-reports-export.test.ts
test/tailormyresume-shutdown-refuses.test.ts
test/tailormyresume-shutdown.test.ts
test/tailormyresume-spend-per-tailoring.test.ts
test/tailormyresume-summary-skills.test.ts
test/tailormyresume-tailorings.test.ts
test/tailormyresume-trial-fixture.ts
test/tailormyresume-trial-security.test.ts
test/tailormyresume-trial.test.ts
test/tailormyresume-wallet.test.ts
```

Backend writers are stopped after this handoff and await independent validation. Do not expand the slice.
