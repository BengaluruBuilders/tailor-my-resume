# PR 326 re-walk (head b8dfaed6, demo debug build, emulator-5554)

Screens: rewalk-*.png here. 360dp = wm 1080 wide; 337dp fs 2.0 = wm 1011 wide. Emulator reset to default size and font scale 1.0.

Scope limit: Settings is not reachable from MainActivity in this branch (Profile tab is a placeholder "ProfileNavKey"; nothing calls navigateToSettings). Settings/delete/sign-out were walked only through the "TailorMyResume dev" launcher (DebugScenarioActivity, Destination = Settings). MainActivity could not be exercised for items 1, 2, 3 (NOT TESTABLE).

1. Sign out toast: PASS (dev launcher). "Signed out" visible on the signed-out screen from the first frames and stable for the whole ~3 s (rewalk-dbg-signout-1/3/5/8.png). MainActivity: NOT TESTABLE.
2. Delete account: PASS (dev launcher). Typed DELETE, tapped Delete: Keep tap, scrim tap and system Back during the lock changed nothing (rewalk-dbg-locked-1/2); lock lasts about 5 s; then the signed-out screen shows "Account deleted" (rewalk-dbg-delete-seq-21/22/24.png). MainActivity: NOT TESTABLE.
3. Action toast vs sign-out: PARTIAL. No toast with a button can be triggered anywhere in the app (no TmrToastAction( caller outside the class). Substitute: plain toast "Your data is ready to share" (Download my data), then Sign out within ~2 s: the old toast was replaced by "Signed out"; nothing stale remained (rewalk-dbg-toast-1/2/13.png). PASS for the plain toast; action variant not exercised.
4. Credits and sheet: 
   - Same number: PASS. Settings "5 left", sheet "Your 5 unused credits" (rewalk-dbg-delete-sheet.png).
   - Button colour: PASS. Enabled button pixel (255,106,43) = #FF6A2B orange, dark text (rewalk-dbg-delete-ready.png).
   - uiautomator during lock: PASS. Delete button View enabled=false, DELETE EditText enabled=false, Keep enabled=false (rewalk-lock-dump.xml).
   - Keyboard stays open without the sheet jumping: FAIL. At lock the IME closes (mInputShown false) and the sheet drops by ~350 px (sheet top y~880 with keyboard, ~1140 locked at 1080x2856 scale): rewalk-dbg-delete-seq-1.png vs seq-4/21. Cause: disabling the focused field drops focus.
5. 337dp fs 2.0 sheet below status bar: PASS (rewalk-337-fs2-delete-sheet.png). Sheet top is under the status bar with the Profile header peeking above; the clock is not overlapped.

New findings
- LOW/MEDIUM: keyboard closes and the sheet jumps down at the start of the delete lock (item 4). The sheet then shows a long locked state (about 5 s) without the keyboard.
- INFO: Settings is unreachable from MainActivity in this branch, so the real-entry-path toast behaviour (items 1-2 via MainActivity) is unverified.
- INFO: dev launcher menu at 337dp fs 2.0 wraps the app title mid-word ("TailorMyResu / me dev"); debug-only.
