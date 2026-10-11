# PR 313 behaviour check (sha b3717362, demoDebug, emulator-5554 booted headless by me, 1280x2856 at 1x; 1011x2856 = 337dp at font 2.0)

Debug placeholders only: every screen renders its NavKey name. Screens opened through the "TailorMyResume dev" menu (one key atop the root), real app via MainActivity.

| # | Check | Result | Files |
|---|---|---|---|
| 1 | Launch MainActivity | SignInNavKey, no top bar, no tabs, no Back; system Back leaves the app | b1_signin_1x.png |
| 2 | Setup flow 1x | Upload: step bar "1 Profile / 2 Job / 3 Tailor" (step 1 of 3), no Back on empty stack. JobResult: Back + "Profile done, 2 Job current" (step 2 of 3). Tailoring: "Profile done, Job done, 3 Tailor", no Back (noBack). System Back from Upload/JobResult returns to previous/menu as the Back button does | c1_upload_1x, c2_jobresult_1x, c3_tailoring_1x |
| 3 | Other chrome 1x | Exported: Close only. Credits: Back + "Settings". Settings: Back + "Profile". ApplicationDetail: Back + "Application". Applications/Profile: no top bar, tab bar | c4..c9 |
| 4 | Tabs | Profile and Applications tabs switch; system Back from Profile tab goes to Applications, Back again leaves. "+" from Applications and from Profile opens Job (step 2, Back); Back returns to the tab it came from | d1-d5, e1-e4 |
| 5 | Bottom sheet | No scenario offers a sheet (Credits + Failed shows no sheet); not walkable until W4 | f1_credits_failed_1x |
| 6 | Rotate on Job (pushed over Applications) | Landscape and back to portrait keep Job and the step bar; Back then returns to Applications | g1-g3 |
| 7 | 337dp + font 2.0 | Step bar wraps to a second row, nothing ellipsized or clipped; Back button above the bar; tab bar labels whole; "+" opens Job; Back returns to Profile tab; Exported Close, Credits/Settings titles present | h1-h10, i0-i3 |
| 8 | Logcat | no FATAL EXCEPTION lines (fatal.txt empty) | fatal.txt |

Finding F1 (low): at font 2.0 / 337dp the tab labels autosize unevenly: APPLICATIONS is shrunk, PROFILE is at full size (h7_applications_337_f2.png), and the "+" sits tight against the APPLICATIONS label. Legible, nothing clipped.
Not walked: screens beyond the debug menu (placeholders have no controls), so reading to review and job-link screens are only reachable by key; vision delegate not used, images read directly.
Reset: font_scale 1.0, wm size reset, auto-rotate on.
