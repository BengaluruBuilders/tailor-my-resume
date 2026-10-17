# Slice04 contract fixtures

`referral-links-v1` is additive section 11 of frontend-owned `docs/BACKEND_CONTRACT.md`.
Existing section 10 authenticated routes, response fields, source LINK canonical digest, status/error,
Integrity, atomicity, grant/replay/lifecycle semantics and fixtures are reused without changes.
Existing nullable `shareUrl` communicates concrete share capability; no new wire field/route.

- `schemas.json` defines new logical link configuration, static offer snapshot and Android association artifacts. These are packaging/configuration boundaries, not API request inputs. The backend may place logical `linkConfig` in existing optional `referralConfig.link`; appId/environment must match actual trusted composition. Disabled/missing means no link capability. No live configuration is supplied.
- `examples.json` contains 6 schema examples, response delta, exact Play outer encoding and 5 snapshot-rendering cases. `.invalid` hosts, synthetic certificate and 30/300-second bounds are isolated fixtures, never served ownership/activation evidence.
- `negative-shapes.json` contains 8 invalid schemas and 6 semantic configuration/snapshot refusals. Origin membership, trusted package/environment mapping, safe integer time bounds, cache <= validity, effective/expiry order, policy-source coherence and runtime scope/clock require implementation tests as well as schema validation. TMR production package is `com.tailormyresume.app`; explicitly configured nonproduction uses `com.tailormyresume.app.staging`. Never infer API environment from that name.
- `transport-vectors.json` contains 3 valid and 18 invalid URLs plus 3 valid and 13 invalid Play-returned inner referrers. Parse the inner string without percent decoding. Existing03 vectors already fix LINK source/hash binding.
- `acceptance-matrix.json` contains 24 focused requirements; PENDING/BLOCKED_EXTERNAL are requirements, not observed test results. Reuse03 transaction/security/lifecycle matrix with actual LINK route proof.
- `../link-flow-decision.md` is the accepted fresh flow decision. `freeze.json` hashes every new input and the unchanged earlier fixture set, and records the exact prior full contract prefix bytes/hash.
- `verify-fixtures.py` uses installed Python/jsonschema, makes no network request and validates fixture shapes, semantics, transport/Play vectors, rendering boundaries and freeze hashes. It does not exercise Android, routes, providers, transactions or hosting.

```bash
python3 docs/testing/earn-credits/2026-10-17/04-referral-links/contract/verify-fixtures.py
```

Backend writer owns share URL/config, static landing/public-offer/assetlinks packager and publication
runbook. Frontend writer owns variant filters/configuration, parser/referrer adapter, dispatch,
private inbox/journal ownership, automatic foreground LINK attempt, share UI and narrow Article VII
proposal. Both must agree to the exact frozen hash before root releases source work. Any material
contract change returns to this single writer and requires renewed agreement.

Contract consistency is not genuine App Links/Play install attribution, Google/Integrity, a hosted
current public offer or permission to activate. Preserve BLOCKED inputs and draft/unmerged disposition.
