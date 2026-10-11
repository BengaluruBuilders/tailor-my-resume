Replacement for the existing tests that encode the removed two-level (tab sub-stack) navigator; apply with `git apply` on revamp/271-nav-shell.
- NavigatorOwnTabTest, NavigatorReturnToTopLevelTest (deleted): openInOwnTab/returnToTopLevel and per-tab sub-stacks no longer exist; covered by NavigatorRootTest (root, tab switch) in the red commit.
- NavigatorTest (rewritten): same cases (navigate, single top, pop one/many, goBack true/false, canGoBack, navigateAll, replace) on the single stack; tab-only cases dropped; adds canHandleBack.
- NavigationStateRestoreTest (rewritten): "restored for another start key opens new start" encoded dropRestoredStrangers (removed; session gate #272 roots SignIn instead); now stack+current screen survive recreation, and a rooted stack survives.
- TopLevelDestinationTest/TopLevelNavItemTest: Settings is no longer a tab; keys list is now TOP_LEVEL_NAV_KEYS (Applications, Profile).
- Onboarding/Profile NavKeyPlaceholderEntryTest, Analysis/Applications/Settings EntryProviderTest (deleted): used the old NavigationState ctor and deleted keys; superseded by the six *EntryKeysTest (every key, text colour asserted, stricter).
- StartOverNavigationTest: ImportResume replaced by Upload; same assertion.
