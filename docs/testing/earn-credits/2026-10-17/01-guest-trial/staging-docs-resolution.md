# Staging documentation resolution — 2026-10-17

Updated `docs/RELEASE.md` in the assigned frontend candidate checkout. The previous Firebase setup sentence instructed operators to enable only Google sign-in for each project, which contradicted the approved staging guest-entry validation and the successful guarded readback showing that staging Anonymous had been disabled.

The revised setup says to enable Google in both projects and Anonymous only in `tailormy-resume-staging`. It explicitly keeps production Google-only. It also separates guest authentication from trial admission: matching Play Integrity verification and backend reward-policy/admission controls remain required, and staging setup must not weaken evidence or activate a production reward policy. It states that provider enablement does not establish successful signup or a real credit grant.

The change leaves existing Firebase property names, user Gradle properties, web-client ID instructions, local build configuration, and credential handling intact. It adds no credentials or account details. No signup retest, guest grant, or reward-admission result is claimed.

Changed release-guide lines: 97–108. The correction replaces the Google-only provider instruction and adds the staging-only guest-auth/admission distinction. Markdown context and the focused diff were reviewed; no build or test was run because this documentation-only correction does not require one.
