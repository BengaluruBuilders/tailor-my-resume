# Profile readiness — issue #392

The later Profile screen now shows **Ready to tailor** when the profile has been reviewed and has no required gaps. The percentage and progress bar are removed from this screen. Missing experience end dates appear under **Required corrections**. Each correction opens the affected role in the existing editor. A profile that still needs confirmation links to the existing review screen.

**LinkedIn & links** is labelled **Optional** in secondary text. Links do not affect readiness. The first-use review, required-gap rule, explicit confirmation, profile storage, and export acceptance checks stay unchanged. No backend or API contract change is required.

The source is [issue #392](https://github.com/BengaluruBuilders/tailor-my-resume/issues/392). The [published before screenshot](https://raw.githubusercontent.com/BengaluruBuilders/tailor-my-resume/95066b23eb65a831607b5b40575854c49ff8371f/design/audits/2026-10-16/screenshots/27-profile.png) shows the 96% score and amber Add tag.

## Results

- The focused Profile render job passed: 7 tests, 0 failures. It recorded 8 captures across ready, required-correction, review-needed, and source-file states.
- `:app:assembleDebug` passed with the synthetic preview overrides below and warnings treated as errors.
- The first visual review found no clipped labels or hierarchy defects at normal or 200% text. Required corrections and optional links use distinct wording and color roles.
- `tools/ci/verify-local.sh` passed with exit 0: policy, release-configuration tests, workflow tests, formatting, unit tests, coverage gates, all Roborazzi verification, release lint, Dependency Guard, and debug/release assembly. The final Gradle invocation completed in 9m 23s. All 171 Profile module tests passed (0 failures, errors, or skips). Native emulator verification follows it.
- The local secret-scan regression suite passed; real-gitleaks cases were skipped because that binary is not installed. The PR CI performs the actual secret scan.

Only the three changed Profile goldens were replaced; the five new goldens cover the new readiness states and the optional-links capture. Experience and editor goldens were preserved.

## Coverage

- ViewModel checks cover readiness with and without optional URLs, required gaps on a reviewed profile, and the separate confirmation state.
- Real entry tests cover the affected role editor, Contact, and Review navigation, with the stored profile unchanged by each navigation action.
- Profile screenshot checks cover ready, required-correction, and review-needed states at the prototype size and at 337 dp with 200% text. They assert that text is not clipped or ellipsized. The optional links row also has a scrolled 200% capture.

## Validation environment

Validation initially used the shared legacy lock. After the owner updated resource coordination, the queued work moved to `.worktrees/run-validation` with two shared slots and one operation per worktree. New runs use two Gradle workers, a 2 GB Gradle heap, two active processors, and single-use daemons. The emulator uses the coordinator’s exclusive emulator slot, 2 GB RAM, and two CPU cores. Two Gradle invocations stalled while configuring projects in parallel. Thread dumps show the root and included-build configuration workers waiting in `TaskPathProjectEvaluator.scheduleProjectsJustInTime`. Disabling parallel configuration-cache operations alone did not resolve it. The final invocation adds `-Dorg.gradle.internal.isolated-projects.parallel=false` with `-Dorg.gradle.configuration-cache.parallel=false`. Isolated projects, configuration cache, and all quality gates remain enabled. No repository or shared Gradle setting changed.

The emulator APK uses command-line preview overrides: staging API `https://127.0.0.1:1/` and blank staging Firebase client properties (`WebClientId`, `FirebaseApiKey`, `FirebaseAppId`, `FirebaseProjectId`). This preserves synthetic sample sessions as in the original audit. These captures do not verify live authentication, AI, credits, billing, or the staging backend. The full repository gate uses the existing configured environment.
