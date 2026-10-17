# Direct Play Integrity rule decision — 2026-10-17

Decision: proceed with a narrow Article VII amendment in the guest-trial frontend PR. Leave the PR unmerged for human review. This is an implementation authorization decision, not approval to enable real admission or deploy production.

## Basis

The adopted orchestration prompt and approved implementation plan explicitly require direct Play Integrity Standard requests, action binding, and a limited Android-ID continuity signal. The frozen `guest-trial-v1` contract, section 8, defines their scope and privacy controls. Current Constitution I.5 permits Firebase Auth, Credential Manager and Billing but does not expressly permit this intentional direct Google call. Its dependency ledger also describes Integrity as unused outside Firebase flows. Leaving those statements unchanged would hide a real behavioral change.

Article VII requires a PR editing the Constitution; it does not require a separate owner confirmation before preparing an unmerged amendment. The user's later direction explicitly delegates unresolved decisions to a fresh agent and requires coding to continue without approval questions. This narrow amendment is necessary to implement the authorized design accurately. Human review and green checks remain required before merge; this run authorizes no merge.

## Authorized scope

Add an I.5 allowance equivalent to: “Direct Google Play Integrity Standard requests are allowed only for an explicit guest promotional admission attempt under BACKEND_CONTRACT.md section 8.” Keep candidate resume/job data restricted to the backend, the existing HTTP stack restriction, backups disabled, and the bans on analytics, crash and ad SDKs. Integrity receives the server-issued opaque request hash; do not add candidate text or raw continuity values to Google request fields.

Correct the dependency ledger to distinguish direct Integrity use for this purpose from reCAPTCHA's unchanged transitive Firebase use. Retain the actual resolved release dependency facts; do not assert that Integrity is unused. No new runtime SDK or dependency baseline growth is authorized by this decision. If implementation requires a direct declaration of an already-resolved library for dependency hygiene, keep the existing version and resolved classpath and explain it in the PR.

If lint actually reports `HardwareIds` at the approved Android-ID read, a suppression may cover only that accessor and only that warning. Cite the official Android API or identifier guidance and explain the limited repeat-trial purpose. Article VII.4 requires the PR to identify and justify this exception; update the Constitution amendment ledger accordingly. Do not add a speculative suppression, enlarge a baseline, disable a module-wide warning, or weaken a gate. Official references for the implementation rationale are `https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID` and `https://developer.android.com/identity/user-data-ids`; the approved research also cites `https://developer.android.com/google/play/integrity/standard`.

Preserve all frozen section 8 controls: no evidence acquisition for policy/wallet reads; explicit disclosed attempt only; bounded app/environment-separated server HMACs; no raw identifier/token/digest logging; erasure/export handling; no combined fingerprint; missing configuration or evidence closes admission; staging never falls back to production. Integrity binding does not certify the client-reported ID or identify a unique human. Production disclosure, retention and real provider/package/certificate/license verification remain activation gates.

## Required evidence

- Record targeted deterministic tests proving policy/wallet reads and disabled/unavailable admission do not collect evidence, explicit attempts bind the exact server-issued request hash, and missing configuration/evidence cannot grant credits. Include the frozen canonical digest vectors and account/lifecycle cancellation or stale-result guards.
- Record the actual relevant lint result before deciding whether `HardwareIds` suppression is needed; then rerun lint after any narrow exception. No baseline update is an acceptable substitute.
- Run the required frontend verification gates, including constitution policy, unit tests, lint, release dependency guard and the repository's full pre-PR command. Report actual outcomes or concrete blockers.
- Show the resolved release classpath remains unchanged if the existing transitive Integrity API is used. Explain direct Google traffic and any suppression in the PR amendment rationale.
- Genuine Firebase/Play Integrity signed-package validation remains separate from fake-provider logic tests. Missing external capability is BLOCKED, never PASS. Keep admission disabled until its real activation checks are satisfied.

## Review performed

Read both repository agent instructions; the approved orchestration/implementation plan and abuse-prevention research passages; active Constitution I.5, III.3 and Article VII with dependency ledger; frozen backend contract section 8; and the exact current `GuestEvidence.kt` Android-ID/Standard Integrity accessor. No source or Constitution was edited. No app validation was run while implementation writers were active. This note records a fresh narrow decision, not a whole-app validation verdict.
