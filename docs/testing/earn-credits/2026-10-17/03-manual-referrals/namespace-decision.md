# Slice03 namespace decision

Decision date: 2026-10-17. Source inspection only; no application edits, tests, builds, device operations, remote changes or activation. Inputs: FE `12099d51b1416b1ac8d735fea55a8ced1d91fe4a`, BE `2a20be12598cb268bd57e4ac029b984d8fbcbea4`, frozen contract SHA-256 `09507f4e322e72e7e34ecf4797dca8bc89178d2c33cdb1280142b025c58dc3a0`. The adopted orchestration prompt and repository instructions apply. Candidate worktrees remain frozen until root releases fixes.

## Verdict and selected remedy

This is a current implementation defect, not a missing live activation choice. Contract 10.4 permits independently configured namespace tokens; the Android implementation rejects valid codes unless their prefix is exactly `tailormyresume.staging` or `tailormyresume.production`. A backend configuration release cannot choose another valid namespace and complete manual entry with this APK.

Select syntax-only Android candidate normalization with authoritative server scope validation. Remove the hardcoded prefix/environment argument from `ReferralCode.kt` and its `RemoteReferralGateway.save()` caller. Keep every ASCII, length, grammar and normalization constraint, the full canonical code, and all journal environment/UID/provider/generation fences. Never derive API destination, tenant, deployment environment, identity or challenge binding scope from code tokens. No new wire metadata or Android build property is needed.

Client acceptance means “candidate saved,” never “valid referral” or “applied.” A structurally valid foreign candidate may be retained and later rejected by the server. This is safe UX filtering: the server already selects the database from authenticated app context and requires both its configured namespace and scoped full-code lookup. No client may select tenant scope; do not weaken any backend canonicalization, lookup, token or prefix check.

Build-bound namespace properties would require a new APK for a backend namespace choice, introduce duplicate deployment configuration, and leave guests dependent on unavailable configuration. Additive wire metadata would expand the frozen schema without helping the security boundary. Neither is justified here.

## Contract and fixtures

The current text does not explicitly require Android to know the namespace mapping. Its requirement to persist the first **syntactically valid** guest candidate supports the selected implementation. “Reject ... wrong scope” remains mandatory server behavior. However, its placement in one canonicalization paragraph and the mixed `invalidCodes` fixture list are ambiguous. The sole contract owner should add this clarification before fixing the client:

> Android canonicalizes and validates candidate syntax without assuming namespace tokens. The server rejects codes outside its explicitly configured app/environment mapping and resolves the complete code only within authenticated deployment scope. Saving a candidate does not validate scope, existence or eligibility. Code tokens never choose the request's app or environment.

This clarification changes no route, schema, field, canonical bytes, hash construction or security requirement. Preserve every existing valid vector and digest/requestHash/fingerprint, section 8/9 bytes, and fixture hashes as required by the owner. Existing `invalidCodes` entries for `tailormyresume.production...` and `t0ilormyresume.staging...` are syntactically legal but invalid for the fixture's server scope. Preserve them as **server-scoped** negative cases; stop asserting that Android syntax parsing rejects every entry. Add separate client syntax tests (or classify those two entries in test code) without rewriting frozen canonical vectors. The fixture verifier may retain scoped server normalization; it must not be misrepresented as the Android candidate parser. Record any contract-file hash change through the sole owner.

## Actual behavior and lifecycle

- Backend `referral-security.ts` exposes strict `appNamespace` and `environmentNamespace` constructor configuration through `TAILORMYRESUME_REFERRAL_CONFIG`; omission produces no ports. `referrals.ts` derives security scope from authenticated app and configured deployment environment, not namespace tokens. Its route fixture already issues/attaches `tailormyresume.local-fixture` codes, which the current APK rejects at save time.
- Own issued codes are displayed verbatim from `GET /referrals/me`; Android does not run its parser over `result.code.value`. Thus a non-fixture own code can display/copy correctly while another user cannot enter it. Parser removal fixes manual entry, not own issuance/display.
- A guest may save a full syntactic candidate with missing policy/config and no Google qualification, evidence collection or award. On proven same-UID Google linking, retain it unchanged; delayed activation uses the original server activation deadline. Existing-account conflict still requires explicit review/rebind. No namespace rewrite, suffix-only fallback or automatic submission is allowed.
- Missing referral configuration closes new issuance/awards; server-owned existing reads and committed recovery remain available under the unchanged deployment scope. Android must retain the candidate while the offer is unavailable. Backend retained-namespace recovery is for the actor's existing scoped receipt, not permission to resolve foreign codes or migrate namespaces.

## Additional backend correction required

The existing no-silent-namespace-change protection is incomplete. `namespaceUsable()` checks for conflicting stored code prefixes in `offer()` and `admission()`. New `code()` issuance calls `issueReferralCode()` with a qualification callback that checks only activation/provider metadata; it never checks namespace compatibility. `offer.codeIssuance` also reports AVAILABLE merely when ports exist. Changing config after codes exist can therefore issue a new owner's code under a second namespace while reward offers close.

Fail new issuance closed on a conflicting stored mapping, report `codeIssuance:UNAVAILABLE`, and preserve existing own-code recovery before configuration/Admin/admission checks. Serialize compatibility check plus first insertion per scope after actor locks, using an existing compatible scope lock, so concurrent instances with different namespace configs cannot both establish conflicting mappings in an empty scope. Do not add a rotation/migration API or silently regenerate/rewrite codes. A durable registry/schema is unnecessary for this bounded fix if compatibility with all retained scoped codes is enforced atomically. This is required by existing 10.4; no wire or policy change is needed.

## Required regression evidence after release of the freeze

1. Android parser accepts uppercase/outer ASCII whitespace for a non-fixture full prefix such as `resume-demo.preview-2`, preserves canonical full bytes, and rejects malformed grammar/Unicode/interior whitespace/URLs/overlength. Existing canonical hashes remain identical.
2. Android gateway stores that candidate as guest through recreation and same-UID Google upgrade, then sends the unchanged full code under its fixed API/environment and original business identity after explicit apply. Own returned non-fixture code displays/copies unchanged. Missing configuration leaves a pending candidate and produces no mutation/evidence collection.
3. Real local TMR routes configured with both non-fixture tokens issue and attach the code with exactly one full inviter reward. Wrong app token, wrong namespace, wrong deployment environment and a real code in another scope cannot resolve or award; assert no attachment/grant/ledger/balance/capacity writes. Generic isolated-app tests supplement this, not replace it.
4. Existing namespace A code plus config B rejects new-owner issuance and new attachment, reports unavailable issuance, and preserves A own reads/recovery. Concurrent A/B first issuance cannot create mixed mappings. Config removal preserves committed recovery; no code or receipt is rewritten.

These are requested checks, not observed PASS results. Existing backend route tests exercise `local-fixture` and retained recovery, but no test/build was executed for this report. Root must route fixes to writers and independent validation after current jobs terminate. No live namespace is selected or approved by this decision.
