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

## Current staging gates

The [guarded staging authentication setup](staging-auth-setup.md) enabled only `signIn.anonymous.enabled` in `tailormy-resume-staging` and read the value back successfully. The client signup and app-to-candidate flow have not been retested, so the earlier anonymous-auth failure is a historical reproduction and remains pending independent retest. Fresh-emulator `.local` routing and genuine Play Integrity/admission activation remain blocked; see [publication status](publication-status.md) and [independent findings](findings.md). No genuine guest grant or completed authenticated app journey is claimed.

## Implementation handoffs

[Frontend handoff](frontend-handoff.md), [backend handoff](backend-handoff.md), and [contract handoff](contract-handoff.md) describe implementation scope and required checks. They are not independent validation.

The follow-up [staging documentation resolution](staging-docs-resolution.md) reconciles the release setup instructions with the staging-only anonymous-auth prerequisite. It does not claim signup or reward validation.
