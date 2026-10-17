# GS02-PUB-001 independent publication revalidation

**Verdict: RESOLVED. Attainable post-publication checks PASS with the disclosed frontend run-cancellation history; genuine external acceptance remains BLOCKED.** The historical initial publication failure in [post-publication.md](post-publication.md) is preserved. No source fix, check rerun, Android build/device operation, commit, push or PR mutation was performed by this validator during the affected audit.

## Actual final mapping

| Item | Independently verified identity |
| --- | --- |
| Backend PR125 | OPEN/DRAFT/unmerged, auto-merge null, head **`4e954057173fd6a25576708450c864fd31d17c19`**, base `codex/earn-01-guest-trial` / `69797ec759f3493b6d4f5d6912a82b1429e30fdb`; no closing issue references |
| Frontend PR407 | Unchanged OPEN/DRAFT/unmerged, auto-merge null, head **`4a297db54a88eaa2771c03b6776cd7df8e0dc43e`**, base `7ffd2f42a423f4037bfea372cd47b1de4b4d035e` |
| Tested executable pair | Frontend `01cfb637e26ca075df78892a352b8b7c62e8531d` / backend `ba0993ada6055ef61dd000327e0617418755b58b`, unchanged |
| APK / contract | Unchanged APK SHA256 `b174910dedae7fdb590113a2bdce87cc4bc106564c2b6908a55f5c63ddcb5bff`; contract `google-activation-v1-rate-limit-1`, SHA256 `2143cac25a92c6b0b9a26c5b65932f638f20373c76f1c7048785b44ba244391a` |

The backend change since initial publication renames the fixture, updates README/asset inventory and adds successful-check evidence only. There is **no delta outside docs/** against the tested source. All223 original checked backend paths still match their pre-check manifest, including source, tests, migrations, dependencies, configuration and gates. Actual GitHub PR diff inventory46paths matches the local incremental base...head comparison. No backend executable or Android matching change is required.

## Finding resolution and evidence integrity

**GS02-PUB-001 resolved:** archived `backend-dump-restore-fixture.ts` is now **`backend-dump-restore-fixture.ts.txt`**, 6,103 bytes, **100% byte-identical** to the original published fixture. SHA256 remains `1851abe84eeba3378722356c712044cabffc77f1e8f3af0d4110a440300d6688`. It is readable inert evidence; no ESLint/tsconfig ignore or weakened gate was added.

All11 updated backend inventory assets match their local SHA256/size and immutable GitHub blob identities. All8 relative Markdown links checked resolve in the new tree; no stale latest-backend fixture link or hash mismatch was found. The PR body points to new-head immutable evidence and preserves the tested pair, original failure, external blockers, partial issue scope and pending human Constitution rule3 review. Frontend's original immutable backend evidence links remain valid historical links; its paired PR link resolves the updated backend draft. See [independent integrity summary](publication-revalidation-evidence/repair-integrity-summary.json).

The final local compressed log matches its inventory; decompressed SHA256 is **`5da5ed7346f2d70cf5656112d0b6fb61f45d5cd43cd3000ef0190205d5da8b07`**. Actual log lines show unchanged typecheck/lint/format/boundary commands, the real custom-format pg_dump test, **721 tests /721 pass /0 fail /0 cancelled /0 skipped**. The five retained repair-evidence assets also independently match their manifest.

The two corrective attempts are disclosed: packaging repair1, then local PostgreSQL loader-environment correction2 after an uncontaminated720-pass/1-failure run. The task-local launcher adds the relocated library path and execs the real PG18 binary with unchanged arguments; it fabricates no archive. Actual binary SHA256 independently matches `276bee795df0a420e5ffb35949620d18bb0a5e9ef9373fcc5a59b58d151cd921`, and actual version is PostgreSQL18.6. No repository/security/test change or skip was used to repair the environment. Earlier overlapping/sandbox logs remain explicitly incomplete, never passing evidence. The corrective cycle is now closed by genuine terminal passing checks, not reset.

## Actual new-head CI

- [Backend Validation39020724416](https://github.com/BengaluruBuilders/apps-backend/actions/runs/39020724416), exact head4e954: **terminal SUCCESS**. Its actual unchanged `npm run check` log independently records **721/721 passed, zero failures/cancellations/skips**, including “a real pg_dump produces a custom-format archive.” This verifies the final published evidence additions too, not merely the preceding local input state. [Terminal job/step metadata](publication-revalidation-evidence/backend-validation-ci.json), [actual result lines](publication-revalidation-evidence/backend-ci-test-lines.txt).
- [Backend Secret scan39020724533](https://github.com/BengaluruBuilders/apps-backend/actions/runs/39020724533), same exact head4e954: **terminal SUCCESS**. [Metadata](publication-revalidation-evidence/backend-secret-ci.json).
- Frontend remains4a297, so prior terminal CI evidence is reused unchanged: full run39019370877 is overall **CANCELLED** because its old Constitution job was replaced, while every code shard and the full combined check **SUCCESS**. Replacement39019398220 is **SUCCESS**, with Constitution/actual secret-scan/script steps successful and code jobs skipped. No whole-run PASS or code execution is inferred from those skips. The original28 local gitleaks tool skips remain historical skips.

## Reused smoke, limitations and handoff

The completed genuine short post-publication smoke in [post-publication.md](post-publication.md) is reused through the verified executable mapping: actual installed APKb174, normal candidate TLS/hostname/CA, wallet0, Settings Google panel, explicit neutral recheck, return to work and preserved Synthetic Guest. No Android rebuild/emulator/credential flow or duplicate full suite was repeated for an archive filename change.

All four original implementation findings remain independently resolved. **Still BLOCKED/unproven:** genuine disposable Google link/direct/conflict/same-account recent-auth recovery, authoritative Integrity project/decode/strict verdicts, approved live policy/cutoff/retention/capacity/disclosures and opened admissions, admitted grants and successful spending, and release acceptance. The proposed narrow backend Constitution exception still requires human review before merge. The full slice/issue/goal is not declared complete; both PRs remain draft and not ready for merge.

No new runtime resource was started or changed. The previous emulator/relay cleanup and preserved candidate/original services remain the resource handoff; this affected audit used read-only local files/Git/GitHub and one PostgreSQL executable version query only. CI watch is terminal; there is no live handle to transfer. Validator is idle. Publisher may add this follow-up and its minimal evidence as docs-only, then request a final head/evidence mapping check without repeating unchanged code/device validation.
