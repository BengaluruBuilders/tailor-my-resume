# PR #337 behaviour walk (head 9a5aa4ba, demo debug, emulator-5554)

Entry: dev launcher > Credits + state > Open screen (normal path blocked: Applications/Profile tabs are placeholders on this branch).

| # | Item | Result |
|---|------|--------|
| 1 | Credits: hero, plural, history | PASS with notes. Purchase rows date only ("Bought 15 credits / 11 Oct / +15"), no price. Spend/grant rows not reachable in demo. Number stacks above caption at 2 digits (prototype: side by side) - low |
| 2 | Paywall packs, prices, Best value, single Buy, Completed/Cancelled/Pending | PASS. 5/15/40, INR from paise, per-resume, 15 preselected, BEST VALUE, double tap = one purchase (+15), "15 credits added" toast + return, Cancelled no change, Pending toast only |
| 3 | Payment failed sheet | PASS for "You weren't charged" copy. Unconfirmed variant not reachable via dev launcher (unit test only) |
| 4 | Restore | PARTIAL. "No previous purchases found" PASS. Failure toast not reproducible in demo (offline radios do not fail the mock; Restored/Failed states also give "none") |
| 5 | Settings vs Credits balance | Same number observed (30 / 30) in demo |
| 6 | Layout 337dp / font 2.0 | PASS, no ellipsis/mid-word break/clipping; hero follows scale; Paige hidden at 2.0 |

Findings
- medium: Paige art overlaps the hero headline ("every job") at 360dp and 337dp, font 1.0 (walk-02, walk-13); prototype has no overlap.
- low: Credits balance number and caption stack vertically for 2-digit balances (walk-05) vs side-by-side in prototype.
- low: payment-failed title wraps "Payment didn't go through" on one line at 360dp; prototype breaks after "didn't".
- low: Buy label at font 2.0 wraps leaving "·" at line end (walk-17).
