# PR 317 behaviour check (sha 93e08756, demoDebug, Pixel_9_Pro AVD booted headless by me as emulator-5556; 1x = 1080x2400 (360dp); narrow = 1011x2400 (337dp) + font_scale 2.0)

Synthetic sample data only. Screens reached through the "TailorMyResume dev" menu (Tailoring review target). Applications and Credits are still placeholders on this branch, so the application-list entry, double-tap on a Tailor button and the Credits screen are not reachable; the credit ledger was read from the demo Room DB (run-as, credit_ledger table).

| # | Check | Result | Files |
|---|---|---|---|
| 1 | Tailoring progress to Tailored | Progress card with percent, keyword stickers and 4 rows ticking, then 100% and Tailored ("Your resume is ready", Resume / Changes 2, Accept changes, Export PDF disabled). Both widths | a0_*, a0b_*, a1_*, e0_*, e3_* |
| 2 | Tailoring failed | Data reset then open: "Tailoring didn't finish / Your credit wasn't used", Try again, Go back. Try again re-runs and fails again with no data (expected); no ledger row from failures. 337dp/f2.0: heading wraps, body scrolls, actions stay pinned | b1_*, b2_*, e1_*, e2_* |
| 3 | Changes: Undo, Resume shows original, Export gate, Accept | Undo on bullet 1 shows "Undone" and the original text; Resume tab then shows the original bullet. Export PDF before accepting: "Accept the changes first" toast. Accept changes: "Changes accepted", Export PDF enabled (uiautomator enabled=true) at both widths | a2_*, a3_*, a4_*, a5_*, a6_*, e5_*, e6_*, e7_*, e9_* |
| 4 | Double-tap stacks one run | Not reachable (Applications placeholder). Each dev-menu open makes one runId and exactly one SPEND row (7 opens, 7 rows, none duplicated) | ledger query |
| 5 | Back during a run | Back is swallowed while running (BackHandler no-op in TailoringRoute; back arrow absent on progress); the run completes and records one SPEND. Failed runs record none | ledger query, a0_* |
| 6 | Rotate once | Landscape then portrait on Changes: tab and Undone state kept, no crash | g1_*, g2_* |
| 7 | Logcat | no FATAL EXCEPTION (fatal.txt empty) | fatal.txt |

Findings (none above low):
- F1 (low): at 337dp/f2.0 the Tailored header (stepper, title, subtitle, tabs, keywords) plus the pinned Accept/Edit/Export bar leave a viewport of about a third of the screen for the paper/changes; the page scrolls and nothing is clipped, but the tabs scroll out of view. Export PDF label wraps to two lines.
- F2 (low): the "Accept the changes first" toast overlaps the back button and the stepper at the top.
- F3 (info): Back during a run is blocked by design, so "Back does not charge" holds by construction; the user cannot cancel mid-run.
- Not exercised at 337dp/f2.0: Undo then reading the Resume tab (my scripted tap hit the second change and the tabs had scrolled off); verified at 1x (a4, a5).
Reset: font_scale 1.0, wm size and density reset, rotation auto, emulator killed (I started it).
