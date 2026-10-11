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
| I.5 | Candidate data leaves the device only to the TailorMyResume backend (`apps-backend`), and only in the `prod` flavour. The app has one HTTP stack: OkHttp, Retrofit, and kotlinx.serialization. Firebase Auth, Credential Manager (Google sign-in), and Google Play Billing are allowed. The `demo` flavour has no `INTERNET` permission. Backups stay off. Analytics, crash, and ad SDKs stay forbidden. | Policy I.5: `INTERNET` only in `app/src/prod/AndroidManifest.xml`, no `allowBackup="true"`, no analytics, crash, or ad library, network libraries only in `core:network` or as `prodImplementation` in `:app`; Dependency Guard (`dependencyGuard`) locks the release classpath of both flavours |

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
| V.7 | The release build uses R8 with resource shrinking. CI builds it on every PR that changes code, and on every push to `main`. If a PR changes only `docs/`, `design/`, or Markdown files, CI skips the Gradle job. The policy job always runs. Release variants are signed only with the Play upload key, read from `TMR_UPLOAD_*` environment variables or Gradle properties. Without them an APK build is unsigned and a bundle task fails. Release never uses the debug key. See `docs/RELEASE.md`. | `assembleRelease` in `build.yml`, the V.7 rule in `check-constitution.sh` |

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
| Roborazzi screenshot tests | Design fidelity is not verifiable by unit tests. The designs in `design/claude-design` are the acceptance criterion, so the only way to prove a screen matches its frame is to compare a rendered screenshot against a committed baseline. | `verifyRoborazziDebug` runs in `tools/ci/verify-local.sh` and in CI with `roborazzi.test.verify=true`. Any unexpected image difference fails the build. |
| Design-system import check (II.5) | Part of the custom lint module above, implemented cheaply as a source scan. The full custom lint module is still deferred. | `tools/ci/check-constitution.sh` fails if a feature module imports a raw Material component or reads a design-system token without going through the design system. |

Kover was chosen over JaCoCo. Roborazzi and Robolectric are build-time test dependencies and
never reach the app binary, so they do not conflict with I.5. Robolectric downloads its
`android-all` jars from Maven at test runtime; that is build tooling, not app network access, and
the release APK still requests no network permission.

### Ledger amendment — 2026-10-10, simplified MVP (I.5)

| Item | Why | Gate |
|---|---|---|
| I.5 no longer requires consent screen S4 | The owner cut the consent screen and the 18+ check from the MVP on 2026-10-10 (PRD 6.1.2). The rest of I.5 stands. | `tools/ci/check-constitution.sh` policy I.5 (unchanged; it never checked S4) |

### Ledger amendment — 2026-10-08, network access for the TailorMyResume backend (I.5)

Adopted under Article VII in the PR that adds the `demo` and `prod` flavours.

| Item | Why | Gate |
|---|---|---|
| I.5 rewritten | The backend contract (`docs/BACKEND_CONTRACT.md`) needs sign-in, sync, tailoring, and billing from the app. The owner approved a narrow network exception on 2026-10-08: backend only, `prod` only, consent screen S4 (PRD 10.4), one HTTP stack. Backups, analytics, crash SDKs, and ad SDKs stay forbidden. | `tools/ci/check-constitution.sh` policy I.5 |
| Flavours `demo` and `prod` (dimension `backend`, `:app` only, III.5) | `demo` keeps the offline behaviour and the developer menu with no `INTERNET` permission. `prod` adds the permission through `app/src/prod/AndroidManifest.xml`. | Policy I.5; `assembleDemoRelease` and `assembleProdRelease` in CI |
| Dependency Guard | Closes the Dependency Guard row of the ledger. Any new release dependency in either flavour fails until the baseline in `app/dependencies/` is updated on purpose. | `dependencyGuard` in `tools/ci/verify-local.sh` |
| Transitive libraries kept in `prod` (review of 2026-10-08) | `com.google.android.datatransport:*` comes from Play Billing 9.1.0, whose own classes call it for Google's billing telemetry; no manifest switch disables it and Billing needs it at runtime. `com.google.android.recaptcha:recaptcha` and `com.google.android.play:integrity` come from `firebase-auth` 24.2.0, whose `FirebaseAuth` and `internal/zza` classes reference them; they are not removable and run only for phone or email flows the app never starts. All of these talk to Google, not to a third party. `firebase_data_collection_default_enabled=false` in `app/src/prod/AndroidManifest.xml` turns off Firebase's default data collection. | Dependency Guard baseline |
| Transitive libraries removed from `prod` | `play-services-location` and `play-services-places-placereport` were declared by Play Billing, but no Billing class references them (class scan of `billing-9.1.0.aar`). `app/build.gradle.kts` excludes them. | Dependency Guard baseline |
