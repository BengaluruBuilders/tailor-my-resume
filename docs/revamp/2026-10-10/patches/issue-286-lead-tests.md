# #286 lead test patch (apply in the issue-286 worktree: `git apply docs/.../issue-286-lead-tests.patch` from its root)

Contract v2 drops `section` and changes `ResumeTailor.tailor`, so five existing tests no longer compile or pin the old contract. No assertion is weakened.
- DtoRoundTripTest: `nullFieldsAreSentOnTheWire...` pinned `"section":null`; replaced by a test that the request has no `section`, no `answer` and no `null` on the wire (stricter, v2 CD1).
- RemoteResumeTailorTest: `aSectionRegenerationSendsTheSection` replaced by `aTailoringRequestNeverSendsASection`; two `tailoring.request.app-1.all` key reads become `tailoring.request.app-1` (same assertions, new key per scout shared_contracts).
- TailorResumeUseCaseTest (3 anonymous tailors), CreateApplicationUseCaseTest (1): `section: EntryCategory?` becomes `answer: QuickAnswer?` in the override signature only; every assertion and the unused import handling is the only change.
- SignOutCleanerTest: `idFor("app-1", null)` becomes `idFor("app-1")` (section parameter removed); same assertions on the wipe.
Coverage: the new tests in the red commit (AnalysisRequestJsonTest, RemoteResumeTailorRequestIdTest, RemoteResumeTailorAnswerTest) assert the v2 request shape; all other existing assertions are untouched.
