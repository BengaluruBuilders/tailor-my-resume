# Issue 261 test replacement: removed cases

Patch: issue-261-test-replacement.patch (against f46a1f88). Result: `:core:designsystem:testDebugUnitTest` 30 tests, 0 failures, 0 skipped.

## Removed whole test cases (6)

| File | Case | Reason |
|---|---|---|
| TmrStatusBarIconsTest | headerlessWhiteTopInLightThemeUsesDarkIcons | light case |
| TmrStatusBarIconsTest | whiteInnerHeaderInLightThemeUsesDarkIcons | light case |
| TmrStatusBarIconsTest | lightThemeHeaderTopUsesDarkIcons | light case |
| TmrStatusBarIconsTest | whiteHeaderWithLightTopTrueUsesDarkIcons | light case |
| TmrA11ySemanticsTest | confirmDialogPanelTitleIsHeadingWithPaneTitle | deleted component TmrConfirmPanel |
| TmrA11ySemanticsTest | loadingWheelIsIndeterminateProgressWithLiveRegion | deleted component TmrLoadingWheel |

## Ported to dark (kept, not removed)
TmrDisabledButtonTest keeps both cases, now with TmrDarkColors and `TmrPreviewTheme { }`. Same assertions and the same 4.5 contrast threshold, except one: `assertNotEquals(onSurfaceVariant, disabled)` in disabledTextButtonLabelIsDimNotPrimaryNorPlaceholder is dropped, because in the dark palette disabledContent and onSurfaceVariant are the same token (0xFF8C918E, Palette.kt), so it fails by design. The primary/disabledContent equalities and the contrast check remain.

## Removed partial assertions inside surviving cases (light halves only)

| File | Case | Removed |
|---|---|---|
| TmrPaletteContrastTest | appSurfacePaletteMatchesTheClaudeDesignCanvas | `with(TmrLightColors)` block (5 asserts) |
| TmrPaletteContrastTest | textAndActionPairsMeetNormalTextContrast | "light" entry in the mode list |
| TmrPaletteContrastTest | statusBarIconPolarityFollowsTheTopSurface | 2 asserts using TmrLightColors |
| TmrBoundaryContrastTest | boundaryTokenIsThreeToOneOnSurfaceAndCard | "light" entry |
| TmrBoundaryContrastTest | boundaryTokenDiffersFromHairlineToken | 2 light asserts (assertNotEquals, outlineVariant 0xFFD5D8D2) |

## Other mechanical edits
- `darkTheme = ...` arguments dropped (TmrPreviewTheme no longer has it) in TmrA11ySemanticsTest and TmrStatusBarIconsTest (`darkIconsAfter` lost its parameter).
- Unused imports removed: TmrLightColors, assertTrue (StatusBarIcons), LiveRegionMode, ProgressBarRangeInfo, hasTestTag.
- No threshold changed.
