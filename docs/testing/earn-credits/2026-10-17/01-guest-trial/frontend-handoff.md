# Slice 01 frontend candidate handoff

Status: exact fully tracked candidate committed/pushed; full required gates PASS. Final staging artifact built and signature verified; independent runtime validation remains pending.

Worktree: `/tmp/tmr-earn-20261017-frontend`; branch `codex/earn-01-guest-trial`.
Frozen contract: `3c9ff77f82fc7a1c9a394c8431bc07a40f1af8df`, contract SHA256
`b76d2202bf1e76f25dfec6191e2b3e2d04cb9dbb419deccfa33979444071a2a6`.

## Resulting behavior

- Guest entry resumes/creates Firebase anonymous identity, checks the live server account and reads policy without reading Android ID or requesting Integrity evidence. The local session is published only after explicit Continue, with UID/session-generation checks. Existing guest work stays accessible when trial admission is unavailable; Credits exposes a later retry.
- An explicit displayed offer and purpose/retention disclosure precede claim. The claim persists one operation key and business digest per UID/backend URL; only current server policy supplies the amount. Stale policy/expired evidence requires refresh and a fresh explicit claim; rate recovery honors Retry-After.
- The real installed Play Integrity 1.3.0 Standard API receives the exact server requestHash. Raw Android ID is read on IO only for an explicit claim, validated, sent only in initialize and not persisted/logged. Missing cloud project configuration closes verification; no debug/emulator trust fallback exists.
- Policy/challenge/initialize DTOs and typed recovery errors match guest-trial-v1; initialize does not echo the challenge secret. Stable credit IDs and actual GUEST_TRIAL amounts survive mapping and receive a distinct history label.
- Wallet/ledger startup works for guests independently of Play purchase restoration. New guest purchases and destructive generic Google switching/sign-out are suppressed; existing balances, history and normal resume/profile/job/tailoring/recovery remain available. Guest manual/imported contact email can be corrected without changing authentication.
- Scope excludes Google linking/signup rewards, referrals, production activation and deployment. No copy promises later rewards.

## Exact candidate and checks

Candidate: `13aa4fef53057036c52fa25aa475e7a1ab303d83`, GitHub feature ref
`refs/heads/codex/earn-01-guest-trial`, explicit non-force push and advertised SHA verified.
All 110 owned changed paths are in `frontend-changed-files.txt`; the final source hash manifest is
`tmr-guest-final-tracked-source-manifest.json`. Contract files/fixtures remain at the frozen commit.
Preexisting untracked planning inputs were preserved outside this commit.

The final actual unchanged `tools/ci/verify-local.sh` passed on the fully tracked candidate:
`/tmp/tmr-guest-verify-local-final-tracked.log`, Gradle 4m44, exit 0, minimum available
5799 MiB, maximum full PSI avg10 0.32, no pressure cancellation. It ran constitution policy,
secret-scan/configuration/workflow regression checks, convention checks, Spotless, unit tests,
domain/data coverage verification, Roborazzi verification, release lint, Dependency Guard and
debug/release assembly with warnings as errors. Cached/up-to-date unchanged tasks are reported
honestly in the complete log. The intact pre-push constitution hook also passed.

Selected complete module XML counts (failures/errors/skips all zero):

| Module | Tests |
|---|---:|
| App | 457 |
| Core model | 7 |
| Core data | 177 |
| Core domain | 783 |
| Onboarding | 201 |
| Profile | 169 |
| Settings | 125 |
| Tailor | 250 |

Focused scoped repairs passed 20/20 before the comment-only tracked-policy correction:
12 guest gateway owner/retry/recovery cases, four Google purchase restoration/settlement cases,
one guest-no-billing case, one sensitive-source scan and two quantity-resource cases (0/1/2).
The final full run then revalidated the tracked candidate. Native screenshot checks cover guest
entry/disclosure, unavailable recovery, enlarged text, Credits, Settings and zero-credit recovery;
existing entry baselines intentionally include the new guest action.

Full raw logs remain in `/tmp`; compressed byte-preserving copies and memory CSVs are alongside
this handoff. `frontend-check-log-index.json` records their raw SHA256 hashes. The failed and
incomplete runs below remain preserved; none is relabeled PASS.

## Runtime

Execution-only overrides are supplied through GRADLE_OPTS to the actual current
`tools/ci/verify-local.sh`, including its nested Gradle configuration checks. Gradle heap 2 GiB,
Kotlin heap 1 GiB, one worker, parallel/tooling/configuration parallel flags false. Repository defaults
remain unchanged, including workers=3, enabled caches and isolated projects. Gradle 9.8 isolation
still reports internal parallel model flags; actual one-worker leases and heap caps are verified.
A task-local monitor records MemAvailable/swap/memory PSI every 10 seconds and cancels the owned
client on sustained low headroom or memory PSI. It changes no required task or warning/error gate.

The narrow constitution I.5/ledger amendment describes the authorized direct Integrity purpose and
existing SDK reuse. It does not certify privacy/Terms/Data Safety or real verifier approval. The actual lint `HardwareIds` finding is suppressed only on the continuity accessor, with the
official API link and explicit-purpose rationale. No lint baseline is added or expanded.

## First full run and repairs

The first actual required script exited 1 after 20m33s. Four tasks failed: app unit tests
(source safety scan and incomplete PurchaseRestorer fixture); app lint (HardwareIds);
onboarding lint (PluralsCandidate); settings lint (PluralsCandidate and ModifierParameter).
The stuck owned test worker was terminated after a preserved thread dump established the
fixture waited forever for a billing listener without a Google session. This run is never a pass.

Artifacts: `/tmp/tmr-guest-verify-local-capped.log`, corresponding `.memory.csv`,
`/tmp/tmr-guest-app-test-thread-dump.txt`. Minimum observed available memory 4130 MiB;
no memory-pressure cancellation. Its owned Gradle/Kotlin daemons were released afterward.

Scoped repairs preserve the safety scan, Google unsolicited settlement assertion and gates.
The fixture now explicitly seeds Google, consumes startup wallet refresh and bounds its listener
wait. A guest regression proves neither billing nor payments is constructed. Ownership errors
remain generic and cancellation remains distinct. Actual plural resources cover 0, 1 and 2 in
both entry and Credits; optional modifier order is corrected. No baseline growth.

## Tracked-file policy correction

The repaired full script passed (`/tmp/tmr-guest-verify-local-repaired.log`, Gradle 7m04,
minimum available 6140 MiB, no pressure cancellation). After the selective candidate commit,
the unchanged pre-push hook found the new accessor comment's citation was on a separate line.
The policy uses git grep and had not scanned that untracked source before commit. The only
subsequent change combines that purpose and official API URL on one comment line. No executable
code, assertion, baseline or gate changed. Global tracked-source constitution policy then passed.

The unpublished candidate was amended to `13aa4fef53057036c52fa25aa475e7a1ab303d83`.
The final tracked-candidate full script is `/tmp/tmr-guest-verify-local-final-tracked.log`.
The staging attempt started immediately before the pre-push finding was cancelled as incomplete:
`/tmp/tmr-guest-staging-candidate.log` exit137 (owned client cancellation, no memory pressure).
It is not validation evidence or a deliverable APK.

## Final staging artifact

`assembleStaging` and its actual `verifyStagingBackendConfig` dependency passed, warnings as errors,
Gradle 3m24: `/tmp/tmr-guest-staging-final.log`, exit 0, minimum available 5814 MiB,
no pressure cancellation. Compressed raw log and memory CSV are alongside this handoff.

- Source SHA: `13aa4fef53057036c52fa25aa475e7a1ab303d83`.
- APK: `/tmp/tmr-earn-20261017-frontend/app/build/outputs/apk/staging/app-staging.apk` (5,936,850 bytes).
- APK SHA256: `cd0082b09855dba860b73ad6e44bd9e0b39977ffd5cbd7ddc7dae5c9b918e4fa`.
- Package: `com.tailormyresume.app.staging`; Firebase project: `tailormy-resume-staging`.
- API override: `https://abhishek-dubey-MS-7B16.local:20002/`; parent-owned candidate backend `69797ec`.
- Existing local debug signing configuration; APK Signature Scheme v2 verified. Certificate SHA256:
  `cd42a070ed44e41b40e5d8c8b24b26c0df70a7e97f3aa33da405b38984ce549c`.
- Integrity cloud project number is absent in this build. Genuine Standard admission is BLOCKED;
  missing configuration fails closed through the neutral retry/continue UI. No trust bypass is present.

`tmr-guest-staging-artifact.json` and `tmr-guest-staging-apksigner.txt` preserve provenance.
No device install, app-data reset, user phone operation, PR, merge or deployment was performed.

## Reproduction and independent validation

Use the published feature ref and verify the exact SHA above. The wrapper copied alongside this
handoff (`tmr-guest-capped-run.py`) supplies execution-only caps and monitoring to each nested
Gradle invocation; repository workers=3 and isolation remain unchanged. Run serially on the chosen
host, after checking headroom and existing processes:

```bash
python3 /tmp/tmr-guest-capped-run.py /tmp/guest-verify.log timeout --signal=INT --kill-after=60s 45m tools/ci/verify-local.sh
python3 /tmp/tmr-guest-capped-run.py /tmp/guest-staging.log timeout --signal=INT --kill-after=60s 30m ./gradlew :app:assembleStaging -PtailormyresumeStagingApiBaseUrl=https://abhishek-dubey-MS-7B16.local:20002/ -PwarningsAsErrors=true
```

The independent validator should reuse the passing exact-source gate evidence. Before any
approved data-preserving install, compare the installed package's signer with the fingerprint above;
never uninstall or clear app data to bypass a mismatch. Exercise Try as guest, live offer/disclosure,
neutral unavailable retry/Continue, resumed guest work, authoritative Credits/history, editable guest
contact email and normal tailoring/failure/recovery. Real claim/replay/owner-switch verification needs
genuine platform configuration and explicit action; synthetic gateway/screenshot fixtures do not
establish a genuine verdict or successful live promotion.

## Remaining gates and stop point

Real Firebase anonymous-provider registration, genuine Play Integrity verdicts, matching project/
package/certificate/version/licensing and backend admission/capacity activation have not been proven
by this frontend task. The APK's missing cloud number alone blocks genuine admission. Privacy,
Terms, retention/Data Safety and production campaign approval remain external activation gates;
this narrow constitution amendment does not certify them. No signup bonus, anonymous-to-Google
linking or referral reward is implemented/promised in this slice. Guest purchase/sign-out/unsafe
Google-switch suppression remains until the later authorized account-linking slice.

Candidate source is frozen. All task-owned idle Gradle/Kotlin daemons were released after the final
build. Local heavy slot is released; frontend implementer stops at candidate handoff and remains
idle during independent validation. No next slice has started.
