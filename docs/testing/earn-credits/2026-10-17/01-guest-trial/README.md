# Guest trial slice 01 evidence

This evidence set was captured against frontend `13aa4fef53057036c52fa25aa475e7a1ab303d83` and backend `69797ec759f3493b6d4f5d6912a82b1429e30fdb`. The Android artifact was staging variant, package `com.tailormyresume.app.staging`, SHA-256 `cd0082b09855dba860b73ad6e44bd9e0b39977ffd5cbd7ddc7dae5c9b918e4fa`, configured for `https://abhishek-dubey-MS-7B16.local:20002/`. The artifact itself is not published.

## Runtime evidence

- [Guest entry](independent-evidence/01-guest-entry.png): exact staging APK on fresh emulator, guest action visible; no balance or grant is claimed.
- [Guest sign-in failure](independent-evidence/02-guest-unavailable-before-routing.png): historical genuine anonymous sign-in failure before candidate API contact, before the guarded provider correction.
- [Retry recording](independent-evidence/03-guest-retry.mp4) and [retry result](independent-evidence/04-guest-retry-result.png): same neutral failure state; no authenticated request or reward is implied.
- [Sanitized runtime observations](independent-evidence/runtime-observations.json) and [host HTTPS checks](independent-evidence/https-transport.log): candidate host health and missing-token rejection only, not Android contact proof.

## Code and data evidence

- [Independent findings](findings.md) records attainable code/security checks and blockers.
- [Sanitized targeted backend result](independent-evidence/targeted-backend.log): 61 passed, 0 failed, 0 skipped across scoped backend cases with disposable PostgreSQL.
- [Restore and deletion rehearsal result](independent-evidence/restore-rehearsal.log), with the [fixture preserved as non-executable text](independent-evidence/restore-rehearsal.mjs.txt). The fixture text is not an application or test-source change.
- [Frontend required-check summary](prepared-evidence/frontend-final-summary.md) and [backend required-check summary](prepared-evidence/backend-required-checks-summary.md). Backend full check passed on its second attempt with one existing `pg_dump` skip; the earlier failed attempt remains disclosed there.
- [Evidence manifest](independent-evidence/manifest.json) records artifact sizes and SHA-256 values.

## Post-publication runtime result

The [independent post-publication verdict](post-publication.md) confirms genuine anonymous signup, authenticated contact with the exact candidate, the closed-policy unavailable screen, Continue, process resume, authoritative zero-credit history and guest-trial recheck. B001 is resolved. B002 has a verified test-transport workaround through a disposable emulator system proxy with normal TLS; inherent `.local` resolution is not claimed fixed. B003 remains blocked: there is no genuine Integrity verdict or active policy, so no real grant or paid tailoring is claimed. The earlier signup-failure evidence below remains as historical context.

The [routing decision](../emulator-routing-decision.md), [post-publication manifest](post-publication-evidence/manifest.json), [actual candidate-state screenshot](post-publication-evidence/08-guest-candidate-state.png), [Continue screenshot](post-publication-evidence/09-guest-continued.png), [process-resume screenshot](post-publication-evidence/11-guest-process-resume.png), [zero-credit/history screenshot](post-publication-evidence/12-authoritative-zero-credits.png), and [recheck screenshot](post-publication-evidence/13-guest-trial-recheck.png) document the updated runtime result. All new evidence is bound to the same APK and code pair.

## Implementation handoffs

[Frontend handoff](frontend-handoff.md), [backend handoff](backend-handoff.md), and [contract handoff](contract-handoff.md) describe implementation scope and required checks. They are not independent validation.

The follow-up [staging documentation resolution](staging-docs-resolution.md) reconciles the release setup instructions with the staging-only anonymous-auth prerequisite. It does not claim signup or reward validation.
