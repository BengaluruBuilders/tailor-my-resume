# Slice 02 independent validation — initial frozen pair

**Verdict: FAIL — implementation/attainable frontend gates require repair. External acceptance remains BLOCKED.** Four stable findings below must be handed to a fresh fixer; this validator changed no product source, tests, dependency, gate, assertion, policy, credentials, or acceptance criteria. This report does not authorize publication or claim the slice is complete.

Validation date: 2026-10-17. Both implementation writers were idle before independent assessment. The validator inherited, observed and preserved the existing full frontend gate and signed staging build without restarting either. No APK from another revision was substituted. Backend full-check evidence is reused only after independent hash and checked-input verification; the unchanged full suite was not rerun merely for independence.

## Exact candidate identity

| Item | Recorded and independently checked identity |
| --- | --- |
| Frontend | `/tmp/tmr-earn-20261017-frontend02`, branch `codex/earn-02-google-signup`, HEAD `83f0ae0b2678e3a559cd3a3ddac154ceb0d5fa4b`; predecessor `7ffd2f42a423f4037bfea372cd47b1de4b4d035e` |
| Backend | `/tmp/tmr-earn-20261017-backend02`, same branch name, HEAD `ba0993ada6055ef61dd000327e0617418755b58b`; predecessor `69797ec759f3493b6d4f5d6912a82b1429e30fdb` |
| Contract | `google-activation-v1-rate-limit-1`, frontend-owned `docs/BACKEND_CONTRACT.md`, SHA256 `2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a` |
| Checked source | Frontend 2,220 paths: zero mismatches against `frontend-source-inputs-83f0ae0b.sha256`; manifest SHA256 `312c99d0ae926d1eecdd2ea502d2ec6c78297600e72257ef1dd136f44ab84195`, also independently generated and checked remotely. Backend 223 paths: zero mismatches against `/tmp/tmr-backend02-precheck-repair1-manifest.json` |
| Runtime environment | Genuine staging Firebase `tailormy-resume-staging`; prepared backend HTTPS `https://abhishek-dubey-MS-7B16.local:20002/`, exact backend candidate. Admissions remain closed; zero active policies per staging preparation. No Android request-to-candidate correlation exists for this pair because its APK build failed |
| Build variant / device | Attempted signed `staging`, `com.tailormyresume.app.staging`; no resulting validated APK, signer, installed candidate or device session. No device operation performed |
| Fixture policy | Backend test composition `local-fixture`, base `guest-local-v1` +2 and signup `signup-local-v1` +5, injected identity/Integrity evidence. These are isolated fixture values, never a live admission configuration |

Contract generator independently passed: `python3 .../02-google-signup/fixtures/generate-fixtures.py --check` verified all four canonical files. Backend requests/responses/hash-vectors are byte-identical to frontend fixtures; policy copies are JSON-equivalent, with disclosed Prettier-only whitespace. The entire predecessor contract remains an unchanged byte prefix, and predecessor guest fixture paths have no diff.

## Confirmed findings

### GS02-001 — P1: recent Google authentication cannot be recovered through the offered retry

**Expected:** A protected operation rejected with `RECENT_GOOGLE_AUTH_REQUIRED` retains its original journal/checkpoint/intent and offers an explicit fresh Google credential interaction. It reauthenticates the current Firebase user with `reauthenticateWithCredential`, verifies the same UID and provider subject, force-refreshes its ID token, then retries the authorized operation. Cancellation, wrong Google selection, outage and process death preserve work and pending intent. It must never repair freshness through account-switching `signIn`.

**Observed:** Independently traced actual candidate control flow: `GoogleAccountCoordinator.recoverable()` maps this error to generic `Retry`; `retry()` is only `open()`. `reconcile()` immediately repeats conflict completion or promotion preparation and requests no credential. Its only Firebase refresh is the `LINKING` branch; `FirebaseAuthClient.refreshGoogle()` performs reload/token refresh, and the Firebase session interface has no same-user reauthentication method. `UpgradeJournal` has no persisted authentication-recovery intent. A switched journal also cannot cancel/dismiss. Therefore a stale-auth rejection offers no operation capable of changing the required auth time and can repeatedly return to the same retry state.

**Reproduction / evidence:** Backend's actual candidate `test/tailormyresume-activation.test.ts`, case “conflict proof binds one fresh destination and lookup-free expired-proof replay transfers nothing”, was independently executed and passed: stale `auth_time` is rejected with 403 `RECENT_GOOGLE_AUTH_REQUIRED`; a fresh auth time permits completion, racing destinations bind once, and committed replay remains lookup-free. See [backend-recent-auth-conflict.log](validation-evidence/backend-recent-auth-conflict.log). For Android, retain a switched uncommitted conflict journal and make completion return that error, then invoke Try again: the source path above repeats without calling the credential source or reauthenticating. This is independently source-confirmed; a new Android runtime regression could not execute because GS02-002 prevents compiling the app. Do not label this a genuine Google/device reproduction.

**Affected files:** Frontend `app/src/main/kotlin/com/tailormyresume/app/auth/GoogleAccountCoordinator.kt:305` and `:377`, `FirebaseAuthClient.kt:73`, `AuthOutcome.kt` (`FirebaseSessionClient`), `UpgradeJournal.kt`. Backend enforcement is `src/apps/tailormyresume/activation-state.ts:528`; do not weaken it. Exact pair is the table above. Covers GA29/GA32/GA38 and recovery requirements; valid capability expiry must still be respected.

**Required repair evidence:** Regression coverage for fresh credential → same UID/subject reauthentication → forced token refresh → durable resume, plus cancel, wrong subject, process death and preserved immutable grant requests/receipts/rate deadlines. Preserve the committed lookup-free replay exception.

### GS02-002 — P1: warnings-as-errors prevent debug, release and signed staging compilation

**Expected:** The unchanged required `tools/ci/verify-local.sh` and signed `:app:assembleStaging -PwarningsAsErrors=true` compile the app with all account-ownership and deletion behavior intact.

**Observed:** Remote full gate exits 1 in 4m07s. `:app:compileDebugKotlin` and `:app:compileReleaseKotlin` fail on the following eight diagnostics. Local staging exits 1 in 2m24s on the same diagnostics; no candidate APK is available.

- `app/.../account/RemoteContentReportRepository.kt:38,46,48`: unnecessary safe call on smart-cast non-null `AccountOperationGuard`.
- `app/.../ai/RemoteJobAnalysisSource.kt:44`, `RemoteJobImporter.kt:20`, `RemoteResumeTailor.kt:63`, `RemoteResumeTextParser.kt:28`: the same diagnostic.
- `app/.../auth/RemoteServerAccountDeleter.kt:21`: unused `Unit` expression.

**Reproduction:** Run the unchanged required script on the recorded frontend inputs/toolchain, or run the recorded signed staging command in [frontend-handoff.md](frontend-handoff.md). No restart was needed: both inherited jobs reached terminal failure on this exact SHA. See [frontend-required-gate.log](validation-evidence/frontend-required-gate.log), lines 879–888 and 1671–1680, and [frontend-staging-failure.txt](validation-evidence/frontend-staging-failure.txt).

**Repair boundary:** Preserve every ownership-ticket check and `AccountPrivateWorkEraser.erase()` call. The unused `Unit` is after a real erase invocation in the `ACCOUNT_DELETED` recovery branch; it is compiler cleanup, not justification to remove private-work deletion. Success, already-deleted recovery, and `isClosed` confirmation must continue erasing owner-scoped checkpoints/journals and freezing late writers. Do not suppress warnings or alter required flags/gates.

### GS02-003 — P2: production database registration test still expects Room version 4

**Expected:** The registration test opens historical data through every registered migration to Room version 5, proves the new handoff/ownership tables exist and existing profile content survives, and retains the prohibition on destructive fallback.

**Observed:** `:core:database:testDebugUnitTest` ran 32 tests with one failure: `DatabaseModuleTest.registersBothMigrationsWithoutDestructiveFallback`, line 42, expected 4, observed 5. The candidate `TmrDatabase` is version 5 and production `DatabaseModule` explicitly registers `MIGRATION_4_5`; this failure is an outdated test expectation, not evidence that production failed to register the new migration. The test stops before its later historical-content/fallback assertions can establish a pass.

**Reproduction / evidence:** Unchanged full gate, exact frontend SHA; [database-module-failure.xml](validation-evidence/database-module-failure.xml). Affected files: `core/database/src/test/kotlin/com/tailormyresume/core/database/DatabaseModuleTest.kt:42`, production `.../di/DatabaseModule.kt` and `Migration4To5.kt`. Repair by asserting the intended version/schema and historical-data preservation; do not remove the safety assertion or use destructive fallback.

### GS02-004 — P2: settings Hilt wiring tests omit the new Google gateway binding

**Expected:** Settings navigation/wiring tests compose the new `GoogleAccountGateway` dependency and continue exercising the real settings/credits entry points.

**Observed:** `:feature:settings:impl:hiltJavaCompileDebugUnitTest` fails with Dagger `MissingBinding` for `GoogleAccountGateway`, requested by `SettingsViewModel` and `CreditsViewModel`. Consequent generated component-symbol failures affect `PaywallRouteWiringTest`, `SettingsCreditsWiringTest` and `SettingsRealEntryWiringTest`. `SettingsAppGatewayFakes` provides the guest gateway but no Google gateway. Production `app/.../di/SignInBindings.kt:42` does contain the coordinator binding; the confirmed failure is test composition, not a demonstrated missing production binding.

**Reproduction / evidence:** Unchanged full gate; [frontend-required-gate.log](validation-evidence/frontend-required-gate.log), lines 1132–1159. Affected test module `feature/settings/impl/src/test/kotlin/com/tailormyresume/feature/settings/impl/navigation/SettingsRealEntryWiringTest.kt:99` and its shared Hilt component. Provide the appropriate controlled test gateway without bypassing the real wiring assertions or changing product dependency behavior.

## Executed and reused evidence

| Check | Verdict and limits |
| --- | --- |
| Backend required `npm run check` | **PASS for executed checks, reused with verified provenance.** Exit 0, 112.04s; 721 total, 720 passed, zero failed, one existing real-`pg_dump` PATH skip. All six original evidence hashes and four prepared asset hashes match [backend evidence manifest](backend-check-evidence/evidence-manifest.json); all 223 checked inputs match frozen candidate. One fixture-cleanup repair closed that required-check cycle. No new product repair attempted here |
| Targeted backend fresh-auth/conflict race/replay | **PASS, independently executed.** Existing candidate test, local real PostgreSQL; synthetic token/Admin fixtures explicitly used. One selected test passed, no failure/cancellation/skip; no genuine Google claim |
| Candidate schema dump, empty restore, deletion reconciliation | **PASS, independently executed fixture/database integration.** Real PostgreSQL18 custom dump and `pg_restore --no-owner --exit-on-error --single-transaction` into a newly created empty disposable database. Fifteen migrations; all four new tables populated, identical JSON rows after restore. Actual candidate deletion service with explicit noop Firebase/file ports removes target rows and destination-bound intent, preserves control rows/campaign capacity, remains idempotent and blocks resurrection. [Summary](validation-evidence/backend-dump-restore-summary.json), [isolated fixture](validation-evidence/backend-dump-restore-fixture.ts). Archive SHA256 `d4465f4401783f4f46b0cad673fc4d9e861ad30ff2803a5013e3d03fd56efa2c`, 62,226 bytes; archive kept private in `/tmp`, not published |
| Canonical fixtures / contract compatibility | **PASS** for generator, three byte-identical binding fixtures, JSON-equivalent policy, unchanged predecessor contract prefix and guest fixture paths. This proves protocol fixture consistency, not app execution |
| Required frontend script | **FAIL**, terminal exit 1, four failed tasks captured in GS02-002/003/004. Preliminary constitution/config/workflow checks ran; real-gitleaks subcases explicitly report unavailable tool skips. No overall gate pass or all-tests pass inferred |
| Signed staging APK | **FAIL**, exit 1, same compile diagnostics; runner minimum available memory 6,819 MiB, `cancelledForPressure=False`. Artifact/package/Firebase/endpoint/certificate extraction and device smoke could not be performed |
| Genuine Android→candidate journey and screenshots | **BLOCKED by compilation**, not executed. Earlier writer Roborazzi images and predecessor APK evidence are not actual device evidence for this candidate |

The restore fixture's first setup attempts failed on an ESM/CommonJS import, restricted loopback access, a guessed campaign column, and a mistaken expected DELETE status (candidate contract correctly returns 202 with `deletion.status=completed`). These were corrected only in the isolated validation harness; no candidate, gate, existing assertion or acceptance requirement changed. Final run passed and all its disposable databases were dropped. No serving candidate/slice01 database was read or written by that fixture.

## Acceptance scope and remaining blockers

All GA01–GA55 contract cases were read. Scoped assessment traced actual server activation/classification/independent subgrant/replay/retention/conflict/deletion code, and Android same-UID versus switched-journal recovery, checkpoint/import ownership, writer fences, report/export and deletion paths. The verified backend suite provides controlled real-PG coverage for 2→7/1→6, positive direct-new classification, both partial-grant directions and fresh-key retries, immutable receipts/current-wallet replay, source versus shared rate/security failures, locking/rollback/aliases, conflict binding, erasure/export and inherited compatibility. These controlled tests are not a 55-case genuine paired acceptance pass. Frontend tests omitted by compile failures, freshness recovery, process-boundary/device behavior, and the exact paired end-to-end journey remain unproven.

Separate external **BLOCKED** requirements remain: genuine disposable Google link/direct/conflict/recovery interaction; authoritative selected/configured Integrity project and decode capability; genuine strict package/certificate/license/device/action-bound Integrity verdict; approved live policy/cutoff/window/capacity/thresholds/retention/disclosures and opened admission; actual admitted grant/spend and published-head platform acceptance. Existing Firebase provider/client/signer metadata supports prerequisites only. The public Firebase project number is not proof of Integrity configuration. No fake verifier or request-header bypass was served through the genuine endpoint. No user phone/session/data was accessed or altered. No claim of slice completion, readiness for merge or external platform success is made.

## Resource and handoff state

Inherited local wrapper 680721 and remote lock/script 135799/135880 are terminal, exit 1; no timeout was treated as terminal evidence. Read-only SSH observations respected existing authentication/host-key settings; initial sandbox SSH-fragment/loopback denials were retried through the authorized escalation route without configuration changes. Remote source/status/hash checks were read-only. Source remains frozen/clean at the exact pair above.

After confirming daemon IDLE records, no active worker tasks and completed build trees, scoped TERM stopped local task JVMs 680782/681010 and remote task JVMs 130550/130692. Remote cleanup used `hub-project exec tailormyresume-earn-android main`; final `ps` shows all four absent. No locks were deleted. Candidate API129620/private PostgreSQL114856, original API/system PostgreSQL/shared Caddy, policies and device state were preserved.

Fresh fixer may now own the scoped frontend repairs. Backend candidate is unchanged. Return the repaired exact pair to this same validator for affected regression checks, the full frontend gate and a newly built signed staging artifact; then perform attainable device smoke with provenance. Do not run a duplicate full backend suite solely because the frontend changed; rerun affected backend checks if the repair changes its protocol or implementation. No publication was performed.
