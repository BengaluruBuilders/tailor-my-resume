# google-activation-v1 canonical fixtures

Synthetic contract examples only. They contain no valid Firebase/Google/Integrity credential or live
policy. Do not select fake verifiers from HTTP fields or enable a real environment from these files.

- `policy.json`: finite deterministic fixture configuration, with live decisions explicitly unset.
- `hash-vectors.json`: exact UTF-8/hex/SHA256 base64url bytes, explicit requested subaction sets,
  signup/base revision-pair binding, private proof hashing and negative binding mutations.
- `requests.json`: strict request examples, including missing-secret same-UID recovery and nullable
  absent signup campaign; deliberately invalid unions/unknown fields for implementer tests.
- `responses.json`: independent grants in both directions, base-first/signup-first recovery,
  committed snapshot replay with current post-spend wallet, and conflict with no transfer.
  `httpExamples` separately wraps the unchanged JSON bodies with HTTP status/headers for
  independent source rate limits, immutable partial replay and whole-operation 429.

Run `python3 generate-fixtures.py --check` in this directory. It reproduces four JSON files and
checks vector bytes/hashes/negative mutations and balance arithmetic. This is fixture consistency,
not executed feature/security/platform evidence. Required application tests are in
[acceptance.md](../acceptance.md). The normative wire definitions are
[BACKEND_CONTRACT.md](../../../../../BACKEND_CONTRACT.md), section 9. The earlier guest-trial-v1
fixtures and complete earlier contract bytes remain unchanged.
