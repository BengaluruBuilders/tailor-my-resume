# PR 318 behaviour check (sha 4218841d, demoDebug, emulator-5554 = Pixel_9_Pro AVD booted headless by me; 1x = 1080x2400 (360dp); narrow = 1011x2400 (337dp) + font_scale 2.0)

Synthetic data only; sample data loaded through the "TailorMyResume dev" menu, then Applications > "+" opens Job. Text typed with `adb shell input text` (it dropped a few characters; irrelevant to the checks).

| # | Check | Result | Files |
|---|---|---|---|
| 1 | Empty state | Paste from clipboard and Use a link visible; Analyze job greyed out (tap shows no card). 337dp/f2.0: both actions whole, page scrolls, Analyze at the bottom, nothing clipped | b1_empty_1x, f1_empty_337_f2, f2_empty_scrolled_337_f2 |
| 2 | Paste 254-char synthetic post | Heading "Job description" (detected label) + "254 characters", Analyze enabled; tap shows "Reading the job... 100%" then lands on Job analyzed placeholder (JobResultNavKey). Same landing at 337dp/f2.0 | b2_detected_1x, b3_analyzing_1x_a, b4_result_1x, h3_result_337_f2 |
| 3 | Short non-job text (24 and 150 chars) | "This doesn't look like a job post / Paste the full listing, including responsibilities and requirements." card; both widths | c1_notjob_short_1x, g1_notjob_short_337_f2 |
| 4 | Use a link > https://example.com/job > Import job | Demo importer fails; back on Job with the not-a-job-post card, Paste from clipboard and Use a link visible, Analyze job disabled (the #318 fix); both widths | d1_link_1x, d3_after_import_1x, i1_link_337_f2, i2_after_import_337_f2, i3_after_import_scrolled_337_f2 |
| 5 | Back | System Back from Job analyzed returns to Job (empty); Back from Job returns to Applications; Back again leaves to the dev menu | e1_back_from_result_1x, j1_back_337_f2 |
| 6 | Logcat | no FATAL EXCEPTION (fatal.txt empty) | fatal.txt |

Observations (no severity above low):
- F1 (low): analysing with no saved profile (app data not loaded) shows the "Couldn't read the job. Try again." snackbar with Retry on every attempt; expected from the "No profile saved" guard in JobViewModel.onAnalyze, but the copy does not say a profile is missing. Not reachable after the first-run flow.
- F2 (low): at 337dp/f2.0 the Analyze job pill is inset and narrower than the full-width Paste/Use a link buttons.
- Not captured: the analysing percent at 337dp/f2.0 (finishes before a screenshot); captured at 1x only (b3_analyzing_1x_a, "Reading the job... 100%"). The dev menu itself is cramped at 337dp/f2.0 (debug only), so Job was opened at font 1.0 and font 2.0 applied on the screen.
- vision delegate not used; images read directly.
Reset: font_scale 1.0, wm size and density reset, emulator killed (I started it).
