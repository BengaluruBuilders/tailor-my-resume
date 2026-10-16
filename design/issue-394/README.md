# Sign-in stories at small sizes and large text

Fixes [issue #394](https://github.com/BengaluruBuilders/tailor-my-resume/issues/394).

The compact story layout puts the headline and explanation before the mascot and gesture hint.
It applies below 400 dp of available story height or above 130% system text. The mascot uses
96 dp in this layout. Text keeps the user's system scale. The Google button and legal links
stay below the bounded, scrollable story area.

Scrolling does not change the selected story. Automatic story changes pause while the story
is scrolled, so the user can read its explanation. A story change returns its scroll position
to the top and allows automatic changes again. The regular layout and Google-only sign-in remain.

## Regression coverage

- `SignInResponsiveLayoutTest`: all three stories at six viewport sizes, including
  320 × 568 dp and 374 × 834 dp, at 100%, 150%, and 200% text. It checks swipe movement,
  explanation reachability, and separation from the Google button and legal links.
- `SignInScreenTest`: reading a scrolled explanation pauses automatic changes; choosing the
  next story allows automatic changes again. Existing tap, hold, Google, and legal-link tests remain.
- `SignInConstrainedScreenshotTest`: each story at both requested viewports and at 100% and
  200% text, before scrolling and with its explanation brought into view.

## Before evidence

The issue contains the published original captures and reproduction recording. For example:

![Before: 320 × 568 dp, 200% text, explanation missing](https://raw.githubusercontent.com/BengaluruBuilders/tailor-my-resume/c8b926f0cc5e80c06919e89fd6a4cfcb767d9874/design/audits/2026-10-16/screenshots/33-signin-small-font200.png)

## Constrained screenshot evidence

Each link shows that story's explanation after scrolling it into view. The screenshot tests also
capture the initial view for each cell. Baselines use native font rendering and stay at the
requested viewport height: 960 × 1704 px or 1122 × 2502 px at density 480.

| Viewport | Text | Story 1, including preparation | Story 2 | Story 3 |
|---|---|---|---|---|
| 320 × 568 dp | 100% | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story1_explanation_small-320x568_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story2_explanation_small-320x568_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story3_explanation_small-320x568_dark.png) |
| 320 × 568 dp | 200% | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story1_explanation_small-320x568-font-200_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story2_explanation_small-320x568-font-200_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story3_explanation_small-320x568-font-200_dark.png) |
| 374 × 834 dp | 100% | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story1_explanation_prototype_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story2_explanation_prototype_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story3_explanation_prototype_dark.png) |
| 374 × 834 dp | 200% | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story1_explanation_prototype-font-200_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story2_explanation_prototype-font-200_dark.png) | [Capture](../../feature/onboarding/impl/src/test/screenshots/signin_constrained_story3_explanation_prototype-font-200_dark.png) |

The four existing narrow large-text baselines are updated. The normal prototype baselines remain
unchanged: the local re-recording differed only by a maximum of two channel values at antialiased
edges, within the existing comparator tolerance.

## Build settings

The initial heavy checks used `/tmp/tmr-visual-fixes-heavy.lock`, no persistent Gradle daemon,
and at most two workers. After the shared coordination update, new operations use
`.worktrees/run-validation` with one operation in this worktree at a time. The emulator uses
2 GB RAM and two CPU cores. Command-local `-Dorg.gradle.internal.isolated-projects.parallel=false` avoids a
Gradle 9.8 included-build configuration deadlock. Command-local
`-Dorg.gradle.configuration-cache.parallel=false` serializes configuration-cache access. Project
isolation and configuration caching stay enabled. No shared Gradle properties change.

## Verification results

- `tools/ci/verify-local.sh`: passed after the scroll observer used `derivedStateOf` to observe
  only the reading state. No lint suppression or baseline exception was added.
- 2,740 Android unit tests: zero failures or errors. This includes the native-rendered viewport
  and swipe matrix and the manual-clock pause/resume regression.
- `verifyRoborazziDebug`, coverage, formatting, release lint, dependency guard, and debug/release
  assembly: passed. The final combined Gradle run completed 2,063 tasks.
- The final run used the shared `run-validation build 394` wrapper, `--max-workers=2`,
  `-Dorg.gradle.daemon=false`, `-Dorg.gradle.jvmargs=-Xmx2g`, and
  `JAVA_TOOL_OPTIONS='-XX:MaxMetaspaceSize=768m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'`.

## Android 16 runtime check

Agent-device drove a private API 36 AVD on `emulator-5592`, with 2 GB RAM and two CPU cores,
under `run-validation emulator 394` from boot through shutdown. It installed the debug APK from
the passing verification run. No Firebase or API build overrides were used, and no Google sign-in
was attempted. The emulator was shut down and its validation slot was released.

| Viewport | Text | Native evidence |
|---|---|---|
| 320 × 568 dp | 100% | [Initial view](evidence/native-small-font100.png) |
| 320 × 568 dp | 200% | [Headline](evidence/native-small-font200-initial.png), [preparation cue](evidence/native-small-font200-preparation.png), [second explanation](evidence/native-small-font200-amber-body.png), third explanation [start](evidence/native-small-font200-lime-body-start.png), [middle](evidence/native-small-font200-lime-body-middle.png), [end](evidence/native-small-font200-lime-bottom.png) |
| 374 × 834 dp | 100% | [Initial view](evidence/native-prototype-font100.png) |
| 374 × 834 dp | 200% | [Headline and preparation](evidence/native-prototype-font200.png) |

The small-screen large-text check stepped through all three stories and used upward swipes and
slow pans within the story region. The preparation cue, each description, and the last word of
the longest description were reachable. The selected story stayed in place while scrolled.
The Google button and legal links kept their positions below the story viewport.

The native automation snapshot exposed only the Google control. Coordinate gestures therefore
used the visible story bounds; screenshots were inspected as pixels. Automated text, scroll-range,
overlap, and timer assertions use native-rendered Compose tests. The longest description is taller
than the native story viewport at 200%, so its runtime evidence spans successive scroll positions.
