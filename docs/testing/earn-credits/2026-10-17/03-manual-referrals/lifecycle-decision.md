# Slice 03 referral lifecycle decision

Date: 2026-10-17. Scope: contract decision only; no implementation, tests, remote action, gate change, or PR publication. Release and external gates remain BLOCKED. All eventual PRs remain draft and unmerged.

## Decision and authority

Use a living user's own detached receipt to preserve attachment and earned credits. Erase the deleted counterparty's identity, code and identifying provenance. Exact code-bearing POST replay ends when that provenance is erased; authenticated GET status remains the recovery path. This is a narrow lifecycle exception to exact replay, not an exception to erasure.

The approved backend plan section 8 explicitly requires both survivor-owned status and removal of identifying provenance. Its section 6 and the frontend plan sections 5/11 preserve committed retries across policy pause, window expiry and expired evidence. Those conditions do not authorize retaining a deleted person's code or its hash. Backend constitution rule 9 requires erasure coverage. No new retention exception or deletion-resistant identity store is adopted.

Inputs inspected: original workspace instructions and both plans; frontend-owned contract sections 8/9 at `/tmp/tmr-earn-20261017-frontend03`; backend predecessor `4e954057173fd6a25576708450c864fd31d17c19`, specifically accounts deletion/locks, promotion schema/service, activation state/schema and export; Android Google promotion recovery and private upgrade journal as existing ownership patterns. The predecessor has no referral implementation. This decision does not validate runtime behavior.

## Contract semantics

| State | Required result for an authenticated live eligible-provider actor with a structurally valid body |
| --- | --- |
| First successful attachment | `201`, one attachment plus the full advertised inviter reward in one transaction. |
| Committed, relationship intact, same key and stable payload | `200` original attachment/reward snapshot; no fresh policy/evidence/provider/capacity work. |
| Same key, changed stable payload, retained operation fingerprint | `409 IDEMPOTENCY_CONFLICT`; no mutation. |
| Intact attachment, original normalized code under a new key | `200` original attachment; preserve original source/time/reward, no extra reward. |
| Intact attachment, different code | `409 REFERRAL_ALREADY_ATTACHED`; no replacement. |
| Detached attachment after inviter erasure | Every later valid attach POST returns `409 REFERRAL_ALREADY_ATTACHED`, including original key/code, new key/same code, and another code. Check own detached state before code resolution, policy, evidence or creation of new operation/challenge state. Do not imply that the submitted code matched. |
| Own GET status after detachment | `200`, own `ATTACHED` receipt with original attachment ID, time, source and historical reward snapshot. No counterparty identity, code, ownership detail, wallet, or foreign grant reference. |
| Actor deleting/deleted | Existing `409 ACCOUNT_DELETED` dominates receipt recovery and mutation; never recreate actor state. Invalid auth/app and invalid request structure retain normal boundary errors. |

The precise GET endpoint and envelope remain the frontend contract owner's choice (`GET /referrals/me` may carry the own attachment). Attachment state takes precedence over current eligibility deadline/admissions; a paused offer or expired window cannot render a committed attachment absent. Reads never create an activation, attribution or grant. Do not add a public lookup or expose a `counterpartyDeleted` flag.

An opaque client request key alone can be retained as actor-owned data, but cannot prove that a submitted code matches the erased original. Returning success solely by that key would lose the same-key/different-payload guarantee. Keeping an equality-testable code digest would retain deleted-person provenance. Neither is needed: GET recovers the user's result without either compromise. Erase the affected code-bearing operation/challenge records rather than retaining misleading partial replay records. Unrelated own signup/trial records remain governed by their existing rules.

## Ownership and erasure

- Inviter deletion removes its code, wallet/history/grants and owned operations. For every surviving invitee, retain only the invitee-owned attachment receipt, scoped to its own UID/app/environment: opaque attachment ID, attached time, original source and nonidentifying reward amount/kind/status/policy snapshot. Null/remove inviter UID, code ID/value/digest, foreign grant reference and other linking/provenance fields. Scrub affected operations, challenges and decision payloads that encode that relationship. The receipt is erased when its own invitee is erased.
- Invitee deletion erases its attachment/identity edge, own operations and attributable provenance. Preserve each surviving inviter's own legitimate grant, wallet amount and ledger event using an opaque detached receipt/source with no embedded invitee UID, code, identity hash or retained back-reference. Aggregate credited count/earned total may continue to describe the inviter's own historical grants. Do not decrement earned balance, claw back spent credit, or reopen consumed campaign capacity.
- Own status/export includes the user's issued code, own attachment status/time/source/reward snapshot, own grants and complete credit history as applicable. Both before and after deletion, exclude another person's UID/email/code ownership and internal fingerprints/evidence. After detachment, no join may recover an erased edge. The receipt proves a historical successful reward, not that a deleted inviter still has a wallet.

Use an independent referral purpose scope such as compact JSON `[appId,environment,"referral"]`; do not repurpose existing guest `[appId,environment]` or signup `[appId,environment,"google-signup"]` scopes. The contract owner must freeze the final literal. One attachment per invitee is independent of policy revision, code, request key and campaign switch. A stable random attachment/grant receipt may identify the inviter's reward entitlement, e.g. `referral:<opaque receipt UUID>`; never derive it from deleted-person identity. A constant per-inviter entitlement would incorrectly limit that inviter to one lifetime referral. Multiple invitees may each produce one distinct full inviter grant.

## Deletion races and replay order

Existing deletion records a tombstone, then processes files, rows and Firebase. The rows-erasure/detachment transaction is the boundary for the POST replay exception. Before it commits, a committed, matching attachment may still return `200` while the inviter's deletion is pending: no new award or beneficiary write is required. Afterwards the same valid POST returns `409 REFERRAL_ALREADY_ATTACHED`. This phase difference discloses no counterparty identity and grants nothing.

Fresh attachment always checks both live accounts under the common sorted user-lock order and rechecks code/attribution/policy in the atomic grant transaction. If deletion's tombstone wins, no new attachment or reward commits; preserve the contract's non-enumerating deletion/code error. If attachment wins first, deletion later detaches the survivor receipt and erases only the deleted owner's resources. Actor deletion always blocks its own replay. Erasure and attachment/replay must serialize so no response reads a partially scrubbed relationship and no in-flight finalize recreates erased rows. Lifecycle hooks run idempotently within the existing rows transaction and obey the same lock order; do not acquire a second user's earlier-sorted lock after already locking a later one.

## Android recovery

Keep a pending manual attempt private and durable, bound to UID, environment and session generation. A timeout is uncertain, not failure. On resume/lost response, reconcile authenticated own GET status before any retry. `ATTACHED` is sufficient server confirmation to render “Referral applied” and clear the pending candidate, code, request ID, payload digest, challenge/evidence and retry state, including after process recreation or policy expiry. Do not claim that the pending code was the accepted code; the account may have attached on another device.

On `409 REFERRAL_ALREADY_ATTACHED`, stop mutation retries and reconcile GET. If GET fails, keep only an owner-scoped reconciliation marker without code/evidence and retry the read; show no new grant promise. A contradictory `NOT_ATTACHED` response is a consistency failure, not permission to retry attachment automatically. On sign-out/account switch, cancel requests, discard stale responses and clear account-specific referral state. Never apply another account's result or create a fresh key to bypass the one-attachment rule. Existing Google activation must remain independent of a failed referral.

## Acceptance cases for implementation and independent validation

1. First attach commits one full inviter grant/ledger/balance change; concurrent same/different keys cannot create a second reward. A sink failure rolls all changes back.
2. Before deletion, exact replay and same-code/new-key recovery return the original receipt even with paused/expired policy, expired window/evidence, exhausted capacity or spent reward. Same-key changed stable payload conflicts; another code cannot replace attribution.
3. Lose the success response, erase inviter, then retry old key/code, new key/code and a different code: all valid POSTs return `409 REFERRAL_ALREADY_ATTACHED`, no evidence/provider invocation or state/reward/capacity changes. GET/export retain only the invitee's original own receipt.
4. Delete invitee after grant: inviter's balance, own grant/ledger and historical totals survive; no invitee UID/code/hash/provenance remains in source strings, JSON outcomes, operations, challenges, decisions or joinable foreign references. Delete inviter: reciprocal guarantee for surviving invitee status. Unrelated users/apps remain intact.
5. Synchronize deletion at tombstone and rows-detachment boundaries against a fresh grant and committed replay. Prove either a complete original grant plus survivor receipt or no grant; never partial attachment, wallet resurrection, duplicate capacity consumption or mixed response snapshot.
6. Inject files/rows/Firebase failures, retry deletion, and run internal recovery after Firebase removal. Repeated hooks preserve survivor entitlements and erase affected provenance once; deleted actors cannot export/replay. Expiry of operation caches does not reopen one-attachment eligibility.
7. Android timeout/process recreation followed by detached GET confirmation clears all code-bearing pending state and displays attached. `409` plus temporary GET failure produces read-only reconciliation. Account switch during either response cannot update the new account. Contradictory GET cannot restart a grant.
8. A second test-only app/environment and changed referral policy revision cannot access, replace or duplicate the receipt. Two different invitees can each reward one inviter; reward sources stay stable across revision changes and contain no identity. Export excludes counterparty information in both intact and detached states.

No tests or builds were run for this design-only decision. Implementation acceptance and all release gates remain pending.
