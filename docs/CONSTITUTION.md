# TailorMyResume Constitution

This document binds every contributor: people, Claude, and delegated agents.
If this document and another document disagree, this document wins.
`docs/ARCHITECTURE.md` comes second. `docs/PRD.md` comes third.

Each rule names its **gate**. A gate is the check that fails when someone breaks the rule.
"Review" means that no machine check exists yet. The [ledger](#ledger) lists the plan for that debt.

Run all gates on your machine with one command:

```bash
tools/ci/verify-local.sh
```

## Article I — Product integrity

These rules come from the core promise in `docs/PRD.md`: "We never invent anything about you."

| # | Rule | Gate |
|---|---|---|
| I.1 | Tailored text uses only facts that the candidate confirmed. Every tailored bullet cites its source IDs. The fabrication guard checks every bullet. If a bullet has a violation, the original text replaces it. | `OfflineFabricationGuardTest`, `TailorResumeUseCaseTest` |
| I.2 | The candidate decides. Every suggestion starts as `PENDING`. Nothing reaches an export without an explicit accept. | `UpdateBulletDecisionUseCaseTest`, `TailorViewModelTest` |
| I.3 | UI copy never claims an ATS score, an ATS pass, or a job, interview, or placement guarantee. Say "keyword coverage" and "readable by common ATS parsers" (PRD 6.4, 7). | Policy I.3 on `res/values*/*.xml` |
| I.4 | No API key, token, or private key goes into the app or the repository. Model credentials live on the backend only. | Policy I.4; gitleaks |
| I.5 | Candidate data leaves the device only to the TailorMyResume backend (`apps-backend`): the staging deployment in the `debug` and `staging` build types, the prod deployment in the `release` build type. The app has one HTTP stack: OkHttp, Retrofit, and kotlinx.serialization. Firebase Auth, Credential Manager (Google sign-in), Google Play Billing, and direct Google Play Integrity Standard requests only for an explicit protected guest admission (`GUEST_TRIAL`, section 8), Google activation including its explicitly requested protected base subaction (`GOOGLE_ACTIVATION`, section 9), or referral attachment (`REFERRAL_ATTACH`, sections 10–11, including bounded foreground LINK/Google-continuation operations), plus the official Google Play Install Referrer service solely for section 11 attribution capture, under `docs/BACKEND_CONTRACT.md` are allowed. Backups stay off. Analytics, crash, and ad SDKs stay forbidden. | Policy I.5: `INTERNET` only in `app/src/main/AndroidManifest.xml`, no `allowBackup="true"`, no analytics, crash, or ad library, network libraries only in `core:network` or `:app`; Dependency Guard (`dependencyGuard`) locks the release classpath |

## Article II — Architecture

TailorMyResume follows Now in Android (NiA). `docs/ARCHITECTURE.md` gives the detail.

| # | Rule | Gate |
|---|---|---|
| II.1 | Module graph: a feature `impl` module depends only on `core:*`, its own `api`, and other features' `api` modules. An `api` module never depends on an `impl` module. A `core` module never depends on a feature. `core:model` is pure Kotlin/JVM. | Policy II.1 |
| II.2 | Unidirectional data flow. A `ViewModel` exposes one `StateFlow` of a sealed `UiState` through `stateIn(WhileSubscribed(5_000))`. A screen composable is stateless. A `...Route` wrapper collects state with `collectAsStateWithLifecycle`. | Review |
| II.3 | No `GlobalScope`. No `runBlocking` in production code. No `!!`, not even in string literals: put UI text in resources. | Policy II.3 |
| II.4 | Inject every dispatcher with `@Dispatcher`. Only `core:common` refers to `Dispatchers.IO` or `Dispatchers.Default` directly. | Policy II.4 |
| II.5 | Features use `Tmr*` components from `core:designsystem`, not raw Material components. | Review |
| II.6 | Each "AI" step sits behind an interface in `core:domain`. A backend implementation replaces the offline one through a Hilt binding. | Review |
| II.7 | Debug builds run StrictMode. If StrictMode logs disk or network access on the main thread, fix it. | `TailorMyResumeApplication`; review |

## Article III — Code quality

| # | Rule | Gate |
|---|---|---|
| III.1 | Spotless with ktlint formats all Kotlin and Gradle Kotlin files. Run `./gradlew spotlessApply` to fix. | `spotlessCheck` |
| III.2 | No code comments. KDoc counts as a comment. A comment may stay only if it cites an external ground: a URL, `issue 123`, `bug 123`, `b/123`, `KT-123`, a CVE, or a spec `line 123`. | Policy III.2 |
| III.3 | Zero warnings. CI compiles Kotlin with warnings as errors. Lint treats warnings as errors. A `lint-baseline.xml` can only get smaller. A new baseline file is an amendment (VII.4). | `.github/ci-gradle.properties`; `lintRelease`; policy III.3 on PRs |
| III.4 | One responsibility per class. Keep functions under about 40 lines. | Review |
| III.5 | Build configuration lives in `build-logic` convention plugins. A module `build.gradle.kts` declares its plugins, namespace, and dependencies. Only `:app` adds build types and packaging. | Review |

## Article IV — Testing

| # | Rule | Gate |
|---|---|---|
| IV.1 | Every change to behaviour ships with a test. Domain logic gets unit tests. ViewModels get unit tests with fakes and Turbine. DAOs get instrumented tests. | `testDebugUnitTest`; weekly `android-test.yml`; review |
| IV.2 | Use fakes from `core:testing`. Do not use mocking libraries. | Policy IV.2 |
| IV.3 | Tests are deterministic. Use `runTest`, virtual time, and `MainDispatcherRule`. Never use `Thread.sleep`. | Policy IV.3 |
| IV.4 | The test stack is JUnit4, Truth, Turbine, and `kotlinx-coroutines-test`. | Review |

## Article V — Build, dependencies, and release

| # | Rule | Gate |
|---|---|---|
| V.1 | Every dependency version lives in `gradle/libs.versions.toml`. A PR that adds a dependency says why. | Policy V.1; review |
| V.2 | Dependabot opens monthly update PRs. Kotlin and KSP update together. | `.github/dependabot.yml` |
| V.3 | CI validates the Gradle wrapper checksum. | `gradle/actions/setup-gradle` |
| V.4 | Keep the build cache, the configuration cache (problems fail the build), and isolated projects on. Keep `org.gradle.workers.max=3`, because this Mac also runs a shared CI runner pool. | Policy V.4 |
| V.5 | JDK 17 toolchain. `minSdk` 26. `compileSdk` and `targetSdk` 36. | `build-logic` |
| V.6 | The Room schema JSON in `core/database/schemas` is committed. If an entity changes, bump the database version, commit the new schema, and add a migration. | "Check Room schema is committed" step in `build.yml` |
| V.7 | The release build uses R8 with resource shrinking. CI builds it on every PR that changes code, and on every push to `main`. If a PR changes only `docs/`, `design/`, or Markdown files, CI skips the Gradle job. The policy job always runs. Release variants are signed only with the Play upload key, read from `TMR_UPLOAD_*` environment variables or Gradle properties. Without them an APK build is unsigned and a bundle task fails. Release never uses the debug key. The one exception is the `staging` build type: it is always signed with the debug key so it can replace a `debug` install, and it is never published. See `docs/RELEASE.md`. | `assembleRelease` in `build.yml`, the V.7 rule in `check-constitution.sh` |

## Article VI — Change process

| # | Rule | Gate |
|---|---|---|
| VI.1 | Changes reach `main` only through a pull request with green CI. Use squash merge. | Review. See the note below. |
| VI.2 | The PR title uses Conventional Commits: `type(scope): summary`. Squash merge makes it the commit message. | "Check PR title" step in `build.yml` |
| VI.3 | One concern per PR. Say what changed, why, and how you verified it. | `.github/pull_request_template.md`; review |
| VI.4 | Write prose in ASD-STE100 Simplified Technical English: PR bodies, commit messages, docs, and UI strings. | Review |
| VI.5 | Run `tools/ci/verify-local.sh` before you open a PR. Run `tools/setup.sh` once. It links a pre-push hook that runs the policy check. | `tools/pre-push` (policy check only) |

Note on VI.1: GitHub blocks branch protection on private repositories on the Free plan.
Until the repository is public or on GitHub Pro, nothing stops a direct push to `main`.
When protection is available, require the `Constitution policy` check.
Do not require `Build, lint, and unit tests` yet. When an owner edits a PR title or base branch, that check reports success without a build.
GitHub then counts the check as passed for a PR with a failed build.
Until a job copies the result of the last full run, a person checks the build result before merge.

The secret scan runs `tools/ci/scan-secrets.sh`. It uses the gitleaks v8.30.1 default rules, pinned in `tools/ci/gitleaks.toml` without the default global path allowlist entries for image, font, document, binary and `gitleaks.toml` paths, and the `.gitleaksignore` file of the base branch. On a pull request the script reads `tools/ci/gitleaks.toml` from the base branch, and uses the PR copy only while the base has none. Before the scan, the script overwrites the workspace `.gitleaksignore` with the base copy, because gitleaks always loads that file from the source directory. A `.gitleaksignore` or `.gitleaks.toml` in the PR cannot allow a secret. A PR can still change what runs: `pull_request` runs the PR version of `build.yml` and `scan-secrets.sh`, so review those two files with extra care. The scan step runs before `check-constitution.sh` and before any other script of the PR, so those cannot rewrite `origin/main` or the scan first. The script scans merge commits in a second gitleaks pass (`--merges --diff-merges=first-parent`), because `git log -p` prints no diff for a merge and a token added only in a merge commit would pass. A merge of the base branch into the PR branch shows the base changes in that pass, so a secret already allowed on the base can be flagged again. The script forces `* diff` in `info/attributes`, so git prints a text diff for every file, including files with a NUL byte and files a PR `.gitattributes` marks `-diff` or `binary`, and gitleaks scans their content whatever the file name. Gitleaks does not match across the NUL bytes of UTF-16 or UTF-32 text, so a third pass lists every blob added or changed in the scanned range (merge first-parent diffs included), and for each blob that holds a NUL byte writes a copy with the NUL bytes removed, plus a strict UTF-8 conversion when the blob starts with a UTF-16 or UTF-32 byte order mark. It commits the copies to a throwaway git repository with `* diff` in its attributes and scans that repository with `gitleaks git` under the same config, because `gitleaks dir` skips a file whose bytes look like a PDF, ZIP or tar file. A failed conversion, git read or gitleaks run fails the step. `tools/ci/test-scan-secrets.sh` tests the script, including a run with the real gitleaks binary.

## Article VII — Amendments

1. Change a rule only through a PR that edits this file.
2. A new rule must name its gate. If no gate exists, write "Review" and add a ledger row.
3. If you change a gate, change the rule text in the same PR.
4. A lint baseline entry, a suppression, or an exception is an amendment. Say why in the PR.

## Ledger

The ledger compares TailorMyResume with the NiA production setup (commit a49ed25).
TailorMyResume already has: convention plugins, a version catalog, Spotless, warnings as errors, lint with
baselines, Dependabot, gitleaks, an R8 release build, a Room schema check, and weekly instrumented tests.

| Next | Why | Trigger |
|---|---|---|
| Fabrication test set as a CI gate | PRD 9 makes "0 critical fabrications" a release gate. This is the core promise. | Next PR |
| Custom lint module | Turns II.2, II.5, and III.2 into real detectors, as NiA's `DesignSystemDetector` does. | After the fabrication set |
| Instrumented tests on every PR | Catches DAO and UI regressions before merge. | When the repository has free minutes (public or paid plan) |

Deferred until a trigger occurs: baseline profiles (after the first Play release).

### Ledger amendment — coverage and screenshot tests moved forward

Adopted in the PR that introduces the design-fidelity UI work.

| Item | Why | Gate |
|---|---|---|
| Coverage report (Kover) | The design-fidelity work is the first large change to `core:domain`, `core:data` and every ViewModel. Adopting coverage only after the fabrication set would have let those modules grow unchecked for five pull requests. | `koverVerify` fails above 80% line coverage on `core:domain` and `core:data`. Every ViewModel test class must exist for a ViewModel that exists. |
| Roborazzi screenshot tests | Design fidelity is not verifiable by unit tests. The prototype in `design/prototype-2026-10-10/`, at 374 x 834 dp, plus the approved state changes in `design/activation-2026-10-12/README.md`, is the acceptance criterion. Compare rendered screenshots against committed baselines. | `verifyRoborazziDebug` runs in `tools/ci/verify-local.sh` and in CI with `roborazzi.test.verify=true`. Any unexpected image difference fails the build. |
| Design-system import check (II.5) | Part of the custom lint module above, implemented cheaply as a source scan. The full custom lint module is still deferred. | `tools/ci/check-constitution.sh` fails if a feature module imports a raw Material component or reads a design-system token without going through the design system. |

Kover was chosen over JaCoCo. Roborazzi and Robolectric are build-time test dependencies and
never reach the app binary, so they do not conflict with I.5. Robolectric downloads its
`android-all` jars from Maven at test runtime; that is build tooling, not app network access, and
the release APK still requests no network permission.

### Ledger amendment — 2026-10-10, simplified MVP (I.5)

| Item | Why | Gate |
|---|---|---|
| I.5 no longer requires consent screen S4 | The owner cut the consent screen and the 18+ check from the MVP on 2026-10-10 (PRD 6.1.2). The rest of I.5 stands. | `tools/ci/check-constitution.sh` policy I.5 (unchanged; it never checked S4) |

### Ledger amendment — 2026-10-10, screenshot acceptance source

| Item | Why | Gate |
|---|---|---|
| The Roborazzi row now takes the prototype in `design/prototype-2026-10-10/`, at 374 x 834 dp, as its acceptance source | The 2026-10-10 prototype replaces the older design in `design/claude-design`. | `verifyRoborazziDebug` (unchanged) |
| D4: one explicit "Accept changes" action in the Changes tab, with per-change Undo, satisfies I.2's explicit accept before export | The prototype has one accept control for all changes. Export PDF stays disabled until it is taken, so no export happens without an explicit accept. I.2 is unchanged. | `UpdateBulletDecisionUseCaseTest` and `TailorViewModelTest` (existing) |
| D5: the before/after number is keyword coverage per PRD 4.2, labelled as keyword coverage, never match, fit or ATS | The prototype shows a before/after number. It is only honest as keyword coverage, so the label says so. I.3 is unchanged. | Policy I.3 scans XML UI strings for selected ATS and guarantee phrases. Review checks the keyword-coverage label and its meaning. |

### Ledger amendment — 2026-10-12, first-use activation

| Item | Why | Gate |
|---|---|---|
| The screenshot acceptance source includes `design/activation-2026-10-12/README.md` for its listed states | The owner approved focused first-use changes after the activation review. The exported prototype stays unchanged and remains the source for all other states. Product integrity rules I.1 to I.5 are unchanged. | `verifyRoborazziDebug` (unchanged) |

### Ledger amendment — 2026-10-12, job step redesign

| Item | Why | Gate |
|---|---|---|
| The screenshot acceptance source includes `design/job-step-2026-10-12/README.md` for the add-job screen, the setup step bar, and the two Paige buttons | The owner approved the Claude Design "Job Step Redesign" on 2026-10-12. It keeps the prototype tokens and changes the step indicator, the hero height, the requirement placement and the button hierarchy. The exported prototype remains the source for all other states. Product integrity rules I.1 to I.5 are unchanged. | `verifyRoborazziDebug` (unchanged) |

### Ledger amendment — 2026-10-08, network access for the TailorMyResume backend (I.5)

Adopted under Article VII in the PR that adds the `demo` and `prod` flavours.

| Item | Why | Gate |
|---|---|---|
| I.5 rewritten | The backend contract (`docs/BACKEND_CONTRACT.md`) needs sign-in, sync, tailoring, and billing from the app. The owner approved a narrow network exception on 2026-10-08: backend only, `prod` only, consent screen S4 (PRD 10.4), one HTTP stack. Backups, analytics, crash SDKs, and ad SDKs stay forbidden. | `tools/ci/check-constitution.sh` policy I.5 |
| Flavours `demo` and `prod` (dimension `backend`, `:app` only, III.5) | `demo` keeps the offline behaviour and the developer menu with no `INTERNET` permission. `prod` adds the permission through `app/src/prod/AndroidManifest.xml`. | Policy I.5; `assembleDemoRelease` and `assembleProdRelease` in CI |
| Build types replace the flavours (2026-10-14) | The owner removed the `demo` and `prod` flavours. `debug` and `staging` use the staging backend, `release` the prod backend. `INTERNET` is declared in `app/src/main/AndroidManifest.xml`. `staging` is always signed with the debug key (the V.7 exception) and is never published. | Policy I.5 and V.7; `assembleRelease` in CI |
| Dependency Guard | Closes the Dependency Guard row of the ledger. Any new release dependency in either flavour fails until the baseline in `app/dependencies/` is updated on purpose. | `dependencyGuard` in `tools/ci/verify-local.sh` |
| Transitive libraries kept in `release` (review of 2026-10-08) | `com.google.android.datatransport:*` comes from Play Billing 9.1.0, whose own classes call it for Google's billing telemetry; no manifest switch disables it and Billing needs it at runtime. `com.google.android.recaptcha:recaptcha` and `com.google.android.play:integrity` come from `firebase-auth` 24.2.0, whose `FirebaseAuth` and `internal/zza` classes reference them; they are not removable. reCAPTCHA remains unused by the app's sign-in flows. Integrity is also called directly for explicit GUEST_TRIAL (section 8), GOOGLE_ACTIVATION including explicitly requested protected base (section 9), and REFERRAL_ATTACH (section 10), under the proposed limited extension below; the earlier guest-only wording did not cover section 9. All of these talk to Google, not to a third party. `firebase_data_collection_default_enabled=false` in `app/src/main/AndroidManifest.xml` turns off Firebase's default data collection. | Dependency Guard baseline |
| Transitive libraries removed from `prod` | `play-services-location` and `play-services-places-placereport` were declared by Play Billing, but no Billing class references them (class scan of `billing-9.1.0.aar`). `app/build.gradle.kts` excludes them. | Dependency Guard baseline |


### Ledger amendment — 2026-10-17, protected guest trial (I.5)

| Item | Why | Gate |
|---|---|---|
| I.5 allows direct Google Play Integrity Standard requests only for explicit guest promotional admission | The approved guest trial requires action-bound evidence. Reuse the existing `com.google.android.play:integrity:1.3.0` runtime dependency from Firebase Auth. No analytics, crash, ad, recall, or third-party identity SDK is added. Candidate profile/job content is not sent to Integrity. | Existing policy I.5 and Dependency Guard; `RemoteGuestTrialGatewayTest` verifies the exact server requestHash is passed to the evidence adapter; server verdict tests gate awards. |
| Purpose-scoped Android ID continuity accessor | The user explicitly requests the disclosed Android-ID repeat-trial signal in the protected admission flow. Read it only after a deliberate claim action; no read on policy/wallet requests, no client logs or raw-ID persistence. Server retention, deletion and activation follow contract section 8. | `GuestEvidenceTest`, `RemoteGuestTrialGatewayTest`; the `HardwareIds` finding is suppressed only on `StandardGuestEvidence.continuity`, reviewed under VII.4 for the disclosed anti-repeat purpose. The official `Settings.Secure.ANDROID_ID` API documents its app-signing-key/user/device scope. No lint baseline or global gate is weakened. |

This amendment records the authorized implementation scope for review. It does not certify privacy,
Terms/Data Safety approval, Firebase anonymous-provider setup, Play licensing, signing-certificate
registration, or genuine integrity verdicts. Missing configuration keeps new admission unavailable.
Official references: [Standard requests](https://developer.android.com/google/play/integrity/standard)
and [ANDROID_ID](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID).

### Proposed Article VII amendment — 2026-10-17, explicit protected action evidence

This unmerged proposal corrects the section 9 documentation gap and adds only section 10 attachment.
The previous I.5 and dependency ledger authorized guest Integrity alone; they did not cover Google activation.
Human review is required before merge. No live admission, disclosure, retention policy or provider acceptance is approved here.

| Item | Why | Gate |
|---|---|---|
| Direct Integrity only for GUEST_TRIAL (8), GOOGLE_ACTIVATION including explicitly requested protected base (9), and REFERRAL_ATTACH (10) | Reuse the resolved Integrity library and separate action bindings. Send only the exact server-issued opaque requestHash to Google, never raw Android ID, candidate content or added identifiers. Evidence cannot cross actions. No new SDK, dependency baseline or network exception. | Review; existing constitution policy, Dependency Guard and lint; GuestEvidenceTest, RemoteGuestTrialGatewayTest, GooglePromotionClientTest, RemoteReferralGatewayTest. |
| Limited Android-ID purposes and accessor rationale | Explicit guest/base allocation only (8/9), plus optional finite independent referral request-velocity counters (10). Referral scope separates app/environment/key version from guest allocation and signup identity. It is not authentication, unique-person identity, a permanent fingerprint or a lifetime device/signup/referral entitlement cap. Current Android referral requests choose the contract-permitted null signal in NONE and OPTIONAL modes and never call the accessor. | Review; existing accessor-scoped HardwareIds suppression and lint; RemoteReferralGatewayTest verifies null in either mode and zero continuity reads. |

No policy, wallet, history, referral status/code display, code creation or recovery-only path collects
Integrity or Android ID. Raw signals/tokens never enter journals, logs or exports. Server counters have
contract-defined finite expiry, rotation, erasure and export handling, with separate account, optional
signal, network and campaign dimensions, no combined fingerprint and no deleted-identity retention.
Guest allocation records are not referral denial records. No live numeric TTL, threshold or capacity is
adopted. Fixture values remain fixtures. The original guest amendment above remains historical.

Existing gates do not enforce every purpose; Review checks the actual data path. Deterministic gateway
tests cover passive/recovery no-collection, explicit attempts, exact hash, action separation, null signal,
owner/erasure fences and unavailable admission. Backend tests own finite counters and scope isolation.
Genuine provider/package/certificate/licensing evidence, live disclosure/Terms/Data Safety, retention,
erasure and campaign approval remain BLOCKED. This candidate is not ready for merge.

### Proposed Article VII clarification — 2026-10-17, foreground referral links

This unmerged proposal clarifies I.5 for contract section 11. It prepares the accepted implementation
for review. It does not activate links, reward admission, hosting, merge or release.

| Item | Why | Gate |
|---|---|---|
| REFERRAL_ATTACH includes a fresh foreground VIEW or visible user-triggered Google continuation | The accepted link journey automatically attaches a retained LINK candidate after activation reconciliation and visible current authenticated offer. Permission is ephemeral and single-use; the business request is durable. Cold links may wait for initial Firebase resolution in-process. Recreated intents, passive restore, referrer callbacks, resume, status reads and recovery-only work cannot collect fresh Integrity. Changed offers require explicit reconfirmation. | Review; RemoteReferralGatewayTest covers first source, display acknowledgement, LINK digest, policy reconfirmation, passive guards and owner fences. Existing policy/lint/full verification remain required. |
| Official com.android.installreferrer:installreferrer:2.2 adapter | Read only the bounded referrer string on first foreground execution; close the service in every path, atomically consume terminal results, bound transient retries to three foreground entries, and preserve manual fallback. No timestamps, installer identity, analytics SDK or new Android-ID purpose. | Review; ReferralLinksTest frozen vectors; ReferralInstallationStoreTest durable consumption and cleanup; Dependency Guard and lint. Genuine Play installation/referrer remains external. |

Only the exact server requestHash reaches Integrity. Raw URLs, referrers, Integrity tokens, challenge
secrets, Android ID and candidate profile content never enter the attribution journal or logs.
Current referral continuity stays null. The minimal consumed marker contains no code, UID or digest;
the separate logical inbox in the same atomic envelope holds at most one pending canonical code and
fixed environment until binding or cleanup. Both stay private with backup disabled. Sign-out,
unrelated switch, dismissal and deletion clear code-bearing state without rearming old attribution.
Same-UID guest linking preserves the candidate. Activity pause abandons a fresh VIEW permission;
temporary credential UI pause does not turn an explicitly initiated Google continuation into a
passive restore. No automatic permission survives process death.

No general network exception, passive evidence collection, gate suppression or lint baseline change
is proposed. Owned domains, matching package/certificate associations, Google/Integrity/Play evidence,
disclosures and publication operations remain BLOCKED until independently satisfied.
Official reference: [Play Install Referrer Library](https://developer.android.com/google/play/installreferrer/library).
