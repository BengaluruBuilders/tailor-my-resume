# PR 309 behaviour check (sha b41ab2c9, demoDebug, emulator-5554 booted headless by this run, 1011x2856 @480 = 337dp, font scale 2.0)

| Check | Action | Result | File |
|---|---|---|---|
| Build | `./gradlew assembleDemoDebug` in worktree issue-269 | succeeded | none |
| Launch, no crash | install -r, launch via launcher (debug menu), Reset app data, Open TailorMyResume, direct MainActivity start | MainActivity resumed; logcat FATAL EXCEPTION count 0 (AndroidRuntime lines in an early capture are the monkey process only) | fatal.txt, launch.png, fresh_start.png |
| Start destination, fresh install | debug menu: Reset app data, Open TailorMyResume | MainActivity resumes on SignInNavKey (uiautomator text); body black placeholder | fresh_start.png, top.txt |
| Roborazzi images | checked the PR tree | PR head has no committed screenshots (0 PNG tracked), so no font-200/dark vision to run | none |
| D5 copy | grep of added strings and Kotlin | no added user-visible string with match/fit/ATS | none |
| Export file names | no screen hosts them in this build | not walkable; covered by unit tests | none |

Outcome: clean. Low note: image inspection not applicable (no images in this PR).
