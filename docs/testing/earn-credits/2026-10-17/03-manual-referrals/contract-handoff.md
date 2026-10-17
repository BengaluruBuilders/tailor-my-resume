# Slice03 manual referral contract handoff

Contract-only phase complete: `manual-referrals-v1`. Contract SHA-256:
`09507f4e322e72e7e34ecf4797dca8bc89178d2c33cdb1280142b025c58dc3a0`.
No runtime, database, deployment or live policy was changed. Both implementation writers must agree to this exact revision before root releases application implementation.

## Revisions and ownership

Frontend worktree `/tmp/tmr-earn-20261017-frontend03`, branch `codex/earn-03-manual-referrals`, from exact published predecessor `ca4fcade751fc0c18c5cd5eb02128933700888a4` (open draft PR407). Backend inspected read-only `/tmp/tmr-earn-20261017-backend02`, exact `4e954057173fd6a25576708450c864fd31d17c19` (open draft PR125). Authenticated `gh` and remote branch reads verified those identities and open/draft states on 2026-10-17. Both predecessor worktrees were clean. Original checkouts' untracked plans/evidence/user work were preserved; parent workspace is not Git. No PR or push in this phase.

Only the contract agent wrote shared contract/fixtures. Frontend implementation owns actual Constitution proposal, DTO/domain/journal/UI/history/Room migration; backend implementation owns generic referral/promotion seams, TMR qualification/reward adapter, migrations, deletion/export and tests. Contract changes must return to this single owner and get renewed exact-version agreement.

## Frozen result

GET `/referrals/me`, POST `/referrals/code`, POST `/referrals/challenges`, POST `/referrals/attach`, all under `/v1/tailormyresume`. Proposed plural/standalone attribution routes are reconciled, not additional aliases. Strict request/response shapes and deterministic hashes live in `contract/`; `freeze.json` records every contract input hash. Sections 8/9 and their prior fixtures remain unchanged, prior contract prefix hash `2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a`.

Opaque scoped 160-bit codes, canonical ASCII case/outer whitespace, five collision retries; independent code issuance works during reward pause with truthful unavailable offer. Server policy advertises revision/amount/deadline/continuityMode; no client amount, beneficiary or hidden signal-config guess. Nullable continuity is optional finite referral velocity only; no passive/recovery collection or lifetime device cap. Current offer refresh/reconfirmation and original committed receipt recovery are explicit.

Invitee attachment + full advertised inviter reward + unique ledger + capacity commit atomically. Challenge owner is invitee, grant beneficiary is inviter. Existing generic commitPromotionGrant fixes policy entitlement and consumes beneficiary-owned challenge; extend both seams additively, preserving single-actor guest/signup callers. Use random receipt entitlement per referral, never UID/code-derived source, and keep velocity separate from lifetime trial signal allocation. Existing sorted `withLiveUsers` guards new two-party work. Rows erasure needs an accounts-owned sorted lifecycle hook before `delete(users)`; a cascade alone is insufficient.

After detachment, erase foreign code/hash/fingerprint and retain only survivor-owned receipt. Valid POST then returns 409 ALREADY_ATTACHED; GET recovers own ATTACHED receipt. Intact committed replay stays 200 across window/policy/evidence expiry and before rows-detachment even during inviter deletion, while actor tombstone always blocks. Android GET-first uncertain recovery clears code-bearing state; failed reconciliation keeps only read-only owner marker. Existing-Google conflict never blindly inherits a guest candidate.

Stable REFERRAL_REWARD ledger IDs/amounts reach DTO/model/Room/history through additive owner/environment-associated nullable server-ID migration; preserve legacy rows/fallbacks and unknown decoding. No automatic tailoring/spending. Invitee erasure keeps inviter economics; inviter erasure keeps invitee already-attached state; aggregate capacity never reopens. Own export contains no counterparty identity or raw/private evidence. Local restore/deletion reconciliation is required during implementation.

## Checks observed

- Fixture verifier PASS: 35 schema examples, 9 negative shapes, 4 canonical hash vectors, 11 invalid codes; 46-case acceptance matrix structurally valid and marked PENDING, prior contract prefix unchanged.
- `tools/ci/check-constitution.sh` PASS.
- `git diff --check` PASS.
- No full Gradle/npm suite, emulator, device, remote workstation or runtime integration was run in this contract-only phase. No application acceptance is claimed.

A first verifier run had a path-index error locating the contract; corrected to the docs parent, then PASS. This did not affect canonical bytes or fixtures. Normal Git hooks remain enabled; no gate bypass.

## Issue and release boundary

Authenticated actual issue bodies inspected: backend #122/#120/#54 and frontend #405/#404, plus approved original plans/maps and predecessor foundations. Slice03 covers their manual attribution, reusable atomic reward, applicable abuse/lifecycle and integrated history criteria only. Slice04 owns URL sharing/App Links/install referrer/domain; slice05 owns remaining billing retirement and legitimate historical purchase obligations. No whole issue closes here.

Accepted decisions are copied as `lifecycle-decision.md` and `constitution-decision.md`. Frontend I.5 still only permits guest Integrity; the frontend writer must propose the narrow Article VII correction for existing GOOGLE_ACTIVATION and new REFERRAL_ATTACH, plus bounded signal-purpose/accessor rationale. Backend writer must propose rule-3 resolved beneficiary and rule-9 survivor ownership scope for human review. No silent governing-file amendment or permission to activate follows from this contract.

Inherited slice02 full Google/Integrity/admitted-grant/spend acceptance remains BLOCKED despite attainable publication audit PASS. Genuine matching staging provider/project/package/certificate/licensing evidence, signed device tests and live capacity/window/retention/namespace/ingress/disclosure decisions remain BLOCKED. Fixture24h/5/100 values are not live choices. External-gate-decision permits coding only with honest blockers/draft disposition. No merge, auto-merge, release, remote migration or live admissions.

Stop condition reached: frozen contract and fixtures plus normal commit. Root obtains both exact-hash agreements, then releases at most one frontend and one backend writer on slice03. No material design decision remains pending in this contract; implementation may report evidence-based mismatches through root.
