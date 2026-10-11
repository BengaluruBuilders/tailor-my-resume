# PR 333 re-walk, head 53d4d88, demo flavour, emulator-5554 (density 480: 1080 px = 360dp, 1011 px = 337dp)

Method: fresh build, app data reset via the dev entry, text typed with adb (the typing sometimes truncates long lines; this does not affect the checks). DB read by run-as copy of hh-database.

## Per item
1. Junk paste: PASS at 360 and 337. Neutral "We couldn't read your resume" (rewalk-337-junk.png, rewalk-360-junk-b.png). profile table empty after it. Fill in myself shows no junk name at 360 (blank). At 337 the form shows "Priya Deshmukh / priya.d@example.com": that comes from the demo session account (mock_backend datastore, local.account.1), not from the junk; the profile table is empty.
2. Re-import: PASS.
   - Priya paste (name, email, phone, headline "Headline line here") then Rohan Mehta paste: profile became fullName Rohan Mehta, email rohan.mehta@example.com, phone/city/headline/summary empty; no Priya field left; entries replaced (Data Scientist, M.Tech). Taken whole from the new resume.
   - Fill in myself with only a phone value typed (asha.rao@example.com in the Phone field) then Priya paste: identity kept whole (name empty, email empty, phone as typed, no Priya name, email, phone, headline); roles/education imported.
   Caveat: my typed phone line for Rohan was truncated by adb to "+91 91", so phone stayed empty; not a product issue.
3. "Priya Deshmukh - Product Analyst": PASS at 360. Saved fullName "Priya Deshmukh" (not run at 337).
4. Upload buttons: FAIL on row, PASS elsewhere.
   - FAIL (medium): at font 1.0 "Fill in myself" wraps to two lines at 360dp (rewalk-360-upload-rest.png); at 337dp both "Paste as text" and "Fill in myself" wrap (rewalk-337-upload-rest.png). The spec was one line at 360.
   - PASS: stacked full-width at 1.15 and 2.0 at 337dp (rewalk-337-upload-f1.15.png, -f2.0.png, -bottom.png).
   - PASS: "5 MB" never splits (2.0: "PDF or DOCX · up to" / "5 MB").
   - Footer at rest: clears the gesture bar at 360 and 337/1.0. At 337/1.15 at rest the "Fill in myself" button bottom edge touches the gesture bar and the footer is below the fold; scrolled to the end it clears (rewalk-337-upload-f1.15-bottom.png, f2.0-bottom.png). Low.
5. Reading back chevron: FAIL (medium). Screen recording at 360, 15 fps: the Reading screen ("Reading your resume", 100%, Takes about 20 seconds) shows the back chevron top-left in every frame it is visible (about 3 frames), rewalk-360-reading-frames.png (last three of twelve) and rewalk-360-reading-chevron.png. The chevron is absent only on the preceding chrome transition frames.

## New findings
- N1 medium: item 4 row labels wrap (above).
- N2 medium: item 5 chevron present on Reading.
- N3 low: 337/1.15 footer below fold, last button against the gesture bar at rest.
- Still open from walk 1: text PDF PriyaDeshmukhResume2026.pdf still lands on Unreadable (seen during this walk by accident via the picker), H2.

## Evidence
rewalk-360-*.png, rewalk-337-*.png in this folder. wm size and font_scale reset to 1.0 afterwards.
