# TailorMyResume — logo guidelines

## 1. The logo
**Idea:** Paige, the app's own resume page with a face, is the brand. Your real story, friendly and true.

| Version | Use | File |
|---|---|---|
| Horizontal lockup | default wherever there is width: store listing, website header, decks | `master/lockup-horizontal.svg` |
| Stacked lockup | square or tall spaces: splash, posters, social cards | `master/lockup-stacked.svg` |
| Symbol | app icon, avatar, favicon, anywhere the name is already visible | `master/paige-symbol.svg` |
| Small symbol | 32 px and below (bigger face, wider dog-ear gap) | `master/paige-symbol-small.svg` |
| Reversed symbol | white or lime on dark (face holes slightly enlarged against glow) | `master/paige-symbol-reversed.svg`, `master/paige-symbol-lime.svg` |
| Wordmark | only when Paige already appears nearby | `master/wordmark.svg` |

The wordmark is Space Mono Bold converted to outlines, tracked −0.02 em. Never retype it.

## 2. Clear space
Keep clear space of **1 × the dog-ear** on every side. That is the width of the folded corner,
36 units on the 256 symbol grid, or about a quarter of the page's width. It scales with the logo.

## 3. Minimum size
| Version | Screen | Print |
|---|---|---|
| Horizontal lockup | 120 px wide | 30 mm wide |
| Stacked lockup | 72 px wide | 18 mm wide |
| Symbol | 16 px, using the small symbol | 5 mm |

## 4. Colour
| Name | HEX | RGB | CMYK (approx.) | Pantone (nearest, confirm with a swatch) |
|---|---|---|---|---|
| Lime | `#A3F43F` | 163 244 63 | 33 0 74 4 | 375 C |
| Ink | `#0A0A0A` | 10 10 10 | 60 40 40 100 | Black 6 C |
| White | `#FFFFFF` | 255 255 255 | 0 0 0 0 | — |

Ink on lime has 14.7:1 contrast, and lime on black 15.6:1. Both pass WCAG AAA.

**Approved pairs:** ink on lime (app icon, primary) · lime on ink or black (in-app, store, dark
surfaces) · ink on white · white on ink. Blue `#5AA9F8` and amber `#F7A940` are story colours for screens, not
logo colours.

## 5. Typography
- Wordmark and headlines: Space Mono Bold. Body: Space Grotesk. Fallbacks: `ui-monospace`, `system-ui`.
- Both families are SIL Open Font License 1.1, which allows logo use. The licences are in
  `core/designsystem/fonts-licenses/`.

## 6. Don'ts
Don't stretch, rotate or outline the logo. Don't add shadows or gradients. Don't recolour it outside the palette.
Don't give Paige other facial expressions in the logo; the expressive poses belong to the in-app mascot.
Don't move the symbol and wordmark relative to each other. Don't place the logo on a busy photo without an
ink or lime container.

## 7. Files
- `master/` — SVG masters with real cut-outs (`fill-rule="evenodd"`). No live text, filters or rasters.
- `export/symbol/` — one-colour and app-icon variants, PNGs, and a web icon set: `favicon.ico`, PNG icons,
  `site.webmanifest` and `head-snippet.html`.
- `export/lockup-*/`, `export/wordmark/` — black, white and lime versions with 1200 px PNGs.
- `android/` — `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` (108 dp viewport; the mark is
  39 × 50 dp, inside the 66 dp safe circle), the `ic_launcher_background` colour, and the 512 px Play Store icon.
- `board/` — presentation board with mockups.
- `build/` — the Python 3 scripts that generate the masters, lockups and Android vectors (stdlib only; the
  wordmark reads `core/designsystem/src/main/res/font/space_mono_bold.ttf`).
- `proof/` — the icon on the emulator launcher and the Android 12+ splash screen.

## 8. Caveats
A professional trademark search has not been done. CMYK and Pantone values are conversions from screen
colour; proof them before print.
