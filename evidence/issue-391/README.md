# Resume review action emphasis — issue #391

Accept changes uses the existing lime primary button while suggestions need acceptance. The selected review tab uses the existing neutral surface and lime text. After acceptance, Export PDF keeps the primary treatment. Edit stays secondary. The acceptance and export logic is unchanged.

Changes remains the initial tab when suggestions exist. Resume is one tap away. The same footer appears on both tabs, and each change keeps its Undo action.

## Original evidence

The [issue](https://github.com/BengaluruBuilders/tailor-my-resume/issues/391) contains the audit environment, reproduction, and acceptance criteria.

- [Before acceptance](https://raw.githubusercontent.com/BengaluruBuilders/tailor-my-resume/79a1c784a336cd76489aa13b99f2f70a27379314/design/audits/2026-10-16/screenshots/19-tailor-review.png)
- [Blocked export](https://raw.githubusercontent.com/BengaluruBuilders/tailor-my-resume/79a1c784a336cd76489aa13b99f2f70a27379314/design/audits/2026-10-16/screenshots/20-export-unaccepted.png)
- [Accepted at 200% text after scrolling](https://raw.githubusercontent.com/BengaluruBuilders/tailor-my-resume/79a1c784a336cd76489aa13b99f2f70a27379314/design/audits/2026-10-16/screenshots/26-review-font200-after-swipe.png)

## Regression coverage

`TailoredScreenshotTest` covers pending and accepted states at 374 × 834 dp with normal and 200% text. Existing 337 dp large-text captures remain covered. Only the tailored result baselines are recorded.

Existing regression tests cover blocked export before acceptance, explicit acceptance, per-change Undo, Changes-first entry, Resume navigation, and export when no changes exist. The added assertions verify selected-tab semantics and that the accepted footer no longer offers Accept changes.

## Completed checks

- `:feature:tailor:impl:recordRoborazziDebug --tests 'com.tailormyresume.feature.tailor.impl.result.TailoredScreenshotTest'`: 16 tests passed. Inspected normal and 200% captures. Pixel differences above 2% per color channel are confined to the selected tab and pending acceptance button.
- `tools/ci/verify-local.sh`: passed. This includes policy, secret-scan tests, release-configuration tests, workflow tests, formatting, unit tests, coverage, screenshot verification, release lint, dependency guard, and debug and release assembly.

The full gate run used `run-validation build 391`, two Gradle workers, no persistent daemon, and `-Dorg.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=768m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8`. Release-configuration subprocesses used the same limits through `GRADLE_CMD`; the main gate used `GRADLE_OPTS`.

All completed Gradle checks used `-Dorg.gradle.internal.isolated-projects.parallel=false` after the first attempt stalled in parallel project configuration. Isolated projects and configuration caching remained enabled. No project or shared Gradle properties were changed.

## Native evidence

Captured on a private Android 16 / API 36 emulator on port 5594, using 2 GB RAM and two CPU cores. Android display overrides were 1122 × 2502 pixels and 480 dpi, giving the actual 374 × 834 dp viewport. System font scale was 1.0 or 2.0. All PNGs retain the native 1122 × 2502 pixels.

| State | Evidence |
|---|---|
| Pending, normal text; Changes opens first | [Screenshot](pending-font100.png) |
| Export before acceptance; warning and no navigation | [Screenshot](blocked-font100.png) |
| Accepted, normal text; Export PDF is primary | [Screenshot](accepted-font100.png) |
| Pending, 200% text | [Screenshot](pending-font200.png) |
| Undo reached by touch swipe, 200% text | [Screenshot](undo-font200.png) |
| Accepted after scrolling, 200% text | [Screenshot](accepted-font200.png) |
| Successful one-page PDF export after acceptance | [Screenshot](exported-font200.png) |

The native walk also opened Resume with one tap and opened the resume editor through Edit. Per-change Undo restored the original wording; export remained blocked until overall acceptance. Large-text body content was reached with a touch swipe because the automation could not detect a semantic scroll container. This agrees with the audit's manual-swipe caveat; no clipping fix is claimed.

The native APK used command-line audit overrides: `tailormyresumeStagingApiBaseUrl=https://127.0.0.1:1/`, and blank `tailormyresumeStagingWebClientId`, `tailormyresumeStagingFirebaseApiKey`, `tailormyresumeStagingFirebaseAppId`, and `tailormyresumeStagingFirebaseProjectId`. It used synthetic Dev console data. Live authentication, billing, AI, and balances were not tested. No shared configuration or production setting changed. The emulator was shut down inside the same `run-validation emulator 391` session.
