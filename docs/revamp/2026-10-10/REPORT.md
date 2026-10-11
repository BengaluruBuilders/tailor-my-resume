# Revamp run 2026-10-10 — report

The app and the backend were rebuilt to the 2026-10-10 prototype and the simplified MVP (PRD 6.1.2). Records: `GOAL.md`, `PLAN.md`, `DECISIONS.md` (D1–D84), `PROGRESS.md`, `reviews/`, `evidence/*/walk.md`.

## What landed on main

| Repo | PRs | Merge |
|---|---|---|
| tailor-my-resume | #255 prototype docs, #300 screenshot ledger | ea135043, 9aedf907 |
| tailor-my-resume | revamp consolidation #346–#349 (all W1–W4 PRs #293–#342, D81/D82) | 53424e07 … b30e4319 |
| tailor-my-resume | #357 account fixes (#350, #354) | 2026-10-11 06:36 UTC |
| apps-backend | #115 contract v2 backend (#101–#112, #114), migrations 0008–0011 additive | bede3ee3, deployed, staging /health 200 |

All prototype screens are built: sign-in, upload/paste/manual/reading/unreadable, review profile, add job, job analysis and quick question, tailoring and tailored resume, edit resume and PDF export, applications home and detail, profile and editors, settings and delete account, credits and paywall. Deleted: consent, 18+ check, light theme, prep questions, cover letter, share card, DOCX, follow-up reminders, offline states.

## How it was checked

- Each PR: red tests first, Reviewer A (sonnet) where allowed, Reviewer B (Opus) with a truth, money or security mandate, fixers, and an emulator walk for UI.
- Consolidation (owner D81: merge now, validate later): integration commits were not separately reviewed; CI and the delivery-gates pregate stayed required.
- Post-merge walk of main (`evidence/qa-main/walk.md`): no crashes, no placeholder screens; findings filed as #350–#356.

## Open at hand-off

- Being fixed by other agents (Conductor): #338, #330, #325, #344, #336, #335, #339. This session's partial PRs #358 (coverage, #352/#330) and #359 (credits, #338/#351) are open with their Reviewer B findings in `reviews/pr-358-reviewB.json` and `reviews/pr-359-reviewB.json` — not merged.
- Other follow-ups: #311, #312, #316, #320, #324, #327, #328, #334, #343, #355, #356, #360; unbuilt W5/W6 items #289–#292; contract docs #296–#298.
- Owner: support email for Help & feedback (#353); Play Console 15/40 packs (#288); refunds before those packs go live (apps-backend #55); eval credentials (#106); staging identity and email provider (#98, #99); product decisions (#250).
- Not done: QA rounds, the Phase 3 staging end-to-end walk with real sign-in, the docs rewrite (ARCHITECTURE, DESIGN_SYSTEM, MOCK_BACKEND).

## Lessons

- A required review status that the agent cannot post blocks every merge; the owner allow-rule (#256) should exist before a run starts.
- Deep stacked branches conflict with every squash-merged parent; land waves as they finish instead of holding a stack.
- A PR body caps at 65,536 characters, so a consolidation with hundreds of deleted tests must be split (D82).
- Check whether another session owns an issue before starting a lane.
