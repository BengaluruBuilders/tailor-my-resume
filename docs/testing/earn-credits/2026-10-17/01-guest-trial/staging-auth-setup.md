# Staging anonymous-auth setup — 2026-10-17

Result: **the bounded staging configuration correction succeeded and was read back**. This is environment setup only; genuine client signup, guest grant/spend acceptance and independent validation remain unproven by this operation.

The executor read `staging-auth-decision.md`, `external-gate-decision.md`, `01-guest-trial/staging-candidate.md`, the workspace/repository instructions and `/home/codex/AGENTS.md`. Reviewed-host SSH prerequisite returned `abhishek-dubey-MS-7B16`. All remote project commands used `hub-project exec tailormyresume-earn-credits main -- …`, retaining its managed lock. Candidate checkout `/home/codex/projects/tailormyresume-earn-credits/repo` was clean, on `codex/earn-01-guest-trial`, with one worktree and HEAD `69797ec759f3493b6d4f5d6912a82b1429e30fdb`. Frontend pair from the decision report: `13aa4fef53057036c52fa25aa475e7a1ab303d83`; contract `guest-trial-v1`.

Before contacting Firebase, the script verified the existing `runtime/main/candidate.env` mode was exactly600, both the configured Firebase project ID and service-account JSON project ID equalled `tailormy-resume-staging`, and candidate HEAD/clean status matched. The existing Firebase Admin credential obtained its own OAuth token internally. No token, credential, private key, principal email or full configuration was printed, copied or persisted. Only the following whitelisted projection was emitted; `null` means the field was omitted, not an explicit true restriction.

| Observation | Before | Fresh readback |
| --- | --- | --- |
| Resource name | `projects/496155242121/config` | `projects/496155242121/config` |
| `signIn.anonymous.enabled` | omitted (`null` projection; not enabled) | `true` |
| `client.permissions.disabledUserSignup` | omitted (`null`) | omitted (`null`) |
| `client.permissions.disabledUserDeletion` | omitted (`null`) | omitted (`null`) |

The fresh baseline additionally required anonymous not enabled, signup restriction not true, an empty returned client-permissions object, and no blocking-function trigger keys, matching the prior decision observations. Any guard mismatch or HTTP failure would have stopped the operation without a permission workaround.

| Request | UTC time | HTTP |
| --- | --- | --- |
| Fresh staging getConfig | `2026-10-17T05:49:26.063Z` |200 |
| Single-field PATCH | `2026-10-17T05:49:26.551Z` |200 |
| Fresh staging getConfig/readback | `2026-10-17T05:49:27.079Z` |200 |

Exact mutation:

```http
PATCH https://identitytoolkit.googleapis.com/admin/v2/projects/tailormy-resume-staging/config?updateMask=signIn.anonymous.enabled
Content-Type: application/json

{"signIn":{"anonymous":{"enabled":true}}}
```

Readback verified the expected staging resource, anonymous enabled, and exact preservation of the baseline signup/deletion flag projection. The single-field update mask preserved all unrelated configuration. The successful enablement is left in place for the independent validator; no rollback was performed.

Rollback semantics, only if later required: obtain the same existing internal credential after the same staging/project guards; freshly GET the same staging config and require resource `projects/496155242121/config` and current anonymous enabled `true`; PATCH the same URL and single-field mask with `{"signIn":{"anonymous":{"enabled":false}}}`; freshly read back disabled effective state and unchanged unrelated flags. Never replace the full config, delete an identity, revoke tokens, reverse grants, or overwrite another actor's concurrent changes as an automatic rollback side effect.

No source edits, builds, client signup, test identity creation/deletion, phone action, PR, IAM grant, key creation, impersonation, reward-policy publication, Integrity change, quota/provider/domain/tenant change or production request occurred. Candidate API20000/private PostgreSQL20001/HTTPS20002 and original staging8080/8443 services were left running untouched. Production `tailormyresume-app` was not contacted. Two preliminary remote inspection commands encountered shell quoting / missing `rg`; neither reached a Firebase mutation, and the read-only inspection continued using available tools.

Independent validation must still retest deliberately disposable staging anonymous signup and candidate authentication. This setup does not establish that the earlier `ADMIN_ONLY_OPERATION` is resolved for the client, that Integrity is configured, that admissions are open, or that any guest reward has been granted.
