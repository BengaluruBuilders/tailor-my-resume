# W1 stacking base: revamp/w1-base

Based on origin/main e387b855. Merge commits, no rebase or squash.

| Order | PR | Branch | Head SHA |
|---|---|---|---|
| 1 | #295 | revamp/257-tailor-extras | 0bf9d9e1736762a10348be828add2877fecc0ea5 |
| 2 | #294 | revamp/258-onboarding-profile-cuts | b4c478527b8caf11e49c36d51348eb37f1b44423 |
| 3 | #299 | revamp/259-analysis-apps-settings-cuts | de3b9e701de1aa3685b0fdb7084c7cc97b6fca0e |

Resulting head: 98da8f2ac846c30bf25760e511569ab043c642dd (no conflicts).

Gate: tools/ci/verify-local.sh passed (BUILD SUCCESSFUL). The first run failed only because the new worktree had no local.properties (SDK location); it was copied from the main checkout and the rerun was green.

Branches based on revamp/w1-base rebase onto main once #295, #294 and #299 merge.
