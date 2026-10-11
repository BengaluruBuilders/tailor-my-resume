# Issue 264 lead-only test replacement

Apply with `git apply issue-264-test-replacement.patch` on branch revamp/264-paige-tokens at head 0d32ccf6 or later.
Proved green in a scratch worktree at 0d32ccf6: `./gradlew :core:designsystem:testDebugUnitTest` BUILD SUCCESSFUL. Thresholds 4.5 and 3.0 are unchanged (decision D32).

Three files change. TmrDisabledButtonTest needs no change: `disabledContent` (#7D7D7D) on card (#111111) is 4.59, so the original 4.5 assertion on card still passes.

| Case | Change | Reason |
|---|---|---|
| TmrPaletteContrastTest.appSurfacePaletteMatchesThePaigePrototype | card, document, brand, onBrand expect #111111, #1C1C1C, #A3F43F, #0A0A0A | Avvio values replaced by Paige |
| TmrPaletteContrastTest.textAndActionPairsMeetNormalTextContrast | 4 pairs added (textSecondary/background, textMuted/surfaceHigh, textDisabled/background, amber/surface), all >= 4.5 | Paige text pairs |
| TmrBoundaryContrastTest.boundaryTokenIsThreeToOneOnSurfaceAndCard | unchanged: still measures `boundary` at 3.0 on background, surface, card, sheet. `boundary` = textDisabled = #7D7D7D: 5.10 on background, 4.59 on surface and card | D32 |
| TmrBoundaryContrastTest.boundaryTokenDiffersFromHairlineToken | outlineVariant = #2A2A2A, boundary = #7D7D7D | Paige values |
| TmrThemeDarkOnlyTest.noLightSymbolsRemain | res/font must hold exactly the 3 Paige TTFs | Fonts are bundled |
| TmrThemeDarkOnlyTest.typographyUsesPaigeFamilies (was typographyUsesDefaultFamily) | mono and grotesk are not Default; sans = grotesk, display = mono | Paige families |
