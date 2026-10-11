# TailorMyResume

**Your next move, better prepared.**

An Android AI job application assistant that turns a candidate's real background and a specific job description into a tailored application and preparation plan.

## Product focus

Help fresh graduates and early-career candidates prepare applications without repeatedly re-entering their background. A saved candidate profile connects resumes, cover letters, interview preparation, and application tracking.

## Initial MVP

- Import a resume into an editable candidate profile.
- Analyze a pasted job description and explain requirements and gaps.
- Tailor resume wording using the candidate's actual experience.
- Generate an editable cover letter and export readable application documents.
- Save each application's documents, preparation questions, notes, and status.

## Later candidates

Voice mock interviews, broader job discovery, and professional profile photos can follow validation of the core application workflow.

## Planned approach

- Native Android with Kotlin and Jetpack Compose.
- Anthropic Claude text capabilities through an authenticated backend.
- Keep permanent API credentials on the backend.
- Preserve user control over edits and exports. Suggestions must not invent qualifications or experience.
- Explain formatting and job-requirement checks without claiming guaranteed ATS acceptance or employment.

## Status

Offline prototype. The app runs PRD features F1, F2, F3, F4 (PDF only), and F5 with a deterministic, on-device engine. There is no backend yet.

Rules and CI gates: `docs/CONSTITUTION.md`. Commands: `AGENTS.md`.

OpenAI API credits are available for experimentation. Application packs and an active-job-search plan are monetization hypotheses to test with candidates.
