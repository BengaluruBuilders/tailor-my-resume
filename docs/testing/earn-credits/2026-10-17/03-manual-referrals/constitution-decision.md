# Slice 03 constitution decision

Date: 2026-10-17. Scope: a proposed frontend Article VII amendment and its acceptance boundary. No source, constitution, contract, gate, runtime, PR or live configuration was changed by this decision.

## Decision and authority

Proceed with the narrow amendment below in the unmerged slice03 frontend candidate. The adopted orchestration prompt expressly authorizes proposed authentication/reward-policy implementation and draft PRs; the user's later direction delegates unresolved decisions and says to continue coding. Article VII requires a PR editing the Constitution, a named gate, and explanation of exceptions. It does not require a separate permission question before preparing that proposal. This authority suffices to implement and verify the proposed scope on the feature branch. It does not adopt a live privacy exception or authorize merge, deployment or collection under an unapproved live policy.

The actual slice03 predecessor (`ca4fcade751fc0c18c5cd5eb02128933700888a4`) permits direct Integrity **solely** for guest admission under contract section 8. Its amendment ledger and dependency explanation also say guest only. Section 9 already specifies direct `GOOGLE_ACTIVATION` evidence through the same adapter. Thus section 9 has a documentation gap as well as the new section 10 referral scope; do not describe either as covered by the existing guest-only wording. The original workspace constitution predates even that guest amendment; make changes against the candidate's actual file.

## Exact recommended scope

Replace only the direct-Integrity clause in I.5 with:

> direct Google Play Integrity Standard requests only for an explicit protected guest admission (`GUEST_TRIAL`, section 8), Google activation including its explicitly requested protected base subaction (`GOOGLE_ACTIVATION`, section 9), or referral attachment (`REFERRAL_ATTACH`, section 10) under `docs/BACKEND_CONTRACT.md` are allowed.

Keep the existing Firebase Auth, Credential Manager and Billing allowances and the rest of I.5. Do not generalize this to all promotional, wallet, AI, sign-in or backend requests. Keep each action's separate canonical binding and contract semantics. Integrity receives the exact server-issued opaque request hash, with no raw Android ID, candidate content or extra identifiers in Google request fields. Reusing one adapter does not permit cross-action evidence reuse.

Add a proposed amendment ledger row with this scope:

> Limited Android-ID use is allowed only during an explicit protected attempt: the guest/base allocation purpose in sections 8/9, and optional independent, finite referral request-velocity counters in section 10. The referral purpose is separate from guest allocation and signup identity purposes, with app/environment/key-version separation. It is not authentication, unique-person identity, a permanent fingerprint, or a new per-device signup/referral entitlement cap. No evidence is acquired on policy, wallet, history, referral status/code display or recovery-only reads. Do not read Android ID merely to create/display a code. Acquire referral continuity only for an explicit new attachment attempt when its configured optional signal dimension is used. Missing optional referral continuity is not an unused-device assertion; follow section 10's nullable-signal contract.

The row must retain contract-defined finite expiry, erasure/export handling, separate account/signal/network/campaign counters, no combined fingerprint, no raw-ID/token logging or raw-ID persistence, and no deleted-identity retention exception. Do not reuse guest allocation records as referral denial records. A temporary velocity throttle is distinct from permanently consuming a device's reward eligibility. No numeric live TTL, threshold or capacity is adopted here. Explicit fixture values remain fixtures.

Update the dependency ledger's guest-only direct-use description to name these exact three actions and sections; leave reCAPTCHA's separate facts intact. The historical guest amendment may remain historical with a clear reference to the new proposed extension. Describe the new row as a proposal in this draft, not an owner-approved live amendment. Reuse the current resolved Integrity library; this decision adds no SDK, recall integration, dependency baseline growth, invasive fingerprint or general network exception.

The existing `HardwareIds` suppression is accessor-scoped. If the same accessor is reused, explain its expanded, bounded purposes in the amendment/PR and update its external-grounded rationale as needed. Do not add a duplicate speculative suppression, enlarge a baseline or weaken lint. Any new actual exception still follows VII.4 and requires its own narrow rationale.

## Required verification and ledger disposition

The new amendment row's gate should be **Review**, plus existing constitution policy/Dependency Guard/lint and named targeted gateway tests for all three actions. The source scan alone does not enforce action/purpose scope. Name actual test classes after implementation; do not invent passing tests. Review verifies the allowed data path and purpose separation; deterministic tests prove no read on passive/recovery paths, explicit attempt only, exact returned requestHash, distinct action binding, optional/null referral signal handling, account-switch/erasure fencing and no evidence collection when admission/configuration is unavailable. Backend tests prove separate finite velocity dimensions, no new device entitlement cap, no cross-app/environment mixing, expiry/rotation/erasure, and unchanged committed recovery. Preserve frozen section 8/9 vectors. Run normal required repository gates without exceptions.

Root must record in `orchestration.md` and the slice03 handoff/PR evidence: (1) the section 9 wording gap and proposed correction; (2) the proposed I.5/ledger/suppression rationale; (3) exact candidate hashes and actual checks; (4) separate fixture versus genuine provider evidence; (5) inherited and new activation blockers. Contract section 10.8 must reference the proposed frontend scope alongside its existing backend rule proposals. The frontend writer owns Constitution/implementation docs; the contract owner alone edits section 10. No section 8/9 behavioral rewrite is required by this wording correction.

Human review of the governing-document proposal remains required before merge. Final live disclosure/Terms/Data Safety, purpose/retention/deletion policy, campaign/threshold configuration, matching provider/project/package/certificate/licensing and genuine signed Integrity acceptance remain **BLOCKED** until independently satisfied. Admission stays unavailable when its prerequisites are missing; no live fallback is inferred. Existing release blockers remain cumulative. An eventual draft must say **external acceptance blocked; not ready for merge**. This report grants no live activation, remote migration, merge or release authority.

## Evidence boundary

Read original root and both repository instructions; original Constitution, adopted orchestration prompt, implementation plan and accepted abuse-control research; prior guest Integrity decision, Google activation decisions and external-gate decision; actual slice03 Constitution, sections 8/9 and section 10 draft, and the existing accessor suppression. No legal determination or new platform-policy research is claimed. No tests/builds/device/remote commands or PR changes were performed.
