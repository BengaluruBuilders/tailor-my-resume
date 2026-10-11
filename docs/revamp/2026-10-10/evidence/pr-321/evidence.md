# PR 321 behaviour check (sha eb640020, demoDebug, emulator-5554 = Pixel_9_Pro AVD booted headless by me; 1x = 1280x2856 (native); narrow = 1011x2400 (337dp) + font_scale 2.0)

Synthetic sample data loaded through the "TailorMyResume dev" menu, then Open TailorMyResume > Profile tab. Taps by `adb input`; date on emulator 2026-10-11.

| # | Check | Result | Files |
|---|---|---|---|
| 1 | Overview | Priya Deshmukh, "Data Operations Associate · 1 yr" (Jul 2024-Jun 2025 + May-Jul 2023 = about 15 months, floored), 96% complete, rows Contact/Summary/Experience 2/Education 1/Skills 8/Achievements 1/LinkedIn & links with amber ADD. After adding a current role: "2 yrs", Experience 3. 337dp/f2.0: name, 2 yrs, 96% and all rows render, list scrolls | b1_profile_1x, h1_profile_after_1x, n1_profile_337_f2 |
| 2 | Experience list | Newest first. The sample has no required-fix row; one appeared when a role was saved with no end date and Current off: amber "Jan 2026 – end date?" | c1_experience_1x, e3 saved row in f2_added_current_1x, n2_experience_337_f2 |
| 3 | Edit role: edit bullet, Save | Single tap on Save returns to Experience with "Role saved" toast; reopening the role shows the edited Bullet 1 | d1_edit_1x, d2_typed_1x, d3_saved_1x, d5_reopen_1x, n3_edit_337_f2 |
| 4 | Add role | Empty form: Save tap does nothing (stays on Add role) but the button looks fully enabled (F2). Title+company+start filled, Current job on: End dims to "Present"; saved, new role first, list shows "Feb 2026 – Now" (edit screen shows "Present", F4) | e1_add_empty_1x, e2_add_empty_save_tap_1x, f1_add_current_on_1x, f2_added_current_1x |
| 5 | Delete role | "Delete this role" at the bottom of Edit role; two taps fired back to back: toast "Role deleted", list updated, popped once (landed on Experience, not Profile) | g1_edit_bottom_1x, g2_after_delete_1x |
| 6 | Settings gear | Touch: no response (F1). With keyboard focus (Shift+Tab to the gear, Enter) Settings opens (SettingsNavKey placeholder) | h2_settings_1x, h2b_settings_try, n1_profile_337_f2 |
| 7 | Rotate once | Edit role in landscape and back to portrait at 337dp/f2.0: same screen, state kept, no crash | r1_rotated_edit_337_f2, r2_back_portrait_337_f2 |
| 8 | Logcat | no FATAL EXCEPTION (fatal.txt empty) | fatal.txt |

Findings:
- F1 (high): the Profile overview header ("Profile" title and "Settings" button) is drawn from y=0 and sits under the system status bar (status-bar inset 156px, Settings node at y 41-103, focus bounds 958,0-1238,144). Touch taps at the Settings button (y 72, 100, 125) never reach it, so Settings cannot be opened by touch; the clock and icons also overprint the title and button, at 1x and worse at 337dp/f2.0. Experience and Edit role headers are inset correctly, so Profile root lacks the status-bar padding.
- F2 (medium): Save on Add role with an empty form looks fully enabled (lime, no dim) although it does nothing and the accessibility node reports enabled; D52 says Save stays disabled until valid, with no error copy. Not marked disabled for users or TalkBack.
- F3 (low): the "Role saved"/"Role deleted" toast overlaps the Experience title while shown.
- F4 (low): list shows "Now" for a current role, the editor shows "Present".
- F5 (low): at 337dp/f2.0 the bottom nav "PROFILE" label renders larger than "APPLICATIONS" (chrome, D40, not this PR).
- Not covered: Save disabled when only some fields are filled (checked empty form only); sample data has no role with a missing end date.
Reset: font_scale 1.0, wm size reset, rotation auto, emulator killed (I started it).
