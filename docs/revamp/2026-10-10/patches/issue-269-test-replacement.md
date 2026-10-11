# issue-269 lead-only test edit

Apply on branch revamp/269-coverage-ui-domain: `git apply docs/revamp/2026-10-10/patches/issue-269-test-replacement.patch` (from repo root).
Change: ObserveStartDestinationUseCaseTest and app AppViewModelTest construct the use case with `TestProfileRepository()` as second argument; assertions untouched. Without it the test source sets do not compile (new ProfileRepository parameter; the plan expected AppViewModelTest unchanged but it constructs the use case too).
Proven green: applied in the lane worktree, `:core:domain:testDebugUnitTest :core:domain:koverVerify -PwarningsAsErrors=true` passes.
