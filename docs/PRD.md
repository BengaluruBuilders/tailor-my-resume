# TailorMyResume — Product Requirements Document

| Field | Value |
|---|---|
| Product | TailorMyResume — "Your next move, better prepared." |
| Platform | Native Android (Kotlin, Jetpack Compose) with a small backend |
| Primary market | India. Final-year students and candidates with 0 to 2 years of experience. |
| Status | Draft 1. Build is gated on the Phase 0 validation test (section 12). |
| Date | 2026-09-30 |
| Evidence | Six research reports in [`docs/research/`](research/). Each claim below cites a report number (R1 to R6). |

## 1. Summary

TailorMyResume turns one saved candidate profile and one job description (JD) into an honest, tailored application.

The candidate imports a resume once and confirms the facts. For each job, TailorMyResume shows which requirements the candidate meets, partly meets, or does not meet. It then rewrites the resume for that job using only confirmed facts. Every tailored line links to the profile fact behind it. The candidate accepts or rejects each change, then exports a clean PDF or DOCX.

The core promise is **"We never invent anything about you."** No competitor states this rule (R2). Plain AI rewrites add unsupported claims in most outputs (R5, section 6.1). Recruiters distrust generic and exaggerated AI text, not AI use itself (R1, R4).

## 2. Problem

Evidence strength: A = primary data, B = reputable press or analyst, C = forum, anecdote, or vendor.

| Problem | Evidence | Grade |
|---|---|---|
| Candidates cannot stand out in large applicant pools. | 48% of Indian job seekers say they cannot stand out (LinkedIn India, Jan 2026). Greenhouse counted 244 applications per job in 2025, against 116 in 2022. | B, A (vendor) |
| Candidates do not know what a JD really asks for, or where they fall short. | JD gap analysis has the strongest support of the five MVP features. | R1 |
| Tailoring takes time. | About 44 minutes per application (US vendor survey). | C |
| Free AI output is generic, and it exaggerates. | 86% of US hiring managers say AI makes it too easy to exaggerate skills (Express–Harris Poll). A 2026 preprint found unsupported claims in 96.7% of baseline AI resume rewrites. | B, B |
| Freshers have thin experience and do not know how to present projects, internships, and coursework. | The top Play Store resume apps do not serve freshers with little experience. | R2 |
| Paid career products in India lose trust through surprise renewals, refused refunds, and guarantees. | Recurring 1-star themes on Play. Internshala Trustpilot score is 3.1. There are consumer complaints about Naukri FastForward. | R2, R3 |

Weak or unproven problems:

- **Re-entering background for each application.** No direct complaint was found. The problem is plausible but not proven. Validate it in interviews (R1).
- **"The ATS rejects my resume."** Research does not support this. Major ATS products parse and rank, and humans decide. Auto-rejection comes mainly from knockout questions and files that the ATS cannot read (R4). Do not build the product on this fear.

## 3. Target users

### 3.1 Primary persona — "Final-year Priya"

- Final-year student or 0 to 1 year graduate at a tier-2 or tier-3 college.
- Degree: B.Tech (CS or non-CS), B.Com, BBA, MBA, or B.Sc.
- Targets start-ups, GCCs, and non-IT corporate roles, off-campus as well as on-campus.
- Has an Android phone. May have a shared or old laptop.
- Uses free ChatGPT and Naukri today. Has not paid for a career tool, or paid once and regretted it.
- Reads and writes English at a working level.

### 3.2 Secondary persona — "Early-career Arjun"

- 1 to 2 years of experience. Wants to switch job.
- Applies to fewer jobs with more care. Has more facts to tailor.

### 3.3 Out of scope for MVP

- Candidates under 18. The DPDP Rules need verifiable parental consent for them (R5).
- Senior candidates.
- Markets outside India. This avoids GDPR at launch (R5). The US is a later option, and it needs a separate plan (R4).

### 3.4 Market size

| Measure | Value | Key assumption |
|---|---|---|
| Graduates per year in India | About 1.10 crore | AISHE 2023-24 (A) |
| SAM | About 3.6M people per year, about ₹178 crore | Engineering, commerce, science, and MBA graduates who seek private-sector jobs, use Android, and read English, plus the same number of early-career switchers |
| Year-3 revenue, base case | About ₹40 lakh from about 5.4k payers | 5% paid conversion. This is optimistic: Naukri's paid penetration is 2.6%. |
| Revenue for ₹1 crore per year | About 13k payers | Same price mix |

Source: R4. A consumer-only India product has a small revenue ceiling. Section 13 lists second revenue paths.

## 4. Goals and non-goals

### 4.1 Goals

1. Prove that freshers pay for honest, tailored applications before a full build (Phase 0).
2. Give a first useful result, a JD gap analysis, within 3 minutes of install.
3. Ship zero critical fabrications. "Critical" means a new employer, degree, certification, number, tool, or date.
4. Keep the model cost per full application at ₹7 or less (median), and at ₹10 or less with regenerations.
5. Pass Google Play review and meet DPDP duties before they start on 13 May 2027.

### 4.2 Non-goals for MVP

- Auto-apply to jobs. Auto-apply feeds the application flood that recruiters distrust (R4).
- An "ATS score" or any claim to "beat the ATS". Show "keyword coverage" only, and explain it (R1, R2, R4).
- Job discovery or job listings (R2, R5).
- Voice mock interviews. They need new consent and Data safety entries, and the EU emotion-recognition ban is a watch item (R4, R5).
- Profile photos.
- A large template library. About 3 clean, single-column templates are enough for MVP (R2 suggests up to 10 later).
- Auto-renewing subscriptions (R3, R6).
- iOS and web apps.

## 5. Positioning and differentiation

| Differentiator | Why it matters | Evidence |
|---|---|---|
| Truth-locked tailoring with a visible source for each line | No competitor states a "never invent" rule. Recruiters distrust exaggeration. | R2, R4, R5 |
| Gap analysis that shows what is missing, and does not hide it | Strongest demand signal. Gaps become preparation tasks, not invented claims. | R1 |
| A fresher evidence path: projects, internships, coursework, and competitions become evidence lines | Top apps fail freshers with thin experience. | R2 |
| Mobile-first, priced in rupees, with no subscription trap | Web leaders price in USD and have weak Android apps. Play reviews punish surprise renewals. | R2, R3 |
| One saved profile reused across every application | Web tools (Teal, Huntr, Careerflow) do this in USD. Kickresume's Android AI "does not remember anything about you". | R2 |

Main threats (R2):

1. **Naukri.** 50M+ installs. It launched an AI Resume Maker on 2025-11-21 and already holds profiles and payments. It is the fastest copier.
2. **Free chatbots** (ChatGPT, Gemini). ChatGPT Go is free for one year in India (R3).
3. **Indeed Career Scout**, if it launches in India.

TailorMyResume must sell the workflow and the trust, not the raw text. Raw text is free.

## 6. Scope

### 6.1 MVP features

Priority: P0 = the release is blocked without it. P1 = ship in MVP if time allows. P2 = after MVP.

| ID | Feature | Priority | Evidence strength |
|---|---|---|---|
| F1 | Resume import to an editable, confirmed profile | P0 | Plausible. Validate in Phase 0. |
| F2 | JD analysis and gap match | P0 | Strong |
| F3 | Truth-locked resume tailoring with per-bullet review | P0 | Strong |
| F4 | Export to PDF and DOCX | P0 | Required for F3 to be useful |
| F5 | Application workspace: saved documents, status, notes | P0 (light) | Tracker search demand in India is near zero. Keep it simple (R1). |
| F6 | Preparation questions per application | P0 | Moderate |
| F7 | Cover letter, optional and short | P0 | Weak India demand. Stronger US demand (R1). |
| F8 | Packs and payment | P0 | Required to test willingness to pay |
| F9 | Account, consent, and compliance screens | P0 | Legal and Play requirement (R5) |
| F10 | Share card (P0). Referral credit and group code (P2). | P0 / P2 | Distribution depends on referral (R6) |

### 6.1.1 MVP lock (2026-10-06)

Reviewed by an external model (gpt-6-astra) on 2026-10-06 and checked against R1, R2 and R6. Nothing
else enters the MVP without a new decision in this section.

| In the MVP | Cut from the MVP |
|---|---|
| F1 import, guided form, evidence questions, fact confirmation | Templates 2 and 3. The MVP has 1 template. |
| F2 JD paste or share, gap match, keyword coverage, "I have this", prep tasks | Referral credit and the group code |
| F3 tailoring with the six guardrails, per-bullet review, 2 regenerations | The single-application product |
| F4 PDF and DOCX export, 1 template, preview before the paywall, share sheet | Job listings and job discovery (section 4.2) |
| F5 application list, status, notes, prep tasks | Reminders and a kanban board |
| F6 prep questions. apna and ResumeGyani already offer prep in India (R2). | |
| F7 cover letter, optional, offered after the resume | |
| F8 free tier and the 5-application pack | |
| F9 Google sign-in, 18+ check, consent, data access, export, and deletion | |
| F10 JD-fit share card only, with no personal data (R6 section 7) | |

Channel attribution for college groups uses Play Install Referrer links, not a sign-up code.

### 6.1.2 Simplified MVP (2026-10-10)

The user simplified the MVP with the prototype in `design/prototype-2026-10-10/` (screen list and copy
in its `README.md`). Where this section and 6.1.1 or sections 6.2 to 8 disagree, this section wins.
The feature sections below are not yet rewritten; read them through this table.

| Area | Simplified MVP | Was |
|---|---|---|
| Flow | Sign in → upload resume → review profile → paste job → job analysis → one quick question → tailor → tailored resume → export PDF → Applications | Paste JD first, sign in after first value, consent screen, fact-by-fact confirmation |
| Sign-in | First screen, three story slides, Google only. 1 free credit at sign-in. No consent screen, no 18+ check. | After first value; consent notice; 18+ check |
| Profile | Import fills the profile; the user reviews it once and must fix required gaps (e.g. a missing end date) before going on. Paste-as-text and a 6-field manual form are the fallbacks. | Confirm every fact; guided form; fresher evidence path |
| Gap analysis | Match now → after tailoring, have and missing keywords, must-haves with reasons. Missing skills are added only if the user confirms them. | Met / partly met / to prepare per requirement; "I have this" sheet; prep tasks |
| Evidence | One quick question per tailoring about an unclear must-have; the answer is a source ("Your answer"). | Evidence path questions in the profile |
| Tailoring review | A Resume tab and a Changes tab (before, after, source). Edit in one editor. Must fit 1 page. | Per-bullet accept, keep, or edit; 2 regenerations |
| Export | PDF only, file name `Name_Company_Role.pdf` (format set in Settings), A4 or Letter | PDF and DOCX, preview before the paywall |
| Prep questions, cover letter | Shown as "Soon". Not in the MVP. | P0 |
| Share card | Not in the prototype. Not in the MVP. | P0 |
| Applications | List with a status bar; statuses Saved, Applied, Interview, Offer, Rejected; "Mark as Applied" with Undo; re-tailor for 1 credit | List, status, notes, prep tasks |
| Pricing | 1 credit = 1 tailored resume, never expire. Packs 5 for ₹199, 15 for ₹449 (best value), 40 for ₹999. Credit history ledger. | Free tier with 3 gap analyses a day, 5-pack at ₹149 |
| Settings | Credits, page size, file name format, product update toggle, download my data, help, delete account (type DELETE), sign out | Separate Your data and Delete account screens, consent read-only view |
| Look | Paige mascot and colour-coded hero cards (blue resume, amber job, lime tailor) on black; full-bleed colour sign-in. Space Mono and Space Grotesk. Dark only. | Avvio: dark and light, Manrope and Archivo Black, #AEFF00 |

#### Not in the MVP — decided by the owner on 2026-10-10

Do not build these, and do not raise them again as gaps, open questions, or review findings:

1. No consent notice screen and no 18+ check.
2. No light theme. The app is dark only.
3. No "Continue with Apple". Sign-in is Google only, even though the prototype draws an Apple button.
4. No follow-up reminders. Drop the "Follow-up reminders" toggle the prototype shows in Settings.
5. No offline states, and no error states beyond the five the prototype draws (sign-in cancelled,
   unreadable file, not a job post, tailoring failed, payment failed). No separate 200% text frames.

### 6.2 F1 — Resume import and profile

User story: as a candidate, I import my resume once, so I do not re-enter my background for each job.

Requirements:

1. The user picks a PDF or DOCX with the system file picker. The app asks for no storage permission.
2. The backend extracts a structured profile: contact, education, experience, projects, skills, certifications, and extras. Every item gets a stable ID.
3. The user reviews every extracted item and confirms or edits it. Only confirmed items are facts for tailoring.
4. The user can start without a resume. A guided form builds the profile from scratch.
5. **Fresher evidence path.** The app asks short questions about projects, internships, coursework, competitions, and positions of responsibility. Each answer becomes a profile fact tagged `user-stated`.
6. The backend deletes the uploaded file after extraction. It keeps only the confirmed profile.
7. The app does not ask for or store date of birth, photo, religion, caste, or marital status. If the source resume contains them, the app drops them and tells the user.

Acceptance criteria:

- Field-level extraction F1 score of 0.90 or more on a golden set of 50 real Indian fresher resumes. The set includes multi-column layouts, objective lines, and declaration lines (R5).
- A scanned or image-only PDF gives a clear message and routes the user to the guided form.
- Import to first confirmed profile takes 3 minutes or less at the median in usability tests.

### 6.3 F2 — JD analysis and gap match

User story: as a candidate, I paste a JD and see what it asks for and where I stand, before I spend time on the application.

Requirements:

1. The user pastes JD text or shares it into the app from another app (Android share sheet). The MVP does not scrape URLs.
2. The backend extracts requirements. Each requirement has a type (skill, experience, education, tool, soft skill), a priority (must-have or nice-to-have), and keywords.
3. The backend matches each requirement to profile facts. Status: met, partial, or gap. Each "met" or "partial" status cites the profile fact IDs.
4. The screen shows met, partial, and gap groups. It shows must-have gaps first.
5. For each gap, the app offers two actions:
   - "I have this." The user adds a fact by answering a short question. The fact is tagged `user-stated`.
   - "Add to my prep plan." The gap becomes a task in the application workspace, for example a proof-of-work project idea.
6. The screen shows **keyword coverage**, for example "You cover 9 of 14 key terms". It never shows an "ATS score", and it never says the resume will pass an ATS.

Acceptance criteria:

- Median time from paste to result is 20 seconds or less.
- On a labeled set of 50 JDs, must-have recall is 0.90 or more.
- The free tier includes gap analysis (section 8).

### 6.4 F3 — Truth-locked tailoring

User story: as a candidate, I get a resume version for this job that uses stronger, relevant wording, and I can check that every line is true.

Requirements:

1. The model returns structured bullets, not free text. Each bullet has at least one `source_id`, a list of edit types (reword, reorder, shorten, emphasise, merge), and the JD keywords that it uses.
2. A JD keyword may appear in a bullet only if the cited facts contain it or a known alias of it.
3. JD requirements with no evidence go to the gap list. They never go into the resume text.
4. Six guardrail layers run on every output (R5, section 6.2):
   1. **Fact ledger.** Only confirmed profile facts are inputs.
   2. **Constrained edit schema.** As in items 1 to 3.
   3. **Deterministic checks.** They run before any model check. Every number, name, tool, and date must match the cited facts. Employer count, order, and titles must not change. An ownership verb must not move up the ladder "assisted < contributed < developed < led < owned". Scale words ("team of", "users", "revenue") need a source.
   4. **Model verifier.** A different model from the generator splits each bullet into atomic claims and checks each claim against the cited facts. An unsupported or contradicted claim triggers one repair pass. If the repair fails, the app keeps the original bullet.
   5. **Per-bullet review.** The app shows the original text, the new text, and the linked fact for each bullet. The user accepts or rejects each change. The app flags bullets with a changed verb or scale word.
   6. **Evaluation and monitoring.** See section 10.3.
5. The user can edit any bullet by hand. A hand edit is the user's own statement. The app does not verify it, and it marks it as `user-edited`.
6. The user can regenerate a section. The first 2 regenerations per application are included in the price.
7. The same source IDs bind F6 and F7. A gap must never become a premise of a prep question or a cover letter sentence.

Acceptance criteria:

- The critical fabrication rate on the evaluation set is **0**. Any critical hit blocks the release.
- The minor unsupported-claim rate is under 2% per output.
- Claim retention (the share of true source claims kept) is above 99%.
- The median time from "Tailor" to a reviewable result is 60 seconds or less. The job runs in the background, and the app notifies the user when it is ready.

### 6.5 F4 — Export

Requirements:

1. Export as PDF and DOCX from one approved JSON document.
2. Use a single column, real selectable text, and standard section headings (Education, Experience, Projects, Skills). Use no tables, text boxes, or images for body text.
3. Offer 1 template at launch. Add more after the MVP.
4. Show a full preview before the paywall. The paywall covers the download, not the preview (R2).
5. Name files in the pattern `Name_Company_Role.pdf`.
6. Share through the Android share sheet: email, WhatsApp, Drive, or Files.

Acceptance criteria:

- Text extracted from the exported file matches the approved JSON. Any extra fact fails the test (R5).
- Two public ATS-parser checkers read name, contact, sections, and dates correctly.
- The app describes exports as "readable by common ATS parsers". It never says "beats ATS".

Technical risk: it is not verified whether Android `PdfDocument` output keeps selectable text. Run a one-day spike in week 1. If it fails, render the PDF on the server from HTML (R5).

### 6.6 F5 — Application workspace

Requirements:

1. Each application stores the company, the role, the JD text, the gap analysis, the tailored resume, the optional cover letter, the prep questions, notes, and the prep tasks.
2. Status values: Saved, Applied, Interview, Offer, Rejected, No response.
3. Show a simple list, sorted by last update. The MVP has no kanban board and no reminders.
4. The workspace works offline for reading. The app stores it in Room as the local source of truth and syncs it to the backend.

### 6.7 F6 — Preparation questions (P0)

1. Generate 8 to 12 likely interview questions from the JD and the gap analysis.
2. For each question, show why the interviewer may ask it, and which profile facts can support the answer.
3. For each gap, show one honest way to address it in an interview.
4. The MVP has no voice and no scoring.

### 6.8 F7 — Cover letter (P0, optional)

1. Generate a short letter of 150 to 220 words. Each paragraph cites source IDs.
2. The same deterministic checks and model verifier apply as in F3.
3. The user edits the letter in the app. Export follows F4.
4. The app offers the letter after the resume. It does not push it.

### 6.9 F10 — Share and referral (share card P0, the rest P2)

1. After a gap analysis, the user can share a "JD fit card" as an image. The card shows the role and the met/partial/gap counts. It shows no personal data.
2. Each referral that leads to a first full application gives both users 1 free application credit.
3. The user can enter a college or group code at sign-up. The code attributes the install to a channel for Phase 0 and launch analysis (R6).

## 7. Core user flow

1. Install. The app shows the value in one screen and the "never invents" promise.
2. The user pastes a JD, or imports a resume. Either order works.
3. The app asks for sign-in with Google, after first value and before any data leaves the device. It then shows the consent notice.
4. Import the resume, then confirm the profile.
5. The app shows the gap analysis. This result is free.
6. Tailor the resume. The user reviews each bullet and accepts or rejects it.
7. Preview the export. The paywall appears here if the user has no credits left.
8. Export and share. The app saves the application to the workspace.
9. Optional: the user adds prep questions and a cover letter.

Target: the median time from install to the first gap analysis is 3 minutes or less (R6).

## 8. Monetization

Willingness to pay is **partial and not validated** (R3). The India/SEA median download-to-paid rate is about 0.7%. Free AI sets the price anchor. Indian job seekers do pay ₹249 to ₹500 for micro products, and ₹1,000 to ₹3,000 for mid-tier services.

### 8.1 Launch model — packs, no subscription

| Tier | Contents | Price (to test) |
|---|---|---|
| Free | Profile import, unlimited profile edits, 3 gap analyses per day, 1 full application (tailored resume, export, prep questions) | ₹0 |
| Application pack | 5 full applications. The credits never expire. | ₹149 (test ₹99 / ₹149 / ₹199) |
| Single application, after MVP | 1 full application | ₹49 (test only if pack conversion is low) |
| Active search pass, after MVP | 30 days, prepaid, no auto-renew, up to 12 applications | ₹299 to ₹399 |

Rules:

1. Sell packs as Google Play consumable in-app products. Play prices in India include GST.
2. Show the price before the user starts a paid action. Show no paywall at install.
3. Put refund and help inside the app, in plain words.
4. Show no job, interview, or ATS guarantee anywhere.
5. The gap analysis stays free because it brings users in. Rate limits control abuse, not cost. The model cost of a gap analysis is under ₹1 (R5).

### 8.2 Unit economics

Assumptions: 15% Play fee, 18% GST inside the price, ₹90 per USD (R3, R5).

| Product | Price | Net after GST and Play fee | Model cost at ₹7 per application | Gross margin |
|---|---|---|---|---|
| 5-pack | ₹149 | About ₹107 | ₹35 | About 67% |
| 5-pack | ₹199 | About ₹143 | ₹35 | About 76% |
| 30-day pass, 12 applications | ₹399 | About ₹287 | ₹84 | About 70% |

If the model cost reaches the heavy case of about ₹36 per application, every product loses money (R3). Log the model cost for each application from day one. Keep model IDs and prices in server config.

The Play fee model changes for India on about 2027-09-30 (R3, secondary sources). Recheck it before launch.

## 9. Success metrics

### 9.1 North star

**Applications exported per week.** An exported application is a tailored resume that the user exports after the review step.

### 9.2 Launch targets (first 90 days after public launch)

| Metric | Target | Kill or rethink signal |
|---|---|---|
| Install to first gap analysis | 60% or more | Under 35% |
| Gap analysis to first export | 40% or more | Under 20% |
| Install to paid | 3% or more | Under 1% |
| Payers who buy a second pack within 30 days | 30% or more | Under 10% |
| Referral rate (invited installs per active user) | 0.2 or more | Under 0.05 |
| Bullets accepted without edit | 60% or more | Under 35% |
| Critical fabrications reported and confirmed | 0 | Any confirmed case triggers an incident review |
| Median model cost per full application | ₹7 or less | Over ₹10 |
| Play rating | 4.2 or more | Under 3.8 |

Retention: job search is episodic. Users leave when they get a job. Do not use D30 retention as a health metric. Track packs per payer, repeat purchase, referral, and return in the next placement season instead (R6).

## 10. Non-functional requirements

### 10.1 Architecture (R5)

- **App.** Kotlin, Jetpack Compose, Room as the local source of truth, and WorkManager for queued jobs and sync.
- **Sign-in.** Google through Credential Manager. Firebase Auth turns it into the ID token that the backend checks.
- **Backend.** `apps-backend` on Fly.io with Neon Postgres and Firebase Auth. The TailorMyResume routes are the model gateway (`docs/BACKEND_CONTRACT.md`). The Anthropic API key stays on the server.
- **Model calls.** Anthropic Claude through the backend (`claude-sonnet-5-5` generates, `claude-opus-5-5` verifies) with structured outputs. Short steps are synchronous routes that the server stops after 45 s. Tailoring is an asynchronous job that the app polls (`docs/BACKEND_CONTRACT.md` section 4.4).
- **Model tiers.** Use a small, low-cost model for extraction, JD analysis, gap match, and prep questions. Use a mid-tier model for tailoring and the cover letter. Use a different model for the verifier. Keep model IDs in config. The owner chose the Anthropic models on 2026-10-08. Set the prices from the official pricing page before launch.
- **Abuse control.** Per-user and per-device rate limits, the Play Integrity API, and a daily cap on free model calls.

### 10.2 Performance

| Operation | Target (median) |
|---|---|
| Resume import to extracted profile | 30 s or less |
| JD paste to gap analysis | 20 s or less |
| Tailor to reviewable result | 60 s or less |
| PDF export on device | 3 s or less |
| Cold start on a low-end device (3 GB RAM) | 2 s or less |

### 10.3 Quality and evaluation

Build this evaluation set before the first release (R5, section 6.3). Run it on every prompt, schema, or model change.

| Set | Size | Purpose |
|---|---|---|
| Normal profile–JD pairs | 100 | Baseline unsupported-claim rate |
| Trap JDs (requirements the profile lacks) | 50 | Measure JD text copied into the resume |
| Thin fresher profiles | 30 | Pressure to invent content |
| Indian-format resumes | 50 | Extraction and grounding |
| Seeded-fabrication outputs | 100 | Verifier and deterministic-check recall (target over 95%) |
| Prompt-injection resumes and JDs | 20 | Resistance to hidden instructions |

Release gates: 0 critical fabrications, 0 verb-strength violations, a minor unsupported-claim rate under 2%, and a person reads 30 random outputs per release beside the source profile.

In production: add a "Report inaccurate content" action on every generated item. Log the reject rate per bullet type. A rising reject rate signals model or prompt drift.

### 10.4 Privacy, security, and compliance (R4, R5)

This section is a product reading of the research. It is not legal advice. Get a legal review before launch.

**DPDP Act 2023 and DPDP Rules 2025.** The main duties start on 13 May 2027. TailorMyResume processes personal data, because a resume is personal data. The MVP must have:

1. A standalone, itemised consent notice before any upload. Owner decision 2026-10-10: not in the MVP (6.1.2).
2. An 18+ confirmation at sign-up. Owner decision 2026-10-10: not in the MVP (6.1.2).
3. In-app access, correction, and erasure of the user's data.
4. Account deletion in the app, plus a web link for deletion.
5. Export of all the user's data.
6. A grievance contact inside the app.
7. A breach response plan: report to the Data Protection Board within 72 hours, and notify affected users.
8. Security logs kept for 1 year.
9. Data minimisation, as in F1 item 7.

**Google Play.**

1. A Data safety form and a privacy policy.
2. An in-app action to report offensive or inaccurate AI content.
3. Target API level 36.
4. A closed test with 12 or more testers for 14 days, if the developer account is a personal account created after 13 Nov 2023 (R6).

**Anthropic.**

1. Anthropic has no per-request `store: false`. Retention is an organisation-level setting. Confirm it and record it in `docs/compliance.md` of `apps-backend`.
2. Ask Anthropic about Zero Data Retention for the organisation.
3. Treat Anthropic as a data processor in the privacy policy.
4. Decide on India data residency before launch.

**Security.**

1. Delete uploaded files after extraction.
2. Encrypt data in transit and at rest.
3. Turn on row-level security on every table.
4. Treat resume and JD text as untrusted input to the model (prompt injection).

### 10.5 Accessibility and language

- Meet Android accessibility basics: TalkBack labels, a 48 dp minimum touch target, and support for dynamic text size.
- The UI language is English. Documents are generated in English. Hindi UI is a later option.

## 11. Go-to-market

Paid install campaigns do not pay back. A paying user costs about ₹1,150 to ₹3,000 through paid ads, against about ₹107 net from a ₹149 pack after GST and the Play fee (R6, section 8.2).

Channels for the first 8 weeks (R6):

| Rank | Channel | Cost | Evidence |
|---|---|---|---|
| 1 | College WhatsApp and Telegram groups, with free packs for group admins | ₹0 to ₹3,000 | B (one case), mostly C |
| 2 | Sessions with college placement (T&P) officers: a "JD gap and resume" workshop | ₹0 to ₹5,000 | C |
| 3 | Founder-led LinkedIn posts that review real resumes against real JDs | ₹0, 4 to 5 hours per week | No case study |

Store listing (ASO): the head terms are taken by apps with 5M to 50M+ installs. Target long-tail terms: "JD match", "tailored resume", "fresher cover letter", "job application tracker" (R6).

Seasonality: placement peaks are September to November and January to March. December is slow (R6). Plan re-engagement messages before each peak.

## 12. Validation and release plan

### 12.1 Phase 0 — concierge pre-sale (before any app build)

Dates: 5 Oct to 1 Nov 2026 (R6).

1. **Audience.** Final-year students and 0 to 1 year graduates at tier-2 and tier-3 colleges.
2. **Offer.** A free JD gap check through a form or WhatsApp. Then a paid pack of tailored applications at ₹149 by UPI. The founder makes each pack by hand with AI tools and applies the "no invented facts" rule.
3. **Budget.** ₹10,000 planned, ₹12,000 hard cap.
4. **Also run.**
   - 8 to 10 structured interviews that test the "re-entering my background" problem (R1).
   - A waitlist page.
   - Recruitment of closed-test testers.

Thresholds:

| Signal | Green | Red |
|---|---|---|
| Free gap-check requests | 120 or more | Under 50 |
| Paid packs | 12 or more, and 10% or more of gap-check recipients | Under 5, or under 4% of recipients |
| Buyers who buy again within 14 days | 30% or more | — |
| Fabricated claims delivered | 0 | Any |
| Founder time per pack | 20 minutes or less | — |
| Waitlist sign-ups | 150 or more | — |

Decisions:

- **Green.** Build the MVP.
- **Amber.** Run 2 more weeks with a ₹99 price, a bundle, or a narrower niche.
- **Requests green, payment red.** Drop paid packs. Look at the B2B path (section 13).
- **Requests red.** Change the channel or the message once. If it is still red, stop.

Caution: do not run a fake purchase button in a Play build. It can breach the Play Deceptive Behavior policy. Pre-sell by UPI outside the app (R6).

### 12.2 Build and launch

Effort: about 24 engineer-weeks for one experienced Android engineer (range 20 to 30), plus about 3 weeks for the Play closed test (R5).

| Release | Contents | Target |
|---|---|---|
| R0 spikes (week 1) | PDF text spike, extraction on 10 real resumes, model cost measurement | Nov 2026 |
| R1 closed beta | F1, F2, F3, F4 (PDF), F5 (light), F9 | Closed test by early Feb 2027 |
| R2 public MVP | Add F4 (DOCX), F6, F7, F8, F10 share card | Before 13 May 2027 (DPDP date) |
| R3 | More templates, referral credit, group code, active search pass, reminders, Hindi UI (if data supports it) | Sep 2027 placement season |

**Scope decided 2026-10-06 (section 6.1.1); timeline still open.** A Jan to Mar 2027 public launch does not fit 24 engineer-weeks for one engineer. The plan above launches a closed beta in the Jan to Mar peak and the public MVP by May 2027. The other options are a second engineer, or a smaller R1 that cuts DOCX and prep questions.

Put the Play developer account on the critical path now. The closed test needs 12 or more opted-in testers for 14 days. Recruit 20 to 25 testers. An organization account avoids the closed test, but its D-U-N-S number can take up to 30 days (R6).

## 13. Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| 1 | Low willingness to pay. Free AI and free Naukri tools meet "good enough". | High | High | Phase 0 gate. Sell workflow and trust. Free gap analysis as the hook. |
| 2 | Fabrication reaches a user and harms trust. | Medium | High | Six guardrail layers. Zero-critical release gate. Report action. |
| 3 | Naukri copies the flow. | Medium | High | Speed, the honesty position, the fresher path, and college distribution. |
| 4 | Import quality is poor on Indian resume formats. | Medium | Medium | Golden set of 50 resumes. Guided form fallback. |
| 5 | Episodic use and high churn after a job offer. | High | Medium | Packs that do not expire. Senior-to-junior referral. Seasonal re-engagement. |
| 6 | Model prices or names change. | High | Low | Model IDs and prices in config. Cost logging for each application. |
| 7 | Exported PDF is not machine-readable. | Medium | Medium | Week-1 spike. Server-side HTML-to-PDF fallback. |
| 8 | Consumer-only revenue ceiling is small (about ₹40 lakh in year 3, base case). | High | Medium | Test second paths after MVP: placement-cell licences for colleges, and a US or Gulf tier. |

## 14. Open questions

1. Is "re-entering my background" a real pain? Answer it in the Phase 0 interviews.
2. Which prices apply to `claude-sonnet-5-5` and `claude-opus-5-5`? Check the official pricing page.
3. Does Google Play allow new auto-renewing subscriptions in India today? It is not needed for MVP.
4. Does Naukri's AI Resume Maker tailor to a JD? Is Indeed Career Scout live in India?
5. Does Android `PdfDocument` output keep selectable text? Answer it in the week-1 spike.
6. Personal or organization Play developer account?
7. Is India data residency for Anthropic needed at launch?
8. One engineer or two for the build (section 12.2)?

## 15. Research index

| Ref | Report | Main use in this PRD |
|---|---|---|
| R1 | [01-demand-and-pain.md](research/01-demand-and-pain.md) | Problem, feature priority, search intent |
| R2 | [02-competitors.md](research/02-competitors.md) | Positioning, threats, Play Store patterns |
| R3 | [03-willingness-to-pay.md](research/03-willingness-to-pay.md) | Pricing, Play Billing, margins |
| R4 | [04-market-and-trends.md](research/04-market-and-trends.md) | Market size, recruiter and ATS reality, regulation |
| R5 | [05-tech-feasibility.md](research/05-tech-feasibility.md) | Architecture, guardrails, cost, compliance, effort |
| R6 | [06-distribution.md](research/06-distribution.md) | Channels, retention, Phase 0 test, Play launch |

The research ran on 2026-09-30. Several agents reached their web search limit, and some official pricing pages were blocked. Each report lists its data gaps. Recheck prices, fees, and legal dates before each release.
