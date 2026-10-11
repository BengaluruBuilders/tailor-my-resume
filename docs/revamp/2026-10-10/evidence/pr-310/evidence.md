# PR 310 behaviour check (sha ceb37b4a, demoDebug, emulator-5554 booted headless by this run, 1011x2856 @480 = 337dp, font scale 2.0)

| Check | Action | Result | File |
|---|---|---|---|
| Build | `./gradlew assembleDemoDebug` in worktree issue-267 | succeeded | none |
| Launch, no crash | install -r, launch via launcher (debug menu), Reset app data, Open TailorMyResume, direct MainActivity start | MainActivity resumed; logcat FATAL EXCEPTION count 0 (AndroidRuntime lines in an early capture are the monkey process only) | fatal.txt, launch.png, fresh_start.png |
| Existing screens | read screenshots, uiautomator | black body with SignInNavKey placeholder; nothing hosts content rows yet (D44) | main.png |
| Font-200 images | vision, then read listrow, filecard, progress, keyword chip myself | list row at 2.0: Experience 3, Skills 7, LinkedIn ADD whole, counts not clipped (vision claimed they were: wrong). Progress rows wrap at spaces, whole. Keyword chip "Missing / Variance / analysis" wraps at spaces, whole. File name wraps after the underscore boundary (D47), whole | content_listrow_font200, content_progress_font200, content_chip_keyword, content_filecard (dark) in the PR |
| Dark images | vision plus own read | text whole and unclipped across card, chips, tags, status chip, delta, soon row. Grey meta on surfaceRaised uses textMuted per D45; vision guessed 4.3-4.4:1 on a few (unmeasured, the PR contrast test pins the pairs) | content_*_prototype_dark.png |

Outcome: clean. Low note for W4: sample copy "Matching 8 keywords", "Fitting to 1 page", "KEYWORDS MATCHED" appears in component harness images; D5 forbids match/fit in coverage copy, so W4 screens must not ship them.
