# Slice02 independent post-publication audit — initial published pair

**Verdict: FAIL — one publication-induced required backend CI failure (GS02-PUB-001).** Published head/base/evidence checks and the bounded genuine closed-admission smoke passed. Frontend CI is terminal with passing code shards/aggregate and a replaced Constitution job, as detailed below. Genuine Google/Integrity/admitted reward/spend acceptance remains **BLOCKED**. A fresh publication fixer is required; no product or gate fix was made by this validator. The four prior implementation findings remain resolved on the tested executable pair.

## Actual PRs, bases and executable mapping

Read-only GitHub metadata confirms both PRs **OPEN, DRAFT, unmerged, auto-merge null**, with no closing issue references:

| PR | Actual published head | Actual base branch / SHA | Tested executable source |
| --- | --- | --- | --- |
| [Frontend407](https://github.com/BengaluruBuilders/tailor-my-resume/pull/407) | `4a297db54a88eaa2771c03b6776cd7df8e0dc43e` | `codex/earn-01-guest-trial` / `7ffd2f42a423f4037bfea372cd47b1de4b4d035e` (parent406) | `01cfb637e26ca075df78892a352b8b7c62e8531d` |
| [Backend125](https://github.com/BengaluruBuilders/apps-backend/pull/125) | `2f36f59f83bc414aeb4f8546e721ab1a12101963` | `codex/earn-01-guest-trial` / `69797ec759f3493b6d4f5d6912a82b1429e30fdb` (parent124) | `ba0993ada6055ef61dd000327e0617418755b58b` |

Independent Git comparison finds exactly38 added frontend docs/evidence paths and10 backend docs/evidence paths since the tested sources, with **no paths outside docs/** changed. Application code, schemas/migrations0013/0014, dependencies, configuration, contract and gate files are unchanged. However, a documentation directory does not make an executable `.ts` artifact invisible to the normal lint gate: that is the confirmed failure below.

The actual GitHub PR diff path inventories (151 frontend /44 backend) match local `base...head` paths exactly; each actual merge-base equals its intended predecessor SHA. This is an incremental stack, not a diff against main. Both worktrees remain clean on their published heads. Contract SHA256 remains `2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a` (`google-activation-v1-rate-limit-1`).

PR bodies correctly cross-link each other and their parents, describe partial issue scope, disclose blocked external acceptance and require draft/no merge. Backend's proposed narrow Constitution rule3 capability exception is explicitly pending human review; no approved merge/live-policy claim is made. Both `closingIssuesReferences` arrays are empty; no whole issue is closed by these drafts.

## Stable publication finding

### GS02-PUB-001 — P1: copied executable evidence breaks published backend required CI

**Expected:** The actual published backend head passes its unchanged required `npm run check`. An archived validation fixture remains readable evidence without accidentally entering application lint/type-check discovery. Preserve the exact original fixture bytes/provenance and the security/quality gates.

**Observed:** [Backend Validation run39019385087](https://github.com/BengaluruBuilders/apps-backend/actions/runs/39019385087), head`2f36f59f83bc414aeb4f8546e721ab1a12101963`, completed **FAILURE**. `tsc --noEmit` passed, then `eslint . --max-warnings 0` failed on the newly published file:

`docs/testing/earn-credits/2026-10-17/02-google-signup/validation-evidence/backend-dump-restore-fixture.ts`

The actual error is a project-service parsing error: that file was not found by the TypeScript project service; ESLint suggests including it in tsconfig or allowDefaultProject. This is an evidence packaging error, not a diagnosed product behavior failure. The later formatting/boundary/test commands in the `&&` chain did not run. Prior720/721 required-test evidence remains valid for sourceba0993 but is **not** a passing required gate for this published head.

**Reproduction:** Read the failed run's “Run npm run check” logs, or run the unchanged `npm run check` on the actual published backend head. Independent validator used the existing CI failure; no duplicate full suite or self-repair was launched. [Preserved failure log](post-publication-evidence/backend-ci-publication-failure.log), SHA256 `6a61da061bae4e8f1ab49d472a0cdddafd0a77f22aa839f02e2a52679a36e1ec`; [terminal run metadata](post-publication-evidence/backend-validation-ci.json).

**Affected evidence and identity:** Published backend fixture above, its README/manifest references and any matching frontend evidence reference chosen by the fresh fixer. Original fixture SHA256 is `1851abe84eeba3378722356c712044cabffc77f1e8f3af0d4110a440300d6688`. Exact initial published and tested pairs are recorded above. No backend source/API/schema repair has been shown necessary.

**Repair boundary:** A fresh fixer can preserve fixture bytes as clearly labelled inert text evidence and update affected links/manifests, then prove the normal published required gate passes. Do not add an ESLint/tsconfig ignore, weaken the project service or gate, silently drop evidence, or claim a new test execution from the old archive. The fixer decides the concrete packaging change. This publication-failure cycle has **0 repairs attempted**; prior closed cycles are not reset or bypassed. Any changed path/asset inventory must be independently checked after the new exact published head. Reuse the short smoke below if executable bytes remain identical.

## Terminal CI results

| Run / scope | Actual terminal result and interpretation |
| --- | --- |
| [Frontend full run39019370877](https://github.com/BengaluruBuilders/tailor-my-resume/actions/runs/39019370877), head4a297 | Overall **CANCELLED**, because older Constitution policy job117357924830 was cancelled/replaced. Detect changes, static checks, APK assembly117357949360, lint, feature/core/app tests, screenshots and release configuration all **SUCCESS**. Full combined “Build, lint, and unit tests” job117359486545 **SUCCESS**. Do not relabel the whole run PASS |
| [Frontend replacement/title run39019398220](https://github.com/BengaluruBuilders/tailor-my-resume/actions/runs/39019398220), same head4a297 | Overall **SUCCESS**. Replacement Constitution policy117358005999 **SUCCESS**, including actual “Scan for secrets”, “Test the secret scan script” and Constitution-rule steps. Its combined check succeeds with code-specific jobs **SKIPPED**. Those skips do not substitute for the executed full-run code jobs above |
| [Backend Validation39019385087](https://github.com/BengaluruBuilders/apps-backend/actions/runs/39019385087), head2f36 | **FAILURE**, exact packaging/lint finding GS02-PUB-001; no automatic rerun/fix requested |
| [Backend Secret scan39019385114](https://github.com/BengaluruBuilders/apps-backend/actions/runs/39019385114), same head2f36 | **SUCCESS**. This is an actual CI security result, separate from the failed backend validation |

Structured job/step evidence is saved in [post-publication-evidence](post-publication-evidence/manifest.json). All watched runs are terminal; no live CI handle is being abandoned. The original28 local preliminary real-gitleaks skips remain disclosed historical limitations; later successful CI scan/script steps do not retroactively turn those local skipped subcases into executed ones. No workflow/check was changed or manually retriggered here.

## Immutable publication/evidence review

Both GitHub recursive trees were read and untruncated. **All37 frontend and9 backend inventoried document/evidence files** match local size/SHA256 and the corresponding immutable GitHub blob identity. All54 relative Markdown links checked resolve against the published trees; no broken link or hash mismatch was found. Actual PR diff paths and merge bases also match. See [complete integrity summary](post-publication-evidence/publication-integrity-summary.json).

The published independent revalidation report retains SHA256 `2b50901fde8e4bfe65f9f996f967a47f3a40265b8f6a9995190bc00ea5200df8`; its asset manifest retains `fcca1127a8ba0f4af90645c859ab131bf42f35a3be6ffb2238ad88db980eef13`. Captions correctly identify the tested pair/closed admission and screenshot06's timing limitation. Backend route/transaction tests and PostgreSQL dump/restore/deletion evidence are accessible through its README; noop Firebase/file deletion ports are expressly labelled. UI images are not substituted for backend proof.

Immutable indexes: [frontend4a297](https://github.com/BengaluruBuilders/tailor-my-resume/blob/4a297db54a88eaa2771c03b6776cd7df8e0dc43e/docs/testing/earn-credits/2026-10-17/02-google-signup/README.md), [backend2f36](https://github.com/BengaluruBuilders/apps-backend/blob/2f36f59f83bc414aeb4f8546e721ab1a12101963/docs/testing/earn-credits/2026-10-17/02-google-signup/README.md). Accessibility is verified using the authorized GitHub API/tree account; this is not a claim that private repository assets are anonymously public.

## Bounded genuine post-publication smoke

**PASS for the attainable closed-admission scope only.** Same task-owned emulator5582 (Android16/API36 Google APIs x86_64), agent-device0.21.28/ADB; no mobile MCP tool was available. Actual installed `base.apk` was read and independently hashed: **6,032,782 bytes, SHA256 `b174910dedae7fdb590113a2bdce87cc4bc106564c2b6908a55f5c63ddcb5bff`**, identical to the independently tested signed artifact. No reinstall/reset/account operation was performed. This APK's executable pair maps to the published docs-only heads above.

The app opened with **0 credits** and the preserved SG fixture. Profile → Settings → Google account & credit offers showed neutral no-offer copy. An explicit Check offers again kept that state; Back to my work returned normally. Contact retained **Synthetic Guest** with empty private contact fields. Two new actual captures are [postpub offer recheck](post-publication-evidence/01-google-offer-recheck.png) and [preserved synthetic profile](post-publication-evidence/02-synthetic-profile-preserved.png); captions and pair mapping are in the manifest. No provider-error flow or lengthy recording was needlessly repeated.

The bounded loopback-only CONNECT relay retained the original APK CA/hostname and encrypted TLS passthrough. Normal health preflight at09:04:47Z returned200/200. Distinct actual app candidate TLS tunnels followed at09:04:48Z. Candidate backend remained exactba0993. At09:06:34Z a safe read-only observation still showed0users,0policies and0economic/activation/intent/operation rows, consistent with the guarded read-only/default-wallet paths. This repeats the prior correlated transport/app inference; no decrypted request, token or UID was collected. See [smoke summary](post-publication-evidence/smoke-summary.json) and [candidate observation](post-publication-evidence/candidate-observation.json).

**Still BLOCKED/unproven:** real disposable Google link/direct/conflict/same-user reauthentication, authoritative configured Integrity project/decode/strict verdicts, approved live policy/cutoff/retention/capacity/disclosures and admissions, actual grants and credit spending, and release acceptance. No mocked result or error screenshot is labelled a positive platform grant. Nothing in this post-publication review completes the whole slice/issue/goal or permits merge.

## Cleanup and fresh-fixer handoff

Owned agent session `tmr-google-postpub` closed, emulator89342 exited0, relay48965/PID724480 exited143 after command-verified scoped termination; listener18765 is absent. No Gradle/build/test process was started, and no local heavy job overlapped the emulator. Candidate API129620/private PostgreSQL114856 and original API55083/system PostgreSQL28765/shared Caddy40092 were rechecked alive and preserved. Source worktrees stay clean; no commit/push/PR mutation, policy, remote migration or production action occurred.

One test-environment cleanup limitation was discovered: this emulator boot restored the old task proxy value despite the prior session's observed in-memory null/null cleanup. This does not invalidate the prior literal readback evidence, but shows reboot persistence had not been established. This session deleted both proxy keys, waited through subsequent work, re-read null/null twice and invoked sync before shutdown. No additional reboot-persistence test is claimed. Personal Pixel/original AVD were untouched. Final local available memory11,309MiB; historical swap left unchanged.

The audit and CI watch are complete, with GS02-PUB-001 the sole new attainable blocker. Validator is idle and releases ownership to a **fresh publication fixer**. After repair, independently verify the new heads, updated inert evidence/links/manifests and actual required CI. If executable code/APK remain unchanged, this smoke and prior code checks can be reused with the new docs-only mapping. No duplicate full suite or device run is needed solely for unchanged documentation.
