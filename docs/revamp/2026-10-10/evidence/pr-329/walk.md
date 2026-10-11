# PR 329 behaviour walk (head 2d047f7b), emulator-5554, demo debug build, synthetic data

Method: 360dp = wm 1080x2400, 337dp = wm 1011x2200, font scale 1.0 and 2.0. Credits added by editing the debug Room ledger (demo balance starts 0); the sample application's stored analysis was edited to carry must-haves, a PARTIAL, two GAPs and a quick question (the demo analyzer returns no question and no must-have priorities).
Prototype: hirehop-wt/scout/design/prototype-2026-10-10/Prototype.dc.html (strings compared by grep; no pixel diff).

| # | Item | Result |
|---|------|--------|
| 1 | Job result: title/company, "Keywords matched", must-have markers, no check on GAP, content descriptions | PASS (company missing on a pasted job, see F1) |
| 2 | One Tailor tap -> question -> Continue/Skip -> one Tailoring -> result; double-tap Tailor and Continue push once | PASS (double tap via two simultaneous taps; back from result lands on job result, no duplicate screens) |
| 3 | Back to job result: Tailor works again; answered row loses "Not clear yet / We'll ask you next" | PASS (row became "Missing" with its reason, see F3) |
| 4 | 0 credits: note "No credits left · get more to continue", Tailor opens paywall | PASS (placeholder PaywallNavKey) |
| 5 | No ellipsis/mid-word break at 337dp, font 2.0; "Internationalization" wraps whole | PASS (walk-31-*-s5/s6; no ellipsis in dumps) |

Content descriptions seen via uiautomator: "Keywords matched: 93% now, up to 93% after tailoring", "In your resume", "Partly in your resume", "Not clear yet", "Missing" on markers; "Step 2 of 3, Job"; "Back". Markers: MET shows check, PARTIAL "~", GAP and unclear "!" (never a check). No string says match/fit/ATS/score.
Copy equals prototype: "What they screen for", "Must-haves", "Up to, after tailoring", "Uses 1 credit · you have N", "No credits left · get more to continue", "One quick question", "Skip this", "Pick an answer, or skip", "Not clear in your resume. We'll ask you next.", "We only use what's backed...", "Missing skills are only added if...", detail placeholder.

## Findings
- F1 (medium) Pasted job whose first lines are "Title" / "Company - City": result header shows only the title, and Tailoring result shows "For Senior Backend Engineer · " with a dangling separator. Probably the demo parser (not this PR), but the trailing "·" on empty company is a UI defect.
- F2 (medium) Pasted-job analysis shows a must-have row only for requirement lines, no reasons, 0% -> 0% coverage ("Up to 0% after tailoring"); demo analyzer yields no question, so the question flow cannot be reached from paste in demo. Backend-only behaviour; flagged for QA scope.
- F3 (low) After answering "Yes, regularly" the requirement shows "Missing" / "not in your resume" (true to the resume, but the user just said yes). Prototype behaviour for the answered row not checked.
- F4 (low) Each Tailor from the job result spends 1 credit even for an already tailored application (3 -> 2 -> 1 on re-tailor).
- F5 (low) Prototype shows coverage as "92" with no percent sign; app shows "93%".
- F6 (low) "Pick an answer, or skip" toast overlaps the back button (walk-19). Mascot overlaps the word "chain" at 360dp (walk-17).
- F7 (low) At font 2.0 the three-step header wraps to two rows and consumes ~25% of the viewport; content scrolls under it (walk-31).
- No high findings.
Screenshots: walk-*.png in this folder.
