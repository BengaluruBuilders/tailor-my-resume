# Issue 268 reconcile: worktree diff vs drafted patch

Saved worktree diff: issue-268-worktree-uncommitted.diff (15 files, +4/-94).
Drafted patch (before this run): 16 files, +4/-118.

## Per-file difference
All 15 shared files have identical hunks (only `index` lines differ).
Only in the patch: core/data/.../repository/StoredUsageAllowanceTest.kt (whole file deleted, -24).
Only in the worktree: nothing.

## Decision
Keep the patch's 16-file state. StoredUsageAllowanceTest tests `StoredUsageAllowance`, which no longer exists
in main sources (the only remaining reference to `UsageAllowance` in the repo was this file), so it cannot compile.
Its two cases (contract cases via UsageAllowanceContractTest, and usageSurvivesARestartOfTheAllowance) belong to
removed behaviour. No assertion about surviving behaviour was removed. No new-only additions are present.

## Proof
tools/ci/verify-local.sh: EXIT 0. `:core:database:compileDebugAndroidTestKotlin`: success.
Final diff: 16 files, +4/-118; issue-268-test-replacement.patch overwritten with `git diff HEAD`.
