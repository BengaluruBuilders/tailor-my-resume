# Emulator routing decision — 2026-10-17

**Recommendation: use a temporary Android system Wi-Fi proxy on the disposable validator emulator, backed by a laptop loopback-only HTTPS CONNECT relay.** Keep the APK, API URL, hostname, certificates and backend unchanged. This is a proposed environment correction, not an executed correction or independent integration PASS.

## Evidence and scope

Read [findings](01-guest-trial/findings.md), [runtime observations](01-guest-trial/independent-evidence/runtime-observations.json), the prepared candidate runtime handoff, and the exact candidate networking/build configuration. Fresh read-only hashing confirms the existing APK at `/tmp/tmr-earn-20261017-frontend/app/build/outputs/apk/staging/app-staging.apk` still has SHA256 `cd0082b09855dba860b73ad6e44bd9e0b39977ffd5cbd7ddc7dae5c9b918e4fa`. Frontend remains `13aa4fef53057036c52fa25aa475e7a1ab303d83`, paired with backend `69797ec759f3493b6d4f5d6912a82b1429e30fdb` and URL `https://abhishek-dubey-MS-7B16.local:20002/`.

Existing evidence establishes Google reachability, workstation IP reachability, failed emulator `.local` resolution even after the temporary hosts bind, and successful host-side HTTPS health200/auth401 using the original hostname/CA. This decision performed no device, server, provider or application-source mutation; only this report was written. The emulator was already stopped. The personal Pixel remains protected.

Android supports `.local` through multicast queries in its modular resolver, including Java InetAddress calls. This is not ordinary unicast DNS. Merely changing `-dns-server` is therefore not a demonstrated correction. Android's current emulator documentation also distinguishes emulator versions before/after36.5 and newer discovery support; do not claim every emulator universally lacks multicast. The evidence supports a multicast/routing or resolver-namespace problem in this particular runtime, not a proven permanent Android defect. A hosts file visible to `adb shell` alone does not prove the separate resolver process sees that mount. Resolving that would require additional process/mount inspection and possibly broader root mutations, without a demonstrated smaller fix. [AOSP resolver](https://source.android.com/docs/core/ota/modular-system/dns-resolver), [current emulator capabilities](https://developer.android.com/studio/run/emulator-networking), [network architecture](https://developer.android.com/studio/run/emulator-networking-address).

## Why this proxy can work with the unchanged APK

`core/network/.../TailorMyResumeHttp.kt` constructs OkHttp5.4.0 without a custom DNS resolver, explicit proxy, ProxySelector, trust manager or hostname verifier. Its default ProxySelector therefore follows Android's system proxy configuration. With an HTTP proxy selected, OkHttp resolves/connects to the proxy and sends the destination authority in CONNECT; the emulator need not resolve the `.local` destination. TLS remains between the app and existing Caddy. This is a source-backed expectation requiring actual runtime confirmation. [OkHttp5.4.0 client](https://raw.githubusercontent.com/square/okhttp/parent-5.4.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt), [route selection](https://raw.githubusercontent.com/square/okhttp/parent-5.4.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RouteSelector.kt), [Android proxy configuration](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/net/Proxy.java).

**No new emulator CA installation is indicated.** Both `staging` and `debug` include `app/src/localStaging/res`; the staging manifest selects `staging_network_security_config.xml`. That existing configuration trusts `@raw/staging_ca` for the exact lowercase hostname and keeps normal system trust for other domains. The checked public PEM at `/tmp/tmr-earn-20261017-frontend/app/src/localStaging/res/raw/staging_ca.pem` has SHA256 `1c1210c1c1083809792b88a824bb88e5426fe66ea0b9bb8516a6f79e16fe6848`. `frontend/docs/RELEASE.md` documents this same arrangement. If genuine app TLS fails, inspect the packaged resource and presented chain; do not install an interception CA, replace certificates, rebuild, disable verification, or add system trust as an expedient.

## Proposed setup for the independent validator

1. Reconfirm the exact identities above and the existing workstation connection before remote observation:

   ```bash
   ssh -o BatchMode=yes -o ConnectTimeout=10 ubuntu-workstation hostname
   getent ahostsv4 abhishek-dubey-MS-7B16.local
   ss -ltn 'sport = :18765'
   ```

   `18765` is a **proposed** task port, not an existing allocated/listening service. If occupied, leave its owner untouched and select/document another unused high port. Reverify the workstation's address if DHCP changed; do not amend unrelated DNS/SSH settings. No remote mutation is necessary.

2. Prepare/review a task-private relay outside application source; none was created by this decision. Bind **only `127.0.0.1:18765`** on the laptop. Android documents `10.0.2.2` as the alias for that host loopback. Relay policy: accept CONNECT only; bounded header size/deadline/concurrency/idle time; reject malformed authority, credentials and other methods; never parse/decrypt tunneled TLS. Allow only case-insensitive exact `abhishek-dubey-MS-7B16.local:20002`, `identitytoolkit.googleapis.com:443` and `securetoken.googleapis.com:443`. Resolve on the laptop using its normal resolver. For the candidate, require the resolved address to match the freshly verified workstation address before connecting; for the two Google destinations reject non-public addresses. Reject other targets. No wildcard/open forward proxy, TLS termination, packet capture, headers/bodies/URL logging or tokens. Sanitized counters/status and fixed allowlist labels suffice. Record its exact PID and remove only this owned process during cleanup. [Loopback alias](https://developer.android.com/studio/run/emulator-networking-address).

3. Before emulator use, validate that reviewed relay from the laptop, using the APK's existing public CA and original destination hostname:

   ```bash
   curl --silent --show-error --noproxy '' --proxy http://127.0.0.1:18765 --cacert /tmp/tmr-earn-20261017-frontend/app/src/localStaging/res/raw/staging_ca.pem --max-time 15 --output /dev/null --write-out '%{http_code}\n' https://abhishek-dubey-MS-7B16.local:20002/health
   ```

   Require200, no `-k`. This proves relay transport only. An unauthorized CONNECT target must be rejected without an outbound connection. Do not infer app contact from either check.

4. Restart only the validator-owned `TmrGuestValidator` runtime with its established private AVD paths and existing bounded launch settings in findings. Verify serial `emulator-5582` and ownership before every ADB/app action. Do not enable emulator `-http-proxy`, inherit a proxy environment setting, or use the original AVD/Pixel. In **this emulator's** Android Settings → Network & Internet → Internet/Wi-Fi → active network → edit/advanced proxy, record existing state, choose Manual, host `10.0.2.2`, port `18765`, and an initially empty bypass list. Confirm the effective proxy applies to its active network. Restart only the synthetic staging app process after setting it so its singleton client sees the setting; retain test userdata. The exact UI labels can vary. [Official system proxy workflow](https://developer.android.com/studio/run/emulator-networking-proxy).

   An empty bypass list deliberately keeps the candidate eligible for proxy selection. The two exact Firebase authorities can pass encrypted through the same relay if Firebase honors the setting; if its SDK ignores it, direct Google connectivity was already established. These are the documented anonymous signup and token refresh endpoints, not a claim that every SDK/background endpoint has been enumerated. If a necessary auxiliary host is blocked, inspect only sanitized authority/failure class before choosing a narrow additional allowlist or Wi-Fi bypass entry; do not broadly forward arbitrary targets. Never bypass `.local` or the candidate hostname. [Firebase Auth endpoints](https://firebase.google.com/docs/reference/rest/auth).

5. Replay real guest entry with the exact existing APK. Require sanitized evidence of the actual app's candidate call and backend response under normal TLS, correlated to the bounded replay. Relay CONNECT success alone proves neither Firebase authentication nor the authenticated request. Capture no tokens, Firebase identifiers, personal screens or full raw logcat. Distinguish proxy resolution failure, TLS failure, Firebase failure and candidate policy response. A manual curl401 is not app provenance.

The **Android system proxy** is the chosen mechanism. The separately named **Emulator Proxy** (`-http-proxy`) routes lower-level TCP traffic, has no per-host bypass, and does not establish that guest `.local` DNS succeeds; do not substitute it silently. Neither mechanism needs HTTPS interception for this use. [Official distinction](https://developer.android.com/studio/run/emulator-networking-proxy).

## Reversal and remaining acceptance boundary

Restore the disposable emulator's recorded Wi-Fi proxy state, close the validator's agent-device session, stop only emulator5582 and the recorded relay PID, and verify the owned listener is gone. Restore/remove only validator-owned temporary hosts mounts/files if still present, or discard that task runtime. Preserve original AVDs/snapshots, the Pixel, candidate API20000/Postgres20001/Caddy20002 and existing staging8080/8443. No laptop resolver, firewall, LAN listener, CA store or workstation service change is proposed.

The parent reports staging anonymous auth has since been enabled through a separate guarded one-field correction; independent signup and real app replay are still pending. The earlier `ADMIN_ONLY_OPERATION` finding remains historical until those affected checks are repeated. Genuine Integrity configuration, Play verdict and published admission policy remain absent; this routing decision does not enable awards or approve genuine grants. A successful replay may establish authenticated app→candidate contact and closed-admission behavior only. It cannot establish full guest-trial acceptance, release readiness, live grants/spending, or real provider deletion.
