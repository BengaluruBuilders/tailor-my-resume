# Release

Release builds are signed with the Play upload key. The key is never in the repo.

## Owner setup (once)

1. Create the upload keystore on your machine and keep a backup:

   ```
   keytool -genkeypair -v -keystore tmr-upload.jks -alias tmr-upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Create the `release` environment (Settings, Environments, New environment):
   - Deployment branches: selected branches, `main` only.
   - Required reviewers: yourself.
   - Add every value below as an environment secret of `release`, not a repository secret:
     - `TMR_UPLOAD_KEYSTORE_BASE64`: output of `base64 -i tmr-upload.jks`
     - `TMR_UPLOAD_STORE_PASSWORD`
     - `TMR_UPLOAD_KEY_ALIAS`: `tmr-upload` if you used the command above
     - `TMR_UPLOAD_KEY_PASSWORD`
     - `TMR_WEB_CLIENT_ID`: web OAuth client ID, Google Cloud console, APIs and Services, Credentials
     - `TMR_FIREBASE_API_KEY`, `TMR_FIREBASE_APP_ID`, `TMR_FIREBASE_PROJECT_ID`: Firebase console, Project settings, General
   - Optional environment variables: `TMR_VERSION_NAME` (default `0.1.0`) and `TMR_VERSION_CODE_OFFSET` (digits only, default `0`).

The prod bundle fails if any of the four Firebase and web-client values is blank, because sign-in would be dead.
Gradle property names (the CI secrets above map to them): `tailormyresumeWebClientId`, `tailormyresumeFirebaseApiKey`,
`tailormyresumeFirebaseAppId`, `tailormyresumeFirebaseProjectId`. The prod API URL is fixed to `https://apps-backend.fly.dev`.

## Staging

`debug` and `staging` use the staging backend and the staging Firebase project, app id `com.tailormyresume.app.staging`
(register that Android app in the staging Firebase project). They read five Gradle properties (environment is not read):
`tailormyresumeStagingApiBaseUrl`, `tailormyresumeStagingWebClientId`, `tailormyresumeStagingFirebaseApiKey`,
`tailormyresumeStagingFirebaseAppId`, `tailormyresumeStagingFirebaseProjectId`. A blank value compiles for `debug`, but
every `staging` task (`assembleStaging`, `installStaging`, `bundleStaging`) fails until all five are set. `staging` is
minified like `release` and is always signed with the debug key, so `debug` and `staging` installs replace each other.
It is never published (Constitution V.7).

### Ubuntu server on the same Wi-Fi

The local staging endpoint is `https://abhishek-dubey-ms-7b16.local:8443/`.
Set `tailormyresumeStagingApiBaseUrl` to this URL in `~/.gradle/gradle.properties` and
build `:app:assembleStaging`. The other four staging Firebase properties are still required.
`https://192.168.88.15:8443/` is an alternative when a phone cannot resolve `.local`;
that IP can change with DHCP. Prefer the hostname or reserve the IP on the router.

The `debug` and `staging` source sets share `app/src/localStaging/res`. Their manifests
select a network security configuration that trusts the Ubuntu Caddy public CA only for
the server's exact hostname and LAN IP. Cleartext traffic stays disabled. Google/Firebase
and other hosts use the Android system trust store. The production source set does not
include the CA or this configuration; its API URL stays fixed.

Caddy runs on the Ubuntu workstation, accepts IPv4 HTTPS on port 8443, and proxies to
the staging Node service on loopback port 8080. No router port forwarding is required.
Keep the server awake and every test phone on the same LAN without guest-network client
isolation. The phones still need internet access for Firebase sign-in and AI requests.

The certificate in `res/raw/staging_ca.pem` is public. The CA private key must stay on
the Ubuntu server. If that CA is replaced, copy only its new public certificate here,
rebuild the APK and update the phones. Do not install a system-wide CA on the phone.

For a physical phone, enable Developer options and Wireless debugging (Android 11+).
Pair with `adb pair <phone-ip>:<pairing-port>`, entering the temporary code at its prompt,
then use `adb connect <phone-ip>:<connection-port>`. The two ports differ and may change.
Install with `adb -s <device> install -r app/build/outputs/apk/staging/app-staging.apk`.
This preserves staging app data and uses the existing debug signing key.

The opt-in on-device connection test checks verified TLS, disabled cleartext, `/health`
and rejection of an unauthenticated account request. Build `assembleDebugAndroidTest`,
install both the debug APK and test APK, then run:

```bash
adb -s <device> shell am instrument -w \
  -e class com.tailormyresume.app.network.StagingConnectionTest \
  -e stagingConnectionCheck true \
  com.tailormyresume.app.staging.test/androidx.test.runner.AndroidJUnitRunner
```

This test runs only when explicitly enabled because it needs the physical LAN staging
server. Install the staging APK again after the test to use the minified staging build.

Verified on 2026-10-16 on a Pixel 7 Pro running Android 17: `.local` resolution, the on-device
TLS/authentication connection check, and installation/startup of the minified staging APK.
The temporary test APK was removed. App and network unit tests passed (504 tests), and the
release APK was checked to exclude both the local CA and the LAN endpoint.

## Firebase projects and local configuration

There are two environments and three Android build types:

| Environment | Build types | Firebase project ID | Android package |
|---|---|---|---|
| Staging | `debug`, `staging` | `tailormy-resume-staging` | `com.tailormyresume.app.staging` |
| Production | `release` | `tailormyresume-app` | `com.tailormyresume.app` |

Register each package in its own Firebase project. Under Authentication, Sign-in method, enable
Google for both projects and Anonymous for the staging project used to validate guest entry. Keep
production Google-only; guest-provider activation there is not part of staging validation. Use
`TMR Staging` as the staging public name and `TailorMyResume` as the production public name. Keep
Analytics disabled.

Enabling the staging Anonymous provider permits Firebase guest authentication only. Guest-trial
admission still requires the separately configured matching Play Integrity verifier and the
backend's active reward policy and admission controls described in [BACKEND_CONTRACT.md, section 8](BACKEND_CONTRACT.md#8-guest-trial-slice-contract-guest-trial-v1-2026-10-17).
Do not weaken integrity evidence or enable a production reward policy as part of staging setup.
Provider enablement alone does not prove signup or a real credit grant.

Run `./gradlew :app:signingReport` to get this machine's staging SHA-1 and SHA-256 certificates.
Add both to the staging Android app. Each developer machine can have a different debug key.
For production, register the upload certificate for local release installs and the Play app-signing
certificate for Play installs. The Play certificate is in Play Console, App integrity.

Download `google-services.json` from each Android app after enabling Google sign-in. The app uses
`FirebaseOptions` directly; do not add the Google services Gradle plugin. Copy these values into
`~/.gradle/gradle.properties`, outside the repository:

| JSON value for the matching Android client | Staging property | Production property |
|---|---|---|
| `project_info.project_id` | `tailormyresumeStagingFirebaseProjectId` | `tailormyresumeFirebaseProjectId` |
| `client_info.mobilesdk_app_id` | `tailormyresumeStagingFirebaseAppId` | `tailormyresumeFirebaseAppId` |
| `api_key[].current_key` | `tailormyresumeStagingFirebaseApiKey` | `tailormyresumeFirebaseApiKey` |
| `oauth_client[].client_id` where `client_type` is `3` | `tailormyresumeStagingWebClientId` | `tailormyresumeWebClientId` |

The web client ID is required by Credential Manager; do not use the Android client ID (`client_type: 1`).
See [Firebase Google sign-in setup](https://firebase.google.com/docs/auth/android/google-signin).
Also set `tailormyresumeStagingApiBaseUrl` to the staging backend's HTTPS URL. Keep it unset until
that deployment is available. Never point staging at production to make a build pass.

The staging backend must verify tokens for `tailormy-resume-staging`; the production backend must
verify tokens for `tailormyresume-app`. Firebase Admin credentials stay on the backend. They never
belong in the Android configuration or repository.

Check the local values with `./gradlew :app:verifyStagingBackendConfig :app:verifyProdBackendConfig`.
These tasks check for missing values. Verify Google sign-in on a device against each backend before
release; a successful build does not prove that the backend accepts the Firebase token.

## Build a bundle

Run the "Release bundle" workflow from the Actions tab on `main` and approve the `release` environment gate. It uploads `release-aab` (kept 3 days).

Locally, set the four variables below (environment or `~/.gradle/gradle.properties`) and run `./gradlew :app:bundleRelease` (prod):
`TMR_UPLOAD_KEYSTORE_FILE`, `TMR_UPLOAD_STORE_PASSWORD`, `TMR_UPLOAD_KEY_ALIAS`, `TMR_UPLOAD_KEY_PASSWORD`.

Without them `assembleRelease` still works and produces an unsigned APK. `bundleRelease` fails with a message naming the missing values.
`./gradlew :app:assembleStaging` needs the five staging properties and uses the debug key; it does not need an upload key.

## Versions

- `TMR_VERSION_NAME`: `MAJOR.MINOR.PATCH` with an optional `-suffix`. Default `0.1.0`.
- `TMR_VERSION_CODE`: integer from 1 to 2100000000. Default `1`. The workflow sets it to `github.run_number + TMR_VERSION_CODE_OFFSET`, so it grows with each new run. Re-running a job reuses its run number, and renaming the workflow file resets the run number. Raise `TMR_VERSION_CODE_OFFSET` above your last uploaded code if either happens. Play rejects a repeated code.

Invalid values fail the build. `tools/ci/test-release-config.sh` checks this.


### Guest trial integrity configuration

Guest trial admission uses Standard Play Integrity with a non-secret cloud project number. Set
`tailormyresumeStagingPlayIntegrityCloudProjectNumber` for debug/staging and
`tailormyresumePlayIntegrityCloudProjectNumber` for release in user Gradle properties. Use the project
that matches the backend verifier for that environment. Values must be positive decimal integers
that fit a signed 64-bit value. Missing or invalid values disable trial verification only; the app
retains existing account/work/balance access and shows temporary-unavailable recovery. Staging never
falls back to the release value. Do not commit actual values or service credentials.

The existing Firebase Auth dependency includes Integrity 1.3.0, whose Standard API the adapter uses.
No new runtime SDK is added. Sideload/debug/emulator installs receive no trust bypass. Real admission
needs matching Firebase anonymous-provider configuration, backend policy/admissions, finite capacity,
package/certificate/version checks, Google verifier access and the production disclosure review in
contract section 8. Test fixtures are application-logic evidence only.

## Referral App Links (disabled by default)

Optional Gradle properties `tailormyresumeStagingReferralOrigin` (debug/staging) and
`tailormyresumeReferralOrigin` (release) independently pin separate approved lowercase HTTPS DNS
origins. Leave them absent until owned hosting and association inputs are reviewed. Empty config
adds no referral VIEW filter and disables link/referrer capture and link share. Enabled config adds
only HTTPS `/r/` routing; strict runtime parsing remains mandatory. No `.invalid` host is shipped
enabled. Parser tests inject fixture origins directly.

The backend owns the sole landing, public-offer and assetlinks packager plus publication runbook.
Use the exact environment package and actual installed signing certificate: staging uses
`com.tailormyresume.app.staging`; release uses `com.tailormyresume.app`. Production requires the Play
App Signing certificate, not the upload key. Serve `/.well-known/assetlinks.json` on the exact owned
HTTPS host with no redirect. Configure the matching backend share origin. A server URL outside
Android's pinned origin/path/displayed own code cannot expand trust or be shared.

Real installed cold/warm App Links and a supported Play install through the configured landing/store
link remain BLOCKED external gates. Forced intents, sideloads, fake referrers and generated static
artifacts are implementation evidence only. The official Install Referrer 2.2 adapter closes its
connection in every path. A minimal consumed marker survives account cleanup; terminal capture and
consumption are atomic. Transient failures allow at most three foreground collection attempts,
with manual code entry immediately available. No remote service or live origin is activated here.
