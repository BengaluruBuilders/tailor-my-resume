# PR 294 behaviour check (issue 258, head b4c47852, demoDebug on emulator-5554, fresh install)

| Criterion | Action | Result | Screenshot |
|---|---|---|---|
| Fresh install launches, no crash | uninstall, install, monkey launch | MainActivity resumes; first screen is the WelcomeNavKey placeholder (class name text only); no FATAL in logcat | 01-launch.png |
| Dev tools entry works | start DebugScenarioActivity | "TailorMyResume dev" menu renders | 02-dev-tools.png |
| Profile opens with sample data | Load sample data, destination Profile, Open screen | ProfileNavKey placeholder with the bottom bar (Applications, Profile, Settings) | 10-profile.png |
| Guided form route | destination Guided form | GuidedProfileFormNavKey placeholder | 11-guided-form.png |
| Evidence route | destination Evidence path | FactEvidenceNavKey placeholder | 12-evidence.png |
| Welcome route | destination Welcome | WelcomeNavKey placeholder | 13-welcome.png |
| Sign-in route reachable | destination Sign in | SignInNavKey placeholder (no real sign-in UI yet) | 14-signin.png |
| Consent route | destination Consent | ConsentNavKey placeholder | 15-consent.png |
| Import resume route | destination Import resume | ImportResumeNavKey placeholder | 16-import.png |
| Confirm facts route | destination Confirm facts | ConfirmFactsNavKey placeholder | 17-confirm.png |
| Back works | BACK from preview | returns to dev menu | 20-back-from-preview.png |
| App reset no crash | Reset app data, Open TailorMyResume | "App data is reset."; app opens on WelcomeNavKey placeholder | 21-after-reset.png, 22-open-app-after-reset.png |
| Settings with sample data | Load sample data, open app, Settings tab | Real Settings screen (account, credits, privacy) | 23-settings.png |
| Consent notice from Settings | tap Consent notice | ConsentNavKey placeholder; BACK returns to Settings | 24-..., 25-back-to-settings.png |
| Sign out no crash | Sign out, confirm | app lands on WelcomeNavKey placeholder; no crash, no ANR | 27-after-signout-confirm.png |

Crash: none (the FATAL lines in logcat came from `uiautomator dump`, not the app).
Note: `delegate vision` could not see build/ (ignored dir), so every screenshot was inspected directly.
Residue for later waves: sign-in, welcome, consent, import, confirm, profile, guided form and evidence are all placeholders; Settings "Consent notice" and sign out lead to a blank placeholder with no way forward except BACK/relaunch.
