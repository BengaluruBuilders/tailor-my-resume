# TailorMyResume

TailorMyResume is an Android app. It turns a candidate's confirmed background and a job description into a
tailored application. It never invents facts about the candidate.

## Read these first

0. `design/prototype-2026-10-10/README.md` and `docs/PRD.md` section 6.1.2 — the simplified MVP
   and its design, from 2026-10-10. They win over older design and scope documents.
1. `docs/CONSTITUTION.md` — the rules and the CI gate for each rule. It wins every conflict.
2. `docs/ARCHITECTURE.md` — modules, models, interfaces, and navigation.
3. `docs/PRD.md` — product scope and release gates.
4. `docs/REDESIGN.md` — the rules and the procedure for the "Friendly hero, Jade" design.
5. `docs/DESIGN_SYSTEM.md` — the tokens and the `Tmr*` components in `core:designsystem`.
6. `docs/MOCK_BACKEND.md` — the interfaces and the on-device state behind every screen.
7. `docs/MVP_PLAN.md` — the MVP build plan, chunk state, and the orchestrator runbook. Start here
   if you are asked to continue the MVP build.

The Jade restyle frames (foundations, Flows 1 and 2) are in `design/jade-restyle/`. They win over
`design/claude-design/` where both show the same screen.

## Design source

**Newest, 2026-10-10:** the clickable prototype in `design/prototype-2026-10-10/` (read its `README.md`)
is the current design and the simplified MVP flow. Its scope is in `docs/PRD.md` section 6.1.2, which
wins over 6.1.1. Where it and the Avvio canvas disagree, the prototype wins. The app does not implement
it yet. Use the Avvio canvas and the older frames only for states the prototype does not draw.
Cut by the owner on 2026-10-10, never to be raised again: consent screen, 18+ check, light theme,
Apple sign-in, follow-up reminders, offline states, and error states beyond the five the prototype
draws (PRD 6.1.2).

The previous visual direction is the Claude Design canvas in `design/avvio-canvas/README.md`
(dark first, lime, Manrope and Archivo Black), requested on 2026-10-07. It covers all 24 screens in
dark and light. It, `docs/AVVIO_REDESIGN.md` and `docs/DESIGN_SYSTEM.md` supersede the Jade appearance
rules and frame styling below. The older exported frames still document states and copy that the
canvas leaves out. Keep them read-only.

If a task changes UI, read the frames for that flow before you write code.

| Need | File |
|---|---|
| **Current design and MVP flow (wins first)** | `design/prototype-2026-10-10/README.md`, then `Prototype.dc.html` |
| Previous look (Avvio) | `design/avvio-canvas/README.md`, then `flow<N>-*.md` outlines |
| Older restyle | `design/jade-restyle/INDEX.md`, then the frames it lists; rules in `docs/REDESIGN.md` section 0 |
| Tokens: type, colour, surface, shape, spacing (older Jade board) | `design/claude-design/foundations/Main.dc.html`, `Colour.dc.html`, `Surface.dc.html` |
| Motion registers `proof` and `hop` | `design/claude-design/foundations/Motion.dc.html` |
| Characters and spot poses | `design/claude-design/foundations/Illustration.dc.html` |
| Components, light and dark | `design/claude-design/foundations/ComponentsLight.dc.html`, `ComponentsDark.dc.html` |
| Screens and states of flow N | `design/claude-design/flow<N>/INDEX.md`, then the frames it lists |
| How the folder works | `design/claude-design/README.md` |
| Direction summary and screen map | `design/01-foundations.md`, `design/02-screens.md` |

| Flow | Screens | Folder |
|---|---|---|
| 1. First run | S1 to S7 | `flow1` |
| 2. Tailoring | S8 to S11 | `flow2` |
| 3. Export and payment | S12 to S15 | `flow3` |
| 4. Profile | S16 to S19 | `flow4` |
| 5. Workspace and account | S20 to S24 | `flow5` |

Each frame has a light file and a dark file. Read the frames as text.
To see a frame, run `python3 -m http.server 8766` in `design/claude-design`, then open
`http://127.0.0.1:8766/flow1/<file>` in a browser.
The files are an export of the Claude Design canvas. Do not edit them by hand.
If the design changes, export the frames again and replace the folder in one PR.

## Motion

- Two registers only: `proof` (default) and `hop` (gap closed, exported, pack purchased, first fact confirmed).
- Read specs from `TmrTheme.motion`. Do not add a duration scale or an easing scale.
- Motion code lives in `core:designsystem`. Navigation wiring lives in `:app`.
- A feature calls an `Tmr*` primitive. It never calls `tween(`, `spring(`, or a numeric duration.
- If `TmrTheme.motion.reduced` is true, nothing moves. The state change stays visible.
- In a lazy list, animate `graphicsLayer` alpha, translation, and scale only.
- Do not build the multi-frame sequences of the board.
- A settled frame must not change. Do not record a screenshot baseline again for a motion change.
- The table of needs, primitives, and tokens is in `docs/DESIGN_SYSTEM.md`, section "Motion".

## Commands

| Task | Command |
|---|---|
| Run every CI gate on your machine | `tools/ci/verify-local.sh` |
| Run the constitution policy check only | `tools/ci/check-constitution.sh` |
| Fix formatting | `./gradlew spotlessApply` |
| Run unit tests | `./gradlew testDebugUnitTest :app:testDemoDebugUnitTest :app:testProdDebugUnitTest` |
| Record screenshot baselines for one module | `./gradlew :feature:<name>:impl:recordRoborazziDebug` |
| Verify screenshot baselines | `./gradlew verifyRoborazziDebug` |
| Run one test class | `./gradlew :core:domain:testDebugUnitTest --tests "com.tailormyresume.core.domain.TailorResumeUseCaseTest"` |
| Run lint | `./gradlew lintRelease :app:lintDemoRelease :app:lintProdRelease` |
| Check the release classpath | `./gradlew dependencyGuard` |
| Update the release classpath baseline | `./gradlew dependencyGuardBaseline` |
| Build the offline debug APK | `./gradlew assembleDemoDebug` |
| Build the backend debug APK | `./gradlew assembleProdDebug` |
| Install the pre-push hook | `tools/setup.sh` |
| Stop Gradle daemons when you finish | `./gradlew --stop` |

## Flavours

`:app` has the flavour dimension `backend`. `demo` is offline and has no `INTERNET` permission. `prod` talks to
the TailorMyResume backend (Constitution I.5). The debug build below belongs to `demo`.

## Debug build

The debug build installs a second launcher entry, "TailorMyResume dev". It loads sample data, resets the app,
and switches the app offline. It also opens any screen in a forced state.

## Continuous integration

CI runs the same gates as `tools/ci/verify-local.sh`, plus the PR title check and a secret scan.
CI compiles Kotlin with warnings as errors. Local builds do not, so run `tools/ci/verify-local.sh`.
The workflows are in `.github/workflows`.
