# TailorMyResume backend contract, v2 + guest-trial-v1

Status: v2 proposed 2026-10-10; guest-trial-v1 frozen for slice 01 on 2026-10-17 (section 8). Owner: the TailorMyResume app.
The backend is `BengaluruBuilders/apps-backend`. The app decides the shapes in this file. The backend may change an
internal detail. A change to a request, a response, a status, or an error code needs a change to this file first.

This file replaces the mock gateways of `docs/MOCK_BACKEND.md` with real routes. Read that file for
the interfaces that each route backs.

The v2 changes come from section 4 of `docs/revamp/2026-10-10/PLAN.md` (rows C1 to C17). The backend work is
`BengaluruBuilders/apps-backend` issues #87 to #100. Section 4.12 lists the routes that v2 removes.

## 1. Decisions

| # | Decision | Why |
|---|---|---|
| D1 | The device keeps the profile and the applications (Room). The server stores no profile, no application, no resume, and no JD after a request ends. Only tailored bullets wait 24 hours for the app to collect them (section 6). The credit ledger keeps an application id and a product id, never application content. Sync is after the MVP. | Smallest DPDP surface. A lost profile is re-imported in 3 minutes. Credits, the money part, live on the server. |
| D2 | The device reads the text of the PDF or DOCX (it does this today). The app uploads no file. TailorMyResume does not use `/v1/files`. | PRD F1.6 ("delete the file after extraction") holds by design. No R2 object to delete. |
| D3 | TailorMyResume gets typed routes under `/v1/tailormyresume/`. It does not call `/v1/ai/extract` directly. The routes reuse the core reserve and settle code. | The server checks every cited id against the facts it got, counts the daily allowance in the same transaction, and runs the verifier model. A generic `input` string cannot do this. |
| D4 | The server owns the counts: credits, the credit ledger, daily analyses, daily job imports, purchases. The device stops counting. | A reinstall must not reset a count. Purchased credits must survive a lost phone. |
| D5 | Dropped in v2. v1 said: one credit unlocks one application, once. | v2 spends one credit for each successful tailoring (section 4.4). Export is free after a tailoring, so no unlock exists. |
| D6 | Tailoring is a job: start, then poll. The other AI routes answer in the same request. | Tailoring runs up to 4 model calls (generate, verify, repair, verify) and can pass 60 s. A dropped mobile connection must not bill twice. |
| D7 | Two fabrication defences. The server checks cited ids, keyword grounding, and claim support (a second model, one repair). The device then runs the existing `FabricationGuard` as the last gate. | Constitution I.1 keeps its CI gate on the device. PRD 6.4 item 4 needs a different model as verifier. |
| D8 | AI routes never get the name, the email, or the phone. They get confirmed facts only (except resume parse, which gets the resume text). | Data minimisation, DPDP. The app adds the name to the export. |
| D9 | Dropped in v2. v1 said: the server refuses AI routes until the user has granted `ai-processing` and `age-18-plus`. | v2 removes the consent gate from the AI routes (section 4.12). The app does not show a Consent screen. |
| D10 | The TailorMyResume day is the calendar day in `Asia/Kolkata`. | India-only product. "Today" must mean the user's day. The core AI quota keeps its UTC day. |
| D11 | The server checks each Google Play purchase and consumes it. The purchase is bound to the user through `obfuscatedAccountId`. | A replayed token from another account gets no credit. The client has one less step that can fail. |
| D12 | The device renders the PDF, so the server never sees an export. The server limits cost, not access. | A modified client can export a resume that it did not pay to tailor. The per-user AI quota and the app cost cap bound what such a client can spend. |

### Compatibility fields (deprecated)

The staging app of the revamp run still parses the v1 shapes (revamp decision D12 in
`docs/revamp/2026-10-10/DECISIONS.md`). The backend keeps these fields until the follow-up
that comes after app issue #287 is deployed. New app code must not read or send them.

| Field or route | v2 behaviour until the follow-up |
|---|---|
| `wallet.freeTailoringsLeftToday` | Still in the answer. Always `0`. |
| `wallet.unlockedApplicationIds` | Still in the answer. Always `[]`. |
| `section` in `POST /tailorings` | Accepted only as `null`. Any other value is `400 INVALID_INPUT`. |
| `POST /applications/{applicationId}/unlock` | Still answers. It spends no credit. |

### Backend prerequisites

The current `apps-backend` has none of these yet. TailorMyResume issue 1 and issue 2 add them.

- Status `402` and `422` in `ApiStatus` and the new codes of section 2 in `ApiErrorCode` (`src/core/errors.ts`).
- Model tiers `extraction`, `generation`, `verifier`, each with its own price. Today `src/apps.ts` has
  `models.default` and an optional `premium`, and `extract()` always uses `default`.
- A function that app routes call to run one structured model call through `reserveAiCall` and
  `settleAiCall`, and that returns the `ai_usage` row id.

## 2. Conventions

- Base URL: the Fly.io host of `apps-backend`. All paths start with `/v1`.
- Every request sends `X-App-Id: tailormyresume` and `Authorization: Bearer <Firebase ID token>`.
  An unknown `X-App-Id` gets the core `400 APP_ID_INVALID` or `400 APP_NOT_FOUND`. A `/v1/tailormyresume/*`
  route called with another registered app id answers `404 NOT_FOUND`.
- JSON bodies, UTF-8, `camelCase` keys. Enum values are the Kotlin enum names (`MET`, `EXPERIENCE`).
- Times are ISO 8601 in UTC (`2026-10-07T18:30:00Z`). A TailorMyResume day is `YYYY-MM-DD` in `Asia/Kolkata`.
- An unknown field in a request body is `400 INVALID_INPUT`. A response can get new fields; the app ignores unknown fields.
- Errors use the existing envelope: `{"error":{"code":"...","message":"..."}}`. The app branches on `code`, never on `message`.
- The server never logs a request body, a response body, a resume, a JD, a job URL, or a fact.
- Sync AI routes stop after 45 s on the server. The app waits 60 s.
- No route needs a consent. The server does not read the consent rows (section 4.1).

### Error codes

The codes in the first table exist in `src/core/errors.ts`. The second table is new.

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | Bad body, a limit in this file is broken |
| 401 | `UNAUTHENTICATED`, `INVALID_TOKEN` | No token, bad token. The app gets a fresh Firebase token and tries once more |
| 403 | `CROSS_APP_TOKEN`, `FORBIDDEN` | Token of another app; a resource of another user |
| 404 | `NOT_FOUND` | Unknown id, or a TailorMyResume route called with another `X-App-Id` |
| 409 | `ACCOUNT_DELETED` | Deletion pending or done |
| 409 | `ANALYSIS_IN_PROGRESS` | The same JD is still being analysed. No model call. The app shows its retry state and does not retry by itself, because every run counts |
| 413 | `PAYLOAD_TOO_LARGE` | Body over the route limit |
| 429 | `RATE_LIMITED` | Request rate. `Retry-After` header |
| 429 | `QUOTA_EXCEEDED`, `BUDGET_EXCEEDED` | Core AI quota (per user per UTC day) or app cost cap |
| 502 | `AI_PROVIDER_ERROR` | The model failed or gave output that fails the schema |
| 405, 500 | `METHOD_NOT_ALLOWED`, `HTTP_ERROR`, `INTERNAL_ERROR` | Core. The app shows a general error |

| Status | Code | When |
|---|---|---|
| 402 | `NO_CREDIT` | `POST /tailorings` with no credit available (section 4.4). The app opens the Credits screen |
| 409 | `PURCHASE_PENDING` | Play reports the purchase as pending |
| 400 | `PURCHASE_INVALID` | Play reports the purchase as cancelled or for another product, or answers 400 or 404 for the token |
| 429 | `ALLOWANCE_EXHAUSTED` | No daily analysis left (20 a day), or no daily job import left (20 a day) |
| 422 | `NOT_A_JOB_POST` | The text of `POST /analyses` is not a job post. The daily analysis is not used |
| 422 | `JOB_IMPORT_FAILED` | `POST /job-imports` could not get a job post from the link |
| 502 | `PLAY_UNAVAILABLE` | The Google Play Developer API did not answer, or answered 401, 403, 429, or 5xx. The app keeps the token and tries again |

`CONSENT_REQUIRED` is removed (section 4.12).

## 3. Shared shapes

### `ProfileFacts`

The confirmed part of the profile. The app sends only confirmed entries and their bullets. It never
sends `fullName`, `email`, `phone`, or `headline`.

```json
{
  "summary": { "id": "summary", "text": "Data analyst with 2 years in retail operations." },
  "skills": ["SQL", "Excel", "Power BI"],
  "entries": [
    {
      "id": "EXP-01",
      "category": "EXPERIENCE",
      "title": "Data Operations Associate",
      "organization": "Saffron Retail",
      "startDate": "Jul 2024",
      "endDate": "Present",
      "source": "IMPORTED",
      "bullets": [{ "id": "EXP-01-1", "text": "Cleaned weekly sales data for 40 stores in Excel." }]
    }
  ]
}
```

| Field | Rule |
|---|---|
| `summary` | Optional. `{id, text}` with 1 to 1,000 characters of text. The id follows the id rule below. Omit it when the profile has no summary |
| `category` | `EDUCATION`, `EXPERIENCE`, `PROJECT`, `CERTIFICATION`, `ACHIEVEMENT` |
| `source` | `IMPORTED`, `USER_STATED`, `USER_EDITED`. The app never sends `USER_ANSWER` (section 4.4) |
| ids | Entry and bullet ids are opaque, `^[A-Za-z0-9:_.-]{1,64}$`, and unique over all entries, bullets, and the summary. An id that starts with `ans-` is reserved for the server (section 4.4) |
| `userStatedSkills` | Optional; omit it, send `null`, or send an array of strings (at most 100, else `400 INVALID_INPUT`). Entries should also appear in `skills`; entries that do not (compared ignoring case) are ignored. The skills it names are those the candidate states without evidence from an entry. The server marks them `STATED_BY_CANDIDATE` to the AI, never presents them as verified experience, and turns a `MET` match backed only by such skills into `PARTIAL`. Evidence ids stay `skill:<skill>`. Older app builds omit the field and are unaffected. |
| sizes | At most 40 entries, 15 bullets per entry, 200 bullets in total, 400 characters per bullet, 100 skills, 60 characters per skill, 200 characters per other text field. This fits in the 256 KB body limit of the routes that take it, also for text in Indian scripts (3 bytes per character) |

**Evidence ids.** The evidence ids of a request are: every entry id, every bullet id, the summary id,
and `skill:<skill>` for each skill, written exactly as sent (a skill id may hold spaces and symbols, as
in `skill:C++`; the id regex does not apply to it). This is the format the device uses today
(`EvidenceSources.SKILL_ID_PREFIX`). Any id that a response cites must be an
evidence id of the same request. The server removes every other id before it answers.

The server also issues `ans-` evidence ids (section 4.4): `ans-<requirementId>` for the fact of an answer and
`ans-<requirementId>-b1` for its bullet. They count as evidence ids of the request for that answer, and they are
the only valid `USER_ANSWER` citations. An `ans-` id may be cited by `result.summary`, and by a bullet under the
placement rule of 4.4 (rule 2). An answer never creates an employer claim.

### `Job` and `Match`

```json
{
  "title": "Associate Analyst",
  "company": "Northwind GCC",
  "location": "Bengaluru · Hybrid",
  "requirements": [
    { "id": "req-1", "text": "Must have strong SQL", "type": "SKILL", "priority": "MUST_HAVE", "keywords": ["sql"] }
  ]
}
```

```json
{ "requirementId": "req-1", "status": "MET", "evidenceIds": ["EXP-01-1", "skill:SQL"], "reason": "SQL in your Saffron Retail role." }
```

| Field | Rule |
|---|---|
| `location` | String or `null`. `null` when the post names no place |
| `type` | `SKILL`, `TOOL`, `EXPERIENCE`, `EDUCATION`, `SOFT_SKILL` |
| `priority` | `MUST_HAVE`, `NICE_TO_HAVE` |
| `status` | `MET`, `PARTIAL`, `GAP`. `GAP` has no evidence ids. `MET` and `PARTIAL` have at least one |
| `reason` | A short reason for the status, at most 120 characters. The server writes it from the cited evidence only. The server removes a reason that is not supported by the cited facts |
| sizes | At most 40 requirements, 10 keywords each |

The server sets the requirement ids (`req-1`, `req-2`, ...). The app sends `Job` and the matches back
unchanged in later requests. The server ignores `location` and `reason` in a request.

### `generationId`

Every AI answer carries a `generationId`: the id of the first `ai_usage` row of that request. The app
stores it with the content and sends it with a content report. The `ai_usage.task` value of a
TailorMyResume call names the prompt version (`tailormyresume.tailor.v1`), so a report links to the model and
the prompt version with no new column.

### `Wallet`

```json
{
  "credits": 6,
  "freeCredits": 1,
  "purchasedCredits": 5,
  "analysesLeftToday": 17,
  "day": "2026-10-10",
  "resetsAt": "2026-10-10T18:30:00Z"
}
```

| Field | Rule |
|---|---|
| `credits` | `freeCredits` plus `purchasedCredits`. The number of tailorings the user can still start, before the credits that a running job reserves |
| `freeCredits` | Earned promotional and historical welcome credits. A tailoring uses a free credit first |
| `purchasedCredits` | Credits from packs. They never expire |
| `analysesLeftToday` | Analyses left on the current TailorMyResume day. The cap is 20 |
| `day`, `resetsAt` | The current TailorMyResume day, and the instant it ends |

The compatibility fields of section 1 are also in the answer until the follow-up.

## 4. Routes

### 4.1 Account (exists in core)

| Route | Use in TailorMyResume |
|---|---|
| `GET /v1/me` | Call once after each Firebase sign-in. It creates the user row |
| `DELETE /v1/me` | Delete account. See 4.11 |

`POST /v1/me/consents` stays in core. The app does not call it. The consent rows that exist stay for
history and appear in `GET /v1/tailormyresume/me/export` (4.10). The server does not read them to
allow or refuse a route. The `noticeVersion` setting of TailorMyResume stays in the config (revamp D15): tests and
the eval runner still read it. Nothing in the routes depends on it.

### 4.2 `POST /v1/tailormyresume/resume/parse`

Backs `ResumeTextParser`. Sync. Counts against the core AI quota. Does not use a daily analysis.
Body limit 128 KB.

Request:

| Field | Type | Rule |
|---|---|---|
| `text` | string | 50 to 30,000 characters, after the device read the PDF or DOCX |

```json
{ "text": "Priya Deshmukh\nB.Tech Computer Science ..." }
```

Response `200`:

```json
{
  "generationId": "6f1c...",
  "profile": {
    "fullName": "Priya Deshmukh",
    "email": "priya@example.com",
    "phone": "+91 98xxxxxx10",
    "headline": "Data analyst, B.Tech CS 2024",
    "summary": "Data analyst with 2 years in retail operations.",
    "location": "Pune",
    "links": [{ "kind": "LINKEDIN", "url": "https://www.linkedin.com/in/priya" }],
    "skills": ["SQL", "Excel"],
    "entries": [
      {
        "ref": "e1",
        "category": "EXPERIENCE",
        "title": "Data Operations Associate",
        "organization": "Saffron Retail",
        "startDate": "Jul 2024",
        "endDate": null,
        "current": true,
        "bullets": [{ "ref": "e1b1", "text": "Cleaned weekly sales data for 40 stores in Excel." }]
      }
    ]
  },
  "droppedSensitive": ["DATE_OF_BIRTH", "MARITAL_STATUS"]
}
```

| Field | Rule |
|---|---|
| `profile.summary` | String or `null`. The summary paragraph of the resume, verbatim |
| `profile.location` | String or `null`. Verbatim |
| `profile.links` | `[{kind, url}]`. `kind` is `LINKEDIN`, `PORTFOLIO`, or `OTHER`. `LINKEDIN` is only for a `linkedin.com` host. Each `url` is `http` or `https` and appears in `text` |
| `entries[].current` | `true` when the resume shows the role as running (for example `Present`). A `true` value always comes with `endDate` `null`. Otherwise `false` |

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | `text` is out of range, or an unknown field |
| 413 | `PAYLOAD_TOO_LARGE` | Body over 128 KB |
| 429 | `QUOTA_EXCEEDED`, `BUDGET_EXCEEDED`, `RATE_LIMITED` | Core limits |
| 502 | `AI_PROVIDER_ERROR` | The model failed or gave output that fails the schema |

Rules:

1. `ref` values are unique in the answer and mean nothing after it. The app gives each entry and bullet
   its own id with `FactIdAllocator`, sets `source = IMPORTED` and `isConfirmed = false`.
2. Extraction is verbatim. The server drops any bullet, title, organization, summary, or location with a
   word that is not in `text` (case-insensitive, punctuation and bullet glyphs ignored). A dropped summary
   or location is `null`. A link that is not in `text` is dropped.
3. The model never returns date of birth, photo, religion, caste, marital status, or gender. When the
   resume has one, the answer names it in `droppedSensitive`: `DATE_OF_BIRTH`, `PHOTO`, `RELIGION`,
   `CASTE`, `MARITAL_STATUS`, `GENDER`. The app tells the user (PRD F1.7).
4. A resume with no entries gives `200` with empty `entries`. The app opens the manual form.
5. The review of the profile happens once, and the app remembers it on the device (`reviewedAt` in Room).
   The server has no flag for it. The rule for a missing end date (an entry with no `endDate` and
   `current` false needs a fix) runs on the device.

### 4.3 `POST /v1/tailormyresume/analyses`

Backs `AnalyzeJobUseCase` (JD analysis and gap match in one route). Sync. Body limit 256 KB.

Request:

| Field | Type | Rule |
|---|---|---|
| `jobText` | string | At least 20 words, at most 20,000 characters |
| `profile` | `ProfileFacts` | Section 3 |

```json
{ "jobText": "About the role ...", "profile": { "skills": [], "entries": [] } }
```

Response `200`:

```json
{
  "generationId": "a81d...",
  "job": { "title": "Associate Analyst", "company": "Northwind GCC", "location": "Bengaluru · Hybrid", "requirements": [] },
  "matches": [{ "requirementId": "req-1", "status": "MET", "evidenceIds": ["EXP-01-1"], "reason": "SQL in your Saffron Retail role." }],
  "question": {
    "requirementId": "req-4",
    "text": "Have you presented to senior stakeholders?",
    "why": "The role asks for this and your resume does not show it.",
    "options": ["YES_REGULARLY", "A_FEW_TIMES", "NOT_YET"]
  },
  "allowance": { "analysesLeftToday": 19, "day": "2026-10-10", "resetsAt": "2026-10-10T18:30:00Z" }
}
```

| Field | Rule |
|---|---|
| `job`, `matches` | Section 3 shapes. One match per requirement, in requirement order |
| `question` | The quick question, or `null`. At most one. See rule 6 |
| `question.options` | Always `YES_REGULARLY`, `A_FEW_TIMES`, `NOT_YET`, in this order |
| `allowance` | The state after this run |

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | Fewer than 20 words, over 20,000 characters, bad profile, or an unknown field |
| 413 | `PAYLOAD_TOO_LARGE` | Body over 256 KB |
| 422 | `NOT_A_JOB_POST` | The text is not a job post (for example a chat message). The server releases the claim. The model call is made and counts against the core AI quota |
| 429 | `ALLOWANCE_EXHAUSTED` | No daily analysis left. No model call |
| 409 | `ANALYSIS_IN_PROGRESS` | Same JD still in flight. No model call, no claim |
| 429 | `QUOTA_EXCEEDED`, `BUDGET_EXCEEDED`, `RATE_LIMITED` | Core limits |
| 502 | `AI_PROVIDER_ERROR` | The model failed or gave output that fails the schema |

Rules:

1. One match per requirement, in requirement order.
2. The server removes evidence ids that are not evidence ids of the request. A `MET` or `PARTIAL`
   match with no id left becomes `GAP`.
3. Daily allowance: 20 analyses per TailorMyResume day. The cap limits abuse. Every run counts, including a
   repeat of the same JD. The claim key is SHA-256 of `jobText` after trim and whitespace collapse, plus a
   run number. A failed analysis counts nothing. With 0 left: `429 ALLOWANCE_EXHAUSTED`, and the model is
   not called. A second POST of a JD whose first run is still in flight: `409 ANALYSIS_IN_PROGRESS`, no
   model call, no claim. The app blocks Analyse at 0 left, keeps the first result of a JD for the session,
   and re-matches on the device when the profile changes. It never calls this route again for that.
4. The count is a claim, because the model call cannot be inside a database transaction. Before the
   call, under the per-user advisory lock, the server checks the count and inserts a `PENDING` claim
   (unique on user, day, kind, key). Pending claims count as used. On success the claim becomes
   `USED`. On failure, and on `NOT_A_JOB_POST`, the server deletes it. A pending claim older than
   10 minutes counts as failed.
5. Keyword coverage is not in the answer. The device counts it from `requirements[].keywords` and
   the profile with its existing alias rules.
6. The quick question. The server picks the first requirement, in requirement order, that has all of
   these: priority `MUST_HAVE`, type `EXPERIENCE` or `SOFT_SKILL`, and status `GAP` or `PARTIAL` after
   rule 2. It asks for one yes or no question about that requirement. If no requirement qualifies, or
   the model gives no valid question, `question` is `null`. The server never writes the question text itself.
7. `reason` comes from the cited evidence only. A `GAP` match has a fixed reason that says the resume does not make it clear.
8. The server stores only the key hash and the day, to count. Never the text.

### 4.4 Tailoring jobs

Backs `ResumeTailor`. Body limit 256 KB. One credit pays for one successful tailoring. No consent is needed.

#### `POST /v1/tailormyresume/tailorings`

Request:

| Field | Type | Rule |
|---|---|---|
| `requestId` | string | UUID from the app, unique per user. A repeat with the same `requestId` returns the same job and starts nothing. The server ignores the body of a repeat |
| `applicationId` | string | `^[A-Za-z0-9_-]{1,64}$`. Device id of the application |
| `job` | `Job` | Section 3 |
| `matches` | `[Match]` | Section 3 |
| `profile` | `ProfileFacts` | Section 3 |
| `answer` | object or `null` | The answer to the quick question. See below. Omit it or send `null` for no answer |
| `section` | `null` | Deprecated. Only `null` is accepted (section 1) |

```json
{
  "requestId": "0b6c4c1e-2f7a-4b7e-9d8f-3a1e5b2c9d10",
  "applicationId": "4f0c...",
  "job": { "title": "...", "company": "...", "requirements": [] },
  "matches": [],
  "profile": { "skills": [], "entries": [] },
  "answer": { "requirementId": "req-4", "choice": "A_FEW_TIMES", "detail": "Presented the monthly variance report to the CFO." },
  "section": null
}
```

`answer`:

| Field | Rule |
|---|---|
| `requirementId` | The id of a requirement in `job.requirements`. Else `400 INVALID_INPUT` |
| `choice` | `YES_REGULARLY`, `A_FEW_TIMES`, `NOT_YET`, `SKIPPED` |
| `detail` | At most 400 characters, or `null` |

Response `202`: `{"tailoring":{"id":"tl_...","status":"RUNNING"}}`. Repeat of a `requestId`: `200` with the
current job. The app polls that job as it does a `202`.

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | Bad body, a limit in this file is broken, `section` is not `null`, a profile source `USER_ANSWER`, or an id that starts with `ans-` |
| 402 | `NO_CREDIT` | No credit is available (rule 1). No model call |
| 413 | `PAYLOAD_TOO_LARGE` | Body over 256 KB |
| 429 | `RATE_LIMITED` | More than 4 jobs run on this API machine. `Retry-After: 10` |
| 429 | `QUOTA_EXCEEDED`, `BUDGET_EXCEEDED` | Core limits |

Credit rules:

1. A credit is available when the wallet credits are more than the number of credits that the user's
   running jobs reserve. A free credit is used first, then a purchased credit.
2. A new job reserves 1 credit when it starts. At 0 available credits the answer is `402 NO_CREDIT`.
3. When the job ends `SUCCEEDED`, the server debits 1 credit and writes one `TAILORING` ledger row (4.6) in
   one transaction.
4. When the job ends `FAILED` or `INTERRUPTED`, the server releases the reservation. The balance does not
   change and no ledger row is written. The app tells the user that the credit was not used.
5. Each new `requestId` costs 1 credit. This includes a new tailoring of an application that has one
   already. A repeat of a `requestId` costs nothing more.
6. A second POST with a new `requestId` while a reserved job runs for the same user needs another
   available credit.
7. There is no free daily tailoring. The welcome credit (4.6) replaces it.

#### `GET /v1/tailormyresume/tailorings/{id}`

```json
{
  "tailoring": {
    "id": "tl_...",
    "status": "SUCCEEDED",
    "result": {
      "generationId": "c2e0...",
      "summary": {
        "text": "Data analyst with 2 years in retail operations.",
        "sourceIds": ["summary"],
        "verification": "PASSED"
      },
      "skills": { "ordered": ["SQL", "Excel", "Power BI"], "added": [] },
      "bullets": [
        {
          "id": "t-1",
          "entryId": "EXP-01",
          "sourceIds": ["EXP-01-1"],
          "proposedText": "Cleaned and checked weekly sales data for 40 stores in Excel.",
          "editTypes": ["REWORD"],
          "keywordsUsed": ["excel"],
          "verification": "PASSED"
        }
      ]
    }
  }
}
```

| Field | Rule |
|---|---|
| `status` | `RUNNING`, `SUCCEEDED`, `FAILED`. While `RUNNING` the answer has `Retry-After: 2` |
| `failureCode` | Only when `FAILED`: `AI_PROVIDER_ERROR`, `QUOTA_EXCEEDED`, `BUDGET_EXCEEDED`, `INTERRUPTED` |
| `result.summary` | `{text, sourceIds, verification}` or `null`. `null` means that the summary does not change. `verification` is `PASSED` or `REPAIRED` |
| `result.skills.ordered` | The profile skills, each once, in the order for this job. Skills that the model left out follow in their old order |
| `result.skills.added` | Skills that the candidate stated (`userStatedSkills`), or that a `YES_REGULARLY` or `A_FEW_TIMES` answer names verbatim in its detail. A `NOT_YET` or `SKIPPED` answer never adds a skill. Nothing else |
| `editTypes` | `REWORD`, `REORDER`, `SHORTEN`, `EMPHASISE`, `MERGE` |
| `verification` | `PASSED`: verifier found every claim supported. `REPAIRED`: passed after the one repair. `REVERTED`: failed after repair, `proposedText` is the text of the first source bullet and `editTypes` is empty. `UNCHANGED`: the model left out a source bullet, so the server added it back as is |

Errors: `404 NOT_FOUND` for an unknown id or the job of another user.

Rules:

1. Pipeline: generate, check, verify with the verifier model (a different model from the generator),
   repair once the bullets and the summary that fail, verify again, revert the bullets that still fail.
   The summary and the skills use the same calls. A job makes no more than 4 model calls.
2. Checks before the verifier: every `sourceIds` value is an evidence id of the request, or an `ans-` id that
   this request's answer issued (4.4); all sources of a bullet belong to `entryId`, except an `ans-` source,
   which is allowed under rule 2 of the answer rules below; every `keywordsUsed` value is in `proposedText` and in the text of a
   cited source; every number in a summary is in a cited source. A failed check counts as a verifier failure.
3. Claim retention: every confirmed bullet is a source of at least one answer bullet. The server adds a
   missing one as `UNCHANGED`.
4. Employer count, order, and titles do not change: the answer has no bullet for an entry outside the request.
5. The job runs in the API process. Each job row stores the boot id of the process that runs it. A
   `RUNNING` job of an older boot id reads as `FAILED` with `INTERRUPTED` at once (for example after
   a deploy), and so does a `RUNNING` job older than 5 minutes. Its reserved credit is released.
   The app starts a new job with a new `requestId`. Model calls that the kill cut off stay `pending`
   in `ai_usage` and count until the UTC day ends, as the core rules say.
6. The server keeps the result for 24 hours after the job ends, so the app can collect it, then deletes
   the row. It never stores the request body or the answer. A job of another user answers `404`.
7. The app polls every 2 s while the screen is open, and from a WorkManager job when it is not, then
   notifies the user (PRD F3 acceptance).

Quick-question answer, and the truth rule (PLAN section 2.3). An answer is a fact source with a limit.

1. `YES_REGULARLY` and `A_FEW_TIMES` make the server add one fact, `ans-<requirementId>`, with source
   `USER_ANSWER`. When `detail` is not empty, the server also adds one bullet, `ans-<requirementId>-b1`,
   with the text of `detail`. The text of the fact is `detail`, or the requirement text when there is no detail.
   `NOT_YET` and `SKIPPED` add nothing; a `detail` sent with them is ignored.
2. When `detail` names the organisation or title of an experience entry E (case-insensitive), a bullet under
   E may cite an `ans-` id as its only source. E may have no existing bullets. Any cited profile bullet must
   also belong to E. Otherwise the answer can support only the summary and the skills. An answer creates no
   entry and no employer claim (revamp D6). The device FabricationGuard (D7) accepts an `ans-` id as a source
   in the same way. The server drops or reverts a bullet that breaks this rule.
3. Only the server makes `USER_ANSWER` facts. A request that sends this source, or an id that starts
   with `ans-`, gets `400 INVALID_INPUT`.
4. The app shows a change that cites an `ans-` id with the source "Your answer". If the answer supports
   only the summary, the Changes tab shows "Summary · New · Your answer", and no experience entry gets a new bullet.

### 4.5 `POST /v1/tailormyresume/job-imports`

Backs `JobLinkViewModel`. Sync. No model call. Body limit 8 KB.

Request:

| Field | Type | Rule |
|---|---|---|
| `url` | string | `https` only, at most 2,048 characters |

```json
{ "url": "https://careers.example.com/jobs/associate-analyst" }
```

Response `200`:

| Field | Type | Rule |
|---|---|---|
| `jobText` | string | The visible text of the page, at most 20,000 characters |
| `sourceHost` | string | The host of the page after the redirects |

```json
{ "jobText": "Associate Analyst ...", "sourceHost": "careers.example.com" }
```

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | The URL is not `https`, is too long, or an unknown field. No fetch is made |
| 422 | `JOB_IMPORT_FAILED` | The page has fewer than 200 characters of text, or the fetch is blocked or fails |
| 429 | `ALLOWANCE_EXHAUSTED` | 20 imports are done on this TailorMyResume day. No fetch |

Rules:

1. The limit is 20 imports for each user on each TailorMyResume day. Every accepted request counts,
   including a request that fails.
2. The server must not fetch an address inside a private or internal network. It checks every redirect.
   The checks are in `BengaluruBuilders/apps-backend` issue #96.
3. The app sends the `jobText` to `POST /analyses` as `jobText`, after the user looks at it.
4. The server never logs the URL or the text.

### 4.6 Credits, wallet, and packs

Backs `UsageAllowance` and the credit part of `PaymentGateway`.

#### `GET /v1/tailormyresume/wallet`

Reads never grant credits or insert a ledger event. An absent wallet reads as zero. Historical balances
stay intact. The app reads after sign-in and uses explicit protected initialization for a new eligible
guest trial (section 8); no GET, export, unlock, purchase, or tailoring path can create WELCOME credits.

Response `200`: `{"wallet": Wallet}`. The `Wallet` shape is in section 3.

Errors: `401`, `409 ACCOUNT_DELETED`, `429 RATE_LIMITED`.

#### `GET /v1/tailormyresume/credits`

The credit ledger. It has no request body.

```json
{
  "credits": {
    "balance": 6,
    "entries": [
      {
        "id": "7d1e...",
        "kind": "TAILORING",
        "amount": -1,
        "applicationId": "4f0c...",
        "productId": null,
        "createdAt": "2026-10-10T09:12:00Z"
      }
    ]
  }
}
```

| Field | Rule |
|---|---|
| `balance` | `wallet.freeCredits + wallet.purchasedCredits` from the wallet counters, which stay authoritative (apps-backend #89). It equals `wallet.credits`. History of wallets made before the ledger may differ from it by the deploy window |
| `entries` | Newest first. At most 100. Every row exposes its original stable server `id`, including legacy rows and `GUEST_TRIAL`; never synthesize IDs from amount/time |
| `kind` | `WELCOME`: a historical welcome event, original amount unchanged. `GUEST_TRIAL`: a protected trial grant, amount from the original server policy (`+2` in this campaign). `PURCHASE`: a pack, amount `+5`, `+15`, or `+40`, with `productId`. `TAILORING`: a successful tailoring, amount `-1`, with `applicationId`. `REFUND`: credits given back, positive amount. `MIGRATION`: the balance that a wallet had before the ledger existed, one row for each wallet that had credits, positive amount |
| `amount` | Signed integer, never `0` |
| `applicationId`, `productId` | String or `null`. Only the kinds above set them |

The server stores no title or company. The app joins `applicationId` with its local data to show the
role and the company, and it takes the price of a pack from Play Billing.

Errors: `401`, `409 ACCOUNT_DELETED`, `429 RATE_LIMITED`.

#### `GET /v1/tailormyresume/packs`

```json
{
  "packs": [
    { "productId": "application_pack_5", "credits": 5, "creditsExpire": false },
    { "productId": "application_pack_15", "credits": 15, "creditsExpire": false },
    { "productId": "application_pack_40", "credits": 40, "creditsExpire": false }
  ]
}
```

Name and price come from Play Billing `ProductDetails` (localised, GST included). The server owns
the credit count of each product. The prices are set in Play Console and are not in this file. The 15 and
40 credit products are an owner step (app issue #288).

Errors: `401`, `429 RATE_LIMITED`.

### 4.7 Purchases

Backs the purchase part of `PaymentGateway`.

The app sets `obfuscatedAccountId` in `BillingFlowParams` to the lower-case hex SHA-256 of the
Firebase uid (64 characters).

#### `POST /v1/tailormyresume/purchases`

Request:

| Field | Type | Rule |
|---|---|---|
| `productId` | string | One of the pack ids of 4.6. Else `400 INVALID_INPUT` |
| `purchaseToken` | string | Opaque token from Play, at most 4,096 characters |

```json
{ "productId": "application_pack_5", "purchaseToken": "opaque token from Play" }
```

The app sends it after Play reports `PURCHASED`, and again at each start for every purchase that
`queryPurchasesAsync` still returns (restore).

Server steps:

1. `productId` must be in the pack list, else `400 INVALID_INPUT`. `purchaseToken` at most 4,096 characters.
2. A token already stored for this user returns the stored purchase, `200`, with no Play call except
   a consume that is still due (step 6).
3. Call `purchases.products.get` for package `com.tailormyresume.app` and the given `productId`.
   The product that Play reports must be the product that the request names, else `400 PURCHASE_INVALID`.
   `purchaseState` 0 goes on, 2 is `409 PURCHASE_PENDING`, 1 is `400 PURCHASE_INVALID`. Play 400 or
   404 is `400 PURCHASE_INVALID`. No answer, or Play 401, 403, 429, or 5xx, is `502 PLAY_UNAVAILABLE`.
4. `obfuscatedExternalAccountId` must equal the hex SHA-256 of the caller's uid. A missing or other
   value is `403 FORBIDDEN`. This also stops a token of another user, so no separate code exists.
5. One transaction: insert the purchase (unique on the token), add the credits to `purchasedCredits`, and
   write one `PURCHASE` ledger row. Store `purchaseType` too: a licence-tester purchase
   (`purchaseType` 0) grants credits, so the closed test works, and is marked `test` in the row.
6. Call `purchases.products.consume`. On failure, keep the grant and answer as normal. The next call
   with the same token, and the hourly backend loop, try the consume again until it succeeds. Play
   refunds a purchase that is not acknowledged within 3 days.

Response `201` (first grant) or `200` (repeat):

```json
{
  "purchase": {
    "orderId": "GPA.3301-...",
    "productId": "application_pack_15",
    "creditsGranted": 15,
    "purchasedAt": "2026-10-10T09:12:00Z",
    "state": "COMPLETED"
  },
  "wallet": {}
}
```

`wallet` is the `Wallet` shape of section 3. The purchase response shape is the same as in v1.

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | Unknown pack, token too long, or an unknown field |
| 400 | `PURCHASE_INVALID` | See step 3 |
| 403 | `FORBIDDEN` | See step 4 |
| 409 | `PURCHASE_PENDING` | See step 3 |
| 502 | `PLAY_UNAVAILABLE` | See step 3 |

The app does not call `consumeAsync`. A pending purchase stays on the device (Play state) and shows
as pending; the server does not know it.

#### `GET /v1/tailormyresume/purchases`

Response `200`: `{"purchases":[Purchase]}`, newest first. `Purchase` is the `purchase` object above. Errors: `401`, `429 RATE_LIMITED`.

### 4.8 `POST /v1/tailormyresume/content-reports`

Backs `ContentReportRepository.report`. Body limit 8 KB.

Request:

```json
{ "applicationId": "4f0c...", "itemKind": "RESUME_BULLET", "itemId": "t-1", "generationId": "c2e0...", "itemText": "..." }
```

| Field | Rule |
|---|---|
| `applicationId` | Device id of the application, or the draft key of Gap analysis (`analysis-draft-<hash>`). `^[A-Za-z0-9_-]{1,80}$` |
| `itemKind` | `REQUIREMENT`, `RESUME_BULLET`, `SECTION`. The kinds `COVER_LETTER` and `PREP_QUESTION` of v1 stay (apps-backend #97): old reports stay readable, `POST /content-reports` still accepts them and export still returns them |
| `generationId` | The id from the answer that made the item, or `null` |
| `itemText` | 1 to 2,000 characters. The reported text, so the team can check it. It is personal data and is kept as section 6 says |

Response `201`: `{"report":{"id","reportedAt"}}`. The same user, `applicationId`, kind, and id again: `200` with
the first report.

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | A field breaks its rule, or an unknown field |
| 413 | `PAYLOAD_TOO_LARGE` | Body over 8 KB |
| 429 | `RATE_LIMITED` | Request rate |

### 4.9 `POST /v1/tailormyresume/me/data-export` (target)

Status: target. The route does not exist, and it is blocked. The backend has no email provider
(`BengaluruBuilders/apps-backend` issue #99, revamp decision D8 in `docs/revamp/2026-10-10/DECISIONS.md`).
The app does not call this route until the blocker is gone.

Backs "Download my data" (`SettingsViewModel`). Target behaviour:

Request:

| Field | Type | Rule |
|---|---|---|
| `device` | object | The data that the device holds (profile, applications). The server does not keep it |

Response `202`: `{"export":{"status":"QUEUED"}}`. The server emails the merged JSON (the `device` object and
the server data of 4.10) to the Google account email of the user, and keeps nothing.

Errors:

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_INPUT` | `device` is missing or too large |
| 413 | `PAYLOAD_TOO_LARGE` | Body over the route limit |
| 429 | `RATE_LIMITED` | Request rate |

On-device interim. Until the route exists, the app builds the archive on the device (`AccountDataExporter`).
It adds `server.json` from `GET /me/export` (4.10), and it gives the file to the user through the share sheet.

### 4.10 `GET /v1/tailormyresume/me/export`

Backs the server part of `AccountDataExporter`. The app adds it to the archive as `server.json`.

```json
{
  "export": {
    "generatedAt": "2026-10-10T10:00:00Z",
    "user": { "id": "...", "createdAt": "..." },
    "consents": [{ "purpose": "ai-processing", "granted": true, "policyVersion": "...", "recordedAt": "..." }],
    "wallet": {
      "credits": 6, "freeCredits": 1, "purchasedCredits": 5, "analysesLeftToday": 20,
      "day": "2026-10-10", "resetsAt": "..."
    },
    "credits": [{ "id": "...", "kind": "PURCHASE", "amount": 5, "applicationId": null, "productId": "...", "createdAt": "..." }],
    "unlocks": [{ "applicationId": "...", "creditKind": "...", "unlockedAt": "..." }],
    "purchases": [],
    "contentReports": [
      { "id": "...", "applicationId": "...", "itemKind": "...", "itemId": "...", "generationId": "...", "itemText": "...", "reportedAt": "..." }
    ]
  }
}
```

Times are ISO strings. `consents` lists the rows that the user made before v2. `credits` lists every ledger row, oldest first, with no cap (apps-backend #89 c8). `unlocks` lists the stored unlock rows while `tailormyresume_unlocks` exists (section 6). `wallet` carries the deprecated fields of section 1 too. The app keeps the sub-objects
as opaque JSON.

Errors: `401`, `409 ACCOUNT_DELETED`, `429 RATE_LIMITED`.

### 4.11 `DELETE /v1/me`

Core route. Unchanged in v2. No body. Response `202`: `{"deletion":{"status":"PENDING"}}`.

The deletion removes every TailorMyResume table of section 6, including the credit ledger. A test of the
ledger table must show this. Errors: `401`, `409 ACCOUNT_DELETED`.

### 4.12 Removed routes

v2 retires these. The app side has stopped using each one. The backend removes each in the issue named in the last column, except that `unlock` and `section` stay as the compatibility routes of section 1 (revamp D12) until the follow-up after app #287 is deployed.

| Removed | What replaces it | App side that stopped using it | Backend issue |
|---|---|---|---|
| `POST /applications/{applicationId}/unlock` (C5) | Nothing. A credit is spent when a tailoring succeeds (4.4). The route stays as a compatibility route that spends nothing (section 1) | `PaymentGateway` and the export step. Export is free after a tailoring | #90 |
| `POST /prep-questions` (C15) | Nothing. The prep feature is deleted | The prep question source and its screens, deleted in app issues #257 and #260 | #97 |
| `POST /cover-letters` (C15) | Nothing. The cover letter feature is deleted | The cover letter source and its screens, deleted in app issues #257 and #260 | #97 |
| The `section` parameter of `POST /tailorings` | Nothing. A new tailoring costs 1 credit | Section regeneration, deleted in app issue #257 | #90 |
| The consent requirement on AI routes (C14) and the error `CONSENT_REQUIRED`. The `noticeVersion` setting stays (revamp D15) | Nothing. The core route `POST /v1/me/consents` and the `consents` table stay for history | The Consent screen and `POST /v1/me/consents` calls, deleted in app issue #260 | #87 |

## 5. Limits that bind the app

| Limit | Value | Owner |
|---|---|---|
| AI calls per user per UTC day | 50 (core). One tailoring is up to 4 calls | Core config |
| AI cost per app per day | $5 (core). Raise before launch | Core config |
| Analyses per TailorMyResume day | 20 runs, repeats count. This cap limits abuse | TailorMyResume config |
| Job imports per TailorMyResume day | 20 requests | TailorMyResume config |
| Free tailorings per TailorMyResume day | None. The welcome credit replaces them | TailorMyResume config |
| Free credits for a new user | 1 (the `WELCOME` ledger row) | TailorMyResume config |
| Credits for one tailoring | 1, for each new `requestId` that succeeds | TailorMyResume config |
| Credit ledger entries in an answer | 100, newest first | TailorMyResume config |
| Requests per user per minute | 60 (core) | Core config |

## 6. Server storage and deletion

| Table | Holds | Kept until |
|---|---|---|
| `tailormyresume_wallets` | user id, free and purchased credits | Account deletion |
| `tailormyresume_credit_ledger` | user id, kind, amount, application id, product id, purchase id, tailoring id, time. No application content | Account deletion |
| `tailormyresume_usage_claims` | user id, day, kind (`ANALYSIS`, `JOB_IMPORT`), key (JD hash or a fixed key), status, time | 30 days, or account deletion |
| `tailormyresume_unlocks` | user id, application id, credit kind, time. No new row after v2 | One release after v2, then a migration drops it. Export it before the drop. Account deletion removes it before then |
| `tailormyresume_purchases` | user id, token, order id, product, purchase type, credits, times, consumed time | Account deletion |
| `tailormyresume_tailorings` | user id, request id, boot id, status, credit reserved flag, result (tailored bullets, summary, skills), times | 24 hours after the job ends, or account deletion |
| `tailormyresume_content_reports` | user id, application id, kind, item id, generation id, item text, time | 180 days, or account deletion |

`DELETE /v1/me` deletes all of them in the rows step, with tests for partial failure and retry, as
the backend definition of done requires. That includes purchases: Google Play is the merchant of
record and keeps the order and tax records, so TailorMyResume keeps no copy.

Two tables hold text derived from the user: the tailored result for 24 hours, and the text of a
reported item for 180 days. The resume text, the JD text, the job URL, the quick-question answer, and the
request bodies are never written to a table or a log. Anthropic has no per-request `store: false`: retention
is an organisation-level setting (see `docs/compliance.md` of `apps-backend`, and PRD 10.4).

## 7. Model use

The provider is Anthropic Claude (decided 2026-10-08). `default`, `extraction`, and `generation` use
`claude-sonnet-5-5`. `verifier` uses `claude-opus-5-5`. The backend sends `reasoningEffort` as
`providerOptions.anthropic.effort`.

| Route | Calls | Model tier |
|---|---|---|
| Resume parse | 1 | `extraction` |
| Analysis | 1 | `extraction` |
| Tailoring | up to 4 | `generation`, then `verifier` |
| Job import | 0 | None |

The `verifier` model must differ from the `generation` model. The backend picks the models and keeps
them in `src/apps.ts`. The `ai_usage.task` of each call is `tailormyresume.<route>.v<prompt version>`.

Prompt injection. All resume, JD, fact, and answer text is untrusted.

1. The prompt puts it between fixed delimiters and says that text inside them is data, not
   instructions. The server removes the delimiter strings from the input first.
2. The output schemas and the id checks of section 4 limit what an injected instruction can do.
3. The verifier gets only the cited source facts and the proposed text, never the JD.
4. The prompt-injection set of the evaluation (PRD 10.3) is a release gate.

A prompt, schema, or model change needs a run of the TailorMyResume evaluation set before merge.

## 8. Work on the app side

These are not backend issues. They go to this repository.

1. Remote implementations of `ResumeTextParser`, `AnalyzeJobUseCase`, and `ResumeTailor` read the v2 shapes
   (app issue #286). They store each `generationId`. They send the quick-question answer in
   `POST /tailorings` and show the summary and the skills order from the result.
2. `PaymentGateway` supports the three packs. `CreditsRepository` reads `GET /credits`. `UsageAllowance`
   reads `GET /wallet` (app issue #287).
3. The app stops calling `POST /v1/me/consents`, the unlock route, `POST /prep-questions`, and `POST /cover-letters`
   (section 4.12).
4. A job link goes through `POST /job-imports`, then `POST /analyses`.
5. `ContentReportRepository` also posts the report. `AccountDataExporter` adds `server.json`. `DeleteAccountUseCase`
   calls `DELETE /v1/me`.
6. Update PRD 10.1. It names Supabase; the backend is Fly.io, Neon, and Firebase Auth.

## 9. Open items

| Item | Proposal |
|---|---|
| Delete and sign in again gives a new free credit | Accept for the MVP. Watch the count of deletions per Google account later |
| Many Google accounts can use the free tier and fill the app cost cap for everyone | Before public launch (not for the closed test): Play Integrity token on wallet creation and AI routes (PRD 10.1), and a per-user daily cost limit below the app cap |
| The $5 daily app cost cap stops every user at about the same time | Size it from the eval run cost before the closed test |
| Security logs kept for 1 year (PRD 10.4) | Backend compliance item: an audit log of sign-ins, purchases, and deletions with no personal content |
| Refunds and voided purchases | Poll the Play Voided Purchases API daily and take the credits back with a `REFUND` ledger row. After the first release |
| `POST /me/data-export` has no email provider | Blocked by apps-backend #99. The on-device interim stays until a provider exists |
| The compatibility fields of section 1 | Remove them in a follow-up after app issue #287 is deployed |
| Profile and application sync | After the MVP, as a separate contract |

## 8. Guest trial slice contract — `guest-trial-v1` (2026-10-17)

This section supersedes conflicting welcome-grant, provider, and GET behavior above. It is the
implementation contract for slice 01 only. The approved earn-credit plans replace the old one-credit
acquisition offer with an eligible two-credit guest trial. Google signup/link rewards and referrals
are later slices. Do not advertise or implement them as available here. Amounts, eligibility, and
availability come from server policy. A missing policy never means two free credits.

### 8.1 Authority, scope, and access

Every route below uses the existing app header, Firebase Bearer auth, app/environment database,
and live-account guard. `ANONYMOUS` means verified Firebase `sign_in_provider=anonymous`;
`GOOGLE` means verified `google.com`; other/missing provider claims mean `OTHER`. The backend
verifies audience/project and provider claims; a client flag, email, UID shape, or body cannot select
account mode. Preserve generic core behavior for apps that do not opt into this TMR policy.

| Route family | Anonymous | Google | Other/missing verified provider |
|---|---|---|---|
| Core `/v1/me`, deletion, TMR wallet/credits/export | Own data; zero balance is valid | Own data/history | Own reads and deletion; no new promotional entitlement |
| `GET /v1/tailormyresume/credits/policy` | Own current eligibility | Own current eligibility; guest offer ineligible | Mode `OTHER`; guest offer ineligible |
| `POST /v1/tailormyresume/credits/grant-challenges` | Guest-trial action | `403 GUEST_REQUIRED` | `403 GUEST_REQUIRED` |
| `POST /v1/tailormyresume/credits/initialize` | Evaluate trial or recover committed result | Recover own committed trial, otherwise `200 NOT_ELIGIBLE / ACCOUNT_MODE_INELIGIBLE` | Same recovery/ineligible rule |
| TMR parse/analyses/job-imports/tailorings and own tailoring recovery/reports | Available under existing quota/cost/balance rules | Same | `403 PROVIDER_NOT_SUPPORTED` for new AI/import work; own job recovery/report access remains |
| Generic `/v1/ai/extract` and `/v1/ai/chat` for TMR app id | `403 FORBIDDEN` before provider/reservation | Same | Same |
| Legacy unlock | No-op; no free grant/debit | Same | Same |
| Packs and purchase history | Read existing shapes | Read existing shapes | Read existing shapes |
| Legacy new purchase submission | `403 GOOGLE_REQUIRED`; no new welcome credit | Existing validation/settlement retained | `403 GOOGLE_REQUIRED` |
| Signup/link/referral mutation and public inviter lookup | No route added in slice 01 (`404 NOT_FOUND`) | Same | Same |

Blocking generic AI prevents bypassing TMR's typed request validation, allowances, credit flow, and
fabrication checks. Keep core quotas and app micro-USD cost caps in addition to credit admission;
a trial grant makes no AI call. This slice does not increase either existing AI limit. No Google
base-grant fallback may bypass the protected trial. Legacy purchase settlement/history is retained;
new payment retirement is a later slice.

`GET /wallet`, `GET /credits`, and `GET /me/export` never grant, top up, consume capacity, or insert
a ledger event. An absent wallet reads as a zero wallet; persistence of a zero wallet belongs to an
explicit mutation. `GET /v1/me` may still create the core user row, with no economic side effect.
The implicit `WELCOME` path must be removed from every wallet, export, unlock, purchase, and tailoring
call site before anonymous acquisition is enabled. A new grant is only an explicit protected POST.

### 8.2 Server policy snapshot

`GET /v1/tailormyresume/credits/policy` has no body. `200` returns `{policy: Policy}`. It is authenticated,
non-granting, and carries `Cache-Control: no-store`. It evaluates the caller's own known entitlement,
mode, and current configured campaign; it does not collect Android ID or call Play Integrity.

| Policy field | Type/rule |
|---|---|
| `version` | String `^[a-z0-9][a-z0-9._-]{0,63}$`, or `null` if no usable policy exists |
| `effectiveAt`, `expiresAt` | UTC ISO-8601 timestamps, or `null` with no usable policy; `expiresAt > effectiveAt` |
| `accountMode` | `ANONYMOUS`, `GOOGLE`, `OTHER` from verified server identity |
| `guestTrial.status` | `AVAILABLE`, `ALREADY_GRANTED`, `NOT_ELIGIBLE`, `UNAVAILABLE` |
| `guestTrial.reason` | `ELIGIBLE`, `ALREADY_GRANTED`, `LEGACY_ACCOUNT`, `ACCOUNT_MODE_INELIGIBLE`, `ADMISSIONS_DISABLED`, `POLICY_UNAVAILABLE`, `CAMPAIGN_EXHAUSTED` |
| `guestTrial.amount` | Positive integer from usable policy (2 in this campaign), or `null` without one; display an amount as an offer only when status is `AVAILABLE` |
| `guestTrial.operation` | `GUEST_TRIAL` |
| `guestTrial.integrity`, `guestTrial.continuity`, `guestTrial.bindingVersion` | `PLAY_INTEGRITY_STANDARD`, `ANDROID_ID`, integer `1` |
| `signupBonus`, `referrals` | Each exactly `{status:"UNAVAILABLE"}` in this slice |

Precedence: recorded committed trial → `ALREADY_GRANTED` (even with policy absent/paused); unsupported
mode → `NOT_ELIGIBLE/ACCOUNT_MODE_INELIGIBLE`; historical classification → `NOT_ELIGIBLE/LEGACY_ACCOUNT`;
missing/invalid/not-yet-effective/expired policy or missing platform/key/database configuration →
`UNAVAILABLE/POLICY_UNAVAILABLE`; disabled admission → `UNAVAILABLE/ADMISSIONS_DISABLED`; exhausted
capacity → `UNAVAILABLE/CAMPAIGN_EXHAUSTED`; otherwise `AVAILABLE/ELIGIBLE`. A committed grant does
not imply the original policy is still usable: nullable metadata stays null in that case.
Policy availability is a snapshot, never a reservation or evidence that a device is unused.

New admission defaults off. Activation requires a complete validated per-app/environment policy,
installed database revision, finite capacity, trusted ingress setup, continuity key ring, and real
integrity verifier/package/certificate/version configuration. Immutable entitlement identity is
independent of policy revision, policy expiry, and HMAC key version. A revision changes admission
rules for uncommitted requests; it does not create a second lifetime entitlement.

Android refreshes policy at entry/retry/resume and before a new challenge after expiry. It never
hardcodes eligibility, two-credit availability, reset timing, or a Google reward promise. On an
unavailable policy/503 it shows a neutral temporary-unavailable state, refreshes the wallet, and
keeps existing profile/application/balance access. No zero fallback may overwrite a previously
loaded wallet on a failed request. Scope responses/pending operations to UID, environment, and
session generation so sign-out/account changes cannot apply stale data.

### 8.3 Strict request and challenge shapes

Both POSTs reject unknown keys, missing/nonconforming fields, invalid JSON, and wrong content type
as `400 INVALID_INPUT`. Strings are not trimmed or case-normalized. Body limits apply to UTF-8
bytes: challenge 2,048; initialize 16,384. Larger bodies are `413 PAYLOAD_TOO_LARGE`.

Shared rules: `requestId` and `challengeId` are canonical lowercase UUID v4 strings;
`policyVersion` follows `Policy.version` without null; digest/hash/challenge values are exactly 43
base64url characters (`[A-Za-z0-9_-]`), no padding. `requestId` is generated once per business attempt
and persisted for retry across process restart. The caller never sends UID, amount, environment,
provider mode, eligibility time, or a client-side integrity verdict.

`POST /v1/tailormyresume/credits/grant-challenges`:

```json
{"operation":"GUEST_TRIAL","requestId":"11111111-1111-4111-8111-111111111111","policyVersion":"guest-local-v1","payloadDigest":"<43-character canonical business digest>"}
```

`201` returns exactly the required fields below (responses may gain fields):

```json
{"challengeId":"22222222-2222-4222-8222-222222222222","challenge":"<43-character server random challenge>","expiresAt":"2026-10-17T00:05:00Z","bindingVersion":1,"requestHash":"<43-character server-computed binding hash>"}
```

The server generates 32 cryptographically random challenge bytes. TTL is at most five minutes and
never beyond policy expiry. Each issuance creates a fresh handle (not an idempotent grant); rate
limit issuance. Bind the stored challenge to app/environment/live actor/action/requestId/payloadDigest/
policyVersion. Hash the bearer challenge secret at rest. The server returns its computed `requestHash`
so Android never guesses a deployment namespace. Android passes that exact string to Play Integrity's
Standard request. The server persists its expected `requestHash` at issuance against immutable authoritative binding
fields, plus the hash of the challenge secret. At initialization it compares the decoded token hash
to that persisted expected hash and rechecks actor/app/environment/action/key/payload/policy/expiry.
It does not need to reconstruct a discarded secret; initialize echoes only `challengeId`, not `challenge`.

`POST /v1/tailormyresume/credits/initialize`:

```json
{"requestId":"11111111-1111-4111-8111-111111111111","policyVersion":"guest-local-v1","continuity":{"type":"ANDROID_ID","value":"0123456789abcdef"},"challengeId":"22222222-2222-4222-8222-222222222222","integrityToken":"synthetic-token-not-valid-in-any-environment"}
```

`continuity` is a strict object with exactly `type:"ANDROID_ID"` and `value` matching
`^[0-9a-f]{16}$`, excluding `0000000000000000` and known unusable `9774d56d682e549c`. The client reads
its app-scoped Android ID only for an explicit admission attempt under the disclosed policy; missing
signal is not an empty/shared identity and cannot yield a new trial. `integrityToken` is an opaque
nonempty string of at most 12,000 characters. No request allows arbitrary evidence JSON or amount.

The Standard Integrity cloud project number comes from non-secret build configuration: release
`tailormyresumePlayIntegrityCloudProjectNumber`; debug/staging
`tailormyresumeStagingPlayIntegrityCloudProjectNumber`. Each is a positive decimal integer that fits
a signed 64-bit value, bound to its matching backend verifier project. Do not commit actual values.
A missing/invalid/mismatched client value closes admission with temporary integrity-unavailable
recovery; it cannot select production as a staging fallback. Android computes only the business
payloadDigest, and passes the exact server-returned requestHash to Standard Integrity.

### 8.4 Canonical digest and integrity binding version 1

The stable business payload is the compact UTF-8 JSON array (no whitespace, no final newline):

```json
["TMR_GUEST_TRIAL_PAYLOAD_V1","ANDROID_ID","0123456789abcdef"]
```

`payloadDigest = base64urlNoPadding(SHA256(bytes))`. Only the validated lower-case hex signal is
variable. No sorting, locale transforms, Unicode normalization, JSON object ordering, or platform
default charset may affect these bytes. Request IDs/actors/action are separately namespaced; the
stable digest excludes policyVersion, challenge, and token so fresh evidence and a new policy can
retry the same uncommitted business payload. Changing the signal with the same operation key is a
conflict. Never log this digest or raw signal.

The binding bytes are the same compact UTF-8 serialization of this ordered array:

```text
["TMR_PROMOTION_BINDING_V1",appId,environmentNamespace,authenticatedUid,"GUEST_TRIAL",requestId,payloadDigest,policyVersion,challengeId,challenge]
```

`requestHash = base64urlNoPadding(SHA256(bindingBytes))`. All array members are strings.
App id/environment/UID come from trusted server routing/auth/config, never request fields.
The decoded Standard Integrity token must match this hash, expected package, approved certificate
set/version policy, and freshness (within five minutes, never older than the issued challenge;
allow at most 30 seconds server-configured clock skew). Require `MEETS_DEVICE_INTEGRITY`; an enabled
Play-distributed policy also requires `PLAY_RECOGNIZED` and `LICENSED`. Do not require the strongest
optional verdict, and do not bypass baseline verification for sideload, debug, emulator, absent
license, unavailable service, or a client that says it is trusted. Missing real configuration is a
closed admission, not a fake success. Deterministic test/local verifier injection cannot activate a
real environment or be selected by an HTTP parameter. Integrity binds the reported Android ID;
it does not certify the ID's truth or prove a unique human.

Golden byte strings and positive/negative vectors are in
`docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/hash-vectors.json`.
`generate-fixtures.py --check` reproduces them with Python's UTF-8/SHA-256/base64url implementation.

### 8.5 Outcomes, status, and recovery

Initialize always returns `200` for a business outcome:

```json
{"wallet":{"credits":2,"freeCredits":2,"purchasedCredits":0,"analysesLeftToday":20,"freeTailoringsLeftToday":0,"day":"2026-10-17","resetsAt":"2026-10-17T18:30:00Z","unlockedApplicationIds":[]},"grants":[{"id":"33333333-3333-4333-8333-333333333333","kind":"GUEST_TRIAL","amount":2,"createdAt":"2026-10-17T00:00:00Z","policyVersion":"guest-local-v1"}],"trial":{"status":"GRANTED","reason":"ELIGIBLE"},"replayed":false}
```

| Field/outcome | Rule |
|---|---|
| `wallet` | Existing full Wallet shape; authoritative current counters, not a replay cache snapshot |
| `grants` | Array of 0 or 1 own trial receipt; UUID id, `kind:GUEST_TRIAL`, positive integer amount, UTC createdAt, original nonnull policyVersion |
| `GRANTED / ELIGIBLE` | One new grant and ledger event atomically committed; `replayed:false` |
| `ALREADY_GRANTED / ALREADY_GRANTED` | Return original grant with current wallet; no top-up, second ledger event, evidence decode, or capacity charge. `replayed:true` for a committed same-key operation; false for another key finding the same lifetime grant |
| `NOT_ELIGIBLE / SIGN_IN_REQUIRED` | Allocated continuity signal; empty grants, own wallet only, no other UID/grant/wallet information. Sign-in is another account path and promises no new base grant |
| `NOT_ELIGIBLE / LEGACY_ACCOUNT` | Historical account; empty grants; balance/history intact |
| `NOT_ELIGIBLE / ACCOUNT_MODE_INELIGIBLE` | Non-anonymous provider; empty grants; balance/history intact |

No refusal writes a reward grant or consumes campaign capacity. Persist bounded non-grant decisions
and attempts for abuse control, but do not permanently cache a temporary refusal as a successful
operation. A fresh challenge may retry the same uncommitted key/payload under current policy.

| Status/code | Meaning and client recovery |
|---|---|
| Existing `400 INVALID_INPUT`, `401 UNAUTHENTICATED/INVALID_TOKEN`, `403 CROSS_APP_TOKEN`, `404 NOT_FOUND`, `409 ACCOUNT_DELETED` | Existing semantics. Same-app other-owner resource is never disclosed. No retry can recreate a deleting account |
| `403 GUEST_REQUIRED` | Challenge requested with non-anonymous identity; refresh policy/account state |
| `403 PROVIDER_NOT_SUPPORTED`, `403 GOOGLE_REQUIRED` | Access matrix above; no provider/credit side effects |
| `409 IDEMPOTENCY_CONFLICT` | Same live actor/action/requestId with different stable payload. Preserve committed original; do not retry with changed payload/key to evade entitlement |
| `409 POLICY_CHANGED` | New/uncommitted request's policy version does not match current usable policy; fetch policy and obtain fresh challenge with same stable key/payload |
| `409 INTEGRITY_CHALLENGE_INVALID` | Expired, consumed, wrong-owner/action/app/environment/key/payload/policy challenge; obtain fresh challenge for same business attempt |
| `403 INTEGRITY_REJECTED` | Decoded baseline verdict/hash/package/certificate/time invalid; no automatic tight retry or accusation of fraud |
| `503 INTEGRITY_UNAVAILABLE` | Verification service timeout/unavailable; fresh evidence and same business key may retry later |
| `503 PROMOTION_UNAVAILABLE` | Missing/invalid/disabled/expired policy/config, unavailable revision/key, or exhausted campaign; preserve wallet, refresh policy later |
| `429 RATE_LIMITED` | Bounded velocity/load shedding; respect `Retry-After` seconds |
| `413 PAYLOAD_TOO_LARGE`, existing `500 INTERNAL_ERROR` | Existing envelopes and no partially committed award |

All errors retain `{error:{code,message}}`, never raw provider output, UID, thresholds, token, or signal.
429 and 503 carry positive integer `Retry-After` seconds; a missing known recovery time uses 60.
Responses carry `Cache-Control:no-store`. When policy is usable, version mismatch is `POLICY_CHANGED`;
without a usable policy it is `PROMOTION_UNAVAILABLE`. Neither blocks committed recovery.

Processing order: validate auth/app/live-user and strict body/size; derive stable digest; check
committed actor/action/key and payload conflict; return committed grant/current wallet before
current policy, expired evidence, admissions, capacity, or provider calls. Then check existing stable
lifetime grant (even under a new key); classify provider/legacy account; validate current policy
and challenge; verify evidence outside SQL locks; evaluate counters; transactionally recheck live
actor, policy, challenge, grant/signal uniqueness and capacity, then consume challenge + insert grant
+ increment freeCredits + ledger + operation receipt together. Same-key concurrent requests return
one grant. One full last award wins; no partial two-credit award. All transactions use a documented
common lock order and the existing live-user guard. A provider/sink/commit failure cannot leave
capacity, wallet, ledger, and grant disagreeing. Expired replay-cache rows cannot remove durable
lifetime grant uniqueness. No replay issues new credits after the original credits were spent.

### 8.6 Shared policy, abuse, and lifecycle foundation

Reusable policy/grant/capacity primitives are opt-in per app/environment, with a transaction-aware
app wallet sink. Core reward services have no TMR imports, Google/Android rules, or global wallet.
Use separate stable grant sources, durable atomic capacity, unique grant→ledger links, controlled
clock/randomness ports, and injected integrity provider. Never call a remote verifier in a SQL
transaction or apply a wallet write through a separate transaction/callback.

Limited Android-ID continuity uses purpose-separated HMACs containing trusted app/environment and
key version. Keep active and retiring digests queryable/locked together so key rotation does not
regrant. Allocation records expire at most 180 days after allocation while the account is live;
erase them on account deletion. An expired record cannot support an indefinite one-trial-per-device
promise. A missing signal or key ring fails new admission; it is not proof of a new device. Device
Recall remains disabled/optional and grants never depend on an unverified beta capability.

Independent durable counters cover actor, accepted continuity signal, short-window network, and
campaign load; no combined device/IP/model fingerprint. Count failed attempts in a bounded
independent transaction when award rollback would otherwise erase them. Network counters are
short-lived load shedding, never identity/duplicate proof; shared NAT or a matching model alone is
not fraud denial. Trust IP only from the configured ingress; reject spoofed proxy headers, normalize
IPv4-mapped IPv6 and /64 IPv6 windows, and omit unavailable addresses from the network dimension
rather than creating one permanent shared unknown identity.

Deterministic **local-test policy only**: lifetime capacity 100 credit units, trial amount 2;
actor 5 attempts/60 seconds; continuity signal 3/60 seconds; network 100/60 seconds; decisions 7 days;
challenge TTL 5 minutes; replay operation TTL 30 days; network counters purge within 1 hour.
Counter periods/expiry/key version are explicit server policy. Production thresholds/capacity are
unset and admission stays off. No monthly user/referral cap is invented. Retention windows are
maximums; account erasure removes attributable rows earlier. Campaign aggregate consumption has
no UID/signal edge and never decreases on account deletion or record expiry.

No raw Android ID/IP, attestation token, canonical bytes/digest, challenge secrets, provider outputs,
resume/JD, device name/model, installed-app list, location, contact, serial, IMEI, or advertising ID
is logged or stored as grant evidence. Store only minimized HMAC/key version, coarse decision reason,
policy revision and grant references within stated limits. HMACs remain personal/pseudonymous data.
Export own grant receipts/eligibility dates and existing full ledger, with no other-user data or
internal abuse hashes/secrets. Add an own `promotions:{grants:[GrantReceipt]}` object inside the
existing export object; no grant is created by export. Erase owned grant/operation/challenge/
decision/continuity/counter rows and wallet/ledger through `DELETE /v1/me`; recovery/partial-failure
and concurrent-grant guards apply. Preserve only nonidentifying consumed campaign totals and the
existing bounded deletion tombstone. Do not claim deletion-resistant abuse prevention or silently
retain an identity HMAC after deletion. Firebase automatic anonymous deletion cannot independently
remove identity while an active server wallet still needs lifecycle reconciliation.

Production disclosure/Terms/Data Safety review of this limited signal/purpose/retention and genuine
Firebase anonymous-provider, package/certificate and Play Integrity verification remain activation
checks. Local fake results are application-logic evidence only. No real admission is enabled by this
contract, by staging Firebase setup alone, or by missing external credentials.

### 8.7 Migration, balance, and slice acceptance

Existing WELCOME/PURCHASE/TAILORING/REFUND/MIGRATION rows, free/purchased balances, reservations,
purchase validation/settlement, and original ledger IDs/times remain unchanged. Do not backfill
credits again or infer a top-up from a historical numeric balance. Mark pre-rollout server identities/
wallets and trusted pre-cutoff Firebase identities as legacy before admissions activate. Unknown
creation/classification data is unavailable for new admission, never an eligible-new default.
No historical account receives the new trial automatically.

Add `GUEST_TRIAL` and unique nullable grant references additively; Google/referral ledger kinds are
reserved for their later slices. Credit history retains stable server IDs and renders a distinct
Guest trial label; an old lenient decoder may see UNKNOWN, so activation requires a compatible client.
Wallet's `freeCredits` now covers earned promotional credits plus historical free credits; spending
still takes a free credit before a purchased one. Successful tailoring consumes exactly one once;
failure/interruption releases its reservation without debit. Retry existing request/job before a
current zero-credit gate; current balance must not prevent recovery of a completed result.

The acceptance map/fixtures in `docs/testing/earn-credits/2026-10-17/01-guest-trial/` are normative
examples for both implementations. Required proof: fresh anonymous sign-in → protected eligible
+2 → authoritative wallet → two successful debits to zero; duplicate/lost-reply/restart/race gives
one grant; failure/interruption/repeated completed job never bills twice; third new successful work
cannot overdraw; reads/legacy routes cannot grant; wrong scope/provider/evidence fails; stale policy,
provider outage and campaign exhaustion preserve balance; key/policy changes cannot reset entitlement;
legacy wallets/history survive; account/environment switching rejects stale replies; export/deletion
cover every new row and non-TMR apps remain unchanged. No broad suite is required for this
contract-only commit; implementations run their repository gates and focused transaction tests.

## 9. Google activation and conflict recovery — google-activation-v1

Status: frozen for slice02 implementer agreement, 2026-10-17; independent rate-limit clarification
`google-activation-v1-rate-limit-1`. Both implementers must record
agreement before application implementation. This extends v2 + guest-trial-v1. Section 8 and its
canonical fixtures stay byte-for-byte unchanged. Production admissions remain off. Issues: backend #121; Android #403.

### 9.1 Trusted identity and immutable eligibility

All endpoints retain Firebase signature/audience verification, app/environment routing and the
live-user guard. Add validated provider subject and `auth_time` to trusted identity context;
Google requires verified `firebase.sign_in_provider="google.com"`, exactly one nonempty Google
subject in verified `firebase.identities["google.com"]`, and same-project Admin `getUser(uid)`
with exactly one matching `google.com` provider UID for new observations/preparation/admission.
Committed receipt recovery requires valid Google token/provider shape and live server account,
but deliberately makes no fresh Admin/Integrity call (9.5). Disabled/deleted users and missing/malformed/
contradictory evidence cannot activate. Email, client flags, claimed UID and unsigned JWT claims
are never identity keys. Bound Admin lookup to 10 seconds, outside SQL locks; recheck local live
state before commit. Token refresh does not prove a new signup or recent authentication.

Persist activation independently of grants/replay caches: own UID, Firebase incarnation/creation,
source (`GUEST_LINK`, `DIRECT_GOOGLE`, `EXISTING_GOOGLE`, `AMBIGUOUS`), source-event time, first
server-verified Google time, eligibility/reason, immutable referral deadline when configured,
original policy snapshot and a separately expiring purpose-scoped provider-subject HMAC alias. App/environment
and purpose `google-signup` separate subject keys. Active/retiring versions remain queryable and
locked together; losing a key closes admission rather than silently resetting uniqueness. No
raw subject survives request processing. Policy/key/login/request changes never reset a live
account's classification or entitlement. `users.created_at`, wallet creation, token `auth_time`
and first backend contact are not first Google activation.

A known guest prepares under anonymous auth before Firebase `linkWithCredential`. Admin confirms
that this UID has no Google provider, preventing stale anonymous tokens preparing an already
Google identity. Durable pre-link provenance is distinct from the short-lived bearer intent;
same-UID crash recovery must not depend on retention of that secret. Verify unchanged Firebase
incarnation and trusted Google subject before recovery. An old anonymous creation alone does not
make a genuinely observed guest-to-Google link an old Google account. Existing legacy/ambiguous
Google classification cannot be overwritten by another intent/key/policy. A retained subject claim for another UID denies a fresh signup as
EXISTING_ACCOUNT without disclosing that UID; it is never permission to read/merge the other wallet.

Direct-new is an app-account rule, not Google/Gmail account age. Sufficient positive evidence is
trusted Firebase app-account creation at/after the configured cutoff and first server Google
observation within the configured new-account admission interval; current verified token/Admin
matching Google subject; and no contradictory durable prior activation/prelink/legacy history or
still-retained subject grant. A brief never-observed anonymous phase within this bounded interval
is allowed; Firebase cannot prove never-anonymous provenance and this contract does not demand it.
Old/out-of-window or contradictory/unprovable history is conservatively EXISTING/AMBIGUOUS. Missing
trusted metadata is temporary IDENTITY_UNAVAILABLE, not eligible-new. Existing durable eligibility
survives that initial interval for later evidence/admission retries, using the original anchor.
No legacy bonus is authorized; new key/token/policy cannot reset old Google classification.

Preparation uses one current unused attempt per live UID/incarnation. A new prepare may supersede
it only after fresh anonymous auth + authoritative same-incarnation Google-absent lookup + no
recorded Google observation/activation. Cancellation/expiry alone cannot renew an anchor. The
same-key create replay recovers its original intent, never renews it; renewing needs a new key
and supersedes the previous secret. An unused canceled prompt weeks earlier does not permanently
anchor a future link. If prepare discovers Google, reconcile/freeze the prior attempt rather
than issue a new intent; return ACCOUNT_LINK_CONFLICT after recording observation. Recheck under
the user lock so concurrent renewal cannot replace an already observed transition. Bound lookup
freshness to the configured interval; never accept a stale snapshot over newer durable state.

For GUEST_LINK, eligibilityAnchorAt is the qualifying attempt's server prepare time, a conservative
lower bound on the link, **not a provider link timestamp**. For DIRECT_GOOGLE it is authoritative
Firebase creation. firstGoogleObservedAt is separately the first server verification of Google.
Freeze anchor/provenance/window/deadline on that observation; delayed completion or token refresh
cannot extend it. A long prepare-to-link interval can shorten referral time. Referral expiry alone
does not cancel a provable pending signup bonus. Missing live cutoff/window/classification policy
still records first observation and provenance through the non-granting observation POST; later
configuration cannot rewrite an observed returning/ambiguous account as new. A pending provable
transition retains its original facts while the account lives, even if grants are paused.

Unobserved pre-link evidence has finite recovery expiry (fixture 30 days). Missing/expired bearer
with valid durable evidence can recover same UID; if durable evidence also expired/erased, return
explicit conservative AMBIGUOUS_HISTORY/RECOVERY_EXPIRED without inventing a direct-new path.
Minimal live-account entitlement/classification/observed-pending facts survive alias/cache expiry
until erasure. Subject HMAC aliases expire from first claim, not last login (fixture 180 days),
and do not renew silently. Same-account regrant remains impossible after alias expiry; cross-UID
suppression then cannot be promised. Post-deletion alias retention is **zero**; deletion/recreation
may qualify again and consumed campaign capacity remains consumed. No permanent per-human or
per-Google prevention claim. Key rotation preserves original alias expiry; missing retained key
closes new admission. Fixture-only secret TTL 5 minutes, operation/prelink cache 30 days, alias
180 days, referral window 24 hours; production values remain explicitly unset.
No device-level signup cap is invented: a reused trial signal may deny base while leaving an
otherwise eligible signup bonus. Referrals remain unimplemented; only activation/deadline
foundations belong here. Own reads, spending and work survive promotional refusal/unavailability.

### 9.2 Coherent policy and offer preconditions

GET `/credits/policy` stays non-granting. Top-level `version/effectiveAt/expiresAt` and `guestTrial`
retain section 8 meanings. Extend `signupBonus` additively with these fields; `referrals` stays
exactly `{status:"UNAVAILABLE"}`. No Android amount/security/clock defaults are allowed.

| Field | Rule |
|---|---|
| `status` | AVAILABLE, ALREADY_GRANTED, NOT_ELIGIBLE, UNAVAILABLE |
| `reason` | ELIGIBLE, ALREADY_GRANTED, EXISTING_ACCOUNT, AMBIGUOUS_HISTORY, RECOVERY_EXPIRED, ACCOUNT_MODE_INELIGIBLE, IDENTITY_UNAVAILABLE, POLICY_UNAVAILABLE, ADMISSIONS_DISABLED, CAMPAIGN_EXHAUSTED |
| `amount` | Positive configured integer or null; offer only when AVAILABLE |
| `version`, `effectiveAt`, `expiresAt` | Current signup revision/bounds, or null |
| `grantPolicyVersion` | Original own committed signup receipt revision, or null; used for base-only retry |
| `operation`, `bindingVersion`, `integrity` | GOOGLE_ACTIVATION, 1, PLAY_INTEGRITY_STANDARD |
| `referralWindowSeconds` | Positive configured integer or null; no attachment permission yet |
| `baseTrial` | `{status,reason,amount,version,grantPolicyVersion}` with section 8 statuses/reasons and nullable policy metadata; can be AVAILABLE for provably new direct Google signup; never for guest link |

Precedence: committed own signup receipt; durable ineligibility; unsupported mode; missing trusted
identity/policy/config; disabled signup admission; exhausted signup capacity; eligible. Anonymous
AVAILABLE is conditional on a new credential linking the same UID, never a conflict bonus promise.
Policy reads collect no Android ID, reserve nothing and do not guarantee device eligibility.

Preserve existing guest internal scope compact JSON `[appId,environment]` and entitlement
`guest-trial` exactly. Signup policy/capacity uses additive scope `[appId,environment,"google-signup"]`
and entitlement `google-signup`; never overwrite the one existing active trial campaign. Resolver
may share generic primitives but cannot silently change guest policy schema/storage/revision rules.
Lock applicable promotion scopes in lexical order after live-user locks; alias/allocation locks
follow scopes in lexical order. Policy publishers acquire only their scope lock and no user lock.

Both grants resolve one coherent current policy snapshot and transactionally recheck its revision
pair. The validated acquisition-classification configuration (cutoff/new-account interval/window and
finite evidence/alias retention) is a shared prerequisite, independent of either grant campaign.
Its immutable classification snapshot is recorded at observation; missing classification config
closes both new grants while preserving first observed facts. Signup revision owns signup amount/
capacity/admission; optional base names trial amount/capacity/admission. A paused or absent signup
campaign is not missing classification config. Sources/outcomes remain separate. Used signal, legacy base refusal, absent/paused/
exhausted base policy cannot silently refuse eligible +5. There is no +2 fallback for unavailable
base. The same independence applies to each source's bounded attempt admission (9.5).
Symmetrically, available protected base may grant when signup campaign is absent/paused/
exhausted: return base GRANTED and signup UNAVAILABLE, preserving eligible pending signup.
If a requested revision for an uncommitted available subaction differs from its usable current
revision (including null when now available), return 409 POLICY_CHANGED before **any new award**.
Unavailable campaign has null revision if absent, or its acknowledged version if present/paused;
never guess amount. A committed subaction binds its original receipt revision and bypasses its
current policy, leaving only new work subject to current rules. Return 200 when at least one new
requested award commits, or for durable business refusal/complete requested receipt recovery. Use 429
RATE_LIMITED under 9.5 when no new award commits and a requested new source is rate-blocked. Use 503
PROMOTION_UNAVAILABLE when no requested new award can proceed solely because required campaigns
are temporarily unavailable, or shared classification/security prerequisites are missing. Refresh/reconfirm revised offers and obtain fresh evidence with the same
stable business key. A committed replay returns original receipts/current wallet before current
policy, pause, expiry, capacity or provider calls. Never combine an offered signup amount with an
unacknowledged new base amount. Missing base config yields a separate unavailable sub-outcome.

### 9.3 Strict endpoints and schemas

Paths below are relative to `/v1/tailormyresume`. All bodies are strict JSON: no unknown fields,
trimming/coercion, client UID/amount/environment/eligibility time/email/provider subject/arbitrary
verdict. UUID v4, version, continuity and token rules follow 8.3. Proof is exactly 32 random bytes,
43 base64url characters, generated/persisted privately by Android **before** create. Server stores
only a purpose-separated proof hash; a lost create reply retries without plaintext recovery.
Never put proof in a URL/query/log. Cache-Control:no-store applies to every response.

| Endpoint | Auth | UTF-8 limit | Strict body | Success |
|---|---|---|---|---|
| POST `/account/upgrade-intents` | Anonymous + Admin confirms unlinked | 2048 | `{requestId,proof}` | 201 new / 200 same key+proof: `{intentId,createdAt,expiresAt,state:"PREPARED"}` |
| POST `/account/upgrade-intents/status` | Source anonymous/same-UID Google, or already bound destination Google | 2048 | `{intentId,proof}` | 200 `{intent:{id,state,createdAt,expiresAt},completion}` |
| POST `/account/activation-challenges` | Google | 2048 | `{operation:"GOOGLE_ACTIVATION",requestId,policyVersion,trialPolicyVersion,payloadDigest}` | 201 section 8 challenge response |
| POST `/account/activate` | Google | 16384 | Activation union below | 200 outcome below |
| POST `/account/activation-observations` | Google | 2048 | `{requestId}` | 200 `{wallet,activation,grants,referralEligibility}`; records first observation/classification, never awards |
| GET `/account/activation` | Google | no body/query | None | 200 `{wallet,activation,grants,referralEligibility}`; no new fact/grant |
| POST `/account/upgrade-intents/complete` | Fresh destination Google | 2048 | `{requestId,intentId,proof,disposition:"PRESERVE_LOCAL_WORK"}` | 200 completion below |

`intentId` is canonical UUID v4; `state` PREPARED, LINKED, COMPLETED, EXPIRED. LINKED requires
server verification, never a client assertion. `completion` is null, `{kind:"SAME_UID_ACTIVATION"}`
or `{kind:"EXISTING_ACCOUNT",disposition:"PRESERVE_LOCAL_WORK"}`. PREPARED means no verified Google
transition/completion; LINKED means own same-UID Google was observed but grant finalization is pending;
COMPLETED is committed same-UID activation or conflict completion. EXPIRED applies only to an
unobserved/uncompleted expired bearer; it does not erase a valid durable same-UID recovery fact.
Status can read a retained proof hash after bearer expiry but cannot authorize new cross-UID use.
The create replay always returns its original create snapshot; status supplies current state.
Source status discloses no
counterparty UID. Status never binds an unbound destination or creates activation/grants. Random,
wrong-proof, other-owner/scope intent always returns 404 NOT_FOUND without private state.

Activate requires all keys, including explicit null:

```json
{"requestId":"11111111-1111-4111-8111-111111111111","policyVersion":"signup-local-v1","trialPolicyVersion":null,"path":"GUEST_LINK","upgradeIntent":{"intentId":"44444444-4444-4444-8444-444444444444","proof":"<43-character private proof>"},"continuity":null,"challengeId":"22222222-2222-4222-8222-222222222222","integrityToken":"synthetic-token-not-valid-in-any-environment"}
```

GUEST_LINK: upgradeIntent exactly `{intentId,proof}` or null for independently proven durable
same-UID recovery; continuity/trialPolicyVersion null. Never refill an absent/spent/denied guest
base. DIRECT_GOOGLE: upgradeIntent null; continuity valid 8.3 object or null; trialPolicyVersion
current version, original own base receipt revision for recovery, or null. policyVersion is also
nullable: current signup version, original own signup receipt revision, or null if signup campaign
is absent. GUEST_LINK with no usable signup admission cannot award and uses normal 503 recovery;
DIRECT_GOOGLE may still admit a separately protected base. Null trial revision with continuity
explicitly acknowledges an unavailable base policy; it cannot waive a now-usable trial revision: return POLICY_CHANGED if a usable revision
now exists. Nonnull trial revision requires continuity. Valid continuity with null
trial revision means unavailable base, never a guessed award. Null continuity means
NOT_ATTEMPTED/EVIDENCE_NOT_PROVIDED. Separate signup identity and Integrity rules always apply.
The same key may not change the business path/intent/signal to evade a refusal.

Challenge's nullable policyVersion and trialPolicyVersion must match activate exactly. Issuance checks Google mode
and policy revisions but need not know the digest's private preimage. Activate recomputes digest
and checks stored actor/action/key/digest/revision pair/expiry. Reuse 8.3–8.4 TTL/randomness/hashed
challenge, returned requestHash and strict package/certificate/licensing/device/freshness checks.
One activation challenge authorizes its direct base sub-attempt; reuse the stable guest-trial
entitlement/allocation/counters, never a second Google base source. No HTTP fake-verifier selector.

### 9.4 Canonical action bytes

Compact UTF-8 JSON arrays, no whitespace/newline; SHA256 → unpadded base64url. All business array
members are strings; empty string represents null:

```text
["TMR_GOOGLE_ACTIVATION_PAYLOAD_V1",path,intentIdOrEmpty,requestedActions,"ANDROID_ID"OrEmpty,androidIdOrEmpty]
```

requestedActions is exactly SIGNUP_AND_BASE when continuity is nonnull, otherwise SIGNUP_ONLY.
It is derived and included in canonical bytes; do not accept a separate client flag. A signup-only
challenge can never authorize adding base. The direct-base action recovers any committed signup
receipt rather than reissuing it. Proof/revisions/challenge/token/display data are excluded. Proof is separately verified in constant
time. Changed path/intent/requested set/signal with actor/action/requestId is IDEMPOTENCY_CONFLICT. Fresh evidence
and revisions may retry an uncommitted stable payload. Persist only a purpose-separated operation
fingerprint, never raw bytes/digest/signal. For durable missing-secret recovery, persist the
original intent ID separately on Android; a null upgradeIntent uses the canonical empty ID and
cannot silently change a previously prepared business key. A new recovery key cannot duplicate
the durable UID/subject entitlement.

Integrity binding is exactly 8.4's ordered outer array with action GOOGLE_ACTIVATION. Its revision
slot is the compact JSON **string** `[signupPolicyVersion,trialPolicyVersion]` (inner JSON null
for an absent campaign/no base request). All outer members remain strings. Store both parsed revision preconditions
with the challenge. Android uses returned requestHash; no deployment namespace guess.

Proof hash is SHA256/base64url of compact UTF-8
`["TMR_UPGRADE_PROOF_V1",appId,environmentNamespace,proof]`. Intent create stable bytes are
`["TMR_UPGRADE_INTENT_V1",proofHash]`. Conflict completion bytes are
`["TMR_EXISTING_ACCOUNT_HANDOFF_V1",intentId,"PRESERVE_LOCAL_WORK"]`; actor/action/key and
server-resolved guest/proof are bound separately. Another destination cannot replay completion.
Positive/negative canonical vectors are in this slice's fixtures.

### 9.5 Outcomes, atomicity and replay

Activate returns 200 for business classifications:

```text
{wallet,activation,grants,signupBonus,baseTrial,referralEligibility,replayed}
activation = null | {source,eligibility,reason,eligibilityAnchorAt,firstGoogleObservedAt,referralDeadline}
signupBonus = {status,reason}; baseTrial = {status,reason}
referralEligibility = {status,deadline}
```

Activation eligibility: ELIGIBLE, EXISTING, AMBIGUOUS; reason ELIGIBLE, EXISTING_ACCOUNT,
AMBIGUOUS_HISTORY, RECOVERY_EXPIRED. UTC dates; deadline nullable. Grants are 0–2 own 8.5 receipts, kind GUEST_TRIAL
or GOOGLE_SIGNUP with original amount/time/revision. POST grants contains receipts of its evaluated entitlements
(guest link: signup only; direct: signup and any own admitted/evaluated base). GET includes all own
trial/signup receipts. Current wallet, never cached pre-spend
wallet. GET returns durable fact/receipts or null and never classifies/grants as a side effect.
The observation POST is idempotent by UID/incarnation and requestId, verifies trusted provider
metadata outside SQL locks, records earliest observation even with missing grant policy and returns
no credits. If policy cannot classify, retain immutable observed facts/provenance and an unavailable
eligibility/deadline, not an eligible-new default. Later config may resolve preserved provable
prelink/new evidence once; it cannot reset dates or convert recorded existing/ambiguous history.
Android calls it after Firebase sign-in/link and on recovery before requesting a new grant offer.
It requires no Integrity or grant policy, since it never awards. On identity lookup outage return
503 IDENTITY_UNAVAILABLE without guessed facts. Add UNAVAILABLE eligibility/reason POLICY_UNAVAILABLE
to the activation shape when only immutable observation is known. RequestId conflicts cannot
change provenance; repeated calls preserve firstGoogleObservedAt.

| Outcome | status / reason |
|---|---|
| New signup/base | GRANTED / ELIGIBLE |
| Existing own receipt | ALREADY_GRANTED / ALREADY_GRANTED |
| Returning/prelaunch signup | NOT_ELIGIBLE / EXISTING_ACCOUNT |
| Unprovable history / expired durable recovery | NOT_ELIGIBLE / AMBIGUOUS_HISTORY, RECOVERY_EXPIRED |
| Base signal allocated elsewhere | NOT_ELIGIBLE / SIGN_IN_REQUIRED; no old owner data |
| Unusable signup or base campaign | UNAVAILABLE / POLICY_UNAVAILABLE, ADMISSIONS_DISABLED, CAMPAIGN_EXHAUSTED |
| Independently rate-limited new signup or direct base | UNAVAILABLE / RATE_LIMITED (activate outcome only; policy GET unchanged) |
| No base / linked guest | NOT_ATTEMPTED / EVIDENCE_NOT_PROVIDED, GUEST_LINK |
| Historical direct base refusal | NOT_ELIGIBLE / LEGACY_ACCOUNT |

Referral status ELIGIBLE, EXPIRED, NOT_ELIGIBLE, UNAVAILABLE with durable deadline or null is a
foundation only; no referral code/attribution/reward endpoint appears here. Later policy cannot
reset the deadline. Pending referral text is never sent to activation or allowed to undo signup.

Atomically commit activation, new signup and admitted direct base, challenge consumption,
receipts, free-credit increments, unique grant-linked ledger rows, operation and capacity. Base
refusal leaves eligible +5 intact; SQL/provider failure leaves no partial new economic writes. The separate no-grant observation
fact remains durable when later admission fails; no failed economic transaction erases it. Each last
capacity unit admits one full configured award; no partial amounts. Durable ineligible fact
prevents later key/policy/auth reclassification. Do not cache temporary unavailability as success;
retry retains original qualifying event/deadline even if bonus admission occurs later.

For uncommitted requested sources, evaluate independent bounded attempt admission: signup uses
its actor/network/campaign counters, with no device cap; direct base reuses existing trial
actor/continuity/network/campaign counters and scope. A source's limit blocks only that source:
base rate limiting cannot abort eligible signup, and signup rate limiting cannot abort eligible
base. Count refused attempts in bounded independent transactions as 8.6 requires; no new key,
policy, challenge or alternate path resets those counters or changes recorded continuity.
Never treat network load shedding as duplicate-person proof. Common ingress/whole-action and
challenge-issuance limits remain whole-operation 429; shared auth/live-user/classification,
identity, challenge/binding, revision and Integrity prerequisites remain mandatory for both.
A partial source result cannot mask a common security failure, provider outage or SQL failure.

When at least one new award commits and another requested source is rate-limited, return 200
with that source UNAVAILABLE/RATE_LIMITED and a positive integer Retry-After header in seconds
(60 if unknown; the maximum advised wait if multiple source limits apply). The JSON outcome
shape is unchanged. Persist the partial operation's immutable sub-outcome/receipt snapshot;
same-key replay returns it with current wallet, bypassing fresh counters/provider calls, and
still carries a positive Retry-After if its snapshot contains RATE_LIMITED (60 if unknown).
Android retains the retry deadline and waits before an explicit new-key/fresh-challenge attempt
for only the missing grant; policy refresh is not evidence that a counter reset.

If no new award commits and at least one otherwise eligible requested new source is rate-blocked,
return whole-operation 429 RATE_LIMITED with positive Retry-After, including when its counterpart
is unavailable, ineligible or already granted. Do not consume the challenge, allocate a signal,
charge capacity, create a grant/ledger/wallet increment or commit a successful operation snapshot.
Retain independent bounded attempts and already durable observation/classification/continuity
facts. Retry the same stable key/payload with fresh evidence after the delay. Recovering a
counterpart receipt alone cannot freeze unresolved rate-blocked new work as a 200 partial success.
Complete requested receipt recovery and durable all-ineligible classifications remain 200;
campaign-only temporary unavailability remains 503 under 9.2. Previously committed receipts,
balances and eligibility anchor/deadline remain intact in every case.

Order: auth/app/live + strict body → stable fingerprint/committed recovery → existing lifetime
receipt/fact → current revision snapshot → bounded Admin/Integrity outside SQL locks → common
live-user/promotion/subject/allocation/capacity locks → recheck revisions/live state/uniqueness →
commit. Committed same-key returns replayed:true; another key finding durable receipts false.
A committed operation returns its immutable base/signup sub-outcome snapshot and original receipt
references; it never reruns a previously UNAVAILABLE subaction under the same key. Explicit new
key + fresh challenge may retry only missing subgrants under the same immutable eligible
DIRECT_GOOGLE fact. If signup originally omitted base continuity, its first later explicit base
attempt may establish it once; after that, changing signal cannot evade allocation refusal.
Each revision-pair member is the original own receipt revision for a committed source, current
acknowledged revision for available new work, or null/versioned unavailable campaign as above.
Thus [original signup revision,current base revision] enables base retry while signup is paused;
[current signup revision,original base revision] enables signup retry while base is paused.
Recover existing receipt first and apply current policy/evidence/capacity only to uncommitted
work; SQL rollback affects only new writes, never historical grants. Neither direction resets
anchor/deadline, refills GUEST_LINK, changes recorded continuity, or reopens durable account-level
legacy/ineligible classification. Already complete requested actions recover before provider calls.
Temporary UNAVAILABLE is an attempt outcome, not permanent loss of the independent entitlement.
Replay precedes current policy/evidence/provider calls but still requires valid auth/live account,
strict body and matching stable fingerprint. Operation-cache expiry cannot erase entitlement. Exact non-grant per-attempt snapshots are
recoverable only within the finite operation cache. After expiry, reconcile durable original
receipts/current wallet and immutable classification; do not claim byte-identical expired refusal
replay or retain arbitrary failed operations forever. Durable receipt recovery uses ALREADY_GRANTED
for granted sources and current read-only eligibility for uncommitted sources, without new award.
Receipt recovery never restores spent credits or recreates a deleted account.

### 9.6 Credential conflict and local preservation

Credential-already-in-use is a distinct Firebase result. Show explicit choice before switching:
keep guest intact, or continue to the existing Google account preserving selected local work.
Explain guest promotional credits stay with guest and this switch earns no new signup bonus.
Prepare intent and durable private local snapshot before changing Firebase identity. Persist no
Google credential/token. No resume/JD upload or server content merge is implied.

Completion requires Google subject verification and auth_time within configured fresh-auth bound
(fixture 5 minutes), and no earlier than intent creation minus at most 30-second skew. Reauthenticate;
token refresh alone does not satisfy this. Resolve source only from capability, destination only
from token. Lock both accounts in **sorted UID order**, then intent/promotion state consistently.
Same UID is a link, not conflict. Both must be live; Admin must still confirm source unlinked to
Google, so a completed same-UID link cannot become cross-UID handoff. Bind first completion to
this destination/key/payload. Consume capability for new actions; same-destination matching replay
returns committed completion/current destination wallet before secret expiry/fresh-auth/provider
calls, while still validating hash, auth/live accounts and destination binding. Another destination
gets non-enumerating 404. No body UID, email/device match or guest UID possession is authority.

```text
{wallet,completion:{kind:"EXISTING_ACCOUNT",disposition:"PRESERVE_LOCAL_WORK",
 creditsTransferred:0,purchasesTransferred:false,runningJobsTransferred:false},replayed}
```

No guest free/purchased credit, grant, purchase, ledger, unlock, server report or server job moves.
No destination work overwrite, auto deletion or guest abandonment. Existing destination receives
no new bonus/invitee status from completion; retain any prior immutable destination fact/grant.
An unclassified destination becomes EXISTING_GOOGLE, preventing later direct-signup bypass.
Deletion of either required account revokes incomplete/completed attributable capabilities.
Denied completion preserves the snapshot and both accounts.

Local snapshot includes profile/contact/summary/skills and source/confirmation facts; entries/
bullets/order; applications with JD/requirements/notes/status/dates/gap analysis/quick answers/
coverage/completed tailored text/accepted and undone edits/acceptance time; kept JD/onboarding;
and readable local export files. Same-UID link preserves every local ID/file/pending job pointer.
Cross-UID import occurs only after server completion, only for selected content and bound destination.
Explicitly choose profile if destination has one; never overwrite silently. Allocate fresh local
application IDs/remap references on collision. Keep completed local work/acceptance; detach source
remote request/job/ledger/billing/report pointers and capabilities. Never poll/resend guest jobs
under destination auth. Keep unfinished job input as local work; a new charged action as destination
needs an explicit start. Retain exports privately or regenerate from completed content without
exposing unrelated-account files. No entire-state-store blind copy, wallet copy or server ID rewrite. Cross-UID detachment includes
generationId in gap analysis, tailored bullets/preparation/report state and PendingTailoringIds;
content, decisions and acceptance remain intact. Before either identity transition, durably save
unsaved JobDraftStore/ResumeImportDraft/current paste/manual-profile/editor inputs, or prevent
entry until that save barrier succeeds. Fence late source writers while snapshotting/importing.

Persist selection/snapshot/phase before cleanup. Recovery journal + atomic Room import + idempotent
file staging + unique handoff ID must survive death before/after switch, completion, import and
cleanup without data loss/duplication. The snapshot is visible only to verified source/bound
destination, never later account C. Cancel old requests, clear account-scoped wallet/ledger/remote
job state, enforce UID+environment+session-generation guards, and do not run blanket
wipeKeepingOnboardingInput/export deletion until durable recovery is safe. Pending deletion/wipe
wins. Bounded local retention must be explicit/disclosed before activation; no silent timed data
loss while UI still promises recovery.

### 9.7 Stable errors, lifecycle and implementation gates

Existing 8.5 errors retain semantics/envelope; 429/503 include positive Retry-After (60 if unknown).
No raw provider output/hash/subject/proof/counterparty UID in response/logs. New operations check
Admin incarnation; committed replay deliberately trusts authenticated live server account and
performs no Admin lookup. Normal account erasure revokes all state. Privileged out-of-band Firebase
same-UID reuse without server lifecycle reconciliation cannot be detected from token claims alone
and is outside this recovery guarantee; it must not be presented as proven safe by fixture replay.

| HTTP / code | Meaning/recovery |
|---|---|
| 403 GUEST_REQUIRED / GOOGLE_REQUIRED | Required trusted provider; no mutation |
| 403 GOOGLE_IDENTITY_REJECTED | Missing/contradictory Google provider/subject evidence |
| 403 RECENT_GOOGLE_AUTH_REQUIRED | Reauthenticate for uncommitted conflict completion |
| 409 ACCOUNT_LINK_CONFLICT | Incompatible same/different UID transition or consumed action |
| 409 UPGRADE_INTENT_EXPIRED | Valid owned proof expired for uncommitted cross-UID completion |
| 409 UPGRADE_INTENT_REQUIRED | No acceptable durable pre-link provenance for claimed guest path |
| 503 IDENTITY_UNAVAILABLE | Trusted metadata unavailable/indeterminate; retry original key |
| 409 POLICY_CHANGED / IDEMPOTENCY_CONFLICT / INTEGRITY_CHALLENGE_INVALID | Refresh offer / retain payload / fresh challenge per 8.5 |
| 403 INTEGRITY_REJECTED / 503 INTEGRITY_UNAVAILABLE / PROMOTION_UNAVAILABLE | No new award; retain wallet/work |
| 404 NOT_FOUND | Unknown/wrong-proof/other-owner/scope; no enumeration |
| 409 ACCOUNT_DELETED | Either required account deleting/deleted; never revive |

Do not embed provider HMAC, raw subject or deleted UID into an opaque surviving campaign/grant
source. Per-account lifetime entitlement belongs to the deletable own grant/fact; finite cross-UID
suppression belongs to the separately expiring alias. Export may include understandable purpose/
evidence category/expiry, never alias values or risk thresholds.

Add GOOGLE_SIGNUP ledger kind and linkage additively. Preserve historical WELCOME/PURCHASE/
TAILORING/REFUND/MIGRATION, balances/reservations/timestamps/settlement; no welcome backfill rerun,
numeric balance classification, destructive reset or automatic legacy grant. Resumable inventory/
classification of pre-rollout Google accounts precedes activation; unknown history stays closed.
Android explicitly renders GOOGLE_SIGNUP history before enabling offers.

DELETE /v1/me erases activation/subject/intent/proof/operation/decision rows with partial-failure,
retry and in-flight-finalization coverage. Handoff records link both parties: either deletion
removes/revokes the edge, never deletes the other account. Preserve only nonidentifying consumed
capacity as 8.6. Existing export.promotions gains `activation` (fact object or null) including own
source/eligibility/reason/eligibilityAnchorAt/firstGoogleObservedAt/referralDeadline; omit counterparty UID, subject/device hashes,
proof and internal decisions. Read/export never awards or creates an activation fact.

Only deterministic test config enables fixture admission. Production cutoff, finite capacities,
amounts, ingress, verifier/project/package/certs, freshness/window/retention/key rotation and
migration revision need explicit validated values. Missing required config closes new grants.
No live settings change here. Backend owner must propose a narrow constitution rule-3 exception
for capability-resolved source locks under token-derived destination auth, same app/environment,
sorted locks, no arbitrary source reads or content/economic transfer. This contract describes
the proposal; it does not silently amend governing backend documents.

## 10. Manual referrals — manual-referrals-v1

Status: frozen contract manual-referrals-v1, 2026-10-17. Both implementation writers must record
agreement on the final file hash before application implementation. This section extends sections 8/9; their bytes, grants, canonical
vectors and security/recovery rules remain unchanged. Admissions remain off. The observable
journey is a qualified Google inviter issuing a code, a newly activated eligible Google invitee
attaching it manually once, and the inviter seeing the complete configured reward in wallet/history.
The invitee receives no referral credit. No tailoring, upload, AI, payment or worker triggers this award.

### 10.1 Ownership and foundation mapping

Use existing `core/promotions/{schema,service}.ts` for immutable policies, aggregate capacity,
challenge/operation/counter storage and transaction-aware grants; `core/accounts/service.ts`
`withLiveUsers` for sorted locks; TMR `activation-{schema,state,security}.ts` for the existing first
Google fact/provider/incarnation; and TMR wallet/ledger/export adapters. Add a generic opt-in
`core/referrals` service/schema. Core imports no TMR code and takes injected qualification,
evidence and transaction-backed reward ports. Another app may use a non-Google event and another
amount/sink in an isolated test composition; it is not registered or enabled in production.

Existing promotion grants are unique on `(scope,userId,entitlement)`. The fixed trial/signup
entitlements cannot represent several inviter rewards. Extend the generic commit seam additively
to accept a server-derived stable entitlement per referral, such as `referral:<random receipt UUID>`.
Its unique source is the durable attachment receipt, independent of policy revision and request key.
Never put a UID, provider subject, code, HMAC or their derivation in a surviving source string.
Do not change existing guest-trial/google-signup scope or uniqueness. The sink receives the same
SQL transaction and cannot open/commit another transaction, call a provider or switch databases.
It adds free credits and exactly one unique grant-linked REFERRAL_REWARD ledger entry.
Existing commitPromotionGrant also consumes a challenge using the beneficiary user ID and checks
lifetime promotion signal allocation. For referrals, the invitee owns the challenge and the inviter
owns the grant. Extend the generic seam to keep those identities distinct; do not consume an
inviter challenge or pass referral velocity signals into lifetime trial-allocation uniqueness.
Consume the invitee challenge exactly once in the attachment transaction; velocity remains separate.

TMR adapter qualification uses the section 9 activation row and its original `referralDeadline`.
Do not add a second first-activation table, classify from wallet values, reset the anchor on login,
reinstall or policy publication, or require a signup grant to have succeeded. An existing Google
account with a recorded activation/classification may invite but cannot become an eligible new
invitee. If no activation fact exists, Android reconciles with section 9 observation; referral GET
never creates/classifies a fact. Guests may keep a pending candidate; they cannot issue/attach.

New code issuance and uncommitted attachment require section 9 verified Google token shape,
matching Admin provider subject/incarnation, timeout <=10s outside SQL locks, and metadata snapshot
age <= the existing configured <=30s bound at commit. Token refresh alone is sufficient for token
freshness; no new recent-authentication ceremony is imposed. Committed own recovery requires
valid Google token/provider shape and live actor, with no new Admin or Integrity call.

### 10.2 Routes, bounds and response shapes

All paths below are under `/v1/tailormyresume`. Firebase auth, `X-App-Id`, configured deployment
environment, prefix isolation and live-account guards apply. No user ID is accepted anywhere.
JSON objects are strict and bodies bounded before parsing; GET accepts no query parameters/body.
All responses carry `Cache-Control: no-store`. UUID/version/digest constraints are section 8.3.

| Route | Access / maximum body | Success |
|---|---|---|
| GET `/referrals/me` | Anonymous, Google, OTHER / none | 200 `{code,shareUrl,offer,attachment,stats}` |
| POST `/referrals/code` | Google with recorded activation / 2048 bytes | `{requestId}`; 201 newly issued / 200 existing `{code,shareUrl,offer,replayed}` |
| POST `/referrals/challenges` | Google / 2048 bytes | `{operation:"REFERRAL_ATTACH",requestId,policyVersion,payloadDigest}`; 201 section 8.3 challenge response |
| POST `/referrals/attach` | Google / 16384 bytes | Strict attachment body below; 201 new / 200 existing matching committed attachment `{attachment,reward,replayed}` |

`GET /referrals/attribution` and plural POST `/referrals/attributions` in older plans were provisional;
they are not introduced. GET `/referrals/me` owns both read states, and singular `/attach` is the one
mutation for MANUAL and future LINK. No public inviter lookup or landing route is added.

```text
code = null | {value:string,createdAt:UTC ISO}
shareUrl = null | configured allowlisted HTTPS URL
stats = {creditedCount:nonnegative safe integer,earnedCredits:nonnegative safe integer}
attachment = null | {id:UUID,status:"ATTACHED",source:"MANUAL"|"LINK",attachedAt:UTC ISO,
                    reward:{recipient:"INVITER",amount:positive integer,status:"GRANTED",policyVersion:version}}
reward = attachment.reward
```

GET returns only the caller's code, current offer, own attachment and own aggregate earned counts.
An invitee never receives inviter UID, code-owner details, wallet, grant ID or ledger ID. Stats count
committed own rewards, including detached receipts after invitee deletion, and their actual amounts.
They are not a public list of invitees. `shareUrl` is null until the app/environment owned origin,
path and associations are approved/configured; no synthetic deployed origin. Slice03 exposes
copyable code/manual entry. URL sharing, App Links, landing and install-referrer runtime are slice04.

POST code has no policy precondition because it creates no award. With valid independent code
configuration, Google qualification and live account it may issue/recover a code while rewarded
admissions are paused/exhausted. Its offer must state unavailable and promise no reward then.
One personal active code per `(scope,owner)` is stable across request keys and policy revisions.
`replayed` is false only for new creation, true when returning existing code. Existing code recovery
precedes Admin/admission checks after token/live ownership; no code rotation/replacement API here.
Unknown/disabled missing code is not silently regenerated on ordinary GET.

### 10.3 Coherent referral policy

Extend `GET /credits/policy`'s `policy.referrals` additively from `{status:"UNAVAILABLE"}` to `offer`
below. GET referrals/me and POST code return exactly the same referral offer shape/snapshot.
Do not replace top-level guest or signup revision fields; referral revision is independently scoped.

```text
offer = {status:"AVAILABLE"|"UNAVAILABLE",reason:offerReason,
         version:version|null,effectiveAt:UTC ISO|null,expiresAt:UTC ISO|null,
         rewardAmount:positive integer|null,continuityMode:"NONE"|"OPTIONAL",
         codeIssuance:"AVAILABLE"|"GOOGLE_REQUIRED"|"ACTIVATION_REQUIRED"|"UNAVAILABLE",
         attachmentEligibility:"ELIGIBLE"|"ATTACHED"|"EXPIRED"|"NOT_ELIGIBLE"|"UNAVAILABLE"|"GOOGLE_REQUIRED",
         deadline:UTC ISO|null}
offerReason = "ELIGIBLE"|"GOOGLE_REQUIRED"|"ACTIVATION_REQUIRED"|"EXISTING_ACCOUNT"|
              "AMBIGUOUS_HISTORY"|"RECOVERY_EXPIRED"|"ALREADY_ATTACHED"|"WINDOW_EXPIRED"|
              "POLICY_UNAVAILABLE"|"ADMISSIONS_DISABLED"|"CAMPAIGN_EXHAUSTED"
```

`status` describes whether this actor can advertise earning as an inviter now. Existing qualified
Google inviters may have AVAILABLE with `attachmentEligibility:NOT_ELIGIBLE`; new Google actors
may have AVAILABLE/ELIGIBLE. Guests have UNAVAILABLE/GOOGLE_REQUIRED. Missing activation has
UNAVAILABLE/ACTIVATION_REQUIRED. Existing attachment takes eligibility precedence; otherwise use
the unchanged section 9 classification/deadline. Missing usable referral policy makes offer
UNAVAILABLE, nullable metadata/amount null; a coherent paused/exhausted policy may retain its
amount/version but UI must not advertise it as earnable. Amount never comes from Android.
A usable policy has effectiveAt <= now < expiresAt and positive bounded amount <=1,000,000;
capacity <=1,000,000,000 credit units and wallet overflow checks. The exact scope literal for referral policies, challenges, operations, fingerprints, counters and
code/attachment ownership is compact JSON `[appId,environment,"referral-reward"]`. One active immutable revision in
SQL per that scope; configuration is validated and explicitly opt-in.

Attach supplies the displayed `offer.version`, never an amount. On usable revision mismatch return
409 POLICY_CHANGED, refresh and explicitly reconfirm current offer before fresh challenge/retry of
the same business key/input. Missing unusable policy is 503 PROMOTION_UNAVAILABLE. No silent new
award at a different amount. Already committed attachments return original amount/time/revision
before current offer/pause/expiry/capacity/evidence checks. Revision/rollback never tops up,
claws back, renews eligibility or makes a second reward. Rollback publishes a new tracked revision.
Fixture window 24h/capacity 100/reward 5 are deterministic tests only; live choices remain unset.
No monthly cap or lifetime device referral/signup cap is adopted.

### 10.4 Code canonicalization and business identity

A full code is `<appNamespace>.<environmentNamespace>.<payload>`. Each namespace is a configured
lowercase `^[a-z][a-z0-9-]{0,31}$` token with an explicit app/environment mapping. Payload is 20
cryptographically random bytes encoded as 32 lowercase RFC4648 base32 characters `[a-z2-7]`
(160 random bits; no padding, separators, UID, timestamp or sequential identifier).
Example fixture namespace is `tailormyresume.staging`, not a new live setting.

Raw input is 1..256 ASCII characters. Trim only outer ASCII SP/TAB/CR/LF; lowercase only ASCII
A-Z; then require the exact full three-part grammar, maximum 98 canonical characters. Reject
interior whitespace, Unicode, percent escapes, URLs, extra parts and `0/1/8/9` in the payload.

Android validates candidate syntax only, without assuming the configured namespace tokens.
A structurally valid full code from another namespace may be saved as a pending candidate.
Saving it does not validate its scope, existence or eligibility and never means Referral applied.
The server rejects codes outside its explicitly configured app/environment mapping and resolves
the complete code only within the authenticated deployment scope. Code tokens never choose the
request's API app, deployment environment, database, identity or challenge binding scope. Keep
the full canonical code and the existing independent journal ownership/environment fences;
never strip or rewrite a namespace or resolve another environment by suffix.

The existing canonical vectors and their hashes remain unchanged. Their invalidCodes list contains
both syntax failures and server-scope negatives: the structurally valid foreign prefixes remain
server-scoped rejection cases. Android consumer tests must distinguish them from malformed
candidate syntax; the scoped fixture verifier is not the Android candidate parser.

Creation tries at most 5 independently generated payloads on unique collisions, then 503
PROMOTION_UNAVAILABLE; concurrent same-owner creation returns one winning code. Namespace
mapping cannot be changed silently for existing codes. New issuance must atomically enforce
compatibility with retained scoped codes and report codeIssuance UNAVAILABLE on a conflicting
mapping; existing own-code reads/recovery retain their existing precedence. No namespace
rotation/migration API, new metadata field or live namespace choice is introduced.

```json
{"requestId":"11111111-1111-4111-8111-111111111111","code":"tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567","source":"MANUAL","policyVersion":"referral-local-v1","continuity":null,"challengeId":"22222222-2222-4222-8222-222222222222","integrityToken":"synthetic-token-not-valid-in-any-environment"}
```

`source` is exactly MANUAL or LINK; slice03 Android submits MANUAL. `continuity` is required and
null or the section 9 strict ANDROID_ID object. It provides independent velocity evidence only;
it does not allocate another trial or establish one-person/one-device referral entitlement.
The offer exposes continuityMode (a capability, not a threshold): NONE means send null and do
not read the signal; OPTIONAL permits collecting it only during an explicit new attachment attempt.
Null is accepted in either mode; the server cannot secretly require a nonnull referral signal.
Unavailable offer uses NONE. Never collect Integrity or Android ID on policy/wallet/history/status/
code display, code issuance or recovery-only paths. Missing signal stays explicit null, never a
common empty counter identity. Policy cannot silently
require a new device cap. Keep raw signal only during the request, never in journals/logs/exports.
Integrity token is 1..12000 characters; only challengeId is echoed, never the secret.

Canonical bytes are UTF-8 compact JSON arrays with fixed order, no spaces/newline/escaping changes:

```text
payload = ["TMR_REFERRAL_ATTACH_PAYLOAD_V1",canonicalCode,source,continuity?.type??"",continuity?.value??""]
payloadDigest = base64url-no-padding(SHA256(payload bytes))
binding = ["TMR_PROMOTION_BINDING_V1",appId,environment,authenticatedUid,"REFERRAL_ATTACH",requestId,payloadDigest,policyVersion,challengeId,challenge]
requestHash = base64url-no-padding(SHA256(binding bytes))
```

Fingerprint stored server-side is the section 9 `TMR_OPERATION_FINGERPRINT_V1` scope+actor+digest
construction. Policy/challenge/token are excluded from business identity. Same actor/action/key
with changed canonical code, source or continuity is 409 IDEMPOTENCY_CONFLICT. Outer whitespace
or ASCII case alone does not change identity. Refreshed evidence/revision keeps key/input.
A changed code/source/signal requires an explicit new attempt; it cannot evade durable attachment.
Server computes challenge hash; Android sends that returned hash to the existing injected Integrity
port. Keep existing package/cert/licensing/baseline/freshness checks, 5-minute maximum challenge TTL
and policy-expiry bound. Challenge binds scope/live actor/action/key/digest/revision. Evidence for
GUEST_TRIAL/GOOGLE_ACTIVATION cannot authorize attachment, nor can attachment authorize those grants.

### 10.5 Commit, replay, counters and concurrency

After auth/strict parse/Google provider shape, acquire actor live guard, check stored same-key
fingerprint and committed own receipt. Matching recovery returns 200 `replayed:true`; another key
with the same still-resolvable committed code returns the original attachment with 200 and true,
regardless of changed source/evidence on that new key. A different code/inviter returns 409
REFERRAL_ALREADY_ATTACHED, without counterparty detail. The original source remains unchanged.
No network call, challenge issuance, counter or capacity charge is needed for committed recovery.
Unknown/disabled/foreign code is 400 REFERRAL_CODE_INVALID before new reward evaluation; never
reveal which private account existed. New self-referral is 409 REFERRAL_SELF.

For new work, validate current policy and immutable activation/deadline; verify Admin/Integrity
outside SQL locks. Discover beneficiary server-side from full scoped code, then start a new
transaction using `withLiveUsers` for deduplicated lexical UID order (actor and beneficiary).
Never add an earlier user lock inside a single-user transaction. Recheck both tombstones,
code owner/enabled state, activation/incarnation/deadline (`now < deadline`), request/attachment,
policy and evidence under locks. Then acquire referral-source, purpose-signal/counter and promotion
scope locks in documented stable order after all user locks, consistent across attach/deletion.
No external call while locks are held. Reciprocal A→B/B→A cannot deadlock or award twice.

The successful transaction inserts attribution + opaque stable receipt, generic grant, unique
ledger entry and free-credit increment, full capacity amount, operation result, and consumed
challenge together. SQL constraints enforce unique invitee attachment and grant/source/ledger
links. There is no accepted/pending/zero/partial attribution. Sink failure, exception, deletion
race or insufficient capacity rolls all reward writes back. Both invitees competing for the last
5 units cannot both commit. Original committed amount survives policy changes and lost replies.

Use separate durable actor, optional purpose-scoped signal, trusted network and campaign velocity
dimensions for code issuance, challenge issuance and new attachment. Reuse existing ingress/key
rotation semantics; scopes/actions separate these from guest/signup counters. Failed new attempts
count even when the grant transaction rolls back; committed replay counts zero. Separate attempt
transaction is not reward liability; no capacity reservation survives a failed award. Do not fold
signal+network into identity or deny a person permanently for shared NAT/model. Account-specific
counters erase with their owner; short network/campaign counters retain only aggregate expiring
provenance. Snapshot/disclose reason category/retention, no raw signals or thresholds in API.

429 RATE_LIMITED is whole-operation failure with no attachment/grant; Retry-After is the ceiling
of the latest reset among exceeded applicable fixed windows (minimum 1), including route throttle
when applicable. Android waits until that deadline and explicitly retries the same uncommitted
key/input with fresh challenge; creating a key or refreshing policy does not bypass counters.
Unavailable identity/Integrity is retryable 503, never guessed eligibility or permanent fraud.

### 10.6 Stable errors and Android recovery

| Status/code | Behavior |
|---|---|
| 400 INVALID_INPUT / REFERRAL_CODE_INVALID | Invalid strict shape / malformed, unknown, disabled or foreign code; correct/clear explicitly |
| 401 existing auth errors / 403 CROSS_APP_TOKEN | No resource disclosure or mutations |
| 403 GOOGLE_REQUIRED / GOOGLE_IDENTITY_REJECTED | Guest/OTHER or contradictory provider; reconcile identity |
| 403 REFERRAL_INELIGIBLE | Existing/ambiguous/recovery-expired durable classification; ordinary use continues |
| 409 REFERRAL_ACTIVATION_REQUIRED | No frozen qualifying fact; reconcile observation, refresh state |
| 409 REFERRAL_ALREADY_ATTACHED / REFERRAL_SELF / REFERRAL_WINDOW_EXPIRED | No replacement/new reward; recover own status / correct input / explain deadline |
| 409 ACCOUNT_DELETED | Actor or required beneficiary deleted/deleting; never recreate wallet |
| 409 POLICY_CHANGED / IDEMPOTENCY_CONFLICT / INTEGRITY_CHALLENGE_INVALID | Refresh+reconfirm offer / preserve original input / fresh evidence as above |
| 403 INTEGRITY_REJECTED / 503 INTEGRITY_UNAVAILABLE / IDENTITY_UNAVAILABLE / PROMOTION_UNAVAILABLE | No reward; existing wallet and signup/work remain usable |
| 413 PAYLOAD_TOO_LARGE / 429 RATE_LIMITED | Bound input / retain candidate and honor Retry-After |

Keep the existing `{error:{code,message}}` envelope. Android branches on code. No error or retry
message promises an award or exposes the inviter. Self/invalid/expired code never reverses signup.

Persist first syntactically valid pending candidate across process recreation, onboarding and
same-UID Google upgrade. A later candidate cannot silently overwrite it; explicit Replace/Clear
must precede a new attempt. Before first send durably bind environment+UID+provider/generation,
canonical code, source, requestId, displayed revision, continuity digest (not raw signal), phase,
challenge handle and retry deadline. Retain stable business input; if current signal differs,
stop and explicitly reprepare rather than send a changed digest with the same key. Never persist
Integrity tokens/challenge secrets. On uncertain/lost replies reconcile own GET first before any mutation retry. If not attached,
retry the original key/input before collecting new evidence, preserving committed recovery. POLICY_CHANGED requires visible refresh/reconfirmation.

An unbound guest candidate follows only that guest's proven same-UID link. Existing-Google conflict
switch must not automatically submit it to the destination; offer explicit review/rebind if that
destination is eligible, otherwise clear it. Preserve the existing section 9 work-choice journal,
Room handoff, local pointer scrubbing and owner-transfer rules. Never merge wallets or use a
referral as evidence to transfer work. Clear account-owned candidate/attempt/cache on sign-out,
delete or unrelated switch; cancel coroutines and reject late responses using UID+environment+
provider/generation fences before durable writes, wallet/history updates or navigation. Erase
fences prevent in-flight jobs restoring deleted state.

Invite friends and optional code entry use current dark Paige Tmr fields/buttons/sheet/toast
patterns; copy comes from current server offer. Keep large-text/keyboard actions reachable. Use
neutral copy when unavailable, and never auto-tailor/spend after receipt or wallet refresh.
Inviter foreground/screen-entry refresh obtains own wallet and `/credits`. Render REFERRAL_REWARD
as its original positive amount with stable server ledger ID through DTO/model/Room/history.
Same-time grants remain distinct. Preserve guest/signup, legacy WELCOME/PURCHASE/TAILORING/REFUND/
MIGRATION, unknown-kind tolerance, free-before-purchased spend and zero-credit job recovery.
History is capped at 100 on ordinary reads; wallet counters remain balance authority. Extend
persisted enum decoding without destructive reset. Current remote snapshots are in memory and
Room credit_ledger has only a local Long ID; its mapping drops server string IDs. Slice03 must
carry nullable stable server IDs through persisted mappings with a tested additive Room migration,
committed schema and nullable unique server-ID/owner association (including environment). Preserve
legacy local IDs/rows and deterministic fallback keys, refuse cross-owner persisted reads, clear
account-owned snapshots on erasure/switch, and never use destructive migration fallback. Local
application deletion must not remove server economic history or mutate wallet balances.
Remaining payment retirement and historical purchase obligations belong to slice05.

### 10.7 Lifecycle and privacy

Ship lifecycle with schema. Keep caller-owned attached status/receipt separate from identifying
cross-user edge. Invitee deletion erases its activation, code-bearing requests/fingerprints,
challenge/counters/signals and identifying attribution edge/provenance. The living inviter retains
its own legitimate balance, grant, ledger ID/time/amount/revision and detached nonidentifying reward
receipt; do not cascade inviter economics through invitee. Delete any receipt→invitee FK/UID/code
or hidden derived source. Inviter deletion disables/removes code and owned wallet/grants/ledger,
erases edge/provenance; living invitee retains its own ATTACHED receipt/time/source/reward snapshot,
without counterparty identity and with no reattachment. Deleting either party never replenishes
aggregate consumed capacity. Deleting the survivor later erases its remaining own receipt.

Erasure takes precedence after the rows-erasure/detachment transaction commits: erase foreign
code/reference/hash and affected code-bearing operation/challenge/decision fingerprints. Every
subsequent structurally valid attach POST returns 409 REFERRAL_ALREADY_ATTACHED before code,
policy, evidence or new operation/challenge state, including original key/code and another code.
GET returns 200 with the own ATTACHED receipt. Do not expose a counterpartyDeleted flag or imply
that the submitted code matched. Before detachment, exact committed replay may return 200 while
inviter deletion is pending: no beneficiary write is needed. The actor must always be live;
actor tombstone overrides recovery. Serialize replay and rows-detachment so snapshots are whole.

On lost response/process recovery Android reconciles GET before any mutation retry. ATTACHED
confirms the account status, not which pending code won. Clear all pending code/key/digest/evidence
and show Referral applied only from server confirmation. On REFERRAL_ALREADY_ATTACHED, stop
mutation retries, clear code-bearing state and reconcile GET. If GET fails, retain only an
owner-scoped read-only reconciliation marker. A contradictory nonattached GET is a consistency
failure; never auto-retry or create another key. UID/environment/provider/generation and erase
fences also apply to this read-only marker. This resolves the bounded lifecycle decision adopted
by the orchestrator; see the slice03 lifecycle-decision.md handoff.

Deletion records a tombstone under the existing user lock, then rows cleanup discovers counterpart
edges and acquires all necessary user locks in lexical order before detaching in the same rows
transaction. It cannot use live-only `withLiveUsers` to erase the already-tombstoned target; use
accounts-owned sorted erasure helper with explicit target authorization and live-survivor recheck.
Re-read edges under locks; if discovery changed, retry without reversing lock order. No app import
in core accounts. Expose a narrow generic lifecycle port or core-referral erase service before
`delete(users)` cascade. Existing rows step currently only calls eraseAiUsage; cascade alone fails.
Concurrent reciprocal attach/delete and survivor delete/delete must finish without orphaning,
reviving a wallet or dropping a survivor reward. Partial files/rows/Firebase failures resume using
the durable deletion job, including after Firebase erasure without a client token.

Export additively extends existing `export.promotions` with `referrals:{code,attachment,stats}`
using the exact own shapes above (no share URL/offer required). Existing own activation/grants/full
ledger remain. Add stable ledger `id` additively to exported credits. Export never creates a code,
fact or award. Exclude counterpart UID/email, raw/hashed code provenance belonging to erased
counterpart, provider/device/network HMACs, counter details, tokens, proofs and internal decisions. Live own-code export does not include
the code that a caller used to attach to somebody else.
Only issuer exports its own live code. Backup/restore must retain aggregate consumed totals and
reconcile deletion tombstones/detachment before serving restored reads or new grants; an old
backup must not resurrect erased edges, reset lifetime attachment or reopen capacity. Document
operational reconciliation; prove local dump/restore + delete/retry in disposable fixture database.

Backend must propose the narrow rule-3 server-resolved beneficiary and rule-9 survivor-ownership
exception for human review in the governing PR; no arbitrary user lookup or cross-app transfer.
This contract does not silently amend constitution/architecture or authorize live retention.

### 10.8 Acceptance, dependency and activation boundary

Canonical/schema fixtures and the case matrix are under
`docs/testing/earn-credits/2026-10-17/03-manual-referrals/contract/`. They are deterministic examples,
not real Google/Integrity evidence or activation values. Preserve section 8/9 fixture hashes.
Cover actual routes/services with real local Postgres, fake external providers, a second isolated
non-Google app/sink, concurrent/replay/policy/capacity/sink/deletion/export cases, plus Android
pending/retry/switch/history and normal/large-text screen tests. Required implementation gates are
backend npm run check and Android tools/ci/verify-local.sh; this contract-only phase runs fixtures,
whitespace and policy checks, with no Gradle/npm/emulator/remote work.

Partial issue ownership: backend #122 manual endpoints/atomicity/lifecycle and #120 reusable core;
#54 action evidence/counters/capacity. Android #405 manual entry/code/pending state, and #404 only
integrated REFERRAL_REWARD history/balance/owner safety. These issues remain open; #405 link/share/
App Links/Play Referrer belongs to slice04 and #404 remaining payment retirement to slice05.
Actual GitHub bodies were read 2026-10-17. Provisional plural routes are reconciled by 10.2.

Frontend Article VII proposal is also required: current I.5 permits direct Integrity solely for
section 8 guest admission, leaving a genuine section 9 wording gap. Frontend writer must propose
the narrow extension naming only GUEST_TRIAL (8), GOOGLE_ACTIVATION including explicitly requested
protected base (9), and REFERRAL_ATTACH (10), with unchanged action bindings, opaque requestHash
only to Google, optional finite referral-velocity purpose, passive/recovery no-collection tests,
and existing policy/Dependency Guard/lint gates plus Review. No new SDK or expanded suppression/
baseline is authorized. See constitution-decision.md for exact wording and accessor rationale.
This contract preserves section 9 behavior and prepares an unmerged governing-document proposal;
human review before merge and genuine live disclosure/provider/retention gates remain required.

Dependencies: exact slice02 FE ca4fcade751fc0c18c5cd5eb02128933700888a4 (draft PR407), BE
4e954057173fd6a25576708450c864fd31d17c19 (draft PR125), both open/unmerged. Slice02 attainable
publication audit PASS does not prove full Google/Integrity/admitted-grant/spend acceptance.
Genuine matching staging Google/Integrity configuration, signed device evidence, live campaign
capacity/window/retention/namespace/ingress decisions and human-reviewed rule proposals remain
BLOCKED activation gates. Share origin/domain/associations/Play install proof remains slice04
BLOCKED. Coding may continue under external-gate-decision.md; no mocked PASS/live fallback,
deployment, migration against remote data, activation, merge or production policy change here.
