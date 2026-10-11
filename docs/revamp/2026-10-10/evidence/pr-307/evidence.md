# PR 307 behaviour check (sha 0205f503, demoDebug, emulator-5554 booted headless by this run, 1011x2856 @480 = 337dp, font scale 2.0)

| Check | Action | Result | File |
|---|---|---|---|
| Build | `./gradlew assembleDemoDebug` in worktree issue-265 | succeeded | none |
| Launch, no crash | install -r, launch via launcher (debug menu), Reset app data, Open TailorMyResume, direct MainActivity start | MainActivity resumed; logcat FATAL EXCEPTION count 0 (AndroidRuntime lines in an early capture are the monkey process only) | fatal.txt, launch.png, fresh_start.png |
| Existing screens | read screenshots, uiautomator | fresh start renders a black body with the SignInNavKey placeholder (as in PR 306/303 evidence); nothing hosts the chrome yet (D44) | main.png |
| Font-200 images | vision, then read tab bar image myself | chrome_tabbar_*, topbar_*, stepbar_*, toast_*, sheet: all words whole, no ellipsis, no mid-word break ("Applied, marked / today" wraps at the space). Vision claimed PROFILE clipped at the right edge: wrong, checked directly, it is whole inside the frame. APPLICATIONS ends about 8px from the FAB and renders smaller than PROFILE (WholeWordAutoSize shrink, D40): tight but whole | chrome_*_font-200_dark.png in the PR (core/designsystem/src/test/screenshots) |
| Dark images | vision | toast, sheet, step bar, top bar text high contrast; unselected tab label grey on black looks about 5.9:1 by eye (vision guessed 3.5-4, unmeasured) | same |

Outcome: clean. Low note: APPLICATIONS and PROFILE labels differ in size at 2.0 (D40 shrink); confirm intended in the W4 screen PR.
