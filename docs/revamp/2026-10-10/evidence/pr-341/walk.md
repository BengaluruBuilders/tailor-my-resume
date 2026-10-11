# PR #341 behaviour walk (head 682e6a75), emulator-5554, demo debug build
Base for upgrade test: issue-281 build 80efc1a3 (schema v3). Widths 360dp, 337dp, 337dp + font 2.0.

| Item | Result |
|---|---|
| Upgrade v3 -> v4 (`adb install -r` over v3 with sample data) | PASS. Opens, no crash, user_version 4, profile and 5 entries intact, contactUserStated=0 on the old row. walk-upgrade-before-v3-profile.png, walk-upgrade-v3-to-v4-profile.png |
| 1. Profile -> Settings gear (282-x4) | PASS (placeholder screen "SettingsNavKey", back works). walk-360-x4-settings.png |
| 2. Contact: Save disabled for blank name and for invalid phone ("abc"), no error copy; enabled when valid; save -> toast "Contact saved", Profile shows Pune, 100%; DB: contactUserStated=1; LinkedIn amber while empty, normal once typed; email read-only, wraps after "@" / "." at 337dp and 337dp/2.0 | PASS. walk-360-contact-*.png, walk-337-contact.png, walk-337f2-contact.png |
| 3. Skills: add, duplicate ("sql" vs "SQL") rejected (Add disabled), 60 chars OK / 70 chars Add disabled, remove chip, Done commits pending text ("Skills saved", DB skills and userStatedSkills), Back discards pending text and added chips | PASS. walk-360-skills-*.png |
| 4. List edit: Summary saves; Education unchanged Save -> isConfirmed 0->1, source/bullets/id unchanged (DB, after I set E-01 to unconfirmed via DB); Achievements changed line keeps id X-01, becomes one USER_EDITED line (org/dates/bullets cleared), confirmed; new line adds confirmed USER_STATED item; blank line removes the item | PASS. walk-360-achievements-*.png |
| 5. 337dp and 337dp/2.0 layout: contact, skills, summary, education, achievements | PASS: no ellipsis, mid-word break or clipping; screens scroll. walk-337-*.png, walk-337f2-*.png |

## Findings
- low: the "Contact saved" toast sits over the Profile header card (covers the name line) for its duration. walk-360-after-contact-save.png
- low: Save/Done is enabled on open with no edits (valid data); the prototype is the same (always shown), so informational.
- info: the Add chip "sql" duplicate shows no message (D52 no error copy); the skills field has no placeholder (disclosed follow-up).
- info: sample data has all entries confirmed, so the confirm check used a DB edit (E-01, X-01 set unconfirmed) before the walk.
- No high or medium findings.

Test setup notes: autofill service disabled during the walk and restored; wm size and font_scale reset.
