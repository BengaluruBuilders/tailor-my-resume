# PR 340 behaviour walk (head af8dc848, demo debug, emulator-5554)
Screens: walk-01..25 in this folder. 360dp = 1080px, 337dp = 1011px.

| # | Item | Result |
|---|------|--------|
| 1 | Home rows, pill, labels | PARTIAL: rows, status chips, credit pill, "NN% keywords" (only when tailored: "Meridian GCC . 38% keywords") pass; header overlaps status bar (F1); sample apps show no keyword meta (F3). Empty state not reachable (reset data lands on a SignInNavKey placeholder). |
| 2 | Detail, file card, Share, Open | PASS: card hidden without export; Share opens one chooser with the PDF (double tap = 1 chooser in task stack); Open opens PdfViewer; after deleting cache/exports/<id> Share re-rendered the PDF (dir recreated). |
| 3 | Status sheet | PASS: five options, Save -> toast "Status set to Interview", row and counts update (Applied 2->1, Interview 1->2); double tap saved once (one toast, one date). |
| 4 | Re-tailor | PASS with credits: one tap and a double tap each pushed one Tailoring screen, one back returned to detail; credits 44 -> 43. 0-credit paywall NOT verified (no way to reach 0 credits in the demo build). |
| 5 | Long names / 337dp / font 2.0 | PASS for the sample names: titles, chips, status card, Next steps wrap at word boundaries, nothing clipped. No genuinely long name in sample data, so not stress-tested. |

## Findings
- F1 (medium-high, visual): Applications home header ("Applications", credit pill, avatar) draws under the status bar at 360dp, 337dp and font 2.0 (walk-01, 21, 25); title collides with clock/icons. Detail and sheet respect insets. Missing statusBarsPadding on home.
- F2 (low): at 337dp font 1.0 the credit pill and avatar wrap below the title (walk-25); at font 2.0 too. Prototype keeps them on one line at 360dp only; acceptable but cramped.
- F3 (low-medium, data): sample applications (3 of 4) have no tailored resume so the meta lacks "NN% keywords" and the detail has no file card. Prototype shows keywords on every row. Matches D67 (hidden when no export) but home meta differs from the issue text "<company> . NN% keywords".
- F4 (medium, copy truth): detail status line says "Not applied yet" for apps with status Applied/Interview from sample data (no date stored). Prototype logic is the same fallback, but it reads untrue next to an "Applied" chip.
- F5 (low): bottom nav "PROFILE" label renders larger than "APPLICATIONS" at font 2.0 (walk-21).
- F6 (info): after Accept/Export, the Export screen shows "Saved to Applications" meta; no issues seen. Prototype file card says "PDF . 1 page . NN% match"; PR uses "PDF . 38% keywords" (D5, correct).
