# Slice04 request provenance decision

2026-10-17. Narrow recommendation to the single independent validator; this document is not an acceptance verdict. No runtime operation, UI interaction, endpoint request, database query, test, source fix, or service/configuration change was performed by this decision agent.

## Recommended action and boundary

Use the existing guest session's **Refresh referral status** action in Invite friends as the bounded non-granting read. Do not use Google, Continue, Apply, Issue, a purchase, wallet creation, or a synthetic HTTP client to establish app contact. Keep acquisition/signup/referral/link admission closed. One timed tap with the existing candidate and relay is sufficient when the independent observation is positive; do not repeat validation merely to improve the appearance of evidence.

At exact frontend `a86e8e3ab7354acab6cb5c78309adb09f91c3796`, the source chain is:

- `feature/settings/impl/.../referrals/ReferralsScreen.kt`: the visible refresh text button calls `actions.refresh`; entering the route and ON_RESUME also refresh, so settle the screen before the measured tap.
- `ReferralsViewModel.kt`: `refresh()` schedules `referrals.refresh()` and rejects overlapping view-model jobs.
- `app/.../referrals/RemoteReferralGateway.kt`: with an existing Firebase user, `refresh()` enters the serialized owner-checked read and calls `api.referrals()`. Without a current Firebase user it returns local inbox state, so preauth display alone proves no network read.
- `core/network/.../TailorMyResumeApi.kt`: `referrals()` is `GET v1/tailormyresume/referrals/me`. The anonymous path does not run the subsequent signed-in wallet/credits refresh. The refresh entry does not call code issuance, attachment, a challenge, Google linking, or Integrity collection. It can reconcile local journal state from a response; it is not a promise of zero local bookkeeping.

The unchanged backend harness `test/guest-trial-local-server.ts` serves `app.fetch` on loopback20000 and emits startup/runtime events, not per-HTTP-request access logs. Empty startup logs therefore cannot establish that no request happened. The established Caddy20002 → candidate loopback20000 mapping must remain independently tied to exact backend `a67d2ad56330c5522235bd4e1ae526b0cf5e3412` and its owned process, rather than inferred from a health response or packaged URL.

## Correlation method

Preserve one short UTC action interval and join the following existing evidence:

1. Installed exact APK/package, running app PID/UID, current anonymous session, and the actual visible refresh action/time. Where available, associate the app's proxy socket inode/tuple with that UID. Installed UID alone does not prove ownership of a particular socket.
2. The scoped relay's candidate destination and tunnel lifetime, including encrypted byte counts and timestamps. A pooled tunnel can carry the refresh without a new CONNECT. A CONNECT or TLS handshake alone proves neither an HTTP request nor successful app response.
3. The unchanged trusted Caddy20002 upstream mapping and current candidate PID/cwd/SHA/listener identity. Record exact socket lifecycle before attributing an fd: file descriptors are reused.
4. Candidate-side positive receive/transmit byte metadata during the UI interval on an accepted socket from the known listener, or a positively identified already-open upstream socket. Cover existing sockets and all process threads; accept-only polling and a brief `ss` snapshot can miss a short request or pooled reuse. Exclude job/database/internal-fd traffic.

No payload, header, token, body, TLS termination, packet content, auth changes or client trust override is needed. Do not execute privileged tracing from this decision. A separately root-coordinated observer must suppress strings and vectors before attachment and be bounded/detached; preserve its actual command and exit. Prefer the already completed independent observation below over adding any new observer.

## Independent observation now available

The validator reported its existing root-coordinated bounded observer, and this agent read `independent-evidence/candidate-thread-syscall-metadata.log`. That log records process190027 with11 threads attached, then:

- `19:58:14.194677 accept4(21, NULL, NULL, SOCK_CLOEXEC|SOCK_NONBLOCK) = 22`;
- `19:58:14.195091 read(22, ""..., 65536) = 1170`;
- `19:58:14.209842 write(22, ""..., 520) = 520`;
- `19:58:20.216033 close(22) = 0`.

Strings/vectors are suppressed in the preserved log. Later fd22 traffic after19:58:29 is a reused descriptor and must not be counted as the earlier candidate HTTP connection. The trace also contains unrelated internal/database byte metadata; that is not app-request evidence.

The validator separately reports: fd21 independently mapped to PID190027's127.0.0.1:20000 listener; candidate cwd/head/source verified; actual emulator5582 app UID10216/PID3762 running the fixture APK; proxy10.0.2.2:18765; existing candidate CONNECT at14:27:23UTC within its90-second idle lifetime; visible Refresh referral status at14:28:14UTC, matching19:58:14 at UTC+05:30; no curl/preflight in that interval; observers detached after timeout(exit124), service preserved. These contextual facts are validator-owned reports, not reexecuted checks by this agent. The validator should preserve their underlying records and explicitly state whether a UID-to-socket tuple was observed.

This positive accept/read/write interval resolves the earlier absence from a short socket watcher or an observer that did not follow threads; that absence was inconclusive. Taken with the destination/process/action chain, it supports a temporal correlation that the actual app contacted the exact candidate. Metadata does **not** independently decode the HTTP path/status, establish authenticated response success, or prove that an unavailable rendered state came from a fresh successful response. Source supplies the expected GET path; the validator owns the final strength of that inference.

Subsequent validator read-only corroboration: emulator `ss -tnpe` returned an established `10.0.2.16:46056 → 10.0.2.2:18765` socket, UID10216, inode66084 (alongside a netlink permission warning). This ties a proxy socket to the package UID, but was collected after the refresh and is not an atomic socket observation at19:58:14. The actual scoped Caddyfile section was also read and contains the exact HTTPS20002 host with internal TLS and `reverse_proxy 127.0.0.1:20000`. Preserve each observation's separate timestamp; do not upgrade this later corroboration into a simultaneous end-to-end tuple trace.

## Escalation only if a stronger claim is required

No harness logging or runtime composition change is recommended now. If acceptance specifically requires decoded request method/path/status rather than candidate contact, record that gap. The minimal future composition would wrap the existing harness `fetch` delegate with an allowlisted constant route label and response-status/timestamp only, excluding all headers, query strings, identities and bodies, and await/return the unchanged original fetch result. It would require an explicitly coordinated harness-only candidate restart, preserved baseline/rollback and renewed runtime identity evidence; do not silently relabel that runtime as the unchanged candidate or add source middleware/logging to the app. This decision does not authorize or execute that alternative.

All external gates remain unchanged: real Google/Integrity/reward/spend, owned domain/static association, genuine installed App Links/default resolver and Play attribution remain outside synthetic VIEW routing and this provenance method. The fixture APK remains disposable-emulator-only; no fixture phone install or public/live activation follows from these observations.
