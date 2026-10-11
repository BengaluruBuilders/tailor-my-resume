# First-use activation changes — 2026-10-12

The [implementation evidence](EVIDENCE.md) contains the flow, screenshots, regression tests, and passing validation output.

The owner approved these changes after the first-use review on 2026-10-12.
This document changes only the states listed below. For those states, it takes precedence over
the 2026-10-10 prototype. Keep that exported prototype unchanged. Keep its tokens, components,
screen order, and all other states.

## Acceptance criteria

| State | Required behaviour |
|---|---|
| Sign in | Keep Google sign-in, the benefit stories, and the free-first-resume offer. Add a short preparation cue for resume or work details and the full job description. |
| Manual profile review without examples | Offer the existing editor to add a truthful example. Keep the invitation optional. Return to review after saving. Do not invent a role or add a new setup wizard. |
| Review profile | Show required corrections and the facts. Keep explicit confirmation. Keep completeness and optional LinkedIn prompts in later Profile use instead of this review. |
| Education correction | Let the user open education editing from review and return after saving. Preserve facts and source identity. Do not flatten structured education fields as a side effect of changing one field. |
| Add job | Explain the minimum of 200 trimmed characters before submission and while text is short. Use the existing validation rule. Keep paste, link import, and the login-page fallback. |
| Job analyzed | Show supported and missing keywords and important requirement reasons before coverage. If a reason exists, show it alongside the status. Do not invent a reason when none exists. |
| Credit disclosure | Use one authoritative balance for the visible count and the tailoring gate. Explain that a successful tailoring uses one credit. Keep server-owned spending in production and local ledger spending in demo. A failed run must not add a client debit. |
| Reading resume | Keep processing feedback. Remove the unsupported estimate of 20 seconds. Do not replace it with another unmeasured estimate. |
| Tailored result with changes | Open the existing Changes tab first. Keep before, after, and source visible in the change cards. Keep Resume one tap away. Label the final number as keyword coverage. |
| Tailored result without changes | Show that there are no changes. Allow PDF export without a redundant accept action. Do not imply that tailoring improved the document. |

## Safeguards and unchanged scope

Keep required-gap checks, confirmed facts, source attribution, Edit, per-change Undo, and explicit
acceptance before exporting suggestions. Keep the optional question and Skip. Keep PDF Open and
Share, Applications, and Mark as Applied. Cover letters and prep questions remain Soon.

Do not restore any feature cut in PRD 6.1.2. Do not move sign-in or add analytics. Do not call a
legacy export unlock to charge for tailoring. The backend contract owns the production debit event.

## Verification

Use focused navigation, state, and data round-trip tests for the changed behaviour. Verify required
corrections, manual evidence editing, education edits, the job-text boundary, reason visibility,
Changes-first entry, explicit acceptance, and no-change export. Verify credit success, failure,
retry, and unavailable-balance cases without purchases or production data.

Inspect and update only the affected Roborazzi baselines at the existing sizes. The prototype plus
this delta is the acceptance source. Record changes in the affected feature modules, then run the
repository gates. Do not use demo timing as evidence of production performance.
