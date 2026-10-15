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

## Firebase projects and local configuration

There are two environments and three Android build types:

| Environment | Build types | Firebase project ID | Android package |
|---|---|---|---|
| Staging | `debug`, `staging` | `tailormy-resume-staging` | `com.tailormyresume.app.staging` |
| Production | `release` | `tailormyresume-app` | `com.tailormyresume.app` |

Register each package in its own Firebase project. Enable only the Google sign-in provider under
Authentication, Sign-in method. Use `TMR Staging` as the staging public name and `TailorMyResume`
as the production public name. Keep Analytics disabled.

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
