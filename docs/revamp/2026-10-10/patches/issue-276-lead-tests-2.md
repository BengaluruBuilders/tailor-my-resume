# #276 lead test patch 2

Six existing app tests fail on the #276 head: NewApplicationTest.plusPushesJobOnTopOfApplicationsAndProfile,
ShellChromeScreenshotTest (chromeStates1x, chromeStatesFont200At337dp), ShellWalkTest (systemBackMatchesTopBarBack,
walkAssertsChromeOnEveryScreen), TabBarVisibilityTest.tabsOnlyOnApplicationsAndProfile.
Cause: ShellHarness.show uses the shell's default entry provider, which now hosts the real Job and JobLink routes
(hiltViewModel needs a Hilt activity; the tests use a plain ComponentActivity and expect the "JobNavKey" label text).
Patch: ShellHarness builds its own entry provider with label entries for Job, JobLink, JobResult and QuickQuestion and the
real entries for every other feature. Verified: :app:testDemoDebugUnitTest green with the patch applied
(git apply on revamp/276-add-job at the feature commit). Not applied on the branch; nothing pushed.
