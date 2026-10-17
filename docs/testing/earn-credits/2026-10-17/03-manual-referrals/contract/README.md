# Slice03 contract fixtures

Contract: `manual-referrals-v1`, frontend-owned `docs/BACKEND_CONTRACT.md` section 10.
The previous contract file prefix (including sections 8/9) has SHA-256
`2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a` and remains unchanged.

- `schemas.json`: strict JSON Schema 2020-12 wire objects, not a runtime dependency. Cross-field policy, ownership, canonicalization and transaction rules remain normative in the contract.
- `examples.json`: synthetic success/replay/paused/guest/detached/export and stable error bodies.
- `negative-shapes.json`: forbidden fields, missing required fields and invalid amount/provider/action inputs.
- `vectors.json`: exact UTF-8 compact canonical bytes, SHA-256 base64url payload/binding/fingerprint hashes; normalization equivalence, changed source/signal differences, foreign/malformed inputs.
- `test-policy.json`: deterministic local values only. Live configuration remains unset/off. A second non-Google app uses its own database/event/7-credit sink.
- `acceptance-matrix.json`: 46 required implementation/validation cases, all PENDING. This is not a test-results report.
- `verify-fixtures.py`: local schema/hash/normalization checks. Uses the already installed Python `jsonschema`; it does not download a schema, install a package or invoke an application runtime.

Run from the frontend root:

```bash
python3 docs/testing/earn-credits/2026-10-17/03-manual-referrals/contract/verify-fixtures.py
```

Fixture claims are limited to structural and deterministic contract consistency. They do not prove route implementation, Google authentication, Integrity verdicts, Postgres transactions, Android rendering, deployment or activation.

The inviter-deletion replay boundary is documented in `../lifecycle-decision.md`. The proposed frontend Article VII scope is in `../constitution-decision.md`; the frontend implementer owns the actual governing-file proposal. Both are contract decisions, not live authorization. Preserve guest/signup behavior and earlier vectors. Use the frozen contract-file hash and `freeze.json` before parallel writers begin; root obtains both exact-version agreements.
