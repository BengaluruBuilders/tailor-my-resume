# TailorMyResume backend contract, v2

Status: v2 proposed 2026-10-10 for the simplified MVP (v1 was proposed 2026-10-07). Owner: the TailorMyResume app.
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
| `freeCredits` | Credits from the welcome grant. A tailoring uses a free credit first |
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
2. A bullet of an experience entry E may cite an `ans-` id only when `detail` names the organisation or the
   title of E (case-insensitive). The `ans-` source sits next to at least one source bullet of E, and the bullet
   stays under E. Otherwise the answer can support only the summary and the skills. An answer creates no entry
   and no employer claim (revamp D6). The device FabricationGuard (D7) accepts an `ans-` id as a source in the same way.
   The server drops or reverts a bullet that breaks this rule.
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

The first call creates the wallet with 1 free credit and one `WELCOME` ledger row in one transaction. A
second call writes no second row. The app reads it right after sign-in.

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
| `entries` | Newest first. At most 100 |
| `kind` | `WELCOME`: the first free credit, amount `+1`. `PURCHASE`: a pack, amount `+5`, `+15`, or `+40`, with `productId`. `TAILORING`: a successful tailoring, amount `-1`, with `applicationId`. `REFUND`: credits given back, positive amount. `MIGRATION`: the balance that a wallet had before the ledger existed, one row for each wallet that had credits, positive amount |
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
    "unlocks": [{ "applicationId": "...", "creditKind": "...", "createdAt": "..." }],
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
