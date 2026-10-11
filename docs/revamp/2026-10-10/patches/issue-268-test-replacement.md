# Issue 268 test replacement: cases changed by design

Patch: issue-268-test-replacement.patch (against branch revamp/268-simplified-data, 16 files). Apply with `git apply`.
Removed behaviour: `JobApplication.notes` and `updateNotes`, `ApplicationStatus.NO_RESPONSE`, `UsageAllowance` (StoredUsageAllowance, TestUsageAllowance, WalletUsageAllowance).

## Removed whole test cases
| File | Case | Reason |
|---|---|---|
| JobApplicationDaoTest (androidTest) | updateNotes_changesNotesAndUpdatedAt | DAO.updateNotes removed |
| OfflineFirstApplicationRepositoryTest | updateNotesChangesNotesAndStampsUpdatedAt | updateNotes removed |
| TestApplicationRepositoryTest | updateNotes_changesOnlyNotes | updateNotes removed |
| RemotePaymentGatewayTest | allowanceFallsBackToTheContractDefaultsWhenTheWalletCannotBeFetched, usageAllowanceReadsTheWallet, consumingAnAllowanceNeverDecrementsLocally | WalletUsageAllowance removed |
| StoredUsageAllowanceTest | whole file deleted (contract cases and usageSurvivesARestartOfTheAllowance) | StoredUsageAllowance removed; last reference to UsageAllowance in the repo |
| TestFakesContractTest | TestUsageAllowanceContractTest class and imports | TestUsageAllowance removed |

## Edited assertions
| File | Case | Change |
|---|---|---|
| OfflineSampleDataControllerTest | theFourApplicationsHaveTheAgreedTitles..., theNewestApplicationUpdateComesFirst... | NO_RESPONSE expectation becomes APPLIED (Meridian GCC sample maps to APPLIED) |
| CreateApplicationUseCaseTest | savesApplicationWithSavedStatus... | drop `saved.notes` isEmpty assertion |
| AccountDataArchiveWriterTest | applicationsTextShowsJdAnalysisResumeAndNotes (renamed ...AndResume), everyLineOfAMultiLineValueIsIndented, myDataJsonHoldsEveryApplicationField | notes input and the 3 notes assertions removed; archive no longer reads notes |
| TestApplicationRepositoryTest | upsertApplication_replacesApplicationWithSameId | edited copy uses `location` instead of `notes` |

## Mechanical compile fixes (no assertion change)
`notes = ...` argument removed in TestModels, TailorTestData, UpdateBulletDecisionUseCaseTest; `updateNotes` overrides removed in FakeJobApplicationDao, Fakes.kt, DeleteAccountUseCaseTest, DeleteAccountUseCasePreviewTest.

Not edited: LocalDataWiperTest (entity keeps `notes` with a default), AiFailureMappingTest and TailorMyResumeApiTest (their NO_RESPONSE is OkHttp SocketPolicy).
