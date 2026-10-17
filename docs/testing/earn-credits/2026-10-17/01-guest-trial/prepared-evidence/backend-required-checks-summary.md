# Backend required-check summary (extracted)

Source logs remain at the slice report root. SHA-256 values below identify the full originals; this short file is a readable extract, not a replacement for source logs.

- Attempt 1: `backend-check-attempt1.log`, SHA-256 `2883e2074e3e65c068820e53f5f0a22e493f54b5469e5b43f97a1d400ff6b916`. Required `npm run check` failed: 690 tests, 688 passed, 1 failed, 1 skipped. The failing existing test was `the sync deadline starts before the first database transaction` (expected HTTP 200, observed 500). The handoff attributes the regression to a redundant global live-user query before the sync AI timeout began.
- Repair accounting: repair 1 of the maximum 2. The implementer report says the redundant query was removed while retaining route-level own-live guards and static `/packs` protection. Focused regression log `backend-repair1-focused.log`, SHA-256 `be47869fe151746fc7cc52ab43bcba5e4f051358998748018b604e9a1a0346bc`: 35 passed, no skips.
- Attempt 2: `backend-check-attempt2.log`, SHA-256 `92111e4b76f0267ea445f975e9763feac21ccd754deeacb1209c2d976c490119`. Required `npm run check` exited 0: types/lint/format/import-boundary stages passed; 690 tests, 689 passed, 0 failed, 1 skipped. The existing skip is `a real pg_dump produces a custom-format archive`, because `pg_dump` was not on PATH. No new skip was reported.
- Memory records: attempt 1 `backend-check-attempt1-memory.log`, SHA-256 `cf6b8e94c0d592cab43b91edce306f090e06eb006465b60bf30a7e3d2fc8e78e`; attempt 2 `backend-check-attempt2-memory.log`, SHA-256 `8c2c5d8e9d2eae49cf44789d853c469a1eb52bde1fabe9c7c4906e437a97eef9`. Handoff summary reports attempt 2 minimum available memory 10,010 MiB and memory PSI avg10 0; logs retain sample data.
- Focused development log `backend-focused-final.log`, SHA-256 `47262af317a63fd6c0b34da640db2450c15e8978f7e7afbd522a1bc4065146a9`: 42 passed, no skips. This is implementation-stage focused evidence.

These checks do not independently validate the feature pair or device journey. The skipped `pg_dump` test also does not establish backup/restore of the newly migrated schema; the handoff explicitly describes that as untested.
