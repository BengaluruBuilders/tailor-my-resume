# PR 333 behaviour walk (issue 274), head 8f23421, demo flavour, emulator-5554

Densities: 360dp (1080 px), 337dp (1011 px), 337dp + font 2.0. Synthetic files: PriyaDeshmukhResume2026.pdf (Flate text PDF, made by script), corrupt.pdf, oversize.pdf (6.0 MB, not kept here), Priya_Resume.docx (pre-existing in Download).
DB checks via run-as copy of hh-database.

## Per item
1. Upload readable file: PARTIAL.
   - DOCX: Reading shows Contact Found, 2 roles, 1 degree, 5 found, Achievements 0 found; imported entries saved isConfirmed=0, source IMPORTED. PASS. Evidence walk-360-reading-docx-100.png, walk-337f2-reading.png.
   - Text PDF: FAIL. A text-based PDF (compressed stream, normal Tj lines) lands on Unreadable with the neutral line. The same text pasted with line breaks parses to 2 roles; pasted as one line also fails. See finding H2.
   - Next step (Review profile) is a placeholder key in this branch ("ReviewProfileNavKey"); not checkable here.
2. Corrupt PDF and >5 MB: PASS with a note. Oversize: "This file is bigger than 5 MB." (file blamed, correct). Corrupt PDF: "PDF · image only / looks like a photo or scan" (blames file; wording is wrong for a corrupt file, low L1). An uncompressed-stream text PDF also shows "image only" (wrong, see H2).
   Network/parse failures use the neutral line (seen for the text PDF and one-line paste). PASS for D60.
3. Paste: PARTIAL.
   - Real resume text: works, Reading counts match. PASS.
   - Empty or whitespace-only: Read text disabled. Acceptable (no neutral line needed).
   - Junk sentence (>20 chars, "quick brown fox ..."): FAIL, finding H1: no neutral line; a profile is saved with the junk as full name and flow goes on to Review.
4. Fill in myself: PASS. Role "Product Analyst" -> Continue (1 role row) -> Back lands on form -> title "Senior Product Analyst" -> Continue: still 1 role row (same id W-bda26004, title updated). Back to Upload, Fill in myself: form prefilled with role, Continue: still 1 role. Role is USER_STATED, confirmed=1 (typed by user).
5. Text at 337dp / font 2.0: PASS with lows (L2, L3, L4, M1). No ellipsis or clipping of email, file name or copy; email wraps at the dot ("priya.d@example./com").

## Findings
- H1 (high, data/truth): Junk paste is accepted. "the quick brown fox..." saves a profile with fullName = the whole sentence, no roles, then continues. Expected the neutral "We couldn't read your resume" state (D60, empty parse). Evidence walk-360-paste-junk.png, walk-360-manual-prefilled-from-junk-paste.png (the manual form later shows the junk as name).
- H2 (high, demo flavour): ContentResolverResumeTextSource joins PDF string tokens with spaces, so line structure is lost; the local parser then finds nothing and a normal text PDF fails to the neutral Unreadable. Also streams that are not Flate (uncompressed) are skipped entirely and reported as "image only". Related to #325; confirm whether prod /resume/parse copes with the one-line text. Evidence walk-360-unreadable-longname via walk-337-unreadable-longname.png.
- M1 (medium): Upload at 337dp/2.0: "Paste as text" and "Fill in myself" become circles with one word per line (walk-337f2-upload-scrolled.png). Prototype row stays two pills.
- M2 (medium, unverifiable): Reading shows the back chevron in the one frame captured (walk-360-reading-100.png), but ScreenChrome sets leading=None for Reading and acceptance says no back button. Reading is visible for about one frame in demo (stage-driven, no 0/40% frames observed), so it could be a chrome cross-fade. Needs a slowed-down check.
- M3 (medium, prototype): Reading flashes for under 0.3 s in demo; the "Takes about 20 seconds" copy and progress rows (0/40/100) are not observable. Not a bug if the backend parse is the slow part.
- M4 (medium, merge): Importing onto an existing profile keeps the old name but fills email and phone from the new resume (Asha Rao with priya.deshmukh@example.com). Mixed identity if a different person's resume is imported.
- L1 (low): corrupt PDF labelled "image only / photo or scan".
- L2 (low): file name wraps at the dot, "...2026." / "pdf" and "Priya_Resume." / "docx" at 337dp (no ellipsis, no mid-word break).
- L3 (low): "up to 5 / MB" orphan at 337dp/2.0.
- L4 (low): Upload footer "Never shared." sits under the gesture bar at rest at 360/337dp; scrolls clear (walk-360-upload.png).
- L5 (low): Achievements "0 found" although both roles have bullets; City not extracted from "Pune, India".
- Prototype differences: none beyond the above (hero, stickers, card copy "PDF or DOCX · up to 5 MB", unreadable tips, six manual fields match).

## Evidence files
walk-360-upload, -picker, -unreadable-corrupt, -unreadable-oversize, -paste, -paste-filled, -paste-junk, -paste-failure, -paste-whitespace, -manual*, -reading-100, -reading-docx-100; walk-337-*, walk-337f2-* (png). Test files in this folder.
