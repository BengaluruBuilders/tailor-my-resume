# PR #321 rewalk at 80efc1a3 (demo debug, sample data, emulator Pixel_9_Pro, density 480; 360dp = 1080px, 337dp = 1011px)

| Item | 360dp | 337dp |
|---|---|---|
| Header below status bar; "Settings" action tappable, opens Settings route | PASS | PASS (profile shot; tap opened Settings) |
| Save visibly dim and inert while title/company empty; bright when filled | PASS | PASS |
| Saving adds role to list | PASS | PASS |
| Ongoing role reads "Present" | PASS | not re-run |
| Delete role | PASS | PASS |
| Header "1 yr" plausible (Jul24-Jun25 + May-Jul23 = ~14 months) | PASS | PASS |
| No ellipsis / mid-word breaks | PASS | PASS |

New observations
- Low: a role saved with no dates renders a lone dash as its date line (rewalk-337-after-save.png).
- Low: start/end are free text; "Jan2025" accepted and shown verbatim (adb typing dropped the space; format not validated).
- Info: Settings and list-edit destinations are placeholder screens (e.g. "SettingsNavKey") in this branch.
- Known/accepted: "Role saved/deleted" toast covers the Experience title.
Note: emulator was not running; started AVD Pixel_9_Pro on port 5554. wm size reset at end.
