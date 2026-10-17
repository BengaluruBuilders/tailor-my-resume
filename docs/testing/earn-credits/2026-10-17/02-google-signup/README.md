# Slice 02: Google signup and account recovery

This evidence bundle records the independent validation of the exact candidate pair below. Implementation checks and the attainable closed-admission device smoke passed; genuine external acceptance remains blocked. These PRs are intentionally draft and not ready for merge.

| Item | Tested identity |
|---|---|
| Frontend source | `01cfb637e26ca075df78892a352b8b7c62e8531d` |
| Backend source | `ba0993ada6055ef61dd000327e0617418755b58b` |
| Contract | `google-activation-v1-rate-limit-1`, SHA-256 `2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a` |
| Final staging APK | SHA-256 `b174910dedae7fdb590113a2bdce87cc4bc106564c2b6908a55f5c63ddcb5bff`; no APK binary is included |
| Runtime | Staging Firebase, candidate HTTPS port 20002, closed admissions/no published policy, disposable Android 16/API 36 emulator |

## Independent verdict and check scope

[Final independent revalidation](revalidation.md) resolves GS02-001 through GS02-004 and found no new blocker in the scoped work. It does not claim genuine Google authentication, Play Integrity, live reward admission/spend, release approval, or full issue completion. The original [initial findings](findings.md) and referenced first-pass failures are retained as historical evidence; the later independent report is authoritative for the final pair.

- Frontend `tools/ci/verify-local.sh`: PASS, 2,844 JUnit tests, zero failures/errors/skips. The report separately discloses 28 preliminary real-gitleaks subcases skipped because the binary was unavailable. [Structured summary](revalidation-evidence/frontend-required-summary.json), [compressed log](revalidation-evidence/frontend-required-final.log.gz).
- Independent affected frontend checks: PASS, 44 focused tests, zero failures/errors/skips. These overlap the full run and are not additive coverage. [Summary](revalidation-evidence/frontend-independent-focused-summary.json), [log](revalidation-evidence/frontend-independent-focused.log).
- Backend required `npm run check`: PASS, 720/721 tests, zero failures, one existing `pg_dump` PATH skip. The independent PostgreSQL 18 dump/empty-restore/deletion-service fixture also passed; Firebase and file deletion ports were explicit no-ops, so no cloud deletion is claimed. See [backend check summary](backend-check-evidence/backend-check-summary.json), [restore summary](validation-evidence/backend-dump-restore-summary.json), and its [isolated fixture](validation-evidence/backend-dump-restore-fixture.ts).
- The signed staging APK was independently hashed, verified, installed and exercised. The app showed the closed-admission state, retained synthetic local work, and read a zero-credit wallet after restart. Transport and app evidence support candidate contact and wallet-read correlation; the report clearly labels this as an inference, not a decrypted request/token trace. Zero economic rows means no reward was claimed.

## Device evidence gallery

All images and video are genuine captures from the validated staging APK and use synthetic data. Captions include the tested revision pair and observed result. Roborazzi fixtures are not substituted for device captures.

| Capture | Caption |
|---|---|
| [03 — closed-admission offer](revalidation-evidence/03-google-offer-admissions-closed.png) | `01cfb637` / `ba0993ad`, contract `google-activation-v1-rate-limit-1`: no offer is available; existing work and return action remain available. No +5/+7 promise is shown. |
| [07 — explicit offer recheck](revalidation-evidence/07-offer-after-explicit-recheck.png) | Same pair and closed-admission candidate: explicit recheck remains neutral with no fabricated reward; Back to my work remains reachable. |
| [09 — restart and zero wallet](revalidation-evidence/09-restart-zero-wallet-preserved.png) | Same pair: after app-only force-stop/reopen, Applications retains the synthetic guest and displays 0 credits. This proves no grant/spend. |
| [08 — synthetic profile preserved](revalidation-evidence/08-synthetic-profile-preserved.png) | Same pair: canceled Google entry retains the synthetic guest contact fixture with no personal contact values. |
| [05 — provider error observed](revalidation-evidence/05-google-entry-observed-result.png) | Same pair: Google Play services returned its own error on an emulator with no Google account. This leaves genuine provider acceptance blocked; it is not a successful authentication or a proved product defect. |
| [04 — entry, error, cancel and reopen recording](revalidation-evidence/04-google-unavailable-retry.mp4) | Same pair: genuine provider UI, cancellation and safe return/reopen. The later successful explicit offer recheck is shown separately in 07. No credential/account was supplied. |

The [validator's complete captions](revalidation-evidence/captions.md) and [unchanged source manifest](revalidation-evidence/manifest.json) inventory every prepared asset and SHA-256. The [publication asset manifest](publication-manifest.json) records the copied report and evidence files by size and SHA-256. The [artifact metadata](revalidation-evidence/artifact.json), [candidate observations](revalidation-evidence/candidate-observations.json) and [transport summary](revalidation-evidence/relay-summary.json) preserve provenance and limits. Original file sizes and hashes are retained; no APK, credentials, tokens, private account identifiers or private environment were published.

## Backend route, transaction and restore evidence

Backend behavior is evidenced by the final source SHA, independent fixture/database evidence and required-check summary, not by these UI images. Review [backend activation tests](https://github.com/BengaluruBuilders/apps-backend/blob/ba0993ada6055ef61dd000327e0617418755b58b/test/tailormyresume-activation.test.ts), [activation transaction implementation](https://github.com/BengaluruBuilders/apps-backend/blob/ba0993ada6055ef61dd000327e0617418755b58b/src/apps/tailormyresume/activation.ts), and the [PostgreSQL restore/deletion fixture](validation-evidence/backend-dump-restore-fixture.ts) with its [result](validation-evidence/backend-dump-restore-summary.json). The [prepared backend check manifest](backend-check-evidence/evidence-manifest.json) identifies each summarized asset and its hash.

The backend changes add migrations `0013` and `0014`. The PR includes the narrow proposed Constitution rule-3 capability exception for token-derived destination identity and capability-resolved source identity; it remains subject to human review and does not permit caller-selected body/path UIDs or a general exception.

## Scope and next gates

This stack partially advances frontend #403 and backend #120, #121, #54 and #123. It does not complete those issues, referral issue #122, later payment/referral work, or the inherited broader guest-trial acceptance. Genuine disposable Google link/direct signup/conflict/recovery, selected/configured Integrity verification, approved live campaign/admission values, admitted grants and successful spending remain blocked or unproven. No production policy, migration, merge, release, or deployment is included.

The frontend PR stacks on slice01 PR #406 at `7ffd2f42a423f4037bfea372cd47b1de4b4d035e`; the backend PR stacks on slice01 PR #124 at `69797ec759f3493b6d4f5d6912a82b1429e30fdb`. Each PR should be reviewed after its predecessor and alongside the linked paired PR. Both remain draft and not ready for merge.
