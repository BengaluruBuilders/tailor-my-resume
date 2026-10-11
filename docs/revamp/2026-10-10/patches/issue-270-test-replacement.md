# Issue 270 test replacement

Test: `OfflinePaymentGatewayContractTest.theCatalogueHoldsOnlyTheFiveApplicationPack` (core/domain, gateway).

Why it changes by design: #270 (D33, PLAN credit packs) makes the mock catalogue hold the 5, 15 and 40 credit packs, with the prototype prices (19,900 / 44,900 / 99,900 paise). The old case asserted a single pack at 14,900 paise.

Replacement: `theCatalogueHoldsTheFiveFifteenAndFortyCreditPacks` (patch `issue-270-test-replacement.patch`). Proven green in a scratch worktree on the #270 head: `:core:domain:testDebugUnitTest :core:domain:koverVerify`.

Apply on the #270 branch, commit as the lead's test replacement, then rerun `tools/ci/verify-local.sh`.
