# PR #331 (#279) behaviour walk, head 59cb51ca, demo debug, emulator-5554

Sizes: 360dp (1080 px), 337dp (1011 px), 337dp + font 2.0. Package com.tailormyresume.app.

| # | Item | Result |
|---|------|--------|
| 1 | Export blocked until Accept changes (walk-10, walk-11); after Accept the Export PDF button enables (walk-12) and opens Exported with file name, "PDF - 1 page - 66 KB", credits line, Saved to Applications (walk-13) | PASS. Credits line "0 left, was 1. Credits never expire." could not be checked against the ledger (Room, not readable with run-as); formula in ExportedViewModel is sum of ledger. |
| 2 | PDF: cache/exports/<applicationId>/Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf, A4 595x842, 1 page, 67321 bytes (under 200 KB); fonts embedded as Type 3 with unicode maps; pdftotext returns selectable text equal to the accepted resume (export.pdf, export-page-1.png). After hand edit the new text appears in the PDF (export-after-edit.pdf, 67328 B). | PASS |
| 3 | Share opens system chooser "Sharing 1 file" with the right file name (walk-04), cancelled; Open opens the Google Docs PDF viewer with the resume (walk-05) | PASS |
| 4 | Back from Exported -> Tailored (walk-14), PDF history count unchanged (3 -> 3). After force-stop and reopening Exported through the debug target: history count 4 -> 4, file mtime unchanged, stored export shown (walk-26) | PASS. Tapping Export PDF again after Edit+Save is a new export (3 -> 4), expected. |
| 5 | Edit resume: bullet editable, "Edit bullet" content-desc present on every bullet (uiautomator), Save shows toast "Changes saved" and returns to Tailored (walk-23); typing a very long bullet hides "Still fits on 1 page" after typing stops (walk-18 vs walk-20), no visible per-keystroke lag; Back discards edits | PASS |
| 6 | 337dp and font 2.0: Exported (walk-07, walk-08) and Edit (walk-30, walk-31) show no ellipsis, no mid-word break (file name wraps only at hyphens), no clipping; content scrolls, Go to Applications stays visible | PASS |

## Findings
- medium: Edit resume has no Summary and no Skills line (prototype shows both as editable textareas). Recorded in D65 as a follow-up, still a visible deviation. The sample resume also exports with no summary section.
- low: Prototype file card shows "PDF - 1 page - 48 KB" in one line; font 2.0 splits "66 / KB" across two lines (walk-07).
- low: Prototype puts the "Did you apply?" row first for status Saved; the debug sample is already Applied so only the chip row was seen, Mark as Applied was not walked (out of this check's list).
- low: Back out of Edit with unsaved changes discards silently, no confirm (walk-22).
- low: pdftotext shows fi/fl ligature glyphs (ﬁnd, ﬁnal) from the font; text still selectable but searching "find" in a viewer may miss these words (ATS parsers may mis-read). Cause: ligatures in the Roboto font via the platform PdfDocument.
- low: debug "Success" tailoring state purchased pack 5 in the sample data; walk data is now changed (reset with Reset app data).
