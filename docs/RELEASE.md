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

## Build a bundle

Run the "Release bundle" workflow from the Actions tab on `main` and approve the `release` environment gate. It uploads `release-aab` (kept 3 days).

Locally, set the four variables below (environment or `~/.gradle/gradle.properties`) and run `./gradlew :app:bundleRelease` (prod) or `./gradlew :app:bundleStaging` (staging, needs the five staging properties):
`TMR_UPLOAD_KEYSTORE_FILE`, `TMR_UPLOAD_STORE_PASSWORD`, `TMR_UPLOAD_KEY_ALIAS`, `TMR_UPLOAD_KEY_PASSWORD`.

Without them `assembleRelease` still works and produces an unsigned APK. `bundleRelease` fails with a message naming the missing values.

## Versions

- `TMR_VERSION_NAME`: `MAJOR.MINOR.PATCH` with an optional `-suffix`. Default `0.1.0`.
- `TMR_VERSION_CODE`: integer from 1 to 2100000000. Default `1`. The workflow sets it to `github.run_number + TMR_VERSION_CODE_OFFSET`, so it grows with each new run. Re-running a job reuses its run number, and renaming the workflow file resets the run number. Raise `TMR_VERSION_CODE_OFFSET` above your last uploaded code if either happens. Play rejects a repeated code.

Invalid values fail the build. `tools/ci/test-release-config.sh` checks this.
