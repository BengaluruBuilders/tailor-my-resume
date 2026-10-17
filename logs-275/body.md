## Summary
After the resume is read, the user lands on "Your profile": completeness card, a Required banner (or "All required info is done"), optional LinkedIn row, Contact, Experience (continuation fragments folded into one role), Skills (5 chips plus "+N") and Education cards, and a "Looks right, continue" button. Add date, Contact, LinkedIn, Experience and Skills open the profile editors (still placeholders until #281/#282). With an open end date the button looks disabled and tapping it toasts "Add the missing end date first". Continue confirms only the Experience and Education entries shown here (I.1), stamps reviewedAt, and opens Job.

Closes #275

## Acceptance criteria
| Criterion | Tests |
|---|---|
| c1 Layout and strings | ReviewScreenshotTest.needsFix90, ReviewScreenTest.needsFixStateShowsEveryPrototypeString, ReviewMapperTest.freshProfileMapsToBannerOptionalRowAndAmberDates |
| c2 Fixed state, 96/100 | ReviewScreenshotTest.fixed96, linkedinAdded100, ReviewViewModelTest.fixedProfileShowsDoneBannerEnabledCtaAnd96And100WithLinkedin, stateRecomputesWhenTheProfileChanges |
| c3 Gap refusal and toast | ReviewViewModelTest.continueRefusedWithRequiredGapSavesNothingAndToasts, MarkProfileReviewedUseCaseTest.refusesWithRequiredGapAndLeavesProfileUntouched, ReviewScreenTest.tappingDisabledContinueShowsToast |
| c4 Navigation | ReviewRouteNavigationTest (addDate, contact/linkedin, experience, skills, education not tappable) |
| c5 Confirm, single-flight, nothing to review | MarkProfileReviewedUseCaseTest.setsReviewedAtFromInjectedClock, ReviewViewModelTest.continueWithoutGapSetsReviewedAtAndEmitsContinue, doubleTapContinuesOnce, emptyProfileEmitsNothingToReview, ReviewRouteNavigationTest.continuePushesJobNavKey |
| c6 Present = Now, blank start | ReviewMapperTest.presentEndDateReadsNowAndIsNotAGap, roleWithoutStartDateShowsNoDates |
| c7 Narrow 337dp / font 2.0 | ReviewNarrowScreenshotTest (2 baselines), ReviewLayoutTest.noLabelBreaksInsideAWordAtFontScale2 |
| c8 Gates | verify-local run: only failure is OnboardingEntryKeysTest (see Follow-ups); everything else green (verifyRoborazziDebug re-run alone for onboarding: pass) |
| c9 I.1 confirm | MarkProfileReviewedUseCaseTest.confirmsOnlyExperienceAndEducation, achievementsStayUnconfirmed, ReviewViewModelTest.openingReviewWritesNothingAndConfirmsNothing, keepsEditsSavedAfterTheScreenState |
| c10 Ids untouched | MarkProfileReviewedUseCaseTest.entryAndBulletIdsUnchanged |
| c11 Shared logical count | CountLogicalEntriesTest.*, ReviewMapperTest.splitRoleShowsOneRowAndCountsOnce, ReadingViewModelTest (unchanged) |
| c12 Manual guard | SaveManualProfileUseCaseTest.doesNotReplaceARoleThatHasDatesEvenIfUserStated (base-fail), stillReplacesTheUntouchedManualRole |
| c13 Real entry (D55) | OnboardingRealEntryHiltTest.reviewAddLinkedinOpensEditContact, reviewContinueSetsReviewedAtAndOpensJob |
| c14 Other states | ReviewScreenshotTest.linkedinAdded100, ReviewMapperTest.skillsOverFiveShowPlusN, skillsAtOrUnderFiveShowNoPill, manualProfileMapsWithoutEmptyCards |

Red commit e059e215 (37 new failing tests, compile-only stubs); head passes them unchanged apart from the test-harness notes below.

## Decisions
D74 defaults: confirm only EXPERIENCE/EDUCATION; achievements stay unconfirmed; split role stays truthful; manual profile shows "Skills · 0" and no Education card; no "From ..." when no file name; private "Present" constant. Also: at font scale >= 1.3 the Required banner and role dates stack (button/dates below text) so no word breaks (D40/D47). Hilt test seeds the real Room-backed ProfileRepository (no TestInstallIn exists in the repo) and uses the real Clock, asserting reviewedAt is non-null. Two red-commit test harness fixes, assertions kept: the Hilt wait pumps the Robolectric looper, and the string check scrolls to each node before asserting it is displayed.

## Pre-review gate
- feature/onboarding/impl/src/test/screenshots/review_needs_fix_prototype_dark.png: new baseline for #275, inspected at 1x
- feature/onboarding/impl/src/test/screenshots/review_fixed_prototype_dark.png: new baseline for #275, inspected at 1x
- feature/onboarding/impl/src/test/screenshots/review_linkedin_added_prototype_dark.png: new baseline for #275, inspected at 1x
- feature/onboarding/impl/src/test/screenshots/review_needs_fix_narrow-337-font-200_dark.png: new baseline for #275, inspected at 337dp/font 2.0
- feature/onboarding/impl/src/test/screenshots/review_fixed_narrow-337-font-200_dark.png: new baseline for #275, inspected at 337dp/font 2.0
- feature/onboarding/impl/src/test/kotlin/com/tailormyresume/feature/onboarding/impl/navigation/OnboardingEntryKeysTest.kt: deleted by the lead in 79088176 (D55); it asserted ReviewProfileNavKey is a placeholder
- feature/onboarding/impl/src/test/kotlin/com/tailormyresume/feature/onboarding/impl/navigation/OnboardingRealEntryHiltTest.kt: edited after red to wait on the real entry; assertions kept
- feature/onboarding/impl/src/test/kotlin/com/tailormyresume/feature/onboarding/impl/review/ReviewScreenTest.kt: edited after red to scroll to nodes (performScrollTo); assertions kept

## Follow-ups
- OnboardingEntryKeysTest.everyKeyResolvesAndShowsNameInTextColour asserts ReviewProfileNavKey is a placeholder and now fails (not edited, per D55); the lead removes it. It is the only failing test in verify-local.
- #339.
- Delegate-written files: none (0 delegate calls).

🤖 Generated with [Claude Code](https://claude.com/claude-code)




## Reviewer B fixes
- Merged origin/revamp/274-upload (53d4d88d); ReadingViewModel.kt conflict resolved per integrate note 1 (base personNameOrNull Contact line and import, lane countLogicalEntries x3; dropped ProfileEntry, continues, looksLikeName imports).
- Required banner message now uses TmrFitText; ReviewLayoutTest.requiredBannerMessageWithALongTitleNeverBreaksInsideAWordAtFontScale2 added.
- verify-local.sh (BASE_REF=origin/revamp/274-upload) green after the merge.
