# issue-272 root tests patch

Apply on revamp/272-session-gate head with `git apply docs/.../issue-272-root-tests.patch`; without it the app unit tests do not compile.
- AppViewModelTest (deleted): encoded AppRootState.FirstRun/Main transitions (onboarding flag both ways, offline sign-out, same-account re-sign-in without wipe, signed-in-incomplete = FirstRun). Every case is re-asserted as Ready(start, accountId) in AppViewModelGateTest (12 tests, includes the offline-gateway sign-out and no-wipe cases).
- RootViewModelStoresTest: NavigationRoot.FirstRun/Main became AccountRoot(null)/AccountRoot("account-1"); all 5 assertions kept verbatim. RootViewModelStoresAccountTest adds the N-account keepOnly case.
- DebugPreviewRootTest / DebugPreviewNavigationRootTest: AppRootState.FirstRun/Main became Ready(SignIn,null)/Ready(Applications,"account-1"), NavigationRoot became the debug-only PreviewRoot; all assertions kept.
- DebugPreviewKeepOnlyTest: initial state Ready(Applications), NavigationRoot.Main became PreviewRoot.Main.accountRoot; assertion kept.
Coverage is at least as strong: no assertion removed or loosened except the deleted class, whose cases are all in the gate test.
