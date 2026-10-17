# Publication status — 2026-10-17

The independent validator captured `findings.md` before anonymous authentication was enabled in the staging Firebase project. The prior `ADMIN_ONLY_OPERATION` is a historical reproduction. The guarded, single-field staging update then succeeded and was read back; see [staging-auth-setup.md](staging-auth-setup.md). The validator has not yet retested signup or app-to-candidate authentication, so B001 remains pending independent retest.

B002 (fresh emulator `.local` routing) and B003 (genuine Play Integrity and admission activation) remain blocked as described in the validator report. No guest grant or authenticated app-to-candidate journey is claimed.

Evidence pair: frontend `13aa4fef53057036c52fa25aa475e7a1ab303d83`; backend `69797ec759f3493b6d4f5d6912a82b1429e30fdb`; Android package `com.tailormyresume.app.staging`; APK SHA-256 `cd0082b09855dba860b73ad6e44bd9e0b39977ffd5cbd7ddc7dae5c9b918e4fa`; candidate endpoint `https://abhishek-dubey-MS-7B16.local:20002/`. The attached APK is not included.
