# Post-merge smoke walk, main b30e4319, demo debug, emulator-5554 at 360dp (1080x2400 @480)

Crash logs: `adb logcat -d -b crash` after every step; every crash-*.log is empty. No crash in the walk.
Screenshots: walk-*.png (same folder). Input was via adb; some typed text lost characters (artifact of adb, not reported).

| # | Step | Result | Shot |
|---|---|---|---|
| 1 | Launch, onboarding story 1-3 (tap right to advance) | pass; has no separate sign-in screen, "Continue with Google" is on every slide | 01-03 |
| 2 | Sign in (demo Google) | pass, goes straight to Profile step | 04 |
| 3 | Paste resume (single line) | FAIL "We couldn't read your resume" (no section lines) - expected for unstructured text, retry path works | 06-08 |
| 4 | Paste resume (multi-line) -> Reading -> Review profile | pass (Reading too brief to capture) | 06b-08b |
| 5 | Continue -> Job step (no Applications home between) | pass / see F7 | 09 |
| 6 | Paste job, Analyze, job result | pass with findings F2, F3 | 10-12 |
| 7 | Tailor my resume with 0 credits -> Paywall -> mock purchase | pass, returns to result with "15 credits added" | 13-14 |
| 8 | Quick question / Tailoring progress | NOT SEEN (go straight to result) | 48 |
| 9 | Tailored result, Accept; Undo | Accept pass; Undo only when changes exist (works in sample data) | 15, 63-64 |
| 10 | Export PDF -> Exported; Share (cancel); Open | pass; PDF correct; credit 15 -> 14 | 17-19 |
| 11 | Applications home, detail, status change, re-tailor | pass (re-tailor charges 1 credit, 14 -> 13) | 20-25 |
| 12 | Profile: Contact, Summary, Skills add/remove, Experience, role editor, Education, Achievements, links | pass | 26-36 |
| 13 | Settings: Download my data (share sheet), Help & feedback, Credits & purchases, Buy more | Download pass; Help DEAD; Credits pass (13 -> 28) | 37-42 |
| 14 | Delete account dialog, Sign out | Sign out pass; Delete pass but F1 | 43-44, 66-67 |
| 15 | Dev entry: sample data, Sign in, Import, Applications, Workspace, Profile, Gap analysis, Tailoring review, Credits, Settings | pass; Exported/Delete destinations land on Applications/Settings (F10) | 50-63, 65 |

## Findings

### High
- F1 Credits shown in Settings and the delete dialog do not match the real balance. Settings row said "16 left" (real 13), "5 left" in sample data (real 26); delete dialog "Your 31 unused credits" (real 28), "5 unused credits" (real 26). Untrue claim in the delete-confirmation text. (37, 43, 61, 66)
- F2 After Delete account, tapping "Continue with Google" showed "Signing in..." and never finished (waited ~15 s). Recovered only by force-stop. Dead end. (67, 68)
- F3 New user, 0 credits: job result footer says "No credits left · get more to continue" while onboarding says "Your first tailored resume is free". Tailor goes to the paywall. Untrue claim. (12, 13)

### Medium
- F4 Keyword promise is not met: job result says "50% now, up to 50% after tailoring" (and 93% -> 93%), the tailored resume then shows 17% (and 25%, 71% in sample). "Up to" equals "now"; tailored score is lower than "now". Changes shown: 0 ("Changes accepted" with 0 changes). (12, 14, 47-48)
- F5 Help & feedback in Settings does nothing (no screen, no intent, no error). (39)
- F6 Settings account row says "priya.d@example.com / Signed in with Google" and Contact says "Email · from Google" and Credits "Receipts go to your Google email", although the email came from the pasted resume (Sam Test). Wrong identity. (37, 27, 40)
- F7 Flow skips the Applications home after "Looks right, continue" (goes to Job step) and Quick question + Tailoring progress screens were never shown. May be per prototype; flagging against the walk path. (09, 48)
- F8 Tailored preview orders Education before Experience, and omits the headline that the PDF shows (PDF is the correct one). (14, 19)

### Low
- F9 Short 170-character job post with clear Requirements triggers "This doesn't look like a job post". (47)
- F10 Dev entry: destination "Exported" opens Applications and "Delete account" opens Settings (not the screen named).
- F11 After Undo there is no redo ("Undone" is final). (64)
- F12 Toast "15 credits added" overlaps the back button; bottom action buttons sit on the gesture-bar area. (14, 15)
- F13 Single-line pasted resume gives a generic "couldn't read" (no hint to use line breaks). (07)
- No placeholder "XxxNavKey" screens, no crash, no dead nav other than F5 found.
