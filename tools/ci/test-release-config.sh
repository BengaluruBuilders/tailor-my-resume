#!/usr/bin/env bash
set -uo pipefail

cd "$(git rev-parse --show-toplevel)"
gradle_cmd="${GRADLE_CMD:-./gradlew}"
failures=0

expect_failure() {
  local name="$1" message="$2"
  shift 2
  local out
  if out="$("$@" 2>&1)"; then
    echo "FAIL $name (build succeeded)"
    failures=$((failures + 1))
  elif grep -q -- "$message" <<<"$out"; then
    echo "ok   $name"
  else
    echo "FAIL $name (message '$message' not found)"
    failures=$((failures + 1))
  fi
}

expect_success() {
  local name="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "ok   $name"
  else
    echo "FAIL $name"
    failures=$((failures + 1))
  fi
}

env_clean() {
  env -u TMR_UPLOAD_KEYSTORE_FILE -u TMR_UPLOAD_STORE_PASSWORD -u TMR_UPLOAD_KEY_ALIAS \
    -u TMR_UPLOAD_KEY_PASSWORD -u TMR_VERSION_CODE -u TMR_VERSION_NAME "$@"
}

expect_failure "bundle without signing values fails" "Release bundles must be signed with the upload key" \
  env_clean "$gradle_cmd" :app:verifyUploadSigning -q
dry_run_plan="$("$gradle_cmd" :app:bundleRelease --dry-run 2>&1)"
if grep -q ":app:verifyUploadSigning" <<<"$dry_run_plan"; then
  echo "ok   bundleRelease runs the signing check"
else
  echo "FAIL bundleRelease runs the signing check"
  failures=$((failures + 1))
fi
expect_failure "version code zero rejected" "TMR_VERSION_CODE must be an integer" \
  env_clean TMR_VERSION_CODE=0 "$gradle_cmd" :app:tasks -q
expect_failure "version code not a number rejected" "TMR_VERSION_CODE must be an integer" \
  env_clean TMR_VERSION_CODE=abc "$gradle_cmd" :app:tasks -q
expect_failure "version code above the Play limit rejected" "TMR_VERSION_CODE must be an integer" \
  env_clean TMR_VERSION_CODE=2100000001 "$gradle_cmd" :app:tasks -q
expect_failure "version name rejected" "TMR_VERSION_NAME must look like" \
  env_clean TMR_VERSION_NAME=latest "$gradle_cmd" :app:tasks -q
expect_success "defaults and valid values accepted" \
  env_clean TMR_VERSION_CODE=2100000000 TMR_VERSION_NAME=1.2.3-beta.1 "$gradle_cmd" :app:tasks -q

blank_props=(-PtailormyresumeWebClientId=x -PtailormyresumeFirebaseApiKey=x -PtailormyresumeFirebaseAppId=x)
expect_failure "blank prod backend property rejected" "tailormyresumeFirebaseProjectId" \
  env_clean "$gradle_cmd" :app:verifyProdBackendConfig "${blank_props[@]}" -PtailormyresumeFirebaseProjectId= -q
expect_success "complete prod backend properties accepted" \
  env_clean "$gradle_cmd" :app:verifyProdBackendConfig "${blank_props[@]}" -PtailormyresumeFirebaseProjectId=x -q

staging_props=(
  -PtailormyresumeStagingApiBaseUrl=https://staging.example.invalid
  -PtailormyresumeStagingWebClientId=staging-client
  -PtailormyresumeStagingFirebaseApiKey=staging-key
  -PtailormyresumeStagingFirebaseAppId=staging-app
  -PtailormyresumeStagingFirebaseProjectId=staging-project
)
expect_success "complete staging backend properties accepted" \
  env_clean "$gradle_cmd" :app:verifyStagingBackendConfig "${staging_props[@]}" -q
for property in ApiBaseUrl WebClientId FirebaseApiKey FirebaseAppId FirebaseProjectId; do
  expect_failure "blank staging $property rejected" "tailormyresumeStaging$property" \
    env_clean "$gradle_cmd" :app:verifyStagingBackendConfig "${staging_props[@]}" \
    "-PtailormyresumeStaging$property=" -q
done
expect_failure "prod config cannot replace staging config" "tailormyresumeStagingFirebaseProjectId" \
  env_clean "$gradle_cmd" :app:verifyStagingBackendConfig "${blank_props[@]}" \
  -PtailormyresumeFirebaseProjectId=prod-project "${staging_props[@]}" \
  -PtailormyresumeStagingFirebaseProjectId= -q
if staging_plan="$("$gradle_cmd" :app:installStaging --dry-run "${staging_props[@]}" 2>&1)" &&
  grep -q ":app:verifyStagingBackendConfig" <<<"$staging_plan"; then
  echo "ok   installStaging runs the staging configuration check"
else
  echo "FAIL installStaging runs the staging configuration check"
  failures=$((failures + 1))
fi

exit "$failures"
