# Production readiness audit — 2026-10-13

Verdict: **not ready to publish**.
The tested Android code passes its local gates.
The reachable production journey passes on API 35.
Signed release runtime and supported Play billing remain blocked.
The background completion contract remains unresolved.

## Scope and environment

- Workspace: `tehran`.
- Branch: `abhishekdubey331/chore-pasted-text`.
- Comparison base at audit start: `origin/main`, `f4e0b69b`.
- Device: existing Pixel_9_Pro, `emulator-5554`, API 35, arm64.
- Initial installation: demo debug 0.1.0 without INTERNET permission.
- Tested installation: prodDebug with INTERNET permission.
- Backend: `https://apps-backend.fly.dev`.
- Installation used `adb install -r` and preserved app data.
- Normal MainActivity navigation used the remote production bindings.
- Debug preview and forced scenarios stayed inactive.
- A dedicated test account and synthetic profile were authorized.
- One successful production tailoring consumed one credit: 1 → 0.
- No purchase, account deletion, storage wipe, or backend mutation occurred.
- The later user instruction authorized issues and one consolidated merged PR.

## Confirmed defects and fixes

| Issue | Severity | Root cause | Result |
|---|---|---|---|
| [#381](https://github.com/BengaluruBuilders/tailor-my-resume/issues/381) | P1 | Both entry gates checked credits before the durable pending request | An unfinished request can resume at zero or unknown balance |
| [#382](https://github.com/BengaluruBuilders/tailor-my-resume/issues/382) | P1 | Detail exposed files but no route to its stored resume | Every stored resume can open the existing review screen |
| [PR #380](https://github.com/BengaluruBuilders/tailor-my-resume/pull/380) | P1 | Sign-in action shared the story's overflowing layout | Sign-in and legal controls stay reachable at small sizes |
| [#385](https://github.com/BengaluruBuilders/tailor-my-resume/issues/385) | P2 | The only Replace control required a source filename | Pasted and manual profiles retain the existing import route |
| [#386](https://github.com/BengaluruBuilders/tailor-my-resume/issues/386) | P2 | Home counted analyzed drafts as tailored resumes | Totals count stored tailored resumes; history retains drafts |

### Paid request recovery

A server can finish and debit before local result persistence succeeds.
The existing durable identifier supports idempotent recovery.
Both entry gates now consult that identifier before the fresh-request balance gate.
The existing production adapter reuses the request identifier.
A fresh zero-credit request still opens the paywall.
A fresh unknown-balance request remains disabled.
Unit and controlled integration tests cover both gates, identifier reuse, settlement, and one debit.
The missing-persistence race was verified with controlled tests.
The live process-stop run had already persisted its result.
That live run proves saved-result recovery, not the missing-persistence race.

### Saved resume recovery

After the last credit was spent, restart left no entry to an unexported saved resume.
The detail screen now opens the existing review destination without tailoring again.
A second live case exposed a stale filename after sign-out removed PDF bytes.
The shared fix offers review whenever the stored resume exists.
Explicit re-export restores the missing file.
Extracted text from the recovered PDF exactly matched the accepted original PDF.

### Documentation corrections

- [#297](https://github.com/BengaluruBuilders/tailor-my-resume/issues/297): answer-only bullets can name an existing entry without a same-entry profile citation.
- [#298](https://github.com/BengaluruBuilders/tailor-my-resume/issues/298): legacy export unlock records use `unlockedAt`.
- [#343](https://github.com/BengaluruBuilders/tailor-my-resume/issues/343): the Constitution separates XML phrase scanning from semantic keyword-coverage review.
- Android guard behavior and read-only backend source supported these corrections.

## Runtime matrix

| Scenario | Actual result | Status | Mode |
|---|---|---|---|
| Google sign-in and session restore | Dedicated account signed in; restart and same-account sign-in restored state | PASS | Live production |
| Pasted resume and manual entry | Synthetic details were confirmed; explicit manual role persisted | PASS | Live production + device |
| Text PDF and DOCX import | Both parsed remotely; long filename remained readable | PASS | Live production |
| Scanned PDF | Image-only input was refused; profile stayed intact | PASS | Device |
| Profile replacement | Import replaced imported roles and retained the confirmed manual role | PASS | Live production + device |
| Contact edit | Long synthetic name persisted after restart | PASS | Device |
| Job validation | Short text stayed disabled; valid 547-character text analyzed | PASS | Live production |
| Network interruption | Analysis failed safely; Retry retained input and succeeded online | PASS | Live production |
| Tailoring and credits | One result; balance 1 → 0; no debit for review/edit/export | PASS | Live production |
| Quick-answer confirmation/decline | Production analysis returned no question; controlled provenance tests passed | NOT RUN live | Unit + controlled integration |
| Pending paid request recovery | Entry gates reused pending identifier without another debit | PASS | Unit + controlled integration |
| Saved review at zero credits | Stored result reopened before export and after same-account return | PASS | Device |
| Acceptance and editing | Export required acceptance; edited text persisted | PASS | Device |
| PDF export | One A4 page, 595 × 842 pt, 49,310 bytes; accepted text matched | PASS | Device + extracted PDF |
| PDF open/share | Android viewer opened; one-file share chooser appeared; no recipient selected | PASS | Device |
| Status and totals | Applied/Undo worked; restart retained Applied; one resume and two history rows | PASS | Device + unit |
| 320 dp and 200% text | Sign-in and footer stayed reachable; story scrolled | PASS | Device + screenshot tests |
| Sign-out | Prior UI disappeared; same-account return restored local records | PASS | Device |
| Different-account isolation and session expiry | Controlled tests passed; no second live account used | NOT RUN live | Unit + controlled integration |
| Play purchase/reconciliation | Prices did not load; no purchase attempted | BLOCKED | Device + controlled tests |
| Signed release runtime | Upload key settings unavailable | BLOCKED | Release gate |
| API 26/34 runtime | No additional emulator used | NOT RUN | — |

A blank contact name could save.
The current required-gap rules require experience dates, not a name.
This observation does not prove a missing-name validation gate.
The audit restored the synthetic long name.

## Checks and failure record

- `tools/ci/verify-local.sh`: PASS on the final source.
- The gate covers policy, formatting, unit tests, coverage, screenshots, lint, dependency checks, debug builds, and unsigned release/R8 builds.
- Kotlin warnings remained errors.
- Targeted production adapter, domain, analysis, applications, profile, and onboarding checks: PASS.
- API 35 instrumented tests: 18 PASS (15 database, 2 PDF import corpus, 1 PDF renderer).
- `:app:verifyProdBackendConfig`: PASS with existing configured values.
- Full-history secret scan: PASS; 617 commits, about 9.49 GB, no leaks, bounded to two Go workers.
- Final PR-range secret scan and GitHub checks are recorded in the consolidated PR.
- Independent release-risk review found no unresolved blocking code issue.
- No assertions, screenshot tolerances, authentication checks, or release guards were weakened.

The first final CI attempt failed on Compose lint:
`Modifier parameter should be the first optional parameter`.
Moving `modifier` before the optional review callback corrected the failure.
The narrow lint check and full gate then passed.
The final counter change also passed targeted tests/lint and the full gate.

The signing gate failed as expected:
`Release bundles must be signed with the upload key. Set TMR_UPLOAD_KEYSTORE_FILE, TMR_UPLOAD_STORE_PASSWORD, TMR_UPLOAD_KEY_ALIAS and TMR_UPLOAD_KEY_PASSWORD. See docs/RELEASE.md.`

A two-worker Gradle invocation stalled during project configuration.
Only that invocation was stopped, with exit 143.
The project ceiling of three workers completed the checks.
An incorrect screenshot filter matched no tests; the corrected class/method filter passed.
Initial integration compilation failures were corrected with test-only guards and Hilt bindings.
No background stress workers were started.

## Remaining release dependencies

- [#383](https://github.com/BengaluruBuilders/tailor-my-resume/issues/383): provide upload signing through the existing release process and test signed production runtime.
- [#383](https://github.com/BengaluruBuilders/tailor-my-resume/issues/383): verify supported Play test purchases, reconciliation, and recovery.
- [#384](https://github.com/BengaluruBuilders/tailor-my-resume/issues/384): reconcile the off-screen WorkManager/notification contract with current MVP scope.
- Live optional-answer branches remain unverified because the analysis returned no question and no credits remain.
- Unsigned release compilation proves compilation and shrinking; it does not prove signed installation or runtime.

## Evidence limits and location

Local evidence is under `.context/release-audit/` and is gitignored.
It includes the flow map, final matrix, bug record, complete useful logs, synthetic PDFs, and screenshots.
Raw HTTP statuses and exact request timings were unavailable from the non-logging client.
Successful remote UI transitions, persisted generated results, and the credit debit supply runtime evidence.
No mocked test is described as a live production journey.
No AndroidRuntime crash or app main-thread disk/network violation appeared.
Observed background OkHttp socket-tag and framework explicit-GC warnings remain recorded locally.
The audit does not claim exhaustive coverage or publication readiness.
