# Issue 90 lead test replacement

Apply on top of the issue-90 head (`git apply issue-90-test-replacement.patch`). Full `npm run check` is green with it (549 tests, 0 failing) on a scratch worktree at the head.

New behaviour is covered by `test/tailormyresume-spend-per-tailoring.test.ts` in the PR (13 cases). The cases below encode removed behaviour (unlock spending, the free daily tailoring and its claim, section regeneration, daily analyses 3). No assertion was loosened except where it was about that behaviour.

## test/tailormyresume-ai-harness.ts (helper, not a case)
- default `dailyCallQuota` 6 to 25: analyses now allow 20 a day, so the core call quota must exceed 20 for `ALLOWANCE_EXHAUSTED` to be reachable before `QUOTA_EXCEEDED`.

## test/tailormyresume-analyses.test.ts (daily analyses 3 to 20)
- scripted mock answers 8 to 25 (more calls per user now).
- "an analysis returns server ids...": `analysesLeftToday` 2 to 19.
- "three distinct JDs pass, the fourth is 429" now "twenty distinct JDs pass, the 21st is 429".
- "a repeat of the first JD is 200 and spends another analysis": remaining 0 to 17.
- "a provider failure does not use an analysis": remaining 2 to 19.
- "three concurrent different JDs with two left": pre-spends 18 analyses so two are left; assertion unchanged.

## test/tailormyresume-analyses-repeat.test.ts (daily analyses 3 to 20)
- scripted answers 8 to 25.
- "ten posts of one JD..." now 25 posts: 20 calls, 20 ai_usage rows, five 429.
- "the same JD as the first after three analyses" now after twenty.
- "...wallet shows three analyses left" now twenty.
- "three distinct JDs each make one model call and the fourth is 429" now twenty and 21st.
- "another user analysing the same JD", "crosses IST midnight": remaining 2 to 19 and 3 to 20.
- "repeating one JD until refused...": unchanged; passes because of the higher harness quota.

## test/tailormyresume-wallet.test.ts
- "the first wallet call creates one free credit": `analysesLeftToday` 3 to 20, `freeTailoringsLeftToday` 1 to 0.
- "an unlock answers 201 then 200..." now "answers 200 every time, spends nothing, returns a FREE unlock".
- "a free credit is spent before a purchased credit" now "an unlock spends no free or purchased credit".
- "an unlock with no credit is 402" now "answers 200 and writes nothing".
- Concurrent unlock cases: statuses become [200, 200], balance unchanged.
- "another user's unlocks never show" and "DELETE /v1/me erases the wallet tables": unlock rows are inserted directly (legacy rows), because unlock no longer writes one; the assertions are unchanged.
- Claim-based `analysesLeftToday` expectations 2 to 19 and 3 to 20 (limit 20).

## test/tailormyresume-ledger.test.ts
- "credits equals free plus purchased...": after unlock credits stay 6 and `unlockedApplicationIds` is empty.
- "unlock writes one TAILORING -1 row..." now "unlock spends nothing and writes no ledger row" (only WELCOME, balance 1).
- "a first request that is an unlock lists WELCOME before TAILORING" now lists only WELCOME (status 200).
- "an unlock with no credit left answers 402" now "a second unlock answers 200 and writes no row".

## test/tailormyresume-reports-export.test.ts
- "the export holds only the caller's ...": after the unlock call `freeCredits` is 1, `unlockedApplicationIds` and `unlocks` are empty.

## test/tailormyresume-tailorings.test.ts
Deleted (behaviour removed; replacements are in the new test file):
- a section regeneration drops a bullet for an entry outside the section
- a free full tailoring marks its claim USED on success, a failure releases it
- a free tailoring finishing after TailorMyResume midnight marks the original day's claim USED
- a section regeneration after a free full tailoring starts without a new claim ...
- a section regeneration with a free tailoring left today is 402 and makes no claim
- a section regeneration is not entitled by an earlier-day USED claim or by any PENDING claim
- an unlocked application tailors with zero credits and makes no claim
- interruption reconciliation rolls back the job update if claim release fails
- a section regeneration without unlock, credit or a same-day USED claim is 402 ...
- a section regeneration right after a same-day free full tailoring is 202 ...
- three section regenerations with today's free allowance unused are each 402 ...
- a USED free tailoring claim of an earlier day no longer entitles the application ...
- a section regeneration by another user of an application with a USED claim is 402 ...
- a section regeneration by an account that still holds its starting credit runs free ... (known hole, see #72)
- a section regeneration just after TailorMyResume midnight is 402 ...

Adapted:
- "the core quota used up during the job": seeds 2 credits so the second run is not refused for credit first.
- "402 NO_CREDIT without unlock...": now zero balance after one spent credit.
- "two concurrent full tailorings on the free path" and "two full tailorings of the same application with different requestIds": one credit, outcomes [202, 402] (the second request no longer gets the running job as 200).
- "a second full tailoring ... RUNNING on a free claim answers 200": now 402 NO_CREDIT while the first holds the only credit.
- "a second full tailoring while the found RUNNING job belongs to another boot" and "a RUNNING job of another boot or older than five minutes": seed rows with `creditReserved` instead of claims; claim assertions replaced by a wallet assertion or removed.
- Unused helpers and imports removed.

shutdown test: passes unchanged.
