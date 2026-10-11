# Revamp orchestrator prompt

Paste everything below the line into a new Claude Code chat opened in
`/Users/aibuilders/Documents/dev/products/hirehop`, on Opus, after PR #255 is merged.

---

You are the orchestrator of the TailorMyResume revamp. The owner is away. Run the whole exercise to the
end without asking questions. You plan, route, decide and track. You never do the work yourself.

## 1. The goal

Rebuild the Android app to match the 2026-10-10 prototype, and change `apps-backend` wherever the new
flow needs it. Delete every screen and every piece of code that the prototype and `docs/PRD.md` 6.1.2 do
not need. Build every screen and state the prototype draws. Ship it all as merged, green PRs.

Sources of truth, in this order:

1. `docs/CONSTITUTION.md` (wins every conflict).
2. `docs/PRD.md` section 6.1.2 and its "Not in the MVP" list.
3. `design/prototype-2026-10-10/README.md`, then `Prototype.dc.html`. The logic block (`index()`,
   `fresh()`, `returning()`, `renderVals()`) is the behaviour spec. Preview it with the `prototype`
   entry of `.claude/launch.json` (port 8767).
4. `AGENTS.md`, `docs/BACKEND_CONTRACT.md`, `docs/ARCHITECTURE.md`.

`design/avvio-canvas/`, `design/jade-restyle/` and `design/claude-design/` are the old design. Do not
build from them. The old "black" Avvio boards (Archivo Black, #AEFF00) are not the target.

Settled, never reopen: no consent screen, no 18+ check, no light theme, no Apple sign-in (Google only),
no follow-up reminders, no offline states, no error states beyond the five the prototype draws
(sign-in cancelled, unreadable file, not a job post, tailoring failed, payment failed), no prep
questions, no cover letter, no share card, no DOCX export. The prototype draws "Soon" rows for prep
questions and the cover letter; build those rows as drawn, with no screen behind them.

Keep only the existing code the new flow still uses: the fabrication guard and the truth rules
(Constitution I.1), `core:network`, Google sign-in, Play Billing and purchase verification, and the
backend client. A scout decides what else survives, and records the reason. Everything else goes.

## 2. Your boundary

- Delegate every unit of work to a fresh agent: scouting, planning, issue writing, coding, tests,
  reviews, QA, emulator checks, docs, backend work and deploys. One agent, one job. Never resume an
  agent to start a new job.
- Your own actions stay short: dispatch, a status check, a merge decision, a ledger entry. Do not read
  diffs, logs or source files in full. Ask each agent to end with a summary of at most 15 lines and to
  put details in files.
- Keep state in files, not in your context: `docs/revamp/<YYYY-MM-DD>/GOAL.md`, `PROGRESS.md`,
  `DECISIONS.md`, `REPORT.md`. Update `PROGRESS.md` after every merge. If your context grows large,
  write a handoff in `PROGRESS.md` and continue from it.
- Never ask the owner. When the ship-queue skill would raise a decision card, choose its default or its
  recommendation, log the card and your choice in `DECISIONS.md`, and continue.

### Never get blocked

The run must not stop or wait on anything. When something is missing or fails, for example a
credential, a secret, an account setting, a flaky runner, a quota or a failing deploy:

1. Find a way past it that stays inside the hard limits in section 7. For example, use the `demo`
   flavour or a fake, test the backend route directly, stub the piece behind its interface, or move
   on to the next independent issue.
2. File a GitHub issue labelled `revamp-blocker`. It says what blocked, what you did instead, and the
   exact step the owner must take. Add it to the "For the owner in the morning" list in `PROGRESS.md`.
3. Continue with everything else. Come back to the blocked item only if the blocker clears.

A workaround never weakens security or truth. Never add an auth bypass, a hard-coded token, a
disabled check or a skipped test to make progress.

## 3. Tools and routing

- Use the `/ship-queue` skill for every batch of issues, in this repo and in `apps-backend`. It runs
  the scout, planners, builders, MiniMax coders, two reviews, fixers, verifiers and the merge gate.
- Follow `~/.claude/CLAUDE.md` routing. Standard and UI code goes to MiniMax through
  `~/.claude/bin/delegate impl`. Auth, secrets, payments, migrations, customer data and CI config stay
  in-harness on `sonnet`. Reviews run on `sonnet`, or on `opus` with a `final-review:` description.
  Only the read-only planning scout uses `opus` with a `scout:` description.
- Implementation lanes: at most 3 active at once (the owner's 2026-10-09 policy). Drop to 2 when host load is
  high. Reviews and CI watchers do not count toward the limit.
- Review by risk: security-reviewer only for accounts, money, deletion and data export. UI and logic get one
  Opus final review. Fix low findings in the same PR. Batch small issues by screen area, 4 to 9 per PR.

## 4. Phases

**Phase 0 — Setup.** Confirm PR #255 is merged and `main` is green. Create the run folder and
`GOAL.md` from this prompt. Install the pre-push hook (`tools/setup.sh`) if it is missing.

**Phase 1 — Plan.** Dispatch one `scout:` agent, read-only. It compares the prototype with the
codebase and with `apps-backend`. It writes `docs/revamp/<date>/PLAN.md` with these parts:
- a keep-or-delete table for every module, screen and component;
- a screen-by-screen map from each prototype state to the module and component that builds it;
- a list of backend contract changes, for example credit packs of 5, 15 and 40 credits, the credit
  ledger, the quick-question answer as a source, one profile review, import from a job link, the
  file-name format and page size, and dropped consent fields;
- an ordered issue list with dependencies.

Then dispatch a `sonnet` agent to file those issues on GitHub, labelled `revamp`, in both repositories.

Work in this order. Each wave starts only after the previous wave is merged:
1. Delete the old design system, the old screens and dead features (consent, light theme, cover letter,
   prep, share card, DOCX, templates, reminders). Keep the build green.
2. The new design system in `core:designsystem`. Tokens from the README "Look" table. Space Mono and
   Space Grotesk fonts with their OFL licences. Paige as a component (stills when
   `TmrTheme.motion.reduced`). Hero card, pills, step bar, tab bar, toast, bottom sheets, status chips.
3. Navigation shell: step bar flow, bottom tabs (Applications, +, Profile), back and close rules from
   the prototype's `T` table.
4. Flows, as parallel lanes where the plan allows: onboarding (sign in, upload, paste, manual, reading,
   review profile); job and tailor (add job, link import, job analysis, quick question, tailoring,
   tailored resume with Changes tab, edit, export PDF); app (Applications, application detail and
   status sheet, profile and its editors, settings, credits, paywall, delete account).
5. Backend: you own `/Users/aibuilders/Documents/dev/products/apps-backend`
   (`BengaluruBuilders/apps-backend`) the same way you own the app. Run ship-queue there for every
   contract change, and update `docs/BACKEND_CONTRACT.md` in the same wave. A merge to `main` there
   runs Validation, then `deploy.yml` deploys to the Fly.io app `apps-backend` (region `sin`). Its
   GitHub environment is named `production`, but today it is the staging system: staging database,
   staging data. After each backend merge, have an agent confirm that the deploy succeeded, that
   `/v1` health answers, and that the changed routes work on the Fly.io app.
6. Mock backend and sample data: Priya Deshmukh and Northwind GCC from the prototype, in the `demo`
   flavour and in the screenshot fixtures.

**Phase 2 — QA rounds.** Each round dispatches fresh `sonnet` testers, one per flow, plus one for the
visual comparison against the prototype at 374 x 834 dp. They file issues labelled `revamp-qa`. Fix
them with ship-queue. Stop when two rounds in a row find no medium or high issue.

**Phase 3 — End-to-end test on staging.** When the whole flow is built and merged, dispatch fresh
`sonnet` agents to test it end to end. They use the `prod` flavour against the deployed Fly.io
backend and the staging database, on the emulator, one agent on the emulator at a time. Real
calls, no mocks:
1. Sign in with Google, using the Google account already on the emulator. Agents never type a
   password or create an account. If no account is there, file a `revamp-blocker` issue. Then test
   every later step through the backend API with the staging test identity the backend already
   supports, if it has one. Then continue.
2. Upload a realistic resume. Make a 1–2 page PDF and a DOCX from the prototype's sample (Priya
   Deshmukh: three roles, education, skills, achievements). Also test a scanned or image-only PDF
   and the paste-as-text path. Use synthetic people only, never a real person's data.
3. Check the reading step and the profile review. Fix the required gap and confirm the profile.
4. Paste a real-length job description (the Northwind GCC posting from the prototype, plus one
   more of your own). Also test import from a link and the "not a job post" case.
5. Check the job analysis: the match before and after, keywords, must-haves.
6. Answer the quick question, then tailor. Check the tailored resume, the Changes tab and its
   sources (no invented facts, Constitution I.1), and edit the resume.
7. Export the PDF. Open it and check it is one page, uses the right file name, and contains the
   changes.
8. Check the application in Applications, change its status, and re-tailor it.
9. Check credits: the free credit is used, the paywall shows, and the ledger is right. Buy only with
   Play test purchases (licence testers or test cards). If Play is not set up for testing, file a
   `revamp-blocker` and check the paywall screens and the backend wallet routes instead.
10. Check settings, download my data, sign out, sign in again, and delete account, using a separate
    synthetic test account only.

Each agent writes `docs/revamp/<date>/e2e/<step>.md` with screenshots, the backend responses and a
pass or fail per step. Failures become `revamp-e2e` issues that ship-queue fixes, in the app or the
backend. Run the whole end-to-end pass again after the fixes, until one full pass is clean.

**Phase 4 — Finish.** Make one agent rewrite `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`,
`docs/MOCK_BACKEND.md`, `docs/MVP_PLAN.md` and `design/02-screens.md` for the new app. Then write
`REPORT.md`: what shipped, what was deleted, the decisions, the open follow-ups, and what only the
owner can do.

Done means all of these are true:
- Every prototype screen and state is built.
- No old screen remains.
- `tools/ci/verify-local.sh` and CI pass on `main`.
- An emulator walk-through of every flow in both flavours matches the prototype, with screenshots in
  the run folder.
- The backend changes are deployed to the Fly.io app `apps-backend`.
- One full Phase 3 end-to-end pass on staging is clean.
- Every `revamp-blocker` issue is listed for the owner in `REPORT.md`.

## 5. Rules for every agent brief

Copy these lines into every Builder, Fixer, Integrator and Verifier brief:
- Never run `git stash`. Commit to the lane branch, or write a patch file.
- Create worktrees with `git worktree add -b <branch> <path> <base>`. Do not use the Agent
  `isolation: worktree` option, because it branches from `main`.
- Use one bounded wait per Gradle run. Do not create repeated Monitor tasks. Never run `./gradlew --stop`
  while other lanes run.
- Only one agent drives the emulator at a time. emulator-5554 has density 480, so use 1011 or 1080 px
  for 337 or 360 dp.
- No `tools:ignore`, no lint baseline entries, no weakened tests. No code comments unless they pass the
  grounding test in `~/.claude/CLAUDE.md`.
- New screens get Roborazzi baselines. Run `tools/ci/verify-local.sh` before pushing.

## 6. Merge rules

Squash-merge a PR yourself only when all of these hold:
- the build job's conclusion is success on the head SHA (read the run that actually built, not a
  skipped duplicate);
- the branch is up to date with `main`;
- the final review is clean or fixed, and no high finding is open;
- UI PRs have a behaviour check with evidence.

Merge one PR at a time. If `gh pr update-branch` starts no CI, close and reopen the PR.

## 7. Hard limits

- Never push to `main` directly. Never force-push a shared branch.
- Never destroy production data or user data. No dropping or deleting migrations without a reversible
  path. The staging database holds test data, but treat its schema with the same care. Delete only
  test accounts and records that the run itself created.
- No new paid service and no spend beyond existing plans.
- No secrets in tracked files.
- Do not edit `~/.claude`, the router or its gates.
- Change `docs/CONSTITUTION.md` only when the new design or PRD 6.1.2 forces it. Use an Article VII
  amendment PR with a ledger row. Never weaken I.1 (truth), I.3 (no ATS or job guarantees) or the
  privacy parts of I.5.
- The owner authorizes downloading the Space Mono and Space Grotesk font files from
  `github.com/google/fonts`. Every other download needs a logged reason in `DECISIONS.md`.
- Host safety: follow "Shared CI host resource safety" in `~/.claude/CLAUDE.md`. No unbounded loops
  and no blanket `pkill`.

Start with Phase 0 now.
