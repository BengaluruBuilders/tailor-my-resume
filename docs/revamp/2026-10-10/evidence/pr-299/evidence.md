# PR 299 behaviour check (head de3b9e70, demoDebug, emulator-5554)

Build: one `./gradlew assembleDemoDebug` in the worktree, installed with adb. Crash scan: no `Process: com.tailormyresume` FATAL in logcat for the whole walk (the only FATAL lines came from the adb uiautomator tool itself).

| Check | Action | Result | Screenshot |
|---|---|---|---|
| Launch | monkey launch | Opens the debug "TailorMyResume dev" screen, no crash | 01-dev-tools.png |
| Sample data | Load sample data | Loads, no crash | 02-sample-loaded.png |
| Destination list | Scroll dev tools | Former routes listed (incl. Your data, Delete account) | 03-dev-tools-scrolled.png |
| Settings | Open screen: Settings | Placeholder "SettingsNavKey" inside the 3-tab bar; BACK goes to Applications tab placeholder | 04, 05 |
| Applications list | Open screen: Applications | Placeholder "ApplicationsNavKey" | 06 |
| Application workspace | Open screen: Application workspace | Placeholder "ApplicationDetailNavKey"; no cover-letter card, no prep-plan heading, no share card, notes or requirement sheet | 07 |
| Gap analysis | Open screen | Placeholder "AnalysisNavKey" | 09 |
| Your data | Open screen | Placeholder "YourDataNavKey" | 18 |
| Delete account | Open screen | Placeholder "DeleteAccountNavKey", no crash | 19 |
| Other routes | Evidence, Tailoring, Bullet, Cover letter, Prep, Export, Pack, Exported, Credits | Real screens from other modules (not touched by this PR), open and BACK works, no crash | 08, 10-17 |

Cover-letter residue: the "Write one" card (screen "Want a short cover letter?"), "Write a cover letter (optional)" on Exported, and "Open my prep plan" on Prep questions are drawn by feature/tailor/impl (strings_cover_letter.xml, strings_exported.xml, strings_prep_questions.xml). They are not on the application workspace, which is a placeholder in this PR.

Limit: the delete-account and download-my-data entry points are not in Settings (Settings is a placeholder), so only the direct routes were opened.
