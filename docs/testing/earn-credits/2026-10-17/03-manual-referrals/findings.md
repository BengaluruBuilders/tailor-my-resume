# Slice03 independent validation

2026-10-17. **Attainable acceptance needs the local evidence repairs below; external acceptance remains BLOCKED. Not ready for merge.** One independent validator reviewed both frozen candidates and exercised the actual signed staging app. No product defect was observed in the attainable guest manual-entry journey. No source, contract, acceptance vector, policy, database, running backend, branch or PR was changed by this validator.

## Exact identity and evidence

| Item | Independently verified identity |
|---|---|
| Frontend | `aa4b41726608c0a5b847fe80f543f01a37ffa130`, clean FE03 worktree |
| Backend | `06bcccf5625b6d92e5c016a5ffd11ebad87a49c0`, BE03 clean except existing untracked dependency symlink |
| Contract | `manual-referrals-v1/namespace-1`, SHA256 `66db09b4ccb06e8224a4c039c491a4e90dec434f56ee78aab478c0a5bb293add` |
| Freeze | SHA256 `0d4878c86324d02a328f999c264c0bd18a73cb30b29a16953ab89cf12bac1ff8` |
| APK | Signed staging, 6,081,450 bytes, SHA256 `0e946b0b36e5b927b1ef59eb98d6629b16604af71ecc8aedb43b56ea4301ad1d` |
| APK → source | Executable input `27cc53d75f9833339976b6849c044e33b2d9f66a`; only `docs/ARCHITECTURE.md` differs to final FE (3 additions/2 removals). No rebuild for documentation |
| Signing / destination | `com.tailormyresume.app.staging`, signer SHA256 `cd42a070ed44e41b40e5d8c8b24b26c0df70a7e97f3aa33da405b38984ce549c`; actual DEX contains staging Firebase project and HTTPS20002 destination |
| Actual staging | Managed workstation repo at BE06bcccf, API PID174517, same source command/cwd, all226 inputs match; original services remain present. No remote mutation |
| Device | Owned TmrGuestValidator, emulator-5582, Android16/API36 Google APIs x86_64, zero Google accounts. Existing synthetic guest only; personal Pixel untouched |
| Policy | Missing/unset; admissions closed. No live namespace/window/capacity/retention/threshold choice |

[Independent input recomputation](validation-evidence/independent-input-verification.json) verifies every2321 FE and226 BE manifest input, with zero mismatches. [Runtime provenance](validation-evidence/runtime-provenance.json), [remote readback](validation-evidence/remote-readback.json), [device procedure/results](validation-evidence/device-smoke.md), and [eleven screenshot captions/hashes](validation-evidence/screenshots-manifest.json) preserve the narrow proof. All screenshots contain synthetic state and come from the actual app.

Required unchanged gates are reused at their exact verified inputs: backend `npm run check` exit0,749/749,0skip, plus focused namespace7/7 ([committed log](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/docs/testing/earn-credits/2026-10-17/03-manual-referrals/logs/namespace-required-check.log)); FE `tools/ci/verify-local.sh` exit0 ([log](frontend-evidence/remote-final/slice03-verify-03.log), [exit](frontend-evidence/remote-final/slice03-verify-03.exit), [result inventory](frontend-evidence/remote-final/slice03-verify-03-summary.json)). Independent read-only remote XML parsing confirms509 retained result files/2867tests/0fail/0error/0JUnit skips. Cache reuse38 executed/2023 up-to-date is disclosed; these are not2867 newly executed tests. No unchanged full suite was redundantly rerun.

The28 preliminary `SKIP real gitleaks not found` cases did not execute. `verify-local.sh` tests the scan script; it does not itself run a genuine repository secret scan. Current `build.yml` installs/checksums8.30.1, runs the real scan, then real-binary script tests in its policy job. Therefore local gate exit0 is valid within that script's actual behavior, but **real scanner/required PR CI remains unproven**, to be verified on publication. No scanner PASS or CI waiver is claimed.

## R03-V001 — P2: Exercise restoration from a backup that predates erasure

**Type:** local validation gap; blocks attainable acceptance for R03-34. This is not a Google/Integrity blocker and is not a demonstrated runtime erasure failure.

**Required:** contract10.7 says an old backup must not resurrect erased edges/reset attachment/reopen capacity, and calls for local dump/restore plus delete/retry proof. Backend approved plan section8 also requires deletion and grant/capacity reconciliation after backup restoration.

**Observed / reproduction:** inspect [lifecycle test337](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/test/tailormyresume-referrals-lifecycle.test.ts#L337): it commits a referral, calls DELETE, and **then** runs pg_dump. The restored receipt is already detached, the user already removed, capacity already5, and resumeDeletion receives the completed tombstone from that same snapshot. It correctly proves preservation of post-detach state. It cannot exercise reapplication of a deletion that happened after the backup, or preservation/reconciliation of later committed economic state. The passing749-test log includes this narrower test.

[MANUAL_REFERRALS.md](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/docs/MANUAL_REFERRALS.md#L35) documents an offline procedure: reconcile authoritative later deletion/grant records and checkpoints, or keep the restored DB unreachable/admissions off if those inputs are unavailable. That documentation is honest and useful. No new automated restore subsystem is inferred as a requirement, and absence of a journal source in this candidate is not independently labelled a product defect. The missing item is an exercised, reproducible local procedure satisfying that documented boundary.

**Minimum acceptance evidence:** in disposable local databases, take a real backup while the relationship/provenance is intact; delete afterward; preserve a synthetic authoritative later tombstone/checkpoint outside the dumped DB; restore the older snapshot while unreachable; reapply normal durable deletion with a failure/retry; verify foreign code/edge/operation/challenge provenance gone, survivor original own receipt preserved, actor tombstone rejects, valid detached POST409/ownGET200, and consumed capacity does not reopen. Also include a backup-before-later-award case demonstrating reconciliation of later receipt/grant/capacity state, or explicitly exercise the documented refusal to serve when the necessary later records/checkpoint are unavailable. Preserve the existing post-detach test. Do not expand into production backup provisioning, remote data or cloud-erasure claims.

## R03-V002 — P2: Complete the frozen local adversarial assertions

**Type:** bounded local test-evidence gap, principally R03-19/23/24; not an observed exploit. Blocks claiming the full frozen matrix passes.

**Required:** the frozen matrix explicitly names actor/action/key/code/source/signal/revision/challenge/hash substitutions and activation/trial evidence reuse (19), simultaneous different inviter codes for one invitee (23), and remaining4 capacity against advertised5 (24).

**Observed / reproduction:** [route test536](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/test/tailormyresume-referrals.test.ts#L536) exercises wrong Integrity token and changed source; [namespace tests](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/test/tailormyresume-referrals-namespace.test.ts) prove app/deployment/namespace isolation. Canonical vector equality proves construction, not actual route rejection for every tampered field. No dedicated referral behavior test was located for the remaining named binding substitutions/cross-action reuse. [Test479](https://github.com/BengaluruBuilders/apps-backend/blob/06bcccf5625b6d92e5c016a5ffd11ebad87a49c0/test/tailormyresume-referrals.test.ts#L479) races two invitees against7-of7 capacity and same-code same-actor attempts; it does not race two different inviter codes for one actor or assert a5-credit offer against4 remaining. Sequential committed different-code rejection is covered at178. Source review finds the intended guards/transactional uniqueness, but does not turn those missing behavior assertions into executed PASS results.

**Minimum acceptance evidence:** add bounded parameterized real-TMR-route/local-PG cases for the missing binding/action substitutions and the two exact concurrency/capacity cases. Assert no new attachment/grant/ledger/wallet/capacity mutation or wrong challenge consumption on each refusal; persistent failed-attempt counters may change as designed. For the different-code race assert one full reward and one durable attribution with safe loser/recovery semantics. Keep canonical vectors and existing assertions unchanged. This requires no real external providers or live settings.

## Actual Android outcome

Settings → Invite friends, non-fixture full-code save, first-code preservation/explicit Replace, actual process force-stop/restart, Clear, normal/200% text, real keyboard, wallet/history entry and ordinary Applications return were observed. The code remains complete, the editor is blank on revisit, and Apply stays disabled for the guest. No automatic tailoring/spend occurred. The available fixture had zero credits/empty history and no saved application; nonempty work/history preservation is covered only by deterministic tests here.

The actual APK embeds the expected CA/hostname destination. TLS passthrough recorded app tunnels to that exact candidate and Firebase refresh; curl using the APK public CA/original hostname returned CONNECT200/health200/verify0. **Encrypted contact is not decoded authentication/provider acceptance.** No admitted Google referral, server-generated own code, actual grant or spend was possible with closed admissions/zero Google accounts; no fake auth/verdict bypass was used.

## 46-case disposition

The [companion mapping](validation-evidence/acceptance-map.md) maps every frozen case to exact source tests and current gate evidence, actual screenshots, a local gap, an external blocker, or later-slice exclusion. `FIXTURE` means passing deterministic behavior at the exact candidate with external provider fakes and real local PostgreSQL/Android test runtime. `PARTIAL` identifies the narrower observed proof; it is never full matrix PASS. Structural fixture verification is not counted as behavior acceptance. R03-V001/V002 are the consolidated actionable local gaps; other limits are explicitly bounded in the map.

## Remaining blockers and stop state

Genuine matching Google/project/package/certificate/licensing/Integrity signed-device admitted5 reward+spend remains BLOCKED (R03-43), as do approved live configuration/disclosure/retention and governing-document human review. Cloud files/Firebase erasure is not proved by no-op fixture ports or local dump/restore. App Links/URL/install referrer and remaining payment retirement are excluded to slices04/05 (R03-44). No issue closes and no merge/release/activation is authorized.

Agent-device session closed. All owned emulator proxy fields removed and read null, including the stale host/port that had survived prior cleanup; font restored1.0. EmulatorPID806706/session3675 exited0; relayPID807572/session79951 exited143; both absent. No Gradle overlapped emulator. Candidate API and original services intentionally remain running. Product source remains unchanged, all owned tool handles terminal, validator idle for a **fresh fixer** after root review. No fixes or PR publication performed.
