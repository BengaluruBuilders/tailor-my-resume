# PR 295 behaviour check (issue 257, head 0bf9d9e1, demoDebug, emulator-5554 Pixel 9 Pro, density 480)

| Criterion | What I did | Result | Screenshot |
|---|---|---|---|
| App launches, no crash | Installed app-demo-debug.apk, opened dev tools, loaded sample data, opened the app | Pass. Applications home renders; logcat crash buffer has no FATAL EXCEPTION for the whole walk | 01, 02 |
| Tailoring flow reaches tailored resume | Opened an application, tapped "Preview and export" | Not observable. The review/preview screens are deleted by this PR; the route shows a placeholder (ExportPreviewNavKey). Expected: W1 leaves placeholders until W4 | 03, 04 |
| Bullets accept/reject/undo/edit | Dev tools Tailoring review and Bullet review | Not observable in UI (placeholders TailorNavKey, BulletReviewNavKey). Covered by TailorViewModelTest, which the PR keeps | 06, 07 |
| PDF export works and opens | Looked for any export entry | Not observable: the export screens are placeholders. Export code and tests are kept | 04, 10, 12 |
| No cover letter, prep questions, DOCX, export preview, regenerate control reachable | Walked workspace and every former route | No working UI for any of them. Residue to note: the application workspace (feature/applications, outside this PR) still shows a "Cover letter / Write one" card and a "Prep plan" heading; the debug dev-tools menu still lists Cover letter, Prep questions, Export preview, Application pack, Exported, Credits. All lead to placeholders. No DOCX or regenerate control seen | 03, 05, 08, 09, 10, 11, 12, 13 |
| No crash on former routes | Opened Tailoring review, Bullet review, Cover letter, Prep questions, Export preview, Application pack, Exported, Credits via dev tools, plus Cover letter and Preview and export from the workspace | Pass. Each shows a blank screen with its NavKey class name; back returns to home; app process alive | 04 to 13 |

Screenshots were inspected with delegate vision: 03 and earlier real UI, 04 to 13 class-name placeholders, no crash dialogs.
