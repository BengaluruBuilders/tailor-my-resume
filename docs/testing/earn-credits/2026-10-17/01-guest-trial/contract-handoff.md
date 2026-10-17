# Slice 01 contract handoff — 2026-10-17

Status: frozen and committed, agreed with backend and Android implementation agents. No source
implementation, push, deployment, merge, migration, or device operation was performed by this agent.

| Repository | Isolated checkout | Branch | Verified fetched base |
|---|---|---|---|
| Frontend | `/tmp/tmr-earn-20261017-frontend` | `codex/earn-01-guest-trial` | `6d73a22dc392451f27fc2999eb788f7b048880f0` |
| Backend | `/tmp/tmr-earn-20261017-backend` | `codex/earn-01-guest-trial` | `3abd46cafb45abaee4f0806f0efa32a02f992375` |

Default branch fetch succeeded for both repositories through reviewed network escalation. Backend
origin/main advanced from discovery's `7e57b03` to `3abd46c` (#119); the pair uses that actual fetched
base. Original staging branches, all unrelated worktrees, and original untracked user files remain
untouched. Copied approved source plans/research remain untracked in the isolated frontend; copied
backend plan remains untracked there. They were deliberately excluded from the contract commit.

Frontend contract commit: `3c9ff77f82fc7a1c9a394c8431bc07a40f1af8df`.
Contract revision: `guest-trial-v1`, section 8 of `docs/BACKEND_CONTRACT.md`.
Full contract file SHA-256: `b76d2202bf1e76f25dfec6191e2b3e2d04cb9dbb419deccfa33979444071a2a6`.
Backend remained at base until handoff; implementers now own source changes after parent go.

Exact routes are authenticated `GET /v1/tailormyresume/credits/policy`,
`POST /v1/tailormyresume/credits/grant-challenges`, and
`POST /v1/tailormyresume/credits/initialize`. Challenge returns server requestHash for Standard
Integrity; initialize echoes challengeId only. The backend persists its expected issued binding hash
and hashed challenge secret, then compares decoded token binding against that immutable scoped row.
The final request schema has no initialize `challenge` field. Stable business hash excludes policy
revision/evidence, while the action-bound hash includes policy/challenge/actor/app/environment/key.
Committed replay returns current wallet and original receipt before current policy/evidence checks.
Replaying a fully spent grant never tops up the wallet.

Cloud project numbers use non-secret build configuration:
`tailormyresumePlayIntegrityCloudProjectNumber` for release and
`tailormyresumeStagingPlayIntegrityCloudProjectNumber` for debug/staging. Values must fit a positive
signed 64-bit decimal integer and match server verification project. Missing config closes admission.
No actual value or credential is committed. Default-off admissions and actual external verification
remain mandatory; Google signup/link/referrals are unavailable in this slice.

Committed acceptance and fixtures (paths relative to isolated frontend):

- `docs/testing/earn-credits/2026-10-17/01-guest-trial/acceptance.md`
- `docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/http-cases.json`
- `docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/request-schemas.json`
- `docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/hash-vectors.json`
- `docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/generate-fixtures.py`

Verified: generator output reproduced with `python3 .../generate-fixtures.py --check`; three fixture
files, strict request cases, negative input/binding cases, canonical UTF-8 hashes, balance arithmetic,
and evidence/policy-refresh stable-business identity passed. `git diff --cached --check` passed.
Only the contract + acceptance + four fixture/generator files were staged in the six-file commit.
No broad suite was run for this docs-only change.

Cross-repository matching work is explicit: backend typed identity/access matrix + zero wallet reads
+ unique protected grants/capacity/continuity/integrity + lifecycle; Android anonymous identity,
server policy DTO/actions and wallet authority + Standard Integrity adapter + account-generation guards.
Generic TMR core AI routes must be blocked before anonymous acquisition to avoid the typed-route
bypass; other apps retain their existing behavior. Historical free/purchased counters and ledger IDs,
legacy settlement, and per-success tailoring semantics are preserved. Stable credit IDs are explicit.

Remaining genuine platform evidence is documented, not treated as a code blocker: Firebase anonymous
provider in correct projects, real signed-package/certificate/license Standard Integrity verification,
matching project number, trusted candidate staging ingress, disclosed Android-ID purpose/retention,
Data Safety/Terms alignment, and explicit production capacity/thresholds. Local deterministic verifier
results prove logic only. User-authorized candidate staging proceeds through GitHub push then Ubuntu
pull; no merge or production deployment is implied. Pixel validation preserves installed data; reset
cases use disposable emulator. See `../decisions-environment.md` for exact local tool/DB prerequisites.
