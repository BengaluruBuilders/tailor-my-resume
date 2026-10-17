# Slice05 Android retirement handoff

2026-10-17. Implementation/check handoff; independent runtime validation and PR publication have not been performed by this writer. All writer source/build/check handles are terminal and task-owned JVMs are stopped.

## Exact candidate and contract

- Branch `codex/earn-05-retirement-compatibility`, owned checkout `/tmp/tmr-earn-20261017-frontend05`. Parent PR409 source is `8a2c02e8b1298ac60d473bda4fca995da962a4ea`; contract-only commit is `f0eb9d5ef7d0f57ccd651937aa77756cb0ba7e65`.
- Validated executable/source commit: **`6b4a55b1d1bb9c9bd7e02e027de9967d97032235`**, normal feature-pushed and pulled exactly for final checks. Backend pair: **`694ec866da51d64c0f766dc95989daec891ee84d`**. The backend source/runtime is owned by its separate writer/staging owner.
- Frozen contract SHA256 `7c9fd14ba1d47b41eb4d32aa50d1ec8c4d2d83493961ce1e6c737c4b51ffc7ff`; freeze metadata `d2cb13037a41e116a6a2f6a85602fbb2e9a3d4d1b35d358e2850fcb9ef6f1a14`. All five agreed contract/fixture hashes remain unchanged. The freeze supersedes embedded preparation DRAFT labels.
- This handoff and its JSON manifests are evidence-only additions after the checked source commit. Compare any final branch head with that commit: it must contain only these documentation/evidence changes. No application code changes after the gate/APK are implied.

## Resulting behavior

`AccountWallet` replaces the feature-facing payment gateway. It exposes entitlement/history and local cache clearing, with matching offline/test/debug bindings. Account balance, account export and cleanup no longer require sale/restore/unlock ports. `RemoteAccountWallet` retains wallet/history availability and purchased breakdown; returned history is fenced by UID, account-work generation and explicit clear. `WalletSource` now also fences its cache by account-work generation, preventing a late reply from a previous same-UID session from being cached before a caller's final guard.

The app-only `HistoricalPurchaseSettlement` retains owned Play purchase query, completion listener, startup repost and bounded transient retries. It requires matching current local Google account, Firebase UID, Google provider identity and account-work generation before querying/posting and before applying a response. Pending Play states grant nothing; backend verification/consumption stays authoritative. Canceled jobs do not spawn retries. Sign-out/account changes cancel retries; lost replies can recover through the next owned-purchase query. `PurchaseRestorer` starts only in a Google session. Billing 9.1 and its unchanged dependency-guard entries remain necessary for this settlement obligation.

Product-detail queries, Billing launches, pack/payment/restore UI, sale models, payment navigation helpers and dead fake catalogues are removed. Three changed Credits baselines preserve the incumbent appearance with Invite friends and historical purchase rows. The original serial name/fields survive only in `LegacyPaymentNavKey`, which immediately redirects to Credits and cannot render a sale or initiate payment.

Both JobResult and ApplicationDetail zero-credit events now open `CreditsNavKey(returnToApplicationId)`. Back preserves the opener; a restored root earning key has an application-detail fallback. Navigation does not tailor or spend. Existing unfinished-tailoring recovery still precedes the balance gate. Credits refreshes on entry/resume and uses existing guest/Google earning gateways and server offers. Historical/new ledger kinds, stable IDs and server-authoritative balance behavior are reused unchanged. The existing DTO intentionally ignores deprecated unlock fields; no unlock flow was revived.

`docs/PAYMENT_RETIREMENT.md`, architecture navigation/port documentation and mock documentation describe the final boundary. No dependency/config/quality-gate exception or production setting was changed.

## Checks and provenance

The unchanged `tools/ci/verify-local.sh` **PASS** at exact source6b4a55b1, supervisor handle33638 terminal exit0. Total elapsed261.6s; final main Gradle invocation3m31s,2065 tasks (272 executed,14 from cache,1779 up-to-date). It covers policy, secret-scan/release-config/workflow harnesses, convention checks, formatting, model/debug tests, domain/data coverage, screenshot verification, release lint, dependency guard and debug/release assemblies with warnings-as-errors. Existing lint baseline filters2 preexisting errors; no new lint issue or baseline entry was added.

Final preserved JUnit evidence has486 XML reports/2708 tests/zero failures/errors/skips. Some unchanged tasks were up-to-date; `frontend-checks.json` lists per-task outcomes, XML timestamps and hashes. Four normal SKIPPED task conditions are recorded: Kotlin plugin configuration-error checks and redundant app lintVital report/release tasks. Final release lint ran. No missing tool or new test was silently skipped.

Real Gitleaks8.30.1 executed all28 real-scanner harness cases with no real-scanner skips. Archive SHA256 `551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`; binary `88f91962aa2f93ac6ab281d553b9e125f5197bbbce38f9f2437f7299c32e5509`. The unchanged actual source scanner also PASS/no leaks for8a2c02e8..6b4a55b1 using normal GITLEAKS/RUNNER_TEMP/push-range hooks. No scanner exceptions or suppression edits.

Focused final app checks passed82 tests, including startup/listener/sign-out settlement, pending→completed, transient repost, anonymous/OTHER rejection, canceled work, same-UID round-trip stale reply, history cache/clear and navigation. Affected domain/testing/analysis/applications/settings checks passed. The six Credits screenshot tests passed during the scoped record run. Only three affected baselines changed; prototype/narrow font200/two-digit captures were visually inspected once. These are test renders, not genuine runtime screenshots or provider evidence.

Remote logs/manifests are under `/home/codex/projects/tailormyresume-earn-android/runtime/main/`: `slice05-verify01.log`, `slice05-verify01-summary.json`, `slice05-verify01-inputs.json`, `slice05-verify01-resources.jsonl`, `slice05-verify01-resource-cleanup-summary.json`, `slice05-source-scan.log` and focused01–04 logs. Copied local evidence is `/home/abhishek-dubey/code/BengaluruBuilders/TailorMyResume/.validation-runtime/slice05-remote-evidence/`. The original checked-input serialization has SHA256 **`cbe0027944a51ced1041df0d644b5cc78a36ef4d1821c6813779dac56999b5de`**. The committed `frontend-source-inputs.json` presents the same 2,357 path/SHA256 pairs as readable records; its presentation SHA256 is **`1d071c0aa253a9de1844b81e53054451f5583c990c73e163f52af8a972a87478`**. The original log and check metadata retain the original serialization digest; R05-F01 resolution records the exact conversion and history repair. Pre/post tracked hashes matched and the remote checkout was clean.

## Signed staging artifact

One default-disabled signed staging APK was built; no new enabled-link fixture APK or device operation occurred.

- APK: `/home/abhishek-dubey/code/BengaluruBuilders/TailorMyResume/.validation-runtime/slice05-android-apk/6b4a55b1d1bb9c9bd7e02e027de9967d97032235/artifacts/tmr-slice05-staging-default-6b4a55b1.apk`
- SHA256 **`45114bd6f2d909bddf66f3d2934ca2ca6945f9e73961a5a7be5fcc96bccb7023`**,6,060,178 bytes.
- `apksigner verify` PASS; existing signer SHA256 **`cd42a070ed44e41b40e5d8c8b24b26c0df70a7e97f3aa33da405b38984ce549c`**. Package `com.tailormyresume.app.staging`,version1/0.1.0,SDK36.
- Explicit API target `https://abhishek-dubey-MS-7B16.local:20002/`; Firebase project `tailormy-resume-staging`. API/project strings were verified in APK DEX. Other required client fields are present; their values/signing material were never copied into evidence or transferred to the workstation.
- Referral origin explicitly empty; merged and packaged MainActivity manifests have zero VIEW filters; no fixture origin in DEX. Unchanged04 link behavior retains its separately scoped predecessor evidence and external blockers.
- Disk source snapshot manifest SHA256 **`5510e158801189fb793bc43b214605f070704e833e61b694c6be274d99bc62ca`**; all tracked files/modes match source6b4a55b1 before/after build. Backend694 is the intended validation pair; build metadata does not itself prove network contact.

Local build handle56006 terminal PASS,4m51s,762 tasks (509 executed,253 from cache), no cancellation. Build command/resource admission/source manifest/logs/artifact inspection are retained below `.validation-runtime/slice05-android-apk/6b4a55b1d1bb9c9bd7e02e027de9967d97032235/`. Gradle2GiB/Kotlin1GiB/one worker/nonparallel; observed owned JVM caps matched. Minimum RAM6.03GB,tmp quota headroom1.339GB,disk396.07GB; maxowned JVM RSS5.06GB. Task JVMs982700/983063 were verified idle and stopped; both absent. Remote task JVMs224686/225542 were also verified idle and stopped; minRAM28.58GB/maxowned JVM RSS8.99GB. No global daemon kill or unrelated cleanup. All writer handles terminal.

## Preserved failures and boundaries

The first formatter found a renamed-test filename/class mismatch; it was fixed without suppression. Formatting then passed. A remote commit lacked configured identity and remote HTTPS push lacked credentials; no credential store was searched or changed. Formatting/baseline commits were preserved through Git bundles, fast-forwarded locally and normal-pushed, followed by exact normal remote pull before tests.

Focused01 atbdcf38df found a removed test-helper import, nullable cross-module smart cast and obsolete DTO field reference; focused02 at4f9d2842 found an unused sales fixture still referencing PlayProduct. Those compile errors were corrected. Focused03 atd5f721f3 passed77/80 tests; its three failures were stale navigation expectations (28 renderable destinations and paywall Close). Corrected catalog checks assert27 actual destinations plus28 serialized declarations including decode-only alias; the replacement shell test verifies earning Back preserves the application. Final focused04 and full gate pass after the corrections. Earlier failure logs remain scoped to their actual revisions and are not relabeled as passing.

Independent validation owns actual APK installation/contact/UI/recreation evidence, exact published-head inspection and PR verdict. Preserve user phone data; use a disposable setup for destructive account/lifecycle checks. Genuine Play pending/unposted settlement, Google/Integrity reward admission, owned HTTPS/App Links/Play referrer, authoritative historical inventory/cutoff, live policy/retention/disclosure approvals and separately authorized measured AI cost evaluation remain external BLOCKED. No store cutoff/product change, live migration/reward activation, recall enforcement, production deployment or merge was performed. Read-only synthetic fixtures cannot prove those facts. No PR was created by this writer; publication remains delegated after independent validation.
