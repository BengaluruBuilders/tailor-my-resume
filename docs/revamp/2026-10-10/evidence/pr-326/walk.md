# PR 326 behaviour walk (head c5ce4d06, demo debug build, emulator-5554)

Screens: walk-*.png here. Widths 360dp, 337dp, 337dp at font scale 2.0.

1. Settings rows and copy: PASS (low diffs below). Rows, copy, footer match the prototype; no ellipsis; email wraps fully (breaks mid-address at 2.0, accepted exception). Credits shown.
2. Download my data: PASS. One tap -> one zip (cache/data-exports/tailormyresume-my-data.zip), one CHOOSER intent, toast "Your data is ready to share". Double tap -> still one chooser, one file. Back cancels. (Prepare is too fast to test a tap "while preparing" distinctly.)
3. Sign out: FAIL on the toast and on re-sign-in.
   - Lands on the signed-out root (shows only the placeholder text "SignInNavKey": Sign in screen is not built in this branch). No "Signed out" toast seen in 8+ frames over ~3 s (walk-360-signout-*.png, tl frames).
   - Re-sign-in cannot be walked: the sign-in screen is a placeholder; only debug "Load sample data" re-seeds, which is not a real sign-in. D59 (same data after sign-in) NOT VERIFIED on device.
4. Delete sheet:
   - DELETE gating: PASS. "DELET" and "DELETEX": button grey. "delete", " Delete ", "DELETE": button enabled.
   - While deleting (~1-2 s): PASS. Delete/Keep dim; Keep tap, scrim tap, system Back each changed nothing (frame diff of the screen empty, sheet stays; nothing underneath moved).
   - After success: root is signed-out placeholder; "Account deleted" toast NOT seen (same as sign out). exports dir removed.
   - Data gone after sign-in again: NOT VERIFIABLE (no real sign-in screen).
5. uiautomator semantics while locked: PASS. Keep "my account" node enabled=false, clickable=true. The scrim node (clickable Button [0,0][1080,484] when unlocked) has no Button/clickable node while locked (walk-lock-dump.xml). Note: the "Delete account" node and the DELETE field stay enabled=true in the dump during the lock.

Findings
- HIGH?/MEDIUM: "Signed out" and "Account deleted" toasts do not show on the device after the re-root (acceptance criterion; PR notes admit a race with the old shell consuming it). Needs confirmation with the real sign-in root; severity high if it persists there.
- MEDIUM: Credits mismatch. Settings read "3 left" (first load) and "0 left" (later loads) while the delete sheet said "5 unused credits" in the same session. D59(5) allows a mismatch until #287, but the sheet text is untrue meanwhile.
- MEDIUM: Enabled "Delete account" button is lime (#A3F43F); prototype uses orange #FF6A2B with dark text.
- LOW: Credits value "N left" is grey; prototype shows it lime #A3F43F.
- LOW: Chevrons on Download my data and Help & feedback rows; prototype has none.
- LOW: Product updates toggle off by default (the prototype default not checked).
- LOW: Delete sheet at font scale 2.0 draws under the status bar (clock overlaps the sheet).
- LOW: During the lock the Delete account node and field remain enabled in the accessibility tree (taps are ignored).
