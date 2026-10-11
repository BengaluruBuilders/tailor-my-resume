# revamp/w2-all (D43)

Purpose: integration branch for the W2 lanes. Built from revamp/w1-all (9a1c4f56), then each W2 tip merged with merge --no-ff. All tips already contain w1-all.

Merged tips, in order:
1. revamp/264-paige-tokens (#304): 72f84dcd80dd7a98221d5bc2fe3c3285a2e3b86b
2. revamp/268-simplified-data (#305): b247a3f318a813a886deed52edfcb1d1640f1dc1
3. revamp/265-chrome-bars (#307): 0205f503308b46030d96b89e388319273d6547ce
4. revamp/266-hero-inputs (#306): 22622eea06d68904c9959db1e9148d8727b4277b
5. revamp/267-content-rows (#310): b007a8c767772ff9feb73b83b9ae3a68a81e127f
6. revamp/269-coverage-ui-domain (#309): a859ae29a307d2b33debd8b15f0bfd0d9e2bda5d
7. revamp/270-answer-guard-accept (#308): cce9ef30a7d3739f09cfe55929305271a693f692

Head: ffb0752e8505b33678fe04e6b616e832b2f667b9

Conflicts: only in the last merge (#308). KeywordsStatedIn.kt, offline/SkillLexicon.kt and KeywordsStatedInTest.kt (add/add or content) were resolved by taking #309's version (ours); #308 will re-copy it. TechLexicon.kt did not conflict. TmrChromeText (#307) and TmrFitText (#310) merged cleanly as separate files.

Gate: tools/ci/verify-local.sh green (BUILD SUCCESSFUL, exit 0). A fresh worktree needs local.properties (sdk.dir) copied from another checkout, or test-release-config.sh fails its bundleProdRelease dry-run check.

Pushed: origin/revamp/w2-all
Worktree: /Users/aibuilders/Documents/dev/products/hirehop-wt/w2-all

Refresh rule: merge new tips into revamp/w2-all with merge --no-ff; never rebase or force-push.

## Refresh 1

- Merged tips: 267-content-rows ceb37b4a (merge 919e176e), 269-coverage-ui-domain b41ab2c9 (merge d5db8b70), 270-answer-guard-accept 2599ce39 (merge 6881bcba).
- Already contained, no merge needed: 264 (72f84dcd), 265 (0205f503), 266 (22622eea), 268 (b247a3f3).
- Conflicts: none. TmrWholeWordText.kt (#310) and #307's WholeWordAutoSize kept as is (D47).
- tools/ci/verify-local.sh: green (BUILD SUCCESSFUL).
