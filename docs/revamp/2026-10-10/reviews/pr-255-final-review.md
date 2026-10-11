# PR #255 final review

- PR: BengaluruBuilders/tailor-my-resume #255, "docs(design): add the 2026-10-10 prototype and the simplified MVP"
- Head SHA reviewed: `21ee644d97aeb3303354fb8233b69ba08e5b4d46`
- Reviewer: final review (read-only). Date: 2026-10-10.
- CI on head: all checks pass (Constitution policy, Static checks, Lint, unit, screenshot, assemble, delivery-gates-pregate).
- Verdict: **MERGE**. No high finding. Two medium findings are doc contradictions to resolve before the
  app is re-planned for the simplified MVP; they do not make the merged state unsafe.

## Checks run

| Check | Result |
|---|---|
| Secrets or tokens in any file | None. Grep for key/token/secret/bearer/private-key/`sk-`/`AIza`/`ghp_`/`xox` patterns over the full diff found nothing. The claude.ai design-project URL in the README needs the owner's login; it is not a credential. The sample job URL uses the reserved `.example` TLD. |
| `.claude/launch.json` | Valid JSON. One entry, `prototype`, `python3 -m http.server 8767 --bind 127.0.0.1 --directory design/prototype-2026-10-10`, `port: 8767`. Only file under `.claude/` in the tree; `.claude/worktrees/` stays ignored. |
| `support.js` scope | Claude Design runtime only. Network: `fetch(location.href)` and sibling `<name>.dc.html` (same origin); React 18.3.1, ReactDOM 18.3.1 and Babel 7.29.0 from unpkg with pinned SRI hashes. `new Function` evaluates the logic block of the local `.dc.html` files, and `x-import` modules (neither prototype file uses `x-import`). `postMessage(..., "*")` to the parent only when framed, carrying design-mode and boot metadata, no user data. No storage, cookies, beacons or sockets. Nothing beyond local rendering of concern. |
| `.dc.html` files | Prototype loads `./support.js` and Google Fonts (Space Mono, Space Grotesk) only. No `data-omelette-injected` script remains. README line refs are accurate: logic starts at line 967, `class Component` 968, `T` at 1171; Space Mono 161 uses, Space Grotesk 47. |
| CONSTITUTION I.1 / I.3 text | Unchanged. |
| CONSTITUTION I.5 | The consent-screen-S4 precondition is removed. The remaining privacy parts stand: backend only, `prod` only, no `INTERNET` in `demo`, backups off, no analytics/crash/ad SDKs, one HTTP stack, Dependency Guard. Article VII ledger row is present ("Ledger amendment — 2026-10-10, simplified MVP (I.5)"); the gate is unchanged and never checked S4, so VII.3 holds. The removal is an explicit owner decision (PRD 6.1.2) that the owner barred from being re-raised, so it is recorded here and not rated. |
| README vs PRD 6.1.2 vs MVP_PLAN | README and PRD 6.1.2 agree on flow, Google-only sign-in, 1 free credit, packs (5/₹199, 15/₹449, 40/₹999), PDF only, A4/Letter, file-name format, the five error states, and the five owner cuts. Contradictions with the *unchanged* constitution and PRD sections are below. |

## Findings

### M1 (medium) — Constitution I.2 conflicts with the simplified tailoring review

- Constitution I.2 (unchanged): "Every suggestion starts as `PENDING`. Nothing reaches an export without
  an explicit accept." Gate: `UpdateBulletDecisionUseCaseTest`, `TailorViewModelTest`.
- PRD 6.1.2 "Tailoring review" replaces "Per-bullet accept, keep, or edit" with "A Resume tab and a
  Changes tab ... Edit in one editor". The prototype has no accept or reject control anywhere
  (`grep -i accept|reject` in `Prototype.dc.html` finds only statuses); "Your resume is ready" goes
  straight to Export PDF.
- AGENTS.md now lists the prototype and PRD 6.1.2 as item 0, "They win over older design and scope
  documents", while item 1 says the constitution "wins every conflict". A builder gets two opposite
  instructions for the export gate.
- Fix (before re-planning): either amend I.2 under Article VII (for example, "the Changes tab is the
  review; Export is the explicit accept of the listed changes", and update the gate tests), or add a
  line to PRD 6.1.2 that the Changes tab keeps a per-change accept/undo to satisfy I.2.

### M2 (medium) — "Your match 61 → 92" score vs the keyword-coverage rule

- PRD 6.1.2 "Gap analysis: Match now → after tailoring"; README `jobResult` "Your match 61 Now → 92 Up
  to, after tailoring"; Applications rows "company · NN match".
- PRD 4.2 (non-goals) says "Show 'keyword coverage' only" and PRD 6.3 item 6 says the screen shows
  keyword coverage, e.g. "You cover 9 of 14 key terms". The 6.1.2 override clause covers 6.1.1 and
  6.2 to 8, so it overrides 6.3 but not 4.2. Constitution I.3 says to say "keyword coverage".
- The I.3 policy regex will not fire on "match", so nothing mechanical catches it. A bare 0–100 "match"
  with a predicted "after tailoring" value reads close to an ATS score.
- Fix: state in 6.1.2 what the number is (for example keyword-coverage percent) and the copy rule for it
  (label it "keyword coverage" or explain it on screen), or record an explicit owner exception to 4.2.

### L1 (low) — MVP_PLAN still points at 6.1.1 as the locked scope

- New note in MVP_PLAN section 1 says the scope is PRD 6.1.2 and the chunks are not re-planned, but the
  numbered "Sources of truth, in order" still has "2. `docs/PRD.md`, section 6.1.1: the locked MVP
  scope", item 3 still points at `design/jade-restyle/`, and section 3 "Locked decisions → MVP
  features" still lists DOCX, share card, consent and the ₹149 5-pack.
- Fix: update list item 2 to 6.1.2 and add the prototype at the top of the list; mark the section 3
  "MVP features" and "Look" rows superseded.

### L2 (low) — PRD override clause is narrower than the contradictions

- 6.1.2 wins over "6.1.1 or sections 6.2 to 8". Unchanged PRD section 1 (Summary: "accepts or rejects
  each change, then exports a clean PDF or DOCX"), 4.2 (keyword coverage only) and 12.2 (R2 adds DOCX
  and the share card) still describe the old scope.
- Fix: widen the clause to "any other section of this document", or add a one-line pointer in 1 and 12.2.

### L3 (low) — I.1 "confirmed facts" path is implicit

- I.1: "Tailored text uses only facts that the candidate confirmed." 6.1.2 replaces fact-by-fact
  confirmation with a single review ("Looks right, continue") and adds the quick-question answer as a
  source ("Your answer").
- Fix: one sentence in 6.1.2 that "Looks right, continue" marks the imported facts confirmed, and the
  quick-question answer is stored as a confirmed fact with a source ID, so the fabrication guard and
  the source chips keep working.

### L4 (low) — README fallback command binds all interfaces

- `.claude/launch.json` binds `127.0.0.1`; the README's alternative
  `python3 -m http.server 8767 --directory design/prototype-2026-10-10` omits `--bind`, so it serves on
  all interfaces. Only design files are served. Add `--bind 127.0.0.1` for parity.

### Info — the prototype needs internet to render

- `support.js` pulls React, ReactDOM (and Babel only for `x-import` jsx) from unpkg with SRI, and the
  prototype pulls Google Fonts. Offline, the prototype will not render. Design-time only; no app impact.
