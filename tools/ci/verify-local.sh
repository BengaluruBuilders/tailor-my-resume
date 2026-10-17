#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

tools/ci/check-constitution.sh
tools/ci/test-scan-secrets.sh
tools/ci/test-release-config.sh
tools/ci/test-staging-play-config.sh
python3 tools/ci/test-build-workflow.py
./gradlew \
  :build-logic:convention:check \
  spotlessCheck \
  :core:model:test \
  testDebugUnitTest \
  :core:domain:koverVerify \
  :core:data:koverVerify \
  verifyRoborazziDebug \
  lintRelease \
  dependencyGuard \
  assembleDebug \
  assembleRelease \
  -PwarningsAsErrors=true \
  --continue
