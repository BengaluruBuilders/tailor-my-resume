# MVP build plan and orchestrator runbook

Last update: 2026-10-07. This file lets a new agent take over as orchestrator and build the TailorMyResume
MVP. Read it from top to bottom before you act. When this file and `docs/CONSTITUTION.md` disagree,
the constitution wins. Update section 2 and the chunk table each time a chunk changes state.

## 1. Sources of truth, in order

MVP update, 2026-10-10: the user simplified the MVP and supplied a new design, the prototype in
`design/prototype-2026-10-10/`. Its scope is `docs/PRD.md` section 6.1.2. The chunks below were
planned for the 24-screen MVP and are not yet re-planned for it.

Visual update, 2026-10-07: the user requested the Avvio-inspired direction in
`docs/AVVIO_REDESIGN.md`. It replaces the Jade look in the historical decisions and frame references
below. Product scope and implementation state are unchanged.

1. `docs/CONSTITUTION.md`: rules and their CI gates.
2. `docs/PRD.md`, section 6.1.1: the locked MVP scope.
3. `design/jade-restyle/` (see its `INDEX.md`): the new look, for foundations, Flow 1 and Flow 2.
4. `design/claude-design/flow<N>/`: screen structure, states and copy for Flows 3 to 5, and for
   states that the restyle export does not show. Apply the restyle tokens and components to them.
5. `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`, `docs/MOCK_BACKEND.md`, `docs/REDESIGN.md`.
6. `AGENTS.md`: commands and motion rules.

## 2. Current state

- `main` has all 24 screens on the on-device mock backend. PDF and DOCX export, the share card
  (`TmrFitShareCard`), prep questions and the cover letter exist.
- The app has no network code. Constitution I.5 forbids it until an amendment.
- C0 merged in PR #59. The app has one export template. `ExportTemplate` is removed.
  `ExportRecord.templateName` records "Plain".
- C1 merged in PR #60. It has the Jade restyle tokens and the `Tmr*` components. Fonts are Plus
  Jakarta Sans (variable) and IBM Plex Mono 500. The OFL texts are in
  `core/designsystem/fonts-licenses/`. `TmrButtonSize.Compact` is 44 dp with a 48 dp touch area.
  Pressed jade is #064D3A. The dark sheet-item border is #3A404A. The dock is 76 dp with a stepped
  shadow.
- C0 to C6 are merged: C0 #59, C1 #60, C2 #64, C3 #62, C4 #67, C5 #63, C6 #65.
- Fixes are merged: #66 (the whole application card opens the workspace), #68 (status-bar icons
  follow the top colour through `TmrScreen(lightTop)`), and #70 (the sign-in back arrow shows in dark).
- Docs are merged: #51, #61, #69.
- The final emulator walk-through of all five flows on `main` (929ca73) passed in light, dark and
  200% font. No crash occurred. Its one medium defect is fixed in #70.
- Open lows that stay:
  - The S3b dark under-18 disc needs a dark illustration variant in `core:designsystem`.
  - The inner header 200% layout should move into `TmrInnerHeader` (follow-up task).
  - C5 cosmetic notes: 200% "14 facts" spacing, header strip alignment on S17 and S18, S18 Employer placeholder.
  - C6 notes: the Sign out icon, the small Delete text on S23.

## 3. Locked decisions

| Decision | Value |
|---|---|
| Look | Jade (#0B7A5C) stays the brand. The reference adds coral #B94C37, marigold #FFC94D, 28 dp cards, 48 dp hero bottom corners, 60 dp pill buttons, pill rows, squiggles |
| Home | Applications home with colour cards for saved applications. No job listings, no job search |
| MVP features | F1 to F10 as in PRD 6.1.1: import, gap check, tailoring, PDF and DOCX export with 1 template, applications, prep questions, optional cover letter, free tier plus a 5-application pack at ₹149, account and consent, share card only |
| Cut | Templates 2 and 3, referral credit, group code, ₹49 single application, reminders, kanban |
| Order | Restyle on the mock backend first (C0 to C6), R0 spikes in parallel (C7), then the I.5 amendment and the real backend (C8 to C10) |
| Copy | Never "student", "college", "campus", "fresher", "graduate". Never an ATS score or a guarantee. ASD-STE100 for all prose |

## 4. Design work still open

Flows 3, 4 and 5 have no restyle frames. A screen chunk for these flows can start from the old
frames plus the C1 components. If the user wants restyle frames first, run Claude Design:

1. Open the canvas https://claude.ai/artifact/766P2hFnsULQbnXVeKj3rM chat in the built-in browser
   pane. The chat is "TailorMyResume Android app design" in https://claude.ai/artifacts/design.
2. Type in the reply box. Shift+Enter makes a new line. Enter sends. A run takes 25 to 35 minutes.
   The browser pane cannot upload images, so describe references in words.
3. Send one flow per message. Use this text, changed for the flow:
   "Design Flow 3, export and payment, light mode, same canvas, same tokens and components, one
   artboard per screen state, in a new row titled Flow 3. S12 Export preview: rendering, ready
   with 1 free credit, PDF selected, DOCX selected, no credit, making the file, render error.
   One template only, no template switch. The paywall covers the download, not the preview.
   S13 Application pack: 5 applications for ₹149, credits never expire, Google Play sheet,
   pending, success, cancelled, failed. S14 Exported: credit used, share sheet, mark as Applied.
   S15 Credits and help: no purchases, purchases, refund and help in plain words.
   Same rules: no ATS score, no job listings, never the words student, college, campus, fresher
   or graduate."
   Flow 4: S16 Profile, S17 Fact editor, S18 Guided form, S19 Evidence path (states in
   `design/02-screens.md`). Flow 5: S20 Applications home (colour cards for in-progress
   applications, pill rows for exported ones, dock), S21 Application workspace, S22 Settings,
   S23 Your data, S24 Delete account.
4. Export: read each new file with the Artifact tool (`read`, `paths: ["project/<file>"]`), copy it
   into `design/jade-restyle/`, and add its rows to `INDEX.md`.
5. Review the frames before code: the copy rules in section 3, PRD scope, shape plus word plus
   colour for every status, 48 dp touch targets.

The user's Claude Design plan showed 75% of the weekly limit used on 2026-10-06.

## 5. How to orchestrate

The ready-to-paste prompt for a new orchestrator chat is `docs/orchestrator-prompt.md`. It runs
the screen chunks through the `/parallel-issues-minimax` skill.

### 5.1 Roles

- The orchestrator (Opus) plans, writes each brief, reviews every diff, runs the gates, and
  reports. It does not write bulk code.
- Implementation: an in-harness subagent with `model: sonnet` and `isolation: worktree` for
  multi-file Kotlin work. Use `~/.claude/bin/delegate impl --out <file> "<brief>"` (MiniMax) for a
  single new file or a test file. Use `delegate tests --out` for test authoring.
- Review: Opus reviews code that Sonnet wrote. A Sonnet subagent may review code that MiniMax
  wrote. The last review before merge may run on an Opus subagent whose description starts with
  `final-review:`.
- Security: any diff in C8, C9 or C10 that touches sign-in, tokens, the database, billing or
  personal data stays in-harness and gets the `security-reviewer` agent.

### 5.2 Rules for every chunk

1. One chunk is one branch and one PR (Constitution VI.3). Branch names: `feat/c<N>-<slug>`.
2. Only one Gradle build runs at a time on this Mac, because it also runs a shared CI runner pool.
   Serialise lanes with `lockf -k <scratchpad>/gradle.lock <command>`. Run `./gradlew --stop`
   through the same lock. Never start CPU loops. Keep `org.gradle.workers.max=3`.
3. A new worktree has no `local.properties`. Copy it, or set
   `ANDROID_HOME=/Users/aibuilders/Library/Android/sdk`.
4. Screenshot captures go through `captureScreenHh` in `core/screenshot`. It fixes a Robolectric
   4.16 rounded-corner race. Never raise thresholds to hide drift.
5. A visual change re-records the baselines of each module it changes. A motion change does not.
6. No code comments (Constitution III.2). No mocking library (IV.2). Features use `Tmr*` only (II.5).
7. Gates before a PR: `./gradlew spotlessApply`, the module tests, `verifyRoborazziDebug`,
   `./gradlew lintRelease`, then `tools/ci/verify-local.sh` (warnings are errors there).
8. Ask the user before a push, a PR, or a merge. GitHub auto-merge is not allowed here.
   PR title: `type(scope): summary`. PR body: what, why, how verified, in simple English.
9. Verify UI on the emulator `emulator-5554`. Launch `com.tailormyresume.app/.MainActivity` by name
   (the debug build has two launcher entries). Mobile MCP `type_keys` does not type into Compose
   fields; use `adb shell input text`.

### 5.3 Brief template for an implementation subagent

Give each brief: the goal in one sentence; the files to read first (this file, the constitution,
the frames by path); the exact module scope; what must not change; the acceptance criteria from
the chunk card; the gate commands; the Gradle lock and `ANDROID_HOME` notes; "do not commit or
push"; and the report format (files changed, gate results, open questions, under 400 words).

## 6. Chunks

| # | Chunk | Depends on | Size | State |
|---|---|---|---|---|
| C0 | One export template | none | 2 h | Merged (#59) |
| C1 | Design system restyle | foundations boards | 2 to 3 days | Merged (#60) |
| C2 | Flow 1 screens | C1 | 2 days | Merged (#64) |
| C3 | Flow 2 screens | C1 | 2 days | Merged (#62) |
| C4 | Flow 3 screens | C1 | 1.5 days | Merged (#67) |
| C5 | Flow 4 screens | C1 | 2 days | Merged (#63) |
| C6 | Flow 5 screens | C1 | 2 days | Merged (#65) |
| C7 | R0 spikes | none | 3 days | Ready, needs user input |
| C8 | I.5 amendment and backend | C7, user decision | 8 to 10 weeks | Blocked |
| C9 | Play Billing | C8 | 1 week | Blocked |
| C10 | Release readiness | C8, C9 | 3 weeks elapsed | Blocked |

C2 to C6 touch different feature modules, so they can run in parallel worktrees after C1 merges.
Only the Gradle builds are serialised.

### C0 · One export template

- Goal: PRD 6.5 item 3 says 1 template. Remove `ExportTemplate.COMPACT`, the template switch in
  `ExportPreviewScreen`, its action, its strings and its tests. Keep PDF and DOCX.
- Scope: `feature/tailor/impl` (`document/`, `exportpreview/`, `export/`), tailor tests and baselines.
- Check `ExportRecord.templateName` callers and the dev menu in `:app` for template references.
- Accept: no template choice on screen; both formats export; tests and gates pass.

### C1 · Design system restyle

- Goal: `core:designsystem` matches `design/jade-restyle/` foundations, light and dark.
- Work: token values (colour with a new coral role, shapes, spacing, type); restyle the `Tmr*`
  components; add only what has no equivalent: solid colour card with monogram, pill list row
  (72 dp, 36 dp radius), squiggle set, bottom action bar (60 dp round secondary + 60 dp pill
  primary); dock as a full-width ink bar with 28 dp top corners, active icon in a raised jade
  circle. Keep public APIs; if one changes, fix every caller in the same PR.
- Do not touch motion. Update `docs/DESIGN_SYSTEM.md`.
- Accept: all gates pass; designsystem and feature baselines re-recorded; Opus reviewed the diff.

### C2 · Flow 1 screens (S1 to S7)

- Frames: `design/jade-restyle/S1*` to `S7*`. Modules: `feature/onboarding/impl` (welcome,
  pastejd, signin, consent, importresume, confirmfacts), `feature/analysis/impl`.
- New behaviour: S1 career choice ("Just starting out", "1 to 2 years in") with the line "We use
  this to pick which questions to ask about your projects and internships". Store it and use it to
  order the evidence path questions (S19). Step chips "Step 1 of 5" to "Step 5 of 5".
- S4 copy says an AI provider processes the text and does not name a vendor. S5 has no file size limit text.
- S3 "Continue with Google" needs Google's official mark before release (placeholder until C8).
- Keep existing states that the restyle does not show (offline, daily limit, errors).
- Accept: each frame matches; dev menu forced states still work; gates pass; emulator walk-through
  from Welcome to Gap check with share card.

### C3 · Flow 2 screens (S8 to S11)

- Frames: `design/jade-restyle/S8*` to `S11*`. Module: `feature/tailor/impl`.
- Every changed bullet shows its fact chip. Flagged verb keeps the user's verb. 2 regenerations
  per application. The cover letter is offered after the resume, with "Not now" of equal weight.
- Accept: as C2, walk-through from Tailor to all bullets reviewed, cover letter, prep questions.

### C4, C5, C6 · Flows 3, 4, 5

- Frames: `design/claude-design/flow3`, `flow4`, `flow5` for structure and states; restyle with
  C1 components. If section 4 produced restyle frames, those win.
- C4 module: `feature/tailor/impl` (`exportpreview`, `exported`, `packpurchase`, `credits`). One template only.
- C5 module: `feature/profile/impl` (profile, facteditor, guidedform, evidencepath).
- C6 modules: `feature/applications/impl`, `feature/settings/impl`, dock wiring in `:app`.
  Applications home uses colour cards for in-progress applications and pill rows for exported
  ones, as in the concept board https://claude.ai/artifact/E7cZe1woz5T2ZdM32skJLf.
- Accept: as C2, plus a full emulator walk-through of every flow at the end of C6.

### C7 · R0 spikes (PRD 12.2)

1. PDF text: export a resume with `AndroidPdfResumeRenderer`, run `pdftotext` on it, and compare the
   text with the approved JSON. Record the result in PRD open question 5.
2. Extraction: the user supplies 10 real resumes with consent. Measure field-level F1 of the
   extraction prompt. Do not commit the resumes.
3. Model cost: measure tokens per full application for each pipeline step and convert to ₹ with
   the rates in server config. Target ₹7 median.
- Output: `docs/research/07-r0-spikes.md`.

### C8 · Constitution amendment and backend (PRD 10.1)

Each item is its own PR, in this order. All stay in-harness with a security review.

1. Amendment PR: change I.5 to allow network access to the TailorMyResume backend only, name the
   consent screen S4, keep backups off and analytics off unless the optional switch is on.
2. Google sign-in with Credential Manager; Firebase Auth turns it into the ID token for the backend.
3. Neon Postgres schema in `apps-backend`, with migrations committed there.
4. TailorMyResume routes in `apps-backend` as the model gateway, one route per step: extract resume, analyse JD,
   tailor, verify, prep questions, cover letter. `store: false`. Model IDs and prices in config.
5. Guardrails: the deterministic checks of PRD 6.4 item 4.3 on the server, a verifier on a
   different model, one repair pass, fall back to the original bullet.
6. App side: replace each mock binding in `core:domain` with a backend binding (Constitution
   II.6), WorkManager for queued jobs and sync, Room stays the local source of truth.
7. Evaluation set of PRD 10.3 and its release gate (0 critical fabrications).
8. Server-side data export and deletion, the web deletion link, security logs for 1 year.

C8 status, 2026-10-08. App side: the I.5 amendment, the `demo` and `prod` flavours, `core:network`,
content reports posted to the backend (retry on the next app start), and `server.json` in the data
export are built and tested against a fake server. Not yet verified against a live server.
Backend: the TailorMyResume routes are open PRs #41 to #50 in `BengaluruBuilders/apps-backend` and are not
deployed. Purchase verification (#33) is not started, so C9 stays blocked.

### C9 · Play Billing

Consumable product for the 5-application pack. Server-side purchase verification. Credits never
expire. Refund and help inside the app.

### C10 · Release readiness

Data safety form, privacy policy, terms, grievance contact, support email, web delete-account URL
(the user must supply the email and URLs), target API 36, Play Integrity, closed test with 12 or
more testers for 14 days, legal check of the consent copy.

## 7. Open items for the user

1. Real resumes for C7 step 2, with consent.
2. Support email, privacy policy URL, delete-account URL.
3. Personal or organisation Play developer account.
4. Restyle frames for Flows 3 to 5 now (section 4), or build them from the old frames.
5. Fonts: done in C1 (PR #60). The app ships Plus Jakarta Sans and IBM Plex Mono.

## 8. Decision log

| Date | Chunk | Decision | Reason |
|---|---|---|---|
| 2026-10-06 | C1 | Use the google/fonts files as shipped. Plus Jakarta Sans is a variable TTF, wired with `FontVariation` weights 400, 600, 700, 800. | Only the google/fonts download was authorized; no font tool is installed. |
| 2026-10-06 | C1 | Add the compact button, pressed jade and the dark sheet-item border in C1. | Screen chunks need them; this is smaller than a detour in C2. |
| 2026-10-07 | C1 | Error stays coral #B94C37. | Every error path pairs an icon or a word with the colour; the frame lists coral for errors. |
| 2026-10-07 | C1 | The dock follows the frame now: 76 dp and a shadow. | C2 to C6 take their bottom padding from `TmrDockDefaults.inset`; a later change would re-record every screen. |
| 2026-10-07 | C1 | The dock shadow is drawn as stepped rings in one layer, not a blur. | Robolectric baselines must match on macOS and on the Linux CI runner. |
| 2026-10-07 | C1 | Accept the wide dock shadow halo as an open low finding. | The extra final review was used; the issue is cosmetic; C6 tunes it on the emulator. |
| 2026-10-07 | C0 | Remove SPACIOUS and the `ExportTemplate` enum, not only COMPACT. Keep `templateName` and record "Plain". | One template remains; old records still show a name. |
| 2026-10-07 | C0 | No emulator walk-through for C0. | It is a behaviour chunk; screenshot tests cover the export preview; the full walk-through runs at C6. |
| 2026-10-07 | C2 to C6 | A lane driver may write strings XML and small glue edits itself. If 8 MiniMax calls do not finish a chunk, the PR opens with the unmet list and Coder pass 2 continues. | Screen chunks need more files than 8 calls; the budget stays per Coder pass. |
| 2026-10-07 | docs | A docs-only record PR gets a lead read of the diff, not an Opus review. | It has no code; the lead read meets the review condition of the merge rule. |
| 2026-10-07 | C3 | Regenerate stays per section. | It keeps the existing behaviour and tests; the PRD counts 2 regenerations per application either way. |
| 2026-10-07 | C3 | Export stays blocked until every line is reviewed. | Constitution I.2: nothing reaches an export without an explicit accept. The constitution wins over frame S8b. |
| 2026-10-07 | C3 | Keep the centred `TmrInnerHeader`. | The frame is left-aligned, but that needs a `core:designsystem` change outside C3; this is the smaller option. |
| 2026-10-07 | C3 | Fact-sheet page numbers stay out. | `TailoredBulletSource` and `FactSource` carry no page data; adding it needs `core:model`. |
| 2026-10-07 | C2 | The small time overrun ends Coder pass 1. It is not a block. | The pass stopped at the budget and opened the PR with the unmet list, as the lane rule says. |
| 2026-10-07 | C2 | The JD minimum stays at 20 words. The hint shows the code value, not the 80 of the frame. | It keeps the existing behaviour and tests; the frame decides the look only. |
| 2026-10-07 | C2 | Drop the S4 "Help improve TailorMyResume" row. | Constitution I.5 forbids analytics until an amendment. |
| 2026-10-07 | C2 | Drop the S3 role line, the S2b location chip and the referral field. | The state holds no such data; the app does not parse location; referral credit is cut from the MVP (PRD 6.1.1). |
| 2026-10-07 | C2 | The career choice is optional. | This is the conservative option; Continue keeps working as before. |
| 2026-10-07 | C5 | Restore the per-fact status chips on the profile home cards. | The status rule (shape, word and colour) wins over a cleaner card. |
| 2026-10-07 | C5 | Fix the vertical padding of `TmrPillRow` inside the C5 PR with one `core:designsystem` modifier. | 200% font clipping fails a C5 criterion; this is smaller than a separate designsystem PR. The affected baselines are re-recorded. |
| 2026-10-07 | C5 | Ordering the evidence path by career choice is a C2 item. | C2 PR #64 builds it. |
| 2026-10-07 | C2 | S6 fact cards have no Remove action. Confirm is full width. Delete the unused string. | `main` has no remove behaviour; adding one is new scope. |
| 2026-10-07 | C6 | The home pill rows show exported applications (`ExportHistoryRepository`). The cards show applications that are not exported. | This follows the MVP_PLAN card text, and the data exists. |
| 2026-10-07 | C6 | Accept three C6 defects. A pill row tap no longer opens the status sheet. The offline delete pill keeps the press scale. Rejected and Saved share coral. | After the export split the workspace keeps the status chip. A fix needs `core:designsystem`. The word and the icon still differ. |
| 2026-10-07 | C3 | The prep gap button label states the true action: "Open prep plan". | The rule against untrue claims wins over the frame copy. |
| 2026-10-07 | C3 | Keep "Closes #55". Merge only after the emulator walk-through passes. | The merge rule makes the last criterion true at merge time. |
| 2026-10-07 | C3 | The file-name wrap and the company monogram stay open lows. C4 fixes them. | The extra final review was used; both are lows; the C4 lane works in the same module. |
| 2026-10-07 | C2 | S7c keeps one "New fact" path. S7d has no Save image. S5b has no "page 2 of 2". | The state has no confirmed-fact list, no save action and no page count; adding them is new scope. |
| 2026-10-07 | C3 | Remove the `tools:ignore PluralsCandidate` suppressions from the PR and use plurals. Fix the two open lows now. | Constitution VII.4: a suppression is an amendment. A fix pass is needed anyway, so the lows cost little now. |
| 2026-10-07 | C6 | The C6 Fixer tunes the dock halo in `TmrDock` (maximum reach 24 dp) and re-records the dock baselines. | MVP_PLAN section 2 assigns it to C6; a constant change is small. |
| 2026-10-07 | C5 | The four cosmetic walk-through notes become listed open lows. They do not start a new pass. | No criterion fails; the final review budget is used. |
| 2026-10-07 | C6 | PR #65 merges after a Flow 5 walk-through. The all-flows walk-through runs once on `main` after the last chunk merges. | The goal names one final walk-through of all flows; running it per PR would repeat it on heads that change again. |
| 2026-10-07 | All | After a pure "merge main and re-record baselines" update, verify with CI green, a scope check and a lead view of the changed baselines. Do not run a new emulator walk. | The delta has no behaviour change; the earlier walk-through still holds. |
| 2026-10-07 | C3 | Fix the 200% header at feature level (the `TailorScreen` pattern), not in `TmrInnerHeader`. | It keeps the scope and avoids re-recording every module now. The root fix in `TmrInnerHeader` is a follow-up. |
| 2026-10-07 | C6 | Merge with the card-body tap regression as a listed low. Fix it in a small follow-up PR in this run. | The arrow and TalkBack still open the workspace; a fix cycle would push C2 and C3 behind `main` again. |
| 2026-10-07 | PR #66 | Verify with CI, a scope check and the click test. The final all-flows walk-through on `main` covers the tap. | It is a 2-file behaviour fix with a direct test. |
| 2026-10-07 | C2 | Under-18 also clears the stored career choice. | The screen says "Nothing was kept"; truthful copy wins. It is one line and a test. |
| 2026-10-07 | App | The status-bar icon contrast is an `:app` issue on `main` (`SystemBarStyle.dark` is always used). Fix it in a separate small PR, not in C2. | It was there before C2; one concern per PR (VI.3). |
| 2026-10-07 | C4 | The hero cards on Exported and Credits use marigold, not coral. | Coral is also the error colour (C1); a coral success card can read as an error. |
| 2026-10-07 | C4 | Keep the "Get an application pack" row visible and disabled offline. | It keeps the existing behaviour; this is the smaller change. |
| 2026-10-07 | C4 | Keep "Price includes GST" and "No subscription. Nothing renews." at 200%. | The price disclosure must stay; truthful copy wins over the frame. |
| 2026-10-07 | Status bar | Add an explicit `TmrScreen` `lightTop` flag. The default is header == null. Profile is false. The C2 consent and import screens are true. The fixer runs after C2 merges. | A header does not tell the top colour; the defect lives on the C2 branch. |
| 2026-10-07 | All | Use `gh pr update-branch` when a PR is behind `main` only by a docs PR. | It makes a merge commit on the server. It needs no force push and no local Gradle build. CI runs again on the new head. |
| 2026-10-07 | C4 | Remove the ₹49 single-application option from S13. | PRD 6.1.1 cuts the single-application product. The MVP has the 5-application pack only. |
| 2026-10-07 | C4 | Accept ₹149 and "5 applications" in the hero and on the button. Accept the "DOCX · Plain" meta. | The hero and the button have different roles. "Plain" follows the C0 decision. |
| 2026-10-07 | C4 | The S13 pending and cancelled states stay as a listed low if `main` could not force them either. | The criterion is to keep the states that were forceable before. The fixer checked `main` first. |
| 2026-10-07 | C4 | Merge after the walk-through at d46fb48 and the tested fixes. Do not run a second C4 walk. | The same rule as C2. The all-flows walk-through on `main` follows at once. |
| 2026-10-07 | Final | Fix the S3 dark back arrow in a small PR now. List the 10 lows in this file as follow-ups. S5 and S6 "Step 4 of 5" matches the frames. | The medium defect fails the dark-mode criterion. The lows are cosmetic or need product input. |

## 9. Run record (2026-10-06 to 2026-10-07)

All lanes used a separate worktree and a separate branch. Every PR got a lead read of the diff.
Code that Sonnet wrote was reviewed by Opus (Reviewer B). In the table, "Reviewer A" is the first
review of MiniMax code. "Reviewer B" is the final review. C2 and C6 also had a security review.

### 9.1 Per-issue table

| Issue | Chunk | Worktree/branch | PR and final head | Reviewer A | Coder pass 2 | Reviewer B | Fixer | CI | Performance | Can close |
|---|---|---|---|---|---|---|---|---|---|---|
| #53 | C0 | `c0-one-export-template` / `feat/c0-one-export-template` | #59, 2ba9a2c | 3 low, all confirmed | Fixed the 3 low findings | 1 low (brittle test) | Changed one test line | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #52 | C1 | `agent-a1e7062fb357f0b2f` / `feat/c1-designsystem-restyle` | #60, 1b3e385 | Skipped (Sonnet code) | None | 1 high, 2 medium, 3 low; round 2: 2 medium, 1 low | Fixed 6 findings, then 3 more, then a CI dock-shadow fix | Green after the dock fix | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #54 | C2 | `c2-first-run` / `feat/c2-first-run` | #64, 907665a | 2 high, 15 medium, and a set of lows | Fixed all findings and S7a to S7d | Security: 0 high, 2 medium, 2 low. B: 1 medium (under-18 job text kept), 8 lows. Round 2: 1 medium, 2 low | 4 fix passes, including the under-18 clear and the career-choice clear | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #55 | C3 | `tailor-stack` / `feat/c3-tailoring` | #62, dca9303 | 3 high, 5 medium, 5 low | Fixed 13 findings and the S10a row | 3 medium, 6 low; round 2: 3 low | 5 fix passes, including 17 lint suppressions removed and the 200% headers | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #56 | C4 | `tailor-stack` / `feat/c4-export-payment` | #67, 093c16c | 4 medium, 4 low | Fixed 8 findings and the lead decisions | 2 low and 1 nit | 2 fix passes, including the removal of the ₹49 option | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #57 | C5 | `c5-profile` / `feat/c5-profile` | #63, a68dd2f | 1 high, 5 medium, 2 low | Fixed 8 findings | 3 medium, 2 low | Fixed 5 findings | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| #58 | C6 | `c6-workspace-settings` / `feat/c6-workspace-settings` | #65, 50a9ae5 | 1 high, 3 medium, 3 low | Fixed the export split and the ledger spacing | Security: 1 low. B: 1 medium, 1 low | 2 fix passes, including the dock halo | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths. One extra `observeExports()` flow combined in `ApplicationsViewModel` | Closed |
| — | Fix | `applications-card-tap` / `fix/applications-card-tap` | #66, 2c68b75 | None | None | No findings | Merged `main` | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| — | Fix | `status-bar-contrast` / `fix/status-bar-contrast` | #68, 422f854 | None | None | Not ready (wrong top colour on Profile); round 2: 1 low | Added the `lightTop` flag and 5 tests | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |
| — | Fix | `signin-back-dark` / `fix/signin-back-button-dark` | #70, a5204e6 | None | None | No findings | None | Green | Not material: UI restyle on the mock backend; no new data access or I/O paths | Closed |

### 9.2 Blocked items

None of the lanes was blocked. These items were not done:

- C7 to C10 are out of scope for this run. They need accounts, keys and legal input from the user.

### 9.3 Open follow-ups

All are low severity. They come from reviews and walk-throughs.

- `TmrInnerHeader` 200% layout should move into the component. A follow-up task exists.
- S3b dark: the under-18 disc needs a dark illustration variant in `core:designsystem`.
- S3 at 200% in dark: the Privacy Notice line overlaps "I'm under 18".
- S7 sticky footer says "You have 0 free" while Credits shows credits. Check the real flow.
- S7 "Open full analysis" expands the rows in place.
- S1 hero cards break words at 200%.
- S8 sticky note takes about 40% of the viewport at 200%.
- S16 "13 facts" touches "Add evidence" at 200%.
- S17 and S18 header strip alignment.
- S18 Employer placeholder.
- S10 and S11 header monogram.
- Small arrow icons on "Preview export" and "Paste a job".
- S19 evidence-path copy leans toward study terms. It has no banned word.
- Settings Sign out icon.
- Small Delete text on S23.
- S13 pending and cancelled states cannot be forced from the debug menu. This was also true on `main` before.
- Release gate: the privacy-policy and web-delete addresses show "Address not set in this build".

### 9.4 Lessons for the next run

- A stale `gh pr checks` rollup can show a skipped duplicate run. Read the run where "Build, lint, and unit tests" ran.
- `gh pr update-branch` sometimes starts no CI. Close the PR and open it again.
- Each merge puts the other open PRs behind `main`. Merge in a planned order.
- Reviewers must view the Font200 baselines of every changed screen.
- Lane briefs must forbid `tools:ignore` from the start.

### 9.5 Next actions for C7 to C10 (need the user)

- C7, R0 spikes: the user gives 10 real resumes and agrees to their use.
- C8: the user decides the I.5 amendment. The user gives the Firebase project and the Fly.io and Neon accounts, the Anthropic API key (backend only) and the Google sign-in client.
- C9: the user opens a Play developer account and makes the 5-pack product in Play Console.
- C10: the user gives the support email, the privacy policy URL and the delete-account URL. The user also fills the data safety form, runs the closed test (12 or more testers, 14 days) and orders a legal check of the consent copy.
