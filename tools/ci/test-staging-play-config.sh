#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
gradle_cmd="${GRADLE_CMD:-./gradlew}"
fixtures="$(mktemp -d)"
trap 'rm -rf "$fixtures"' EXIT

run() {
  env -u TMR_UPLOAD_KEYSTORE_FILE -u TMR_UPLOAD_STORE_PASSWORD -u TMR_UPLOAD_KEY_ALIAS \
    -u TMR_UPLOAD_KEY_PASSWORD -u TMR_STAGING_UPLOAD_KEYSTORE_FILE -u TMR_STAGING_UPLOAD_STORE_PASSWORD \
    -u TMR_STAGING_UPLOAD_KEY_ALIAS -u TMR_STAGING_UPLOAD_KEY_PASSWORD "$gradle_cmd" \
    -PTMR_UPLOAD_KEYSTORE_FILE= -PTMR_UPLOAD_STORE_PASSWORD= -PTMR_UPLOAD_KEY_ALIAS= -PTMR_UPLOAD_KEY_PASSWORD= \
    -PTMR_STAGING_UPLOAD_KEYSTORE_FILE= -PTMR_STAGING_UPLOAD_STORE_PASSWORD= \
    -PTMR_STAGING_UPLOAD_KEY_ALIAS= -PTMR_STAGING_UPLOAD_KEY_PASSWORD= "$@"
}
reject() {
  local name="$1" message="$2" output
  shift 2
  if output="$(run "$@" 2>&1)"; then
    echo "FAIL $name (unexpected success)"; exit 1
  fi
  if ! grep -qF "$message" <<<"$output"; then
    echo "FAIL $name (expected message absent)"; echo "$output"; exit 1
  fi
  echo "ok   $name"
}
props=(
  -PtailormyresumeStagingApiBaseUrl=https://abhishek-dubey-MS-7B16.local:20002/
  -PtailormyresumeStagingWebClientId=496155242121-fixture.apps.googleusercontent.com
  -PtailormyresumeStagingFirebaseApiKey=fixture
  -PtailormyresumeStagingFirebaseAppId=1:496155242121:android:fixture
  -PtailormyresumeStagingFirebaseProjectId=tailormy-resume-staging
  -PtailormyresumeStagingPlayIntegrityCloudProjectNumber=496155242121
)
reject 'missing dedicated staging signing' 'require dedicated TMR_STAGING_UPLOAD_' :app:verifyStagingUploadSigning -q
reject 'production signing cannot supply staging signing' 'require dedicated TMR_STAGING_UPLOAD_' \
  :app:verifyStagingUploadSigning -PTMR_UPLOAD_KEYSTORE_FILE=/nonexistent/prod.jks \
  -PTMR_UPLOAD_STORE_PASSWORD=fixture -PTMR_UPLOAD_KEY_ALIAS=fixture -PTMR_UPLOAD_KEY_PASSWORD=fixture -q
reject 'partial staging signing' 'require dedicated TMR_STAGING_UPLOAD_' :app:verifyStagingUploadSigning \
  -PTMR_STAGING_UPLOAD_KEYSTORE_FILE=/nonexistent/staging.jks -q
reject 'missing staging keystore file' 'existing dedicated staging upload keystore' :app:verifyStagingUploadSigning \
  -PTMR_STAGING_UPLOAD_KEYSTORE_FILE=/nonexistent/staging.jks -PTMR_STAGING_UPLOAD_STORE_PASSWORD=fixture \
  -PTMR_STAGING_UPLOAD_KEY_ALIAS=fixture -PTMR_STAGING_UPLOAD_KEY_PASSWORD=fixture -q
run :app:verifyStagingPlayIdentity "${props[@]}" -q
for property in FirebaseProjectId FirebaseAppId WebClientId PlayIntegrityCloudProjectNumber; do
  reject "missing $property" 'requires Firebase and Play Integrity project' \
    :app:verifyStagingPlayIdentity "${props[@]}" "-PtailormyresumeStaging$property=" -q
done
reject 'production Firebase rejected' 'requires Firebase and Play Integrity project' :app:verifyStagingPlayIdentity \
  "${props[@]}" -PtailormyresumeStagingFirebaseProjectId=tailormyresume-app -q
for api in https://apps-backend.fly.dev https://example.com http://abhishek-dubey-MS-7B16.local:20002/ \
  https://abhishek-dubey-MS-7B16.local:20002/path; do
  reject "unapproved API $api" 'requires the approved LAN HTTPS staging API' \
    :app:verifyStagingPlayIdentity "${props[@]}" "-PtailormyresumeStagingApiBaseUrl=$api" -q
done
# https://docs.gradle.org/current/userguide/signing_plugin.html
# These are configuration-only fixtures, never key material or artifacts.
touch "$fixtures/staging.jks"
signing=(-PTMR_STAGING_UPLOAD_KEYSTORE_FILE="$fixtures/staging.jks" -PTMR_STAGING_UPLOAD_STORE_PASSWORD=fixture \
  -PTMR_STAGING_UPLOAD_KEY_ALIAS=fixture -PTMR_STAGING_UPLOAD_KEY_PASSWORD=fixture)
for task in assembleStagingPlay bundleStagingPlay installStagingPlay; do
  plan="$(run ":app:$task" --dry-run "${props[@]}" "${signing[@]}" 2>&1)"
  for check in verifyStagingBackendConfig verifyStagingPlayIdentity verifyStagingUploadSigning; do
    grep -qF ":app:$check" <<<"$plan"
  done
  echo "ok   $task includes all staging Play guards"
done
cat > "$fixtures/model.gradle" <<'GROOVY'
gradle.beforeProject { project ->
    if (project.path == ':app') {
        project.afterEvaluate {
            def android = project.extensions.getByName('android')
            def types = android.buildTypes.collectEntries { type ->
                [(type.name): [signer: type.signingConfig?.name, debuggable: type.debuggable, minify: type.minifyEnabled,
                 shrink: type.shrinkResources, suffix: type.applicationIdSuffix,
                 api: type.buildConfigFields['TAILORMYRESUME_API_BASE_URL']?.value,
                 project: type.buildConfigFields['TAILORMYRESUME_FIREBASE_PROJECT_ID']?.value,
                 integrity: type.buildConfigFields['TAILORMYRESUME_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER']?.value,
                 fallbacks: type.matchingFallbacks]]
            }
            project.tasks.register('inspectStagingPlayBuildModel') {
                doLast { println('BUILD_MODEL=' + groovy.json.JsonOutput.toJson(types)) }
            }
        }
    }
}
GROOVY
run :app:inspectStagingPlayBuildModel -I "$fixtures/model.gradle" "${props[@]}" -q > "$fixtures/model.out"
python3 - "$fixtures/model.out" <<'PY'
import json, sys
m=json.loads(next(line.removeprefix('BUILD_MODEL=') for line in open(sys.argv[1]) if line.startswith('BUILD_MODEL=')))
assert m['debug']['signer']=='debug' and m['staging']['signer']=='debug'
assert m['release']['signer'] is None and m['stagingPlay']['signer'] is None
assert not m['stagingPlay']['debuggable']
for name in ['staging','stagingPlay','release']:
    assert m[name]['minify'] and m[name]['shrink']
for name in ['debug','staging','stagingPlay']:
    assert m[name]['suffix']=='.staging'
    assert m[name]['project']=='"tailormy-resume-staging"'
    assert m[name]['integrity']=='"496155242121"'
assert m['release']['api']=='"https://apps-backend.fly.dev"'
assert m['stagingPlay']['fallbacks']==['release']
print('ok   unchanged default signer/environment model and minified stagingPlay')
PY
run :app:verifyStagingUploadSigning "${signing[@]}" -q
reject 'same production keystore path rejected' 'existing dedicated staging upload keystore' \
  :app:verifyStagingUploadSigning "${signing[@]}" -PTMR_UPLOAD_KEYSTORE_FILE="$fixtures/staging.jks" -q
reject 'debug alias rejected' 'existing dedicated staging upload keystore' :app:verifyStagingUploadSigning \
  "${signing[@]}" -PTMR_STAGING_UPLOAD_KEY_ALIAS=androiddebugkey -q
reject 'staging signing cannot supply production signing' 'Release bundles must be signed with the upload key' \
  :app:verifyUploadSigning "${signing[@]}" -q
run :app:inspectStagingPlayBuildModel -I "$fixtures/model.gradle" "${props[@]}" "${signing[@]}" -q > "$fixtures/signed-model.out"
python3 - "$fixtures/signed-model.out" <<'PY'
import json, sys
m=json.loads(next(line.removeprefix('BUILD_MODEL=') for line in open(sys.argv[1]) if line.startswith('BUILD_MODEL=')))
assert m['stagingPlay']['signer']=='stagingUpload'
assert m['release']['signer'] is None
assert m['debug']['signer']=='debug' and m['staging']['signer']=='debug'
print('ok   dedicated signing selected only for stagingPlay (configuration fixture)')
PY
