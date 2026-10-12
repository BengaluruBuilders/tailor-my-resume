# Job step redesign — 2026-10-12

Source: the Claude Design project "Job Step Redesign"
(`https://claude.ai/design/p/8c1c7247-c749-43e3-bb2d-1c524d9b2186`, file `Job Step Redesign.dc.html`).
The owner approved it on 2026-10-12.

This document changes only the states listed below. For those states, it takes precedence over the
2026-10-10 prototype. Keep that exported prototype unchanged. Keep its tokens, colours, screen
order, and all other states.

## Changes

| # | Change |
|---|---|
| 1 | Monospace is kept for the headline only. Buttons, step text, and helper text use the proportional sans (Space Grotesk), so labels no longer wrap or space out. |
| 2 | The step chips become "Step N of 3 · Name" with a three-segment progress bar, beside the back button in the top bar. |
| 3 | The hero card is shorter (196 dp), which gives the input more room. The illustration is unchanged. |
| 4 | The requirement moves directly under the field and is shortened. A live character count sits on the field label row. |
| 5 | One lime button per screen. "Analyze job" is the primary action. Paste and link are equal secondary options. |
| 6 | The disabled button label uses `textMuted`, not `textDisabled`, so it reads at a usable contrast. |

Changes 1, 2 and 6 live in `core:designsystem` and therefore apply to every screen that uses the
step bar or the `TmrPrimaryButton` and `TmrSecondaryButton` pair.

## Acceptance criteria

| State | Required behaviour |
|---|---|
| Add job, empty | Short hero, the field label with "0 / 200", the shortened requirement under the field, two secondary actions, a disabled but readable "Analyze job". |
| Add job, with text | The count follows the trimmed length and turns lime at 200. "Analyze job" becomes lime and enabled. |
| Setup steps 1 to 3 | The step label and the progress bar sit on the top-bar row. The spoken description stays "Step N of 3, Name". |

The 200-character minimum, the trimming rule, paste, link import, and the login-page fallback are
unchanged. No copy claims a match, a fit, or an ATS score.

## Evidence

Captured on a Pixel 7 Pro (Android 16, `demo` debug build).

| State | File |
|---|---|
| Before | `before-add-job.png` |
| After, empty | `after-add-job.png` |
| After, 269 characters typed | `after-add-job-filled.png` |

The exported design file is `Job Step Redesign.dc.html`. It is an export of the Claude Design
canvas; do not edit it by hand. Its `mascot.png` and `original.png` stay in the canvas project.
