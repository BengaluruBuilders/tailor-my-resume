# Slice01 independent post-publication validation — 2026-10-17

**Verdict: attainable published-head/code/evidence/CI and genuine closed-admission smoke PASS. Full guest-trial acceptance remains BLOCKED by GT01-B003.** No actionable source defect or unexplained omitted attainable gate was found. Both PRs remain open drafts, unmerged. This supports the existing draft/next-coding exception; it is not a ready/merge/release or complete-slice verdict.

## Published identities and CI

| Repository | PR | Actual base | Actual head | State/checks |
|---|---|---|---|---|
| Frontend | [406](https://github.com/BengaluruBuilders/tailor-my-resume/pull/406) | `main`, `6d73a22dc392451f27fc2999eb788f7b048880f0` | `037d3ee88765d9f22d2199396d79996e155ee97a` | OPEN/draft/unmerged;11 reported checks SUCCESS |
| Backend | [124](https://github.com/BengaluruBuilders/apps-backend/pull/124) | `main`, `3abd46cafb45abaee4f0806f0efa32a02f992375` | `69797ec759f3493b6d4f5d6912a82b1429e30fdb` | OPEN/draft/unmerged; Validation and Secret scan SUCCESS |

Frontend code/APK remains checked `13aa4fef53057036c52fa25aa475e7a1ab303d83`; both following commits change22paths only under `docs/`, including the justified staging-provider setup correction in RELEASE. No application/build input changed. Frozen contract SHA256 remains `b76d2202bf1e76f25dfec6191e2b3e2d04cb9dbb419deccfa33979444071a2a6`. APK SHA256 remains `cd0082b09855dba860b73ad6e44bd9e0b39977ffd5cbd7ddc7dae5c9b918e4fa`, package `com.tailormyresume.app.staging`, staging variant. Required local gates and61-case independent/restore results are reused; no needless full rerun. [Frontend CI snapshot](post-publication-evidence/frontend-pr.json), [backend snapshot](post-publication-evidence/backend-pr.json).

PR descriptions correctly cross-link the pair, partial issue scope, Constitution/HardwareIds rationale and immutable evidence. Authenticated GitHub reads of published README/manifest matched local bytes; all26 evidence-document blobs in the published tree matched their local Git blob IDs;9 original manifest hashes matched; README/findings relative links had no missing files. Images were visually inspected. The validation fixture is byte-preserved as `.mjs.txt`, never executable product code. Earlier failed signup images are clearly historical. This verifies repository-readable assets and link targets; it does not claim a separate browser screenshot of GitHub's video player. New assets below await an evidence-only publisher commit and immutable link update; after that, audit final docs/head mapping without repeating unchanged app checks.

## Genuine affected runtime replay

Same backend69797ec serves the private candidate; same APK uses **`https://abhishek-dubey-MS-7B16.local:20002/`**, Firebase **`tailormy-resume-staging`**, namespace **`local-fixture`**, **no published policy/admissions closed**. Device: owned `TmrGuestValidator`, emulator5582, Android16/API36 Google APIs x86_64; task-only SDK-generated userdata, installed agent-device0.21.28/ADB. Pixel and original AVD were not operated.

1. The separately executed staging-only `anonymous.enabled=true` correction was not assumed to fix the app. Actual app Try as guest now successfully creates/resumes a genuine Firebase anonymous session and reaches the protected candidate. At05:59:11Z the relay records the app's candidate TLS tunnel; the candidate creates its first user at05:59:11.800Z, between baseline0users at05:58:14.307Z and after observation1user. The real SDK and candidate Firebase verifier remain unchanged; no fake auth/verdict HTTP flag exists.
2. App displays the true **trial unavailable** message with Retry and Continue as guest, rather than historical sign-in failure. Continue reaches Upload. Only a synthetic manual name was entered, avoiding parsing, model calls and real resume content. A stale accessibility ref prevented the optional synthetic email fill; no email was supplied. These tool-control errors were corrected without source change and are not app findings.
3. Relaunching only the synthetic app process preserves the guest/profile, lands in Applications with **0 credits**, and opens Credits with empty history. “Check guest trial” produces “The guest trial is temporarily unavailable. Your existing balance and work remain available.” No Buy/claim/reward is offered in this state. Final06:04:44.139Z observation remains1user and **0policy/0grant/0wallet/0ledger rows**. Read-only wallet uses the candidate's authoritative zero default without creating a wallet/grant row.

Combined exact APK/endpoint identity, normal hostname/CA TLS, allowlisted app tunnel, candidate user creation timestamp, actual policy/Continue/zero/recheck UI and unchanged verifier/service composition establish **actual authenticated app→this candidate contact**, rather than merely a curl health response. Tunnel metadata alone is not authentication proof. No token, UID, canonical digest, HTTP headers/bodies or private screen was logged/published. No real grant, paid AI, successful tailoring or cloud deletion occurred; the intentionally disposable staging identity remains for later test cleanup under its own authorization.

## Routing setup and reversal

Read the final [routing decision](../emulator-routing-decision.md). Fresh host resolution and remote address both returned192.168.88.15. The relay binds **only127.0.0.1:18765**; exact case-insensitive CONNECT allowlist is candidate:20002, identitytoolkit.googleapis.com:443 and securetoken.googleapis.com:443. It rejects other targets before connecting, checks candidate's resolved address and Google's public addresses, bounds headers/deadlines/concurrency/idle/lifetime, and forwards encrypted TLS bytes without termination. [Reviewed fixture as text](post-publication-evidence/connect-relay.py.txt).

Host preflight with the **existing APK CA**, original hostname and no TLS bypass returns200; an unauthorized CONNECT target returns403. [Preflight](post-publication-evidence/relay-preflight.log). No CA, certificate, APK, backend or laptop resolver change was made. Implementation used a disposable-emulator **Android global system-proxy setting** (`http_proxy=10.0.2.2:18765`, empty effective exclusion list), rather than editing per-SSID Wi-Fi UI; it exercises the same default ProxySelector. It was not emulator `-http-proxy`. The initial empty-string shell command reported Bad arguments; effective empty setting and actual candidate selection were verified, rather than guessing bypass syntax. No additional authority or wildcard was added. Firebase auth succeeds with no Firebase CONNECT logged; that observation does not claim its SDK honors the proxy. Unsupported/background traffic is rejected, never decrypted; only fixed labels/counts were retained. [Summary](post-publication-evidence/relay-summary.json).

After capture, both proxy keys were deleted and read back **null/null**, restoring their observed initial state. Session `tmr-guest-postpr` closed. Owned emulator command session72218 exited0; relay session27821 stopped by its verified own PID, exit143;18765 listener is gone. Original AVD/Pixel and workstation services/policy remained untouched. This resolves B002 only for bounded test transport; inherent emulator `.local` multicast/namespace behavior is still unproven. No memory/OOM failure occurred; no other local heavy job ran.

## Stable finding disposition and boundary

- **GT01-B001 RESOLVED:** genuine actual-app anonymous signup/protected candidate contact observed after the staging-only provider correction. Historical400 ADMIN_ONLY_OPERATION and its assets remain intact, with current captions.
- **GT01-B002 TEST TRANSPORT RESOLVED:** actual app normal-TLS candidate contact succeeds via the verified reversible system proxy; direct emulator multicast resolution is not claimed fixed.
- **GT01-B003 BLOCKED, unchanged:** APK has no Integrity cloud number; no serving policy/admission publication or genuine matching Play package/certificate/version/licensing/Standard verdict evidence exists. No real+2grant→two debits journey, real platform abuse/reset continuity, or provider deletion PASS is claimed. Required rollout policy/capacity/cutoff/disclosure inputs remain separate activation gates.

Satisfied now: real anonymous identity→protected candidate unavailable-policy→Continue, server-owned0wallet/history, guest recheck and process resume. Still unproven: real admitted reward/spend, genuine Integrity and full platform acceptance. Source/security/fixture-spend/restore evidence remains valid and clearly separate. No fixer is needed for this verdict. Next: publisher commits only updated docs/evidence, refreshes PR immutable links/status, then validator audits final heads/mapping; retain draft and cumulative B003.

## New captions and integrity

[Manifest](post-publication-evidence/manifest.json) pins the pair/APK/contract/device/environment and all15new artifacts. All visuals are genuine exact staging APK, paired backend69797ec, **no policy**, owned emulator5582:

- [07 video](post-publication-evidence/07-guest-candidate-replay.mp4): real anonymous entry reaches candidate's unavailable offer and Continue. Android encoded7.6seconds during10.4second recording; no grant shown.
- [08](post-publication-evidence/08-guest-candidate-state.png): candidate trial unavailable after successful authentication; distinct from historical signup failure.
- [09](post-publication-evidence/09-guest-continued.png): Continue reaches safe upload/manual onboarding.
- [11](post-publication-evidence/11-guest-process-resume.png): restarted process resumes guest/synthetic profile and0credit home.
- [12](post-publication-evidence/12-authoritative-zero-credits.png): authoritative0balance, empty history and guest-trial recheck; no purchase claim.
- [13](post-publication-evidence/13-guest-trial-recheck.png): completed recheck remains unavailable and explicitly preserves balance/work.

[Before](post-publication-evidence/candidate-before.json), [after](post-publication-evidence/candidate-after.json), [final](post-publication-evidence/candidate-final.json), and [sanitized observations](post-publication-evidence/runtime-observations.json) supply timestamp/count correlation, identities and cleanup. Only synthetic UI is included. Two earlier selector-error diagnostic captures were not published or represented as successful policy evidence.
