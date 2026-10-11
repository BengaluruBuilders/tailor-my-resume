# TailorMyResume design system catalogue

Module: `core:designsystem`. Package root: `com.tailormyresume.core.designsystem`.
The current visual direction is [AVVIO_REDESIGN.md](AVVIO_REDESIGN.md). The old Jade frame exports
remain a record of content and states. The canvas in `design/avvio-canvas/README.md` is the source for
tokens: black and white grounds, neutral cards, lime `#AEFF00`, Manrope with Archivo Black headlines.

Read tokens with `TmrTheme.colors`, `TmrTheme.typography`, `TmrTheme.spacing`, `TmrTheme.shapes`,
`TmrTheme.elevation`, `TmrTheme.motion`, `TmrTheme.isDark`. Dark mode follows the system.
Pass all user text as parameters. Components have no string resources.
Use only the names in this file. The old alias names are gone.

## Changes for feature adoption

A feature must change these things when it adopts the integration changes.

1. Bottom bar zone. `TmrScreen` now ends the content area at the top of the bar zone. Remove any
   spacer, extra bottom padding, or `Modifier.padding(bottom = ...)` that you added to keep content
   clear of the bar. Use the `PaddingValues` that `TmrScreen` gives you and nothing else.
2. Notice above the bar. Move a notice card or reason text that sits above the bar out of the
   scrolling content. Pass it as `bottomBarNotice = { ... }`. Keep `creditDisclosure` inside
   `TmrBottomActionBar` for the credit line only.
3. Stacked bar. Do nothing. `TmrBottomActionBar` stacks its actions when the font scale is 1.5 or
   more, in the reverse order of the row. Write the actions as in the row (outline action first, primary
   action last). The primary action is then first in the stack. Give each action `Modifier.weight(1f)` as
   before. Remove any local stacked layout.
4. Toasts. Replace local snackbar copies with `rememberTmrToastState()`, `TmrToastHost(state)` in
   `TmrScreen(snackbarHost = ...)`, and `state.show(message, actionLabel)`. Remove every
   `androidx.compose.material3.Snackbar*` import from the feature.
5. Icons. Replace substitute icons with `TmrIcons.Lock`, `Link`, `Info`, `Download`, `ExpandMore`,
   `ExpandLess`, `Description`, `Flag`, `Share`, `OpenInNew`, `Search`.
6. Coverage block. For a block with no partial count, pass `partial = null, partialLegend = null`.
   `KeywordCoverageMeter` already does this. `TmrCoverageBlock(summary = ...)` adds one line between the
   fraction and the bar (Gap analysis: "You cover 9 of 14 key terms"). 
7. Home header. A home screen with a list uses `TmrCollapsingHomeHeader`. Do not swap two headers on a scroll position. The swap moves the list by
   the height difference in one frame, and the list flickers.
8. Dock constants. Use `TmrDockDefaults.height` and `inset`. Do not repeat the arithmetic.
9. Motion. `TmrTheme.motion` has `proofSpecs`, `hopSpecs`, and `reduced`. The old Int durations and easings are gone.
10. Fact ids. Show `FactDisplayIds` (module `core:domain`) values, not raw bullet ids. See
    `docs/MOCK_BACKEND.md`.

## Tokens

The app is dark only. The values come from the plan for the revamp (GitHub issues labelled `revamp`) and the
prototype in `design/prototype-2026-10-10/`. The phone screen is 374 x 834 dp. Every token from the
older Avvio palette keeps its name as a computed alias of a Paige token, so existing components
compile unchanged. `disabledContent` is `textDisabled` and `onSurfaceVariant` is `textMuted`; they
differ on purpose.

### Colour (`TmrTheme.colors`)

| Token | Value | Use |
|---|---|---|
| `background` | `#000000` | Every screen except Sign in |
| `surface` | `#111111` | Cards, option rows |
| `surfaceRaised` | `#161616` | Raised cards |
| `surfaceHigh` | `#1C1C1C` | Top-bar buttons, step pills, done steps |
| `sheet` | `#141414` | Bottom sheet |
| `sheetOption` | `#1A1A1A` | Unselected option in a sheet |
| `uploadCard` | `#0D0D0D` | Upload card fill |
| `fill` | `#242424` | Chips, inputs |
| `disabledFill` | `#222222` | Disabled primary button |
| `line` | `#2A2A2A` | Unselected option border, lines |
| `lineStrong` | `#333333` | Toggle off track |
| `lineHigher` | `#3A3A3A` | Dashed upload border, sheet handle |
| `tabDivider` | `#1A1A1A` | Tab bar top line |
| `text` | `#FFFFFF` | Primary text |
| `textSecondary` | `#C8C8C8` | Body copy |
| `textMuted` | `#A6A6A6` | Labels, later steps |
| `textDisabled` | `#7D7D7D` | Placeholder, disabled, pending rows |
| `ink` | `#0A0A0A` | Text on light fills |
| `lime` | `#A3F43F` | Primary, selected, Interview |
| `limeSelected` | `#141A0A` | Selected option fill |
| `limeSoft` | `#E4FBB8` | Keyword highlight on the paper |
| `amber` | `#F7A940` | Needs attention, Applied, Offer fill |
| `amberHighlight` | `#FFE2C2` | Answer highlight on the paper |
| `blue` | `#5AA9F8` | Saved, resume and profile hero |
| `cheek` | `#FF6A2B` | Paige cheeks, DOCX sticker |
| `paper` | `#FFFFFF` | Resume paper, toast, Paige page |
| `paperFold` | `#DCDCD5` | Paige folded corner |
| `segOffer` | `#E9E8E4` | Offer segment in the status bar |
| `segRejected` | `#555555` | Rejected segment |
| `rejectedBorder` | `#444444` | Rejected chip border |
| `scrim` | `#000000` at 65% | Sheet scrim |

`textDisabled` (`#7D7D7D`) is 4.59:1 on `surface` and `card` and 5.10:1 on `background`. `boundary` and
`outline` alias it, so component borders reach 3:1. Use it for placeholder, disabled and pending
text and for component borders.

### Type (`TmrTheme.typography`)

Space Mono (400 and 700) and Space Grotesk (a variable font, weights 400 to 600 and 700) are bundled in
`core/designsystem/src/main/res/font/` as `space_mono_regular.ttf`, `space_mono_bold.ttf` and
`space_grotesk.ttf`. The OFL licences are in `core/designsystem/fonts-licenses/`. Text uses `sp`.
Letter spacing is in `sp`; line height is the ratio times the size.

| Token | Family and weight | Size / line height / spacing |
|---|---|---|
| `headline` | Space Mono 400 | 28 / 30.8 / -1.0 |
| `headlineSmall` | Space Mono 400 | 26 / 28.6 / -1.0 |
| `title` | Space Mono 400 | 22 / 25.3 / -0.6 |
| `display` | Space Mono 700 | 34 / 34 / -1.5 |
| `displayLarge` | Space Mono 700 | 60 / 60 / -3 |
| `label`, `labelWide` | Space Mono 400 | 12 / - / 0.6 and 0.8; the component uppercases |
| `button` | Space Mono 400 | 13 |
| `mono15`, `mono14` | Space Mono 400 | 15 and 14 |
| `body` | Space Grotesk 400 | 15 / 21.75 |
| `bodyLarge` | Space Grotesk 400 | 16 |
| `bodySmall` | Space Grotesk 400 | 14 / 21 |
| `caption` | Space Grotesk 400 | 13 |
| `strongLarge`, `strongSmall` | Space Grotesk 600 and 700 | 15 and 14 |

Legacy names (`displayL`, `headlineM`, `titleL`, `bodyM`, `labelL`, `numeralHero`, `factId` and the
rest) map onto the nearest Paige style. The `paper*` styles come with the resume paper preview.

### Shape (`TmrTheme.shapes`)

`pill` is a full pill. `hero` 32, `cardLarge` 24, `card` 20, `cardSmall` 16, `toast` 18,
`paperCorner` 12 and `bar` 2 dp. The older `sheet`, `modalSheet`, `field` and `pillRow` shapes stay.

### Spacing (`TmrTheme.spacing`)

`gutter` 14, `cardPadding` 16, `sectionGap` 24, `touch` 48, plus the named steps from 2 to 64 dp.
Paige sizes: `topBarButton` 44, `primaryButtonHeight` 56, `tabItem` 54, `tabCentreDisc` 56,
`toastTop` 96, `sheetPaddingTop` 12, `sheetPaddingHorizontal` 18, `sheetPaddingBottom` 40,
`sheetHandleWidth` 44 and `sheetHandleHeight` 4 dp.

### Icons

`TmrIcons` adds `ChevronRight`, `Gear`, `Plus`, `File`, `Open` and `GoogleG`. `GoogleG` is a
48 x 48 vector with the four brand-coloured paths, not an asset.

### Elevation (`TmrTheme.elevation`)

`level0` to `level4`, `hero` (level 2), `dock` (level 3), `modal` (level 4).
Use `Modifier.tmrShadow(shadow, shape)`.

### Motion (`TmrTheme.motion`)
`idle` holds the ambient loops. Paige bob: `bobAmplitude` 5 dp, translateY = 5 x sin(t / 420 ms +
phase), `bobAngularPeriodMs` 420 and `bobPhaseMax` 4. Tailoring wobble: `wobbleDegrees` 4 with
`wobbleAngularPeriodMs` 300, and `wobbleOffset` 6 dp with a 210 ms angular period. Story bars last
`storyDurationMs` 5000 per slide. The spinner turns once per `spinnerTurnMs` 900. When `reduced` is
true, every amplitude is 0, the story slides do not auto-advance, the spinner is a static ring, and
taps still step. `TmrIdleSpecs.bobOffsetDp(timeMs, phase)` is the bob formula.

`proofSpecs`: `spatial`, `spatialFast`, `offset`, `size`, `fade`, `color`, `staggerMs`, `staggerMax`.
`hopSpecs`: `spatial`, `scale`. Use `hopSpecs` only for gap closed, exported, pack purchased,
first fact confirmed. `reduced` is true when animations are off; the spatial specs then use `snap()`.
There are no duration fields and no easing fields. `size` and `color` repeat the board values of
`spatial` and `fade` for the `IntSize` and `Color` value types.

The primitives are in `component/TmrMotion.kt`. A feature calls a primitive. A feature never calls
`tween(`, `spring(`, or a numeric duration.

| Need | Primitive | Token |
|---|---|---|
| Show or hide a part | `TmrVisibility(visible)` | `fade` |
| Show or hide a part with a 12 dp rise | `TmrVisibility(visible, rise = true)` | `fade`, `offset` |
| Switch between loading, content, empty, and error | `TmrContentSwitch(targetState, contentKey = { it::class })` | `fade` |
| Press feedback, scale 0.97 | `Modifier.tmrPressScale(interactionSource)`. `Tmr*` buttons and cards with `onClick` have it. | `spatialFast` |
| Expand and collapse a block | `TmrExpandable(expanded)` | `size`, `fade` |
| List enter: 12 dp rise, 30 ms stagger, first 6 items | `rememberTmrListEnterState()`, then `Modifier.tmrListEnter(state, index)` on each item | `spatial`, `staggerMs`, `staggerMax` |
| Navigation forward: the new screen rises 48 dp over the old screen | `rememberTmrNavTransitions().forward(scope, hierarchical = true)` | `fade`, `offset` |
| Navigation back and predictive back: the screen sinks 48 dp | `rememberTmrNavTransitions().back(hierarchical = true)` | `fade`, `offset` |
| Dock tab switch: the new screen fades in and shifts 24 dp from the side of the tapped tab | `tab(scope, direction, pop)`. `direction` is the sign of the tab index change | `fade`, `offset` |
| A tab screen reached from a pushed screen: fade only | `forward(scope, hierarchical = false)` and `back(hierarchical = false)` | `fade` |
| Selected colour of a chip or a checkbox | Built into `TmrFilterChip`, `TmrCheckbox` | `color` |
| Dock selection: the icon and label of the selected item change tint | Built into `TmrDockItem` | `color`, `spatialFast` |
| Progress moment | `Animatable` with a `hopSpecs` value | `hopSpecs.scale`, `hopSpecs.spatial` |

Rules of the primitives:
- If `reduced` is true, nothing moves. A fade stays, and the state change stays visible.
- The list enter plays one time, on first composition. It does not play again when the person comes
  back to the screen.
- The 48 dp rise is the sheet rise of the board. It moves the whole pushed screen, because a pushed
  screen has no sheet surface and its content can load after the first frame.
- Give `TmrContentSwitch` a `contentKey` for a state that carries data. Without it, each data change fades.
- Every spec is a spring or a 150 ms fade, so a new target interrupts the old one. Input never waits.
- A settled frame is the same as the frame without motion. Screenshot baselines do not change.

Do not animate:
- The size, the padding, or the position in the layout of an item in a lazy list. Use `graphicsLayer`
  alpha, translation, and scale only. Do not put `TmrExpandable` in a lazy list item.
- Blur, `RenderEffect`, or a shadow.
- A second stagger chain while one chain plays.
- The multi-frame sequences of the board: gap-analysis reveal, bullet diff wipe, source-link connector.
- Navigation, an error, or text that the person must read, with `hopSpecs`.
- `TmrBottomSheet` and `TmrSwitch`. Material moves them with its standard motion scheme.

## Screen chrome

```kotlin
fun TmrScreen(modifier, header: (@Composable () -> Unit)? = null, sheet: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null, snackbarHost: @Composable () -> Unit = {},
    floatingAction: (@Composable () -> Unit)? = null,
    bottomBarNotice: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit)
```
Compose header, overlapping content, and a bar. Use `sheet = true` under a home header. Use
`sheet = false` with hero cards. Board: Home header, Inner header, Sheet, Bottom action bar.

The bar zone. If there is a `bottomBar`, the zone is the notice plus the bar plus the navigation
bar inset (the bar measures its own inset). The content area ends at the top of the zone. Content
is clipped there and never scrolls behind the notice or the bar. The sheet and the screen
background still fill the area behind the zone.

`PaddingValues` that `content` receives:

| Case | top | bottom |
|---|---|---|
| No header | status bar inset | see below |
| Header and `sheet = true` | 24 dp | see below |
| Header and `sheet = false` | 0 dp | see below |
| `bottomBar` or `bottomBarNotice` set | as above | 20 dp (`spacing.gutter`), the gap above the last item |
| Neither set, no dock | as above | navigation bar inset |
| Neither set, dock inset provided (`LocalTmrBottomInset`) | as above | navigation bar inset plus 88 dp; add 48 dp at 150% font scale or more |

The old bottom padding was the bar height plus the gutter. It is now 20 dp, because the content area
no longer reaches under the bar. A feature that passed the `PaddingValues` to its list needs no
change. A feature that added its own bar clearance must remove it, or the clearance is doubled.

`bottomBarNotice` is placed directly above the bar with a 20 dp side gutter and an 8 dp gap to the
bar. Put a notice card or a reason line in it. Without a bar, it sits above the dock inset. The
`snackbarHost` and `floatingAction` lift above the notice.

Content runs behind the floating dock. It is not clipped. The bottom padding above lets the last item scroll
clear of the dock.

```kotlin
fun TmrCollapsingHomeHeader(collapse: TmrHeaderCollapseState, title: String, greeting: String, headline: String,
    modifier, trailing, action, illustration)
fun rememberTmrHeaderCollapseState(): TmrHeaderCollapseState
```
The home header for a screen with a list. The scroll sets the height. Fully collapsed, it is a short Deep bar (84 dp including the 24 dp overlap)
with the title and the trailing slot. Put `Modifier.nestedScroll(collapse.connection)` on the list.
A scroll up collapses the header before the list moves. A scroll down expands the header after the
list is at the top. Parallax: the headline, the action, and the hero move up at half the speed of
the sheet, the sheet covers them, and they fade out in the first half of the collapse. The greeting
fades out in the first half. The title fades in during the second half, so the two texts never show
together. The trailing slot stays visible. If `TmrTheme.motion.reduced` is true, there is no parallax:
the headline, the action, and the hero stay in place while the sheet covers them.
The scroll drives the collapse, so there is no motion token.

```kotlin
fun TmrInnerHeader(title: String, modifier, subtitle: String? = null, onBack: (() -> Unit)? = null,
    backContentDescription: String = "", backIcon: ImageVector = TmrIcons.ArrowBack,
    trailing: (@Composable () -> Unit)? = null, belowTitle: (@Composable () -> Unit)? = null,
    extended: Boolean = true, overlap: Dp = ...)
```
Header of every pushed screen. Titles align to the start. `extended = true` is at least 164 dp for hero cards (overlap 80).
Board: Inner header + hero card.

```kotlin
fun TmrSheet(modifier, contentPadding: PaddingValues = ..., content: @Composable ColumnScope.() -> Unit)
```
Surface with 24 dp top corners.

```kotlin
fun TmrDock(modifier, content: @Composable RowScope.() -> Unit)
fun RowScope.TmrDockItem(selected: Boolean, onClick: () -> Unit, label: String, modifier,
    icon: @Composable () -> Unit)
fun TmrDockIcon(icon: ImageVector)
```
Floating 64 dp `tool` pill, inset by the gutter at the sides and 8 dp below, for the three top-level tabs only.
Each item is an icon above a `labelM` label and takes an equal share of the width. The selected item is
`onToolSelected` (lime), the others `onToolVariant`. In dark the pill has an `outlineSoft` hairline.
At 150% font scale or more, the dock grows to 112 dp, adds 16 dp interior side padding,
and lets each label wrap to two lines. `TmrScreen`
adds the matching 48 dp to the dock clearance so the last list item remains reachable.
`TmrDock` applies the navigation bar inset. Do not add it in `:app`.
Put each `TmrDockItem` inside `TmrDock`; it is a `RowScope` extension.

```kotlin
fun TmrBottomActionBar(modifier, contentPadding: PaddingValues? = null,
    creditDisclosure: (@Composable () -> Unit)? = null, actions: @Composable RowScope.() -> Unit)
```
Full-width bar on the page background with a hairline on top, for 1 to 3 actions, 10 dp between actions.
Give each action `Modifier.weight(1f)`. Board: Bottom action bar.

```kotlin
fun TmrBottomActionBar(modifier, contentPadding: PaddingValues? = null,
    creditDisclosure: (@Composable () -> Unit)? = null,
    stacked: Boolean = LocalDensity.current.fontScale >= 1.5f,
    actions: @Composable RowScope.() -> Unit)
```
`stacked` puts one action per line, each at full width, in the reverse of the order given. The primary
action comes last in the row, so it comes first in the stack. The default turns it on at
a font scale of 1.5 or more. Pass `stacked = true` or `false` to force a mode. Pass `primaryLast = false` when
the first action is the primary one (Welcome); the stack then keeps the order given.

```kotlin
object TmrDockDefaults { val height: Dp /* 64 */; val inset: Dp /* 88 */ }
```
`inset` is `LocalTmrBottomInset` for a tab screen. `:app` uses these values.

## Actions
All buttons are 60 dp pills with the `button` type style. A disabled filled button is grey (`outlineVariant`
fill, `onSurfaceVariant` text). An outline button has an ink border.

```kotlin
fun TmrPrimaryButton(label: String, onClick: () -> Unit, modifier, enabled = true, leadingIcon: ImageVector? = null, trailingIcon: ImageVector? = null)
fun TmrSecondaryButton(...same...)   // tint
fun TmrOutlineButton(...same...)
fun TmrTextButton(...same...)
fun TmrDestructiveButton(label, onClick, modifier, enabled = true, leadingIcon = null)
fun TmrInkButton(label: String, onClick: () -> Unit, modifier, enabled = true, leadingIcon = null, trailingIcon = null) // ink fill, light in dark
fun TmrButton(onClick, modifier, enabled, contentPadding, content: RowScope.() -> Unit)      // primary, slot form
fun TmrButton(onClick, modifier, enabled, text: @Composable () -> Unit, leadingIcon: ...)
fun TmrOutlinedButton(...same two forms...)
```
Board: Buttons.

```kotlin
fun TmrIconButton(icon: ImageVector, contentDescription: String, onClick, modifier, enabled = true, tint, containerColor, borderColor, shape, size: Dp = 48.dp)
fun TmrHeaderIconButton(icon, contentDescription, onClick, modifier)
fun TmrBackButton(contentDescription, onClick, modifier, onHeader: Boolean = false)
```
`TmrHeaderIconButton` is a 48 dp Bone circle with a Deep icon on the dark header.
`TmrBackButton` is the only back control: the `TmrIcons.Back` chevron in a borderless 48 dp circle. With
`onHeader = true` it is an `TmrHeaderIconButton`; otherwise the fill is `card` and the icon is `onSurface`.

## Surfaces
```kotlin
fun TmrCard(modifier, contentPadding: PaddingValues? = null, trailingAction: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null, content: ColumnScope.() -> Unit)
fun TmrHeroCard(modifier, contentPadding, onClick: (() -> Unit)? = null, content: ColumnScope.() -> Unit)
fun TmrMonogram(text: String, modifier, size: Dp = 40.dp)
```
Card: quiet neutral fill, 1 dp border, 20 dp corners. Hero card: document fill, 22 dp corners, subtle shadow. Board: Application card, Bullet review card.

```kotlin
enum class TmrPillRowStyle { Coral, Jade, Marigold, Ink, Neutral }
fun TmrPillRow(title: String, onClick: () -> Unit, modifier, style = Coral, subtitle: String? = null,
    icon: ImageVector? = null, monogram: String? = null, trailingIcon: ImageVector? = ArrowForward)
```
A pill list row: 72 dp tall, 24 dp radius, a 48 dp icon circle and a Deep, Lime, or quiet neutral surface. `Ink` inverts to light in dark. Board: Pill list rows.

```kotlin
fun TmrIconActionBar(secondaryIcon: ImageVector, secondaryContentDescription: String, onSecondaryClick: () -> Unit,
    primaryLabel: String, onPrimaryClick: () -> Unit, modifier, primaryEnabled = true,
    primaryTrailingIcon: ImageVector? = null, secondaryBadge: String? = null, ink: Boolean = false)
```
The bottom action bar of the boards: a round 60 dp secondary icon button (with an optional marigold count
badge) and a 60 dp primary pill. `ink = true` makes the primary an ink pill.

## Status, chips, tags
```kotlin
enum TmrStatusKind { Met, Partial, Gap }
fun TmrStatusDisc(kind, modifier, size: Dp = 18.dp, contentDescription: String? = null)
fun TmrStatusChip(kind, modifier, label: String? = null, onHero: Boolean = false)   // disc and word; onHero is a white pill
fun TmrApplicationStatusChip(kind: TmrApplicationStatusKind, modifier, label: String? = null, dotSize: Dp)
fun TmrProvenanceChip(kind: TmrProvenanceKind, modifier, label: String? = null)
fun TmrFactId(id: String, modifier)
```
Always pass `label`. The default label is the enum name and is not user copy.
Board: Chips, pills and the report action; Requirement rows.

## Content
```kotlin
fun TmrCoverageBlock(met: Int, partial: Int?, gap: Int, caption: String, metLegend: String, partialLegend: String?, gapLegend: String, modifier, summary: String? = null)
fun TmrCoverageBar(met: Int, partial: Int, gap: Int, modifier)
fun TmrEvidenceMark(text: String, modifier, style: TextStyle? = null, tint: Color? = null)
fun evidenceMarkSpanStyle(): SpanStyle
fun TmrEvidenceText(text: AnnotatedString, modifier, style, color)
fun TmrStepProgress(stepNames: List<String>, currentStepIndex: Int, modifier, ordinalLabel: String? = null, stepDetails: List<String?> = emptyList(), footnote: String? = null)
fun TmrFilterChip(label, selected, onClick, modifier, leadingIcon, count, colors)
fun TmrDivider(modifier, style, thickness, color)
```
Coverage block shows `met / total` as plain text plus one segment per key term. There is no gauge.
Pass `partial = null` to hide the partial legend and to count no partial segment.
For the evidence underline inside running text use `TmrEvidenceText` with spans from
`evidenceMarkSpanStyle()`. A plain `Text` shows only the fill. Board: Coverage block, Bullet review card, Step progress.

## Inputs and overlays
```kotlin
fun TmrTextField(value, onValueChange, modifier, label, placeholder, supportingText: (@Composable () -> Unit)?, errorText: String?, trailingSlot, enabled, singleLine, minLines, visualTransformation, keyboardOptions, keyboardActions)
fun TmrCheckbox(checked, onCheckedChange, modifier, enabled = true)
fun TmrSwitch(checked, onCheckedChange, modifier, enabled = true)
fun TmrBottomSheet(onDismissRequest, modifier, sheetState, contentPadding, title: String? = null, subtitle: String? = null, content)
fun TmrSheetActionRow(icon: ImageVector, title: String, onClick, modifier, subtitle: String? = null)
fun TmrConfirmDialog(title, confirmLabel, cancelLabel, onConfirm, onCancel, modifier, message, destructive)
fun rememberTmrToastState(): TmrToastState
suspend fun TmrToastState.show(message: String, actionLabel: String? = null, duration: TmrSnackbarDuration = Standard): TmrToastResult
fun TmrToastState.dismiss()
fun TmrToastHost(state: TmrToastState, modifier)
enum class TmrToastResult { Dismissed, ActionPerformed }
fun TmrOfflineBanner(message, modifier, supportingText, visible = true, actionLabel: String? = null, onAction: (() -> Unit)? = null)
fun TmrErrorCallout(title, modifier, supportingText, actionLabel, onAction)
fun TmrLoadingWheel(contentDesc: String, modifier)
```
Board: Text field, Consent row, Bottom sheet, Dialog, Offline banner and snackbar.

Toast. `TmrToastHost(state)` shows one toast at a time as the inverse-surface snackbar of the board.
Pass it as `TmrScreen(snackbarHost = { TmrToastHost(state) })`. Call `state.show(...)` from a coroutine.
`show` returns `ActionPerformed` when the person taps the action label (for example "Undo").
The public API has no `androidx.compose.material3.Snackbar*` type, so a feature can use it under
Constitution II.5. `TmrSnackbar` and `showTmrSnackbar` are the parts of the toast. A feature must not call them.

## Frames
```kotlin
object TmrPaperColors { Page, Ink, Body, Rule }
```
The export paper is black on white in both themes.

## Icons
`TmrIcons` also has `Lock`, `Link`, `Info`, `Download`, `ExpandMore`, `ExpandLess`, `Description`, `Flag`, `Share`, `OpenInNew`, `Search`.

## Illustration
`TmrSpotIllustration` is owned by the ILLUS package. Pass it into `TmrCollapsingHomeHeader.illustration`.
