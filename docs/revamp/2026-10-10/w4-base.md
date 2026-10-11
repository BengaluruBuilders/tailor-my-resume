# revamp/w4-base (D62)

Purpose: one integration branch holding contract v2 (#314), the tailor screens (#317), add-job (#318) and billing v2 (#319) on the nav shell, so #277 and #322 build on v2 + tailor together.

Base: origin/revamp/271-nav-shell b3717362.

Merged (--no-ff, in order):
- 62665a1a  origin/revamp/286-network-v2 (#314)
- 401ca67f  origin/revamp/278-tailored (#317)
- 6af9bf19  origin/revamp/276-add-job (#318)
- e146fecf  origin/revamp/287-billing-v2 (#319)

Conflicts: none. ShellHarness and the version catalog merged cleanly.

Integration commit 04c90ad6 "fix: adapt the tailoring screens to the contract v2 ResumeTailor (D62)":
- feature/tailor/impl/src/test/.../result/ChangeCardMapperTest.kt
- feature/tailor/impl/src/test/.../tailoring/TailoringRunnerTest.kt
- feature/tailor/impl/src/test/.../tailoring/TailoringViewModelTest.kt
Only the fake `ResumeTailor.tailor` override changed (`section: EntryCategory?` to `answer: QuickAnswer?`), plus imports. TailoringRunner already passed `quickAnswer = application.quickAnswer` to TailorResumeUseCase, so no main-source change. No assertion changed.

Verify: tools/ci/verify-local.sh at 04c90ad6 - BUILD SUCCESSFUL, exit 0.

Refresh rule: merge new tips of the source branches into w4-base (never rebase, never force-push); run verify-local after each merge.
