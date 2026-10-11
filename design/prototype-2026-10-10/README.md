# Prototype 2026-10-10 — current design and simplified MVP

Claude Design project "Resume Onboarding Flow", received 2026-10-10:
https://claude.ai/design/p/69e938ce-29b9-400c-ab69-5da2975dbeae?file=Prototype.dc.html

This prototype is the current design source and the current MVP flow. It wins over
`design/avvio-canvas/`, `design/jade-restyle/` and `design/claude-design/` wherever they show the same
thing. The scope it implies is recorded in `docs/PRD.md` section 6.1.2. The older folders still hold
states the prototype does not draw (offline, 200% text, system sheets). Keep them read-only.

## Files

| File | What it is |
|---|---|
| `Prototype.dc.html` | The whole app as one clickable prototype. A phone frame on the right, a screen index on the left. Lines 1–966 are the template, lines 967–1382 the logic (`class Component`). |
| `Paige.dc.html` | Paige, the mascot component: a resume page with a face. Props `eyeW eyeH eyeT mouthW mouthH ticks arms sad tf`. |
| `support.js` | The Claude Design runtime that renders `.dc.html` files (`<x-dc>`, `sc-if`, `sc-for`, `{{ }}`). Not app code. |

The project's `uploads/` folder holds old Avvio boards (black, heavy uppercase Archivo Black, for
example S14 Exported) that the user pasted as references. They are the old design, so they are not
copied here. Do not build from them.

The files are an export. Do not edit them by hand. If the design changes, export again and replace
this folder in one PR. The exported HTML has the editor's injected `data-omelette-injected` script
removed; nothing else changed.

## How to read it

- As text: the logic block is the spec. `index()` lists every screen and state, `fresh()` and
  `returning()` hold the sample data, `renderVals()` holds every copy variant and colour rule.
  `T` (line ~1171) says which screens show the step bar, back, close, title and the top-right action.
- In a browser: start the `prototype` entry of `.claude/launch.json` (or
  `python3 -m http.server 8767 --directory design/prototype-2026-10-10`), then open
  `http://127.0.0.1:8767/Prototype.dc.html`. "New user" starts at sign-in; "Returning user" starts at
  Applications with three sample applications. The chips jump to any state.
- Live: open the project link above (needs the owner's claude.ai login).

## Look

Playful and colour-coded, with Paige the mascot. Sign-in is full-bleed colour (one colour per story
slide). Every other screen sits on black, and each setup step opens with a large rounded colour hero
card (radius 32) holding the step label, a two-line Space Mono headline, sticker pills
("PDF", "DOCX") and Paige: blue #5AA9F8 for the resume and profile, amber #F7A940 for the job, lime
#A3F43F for tailoring. This is not the old Avvio look (heavy uppercase Archivo Black, #AEFF00).
Dark only; the app has no light theme. Phone frame 392 x 852 with a 9 px bezel, so the screen is
374 x 834 px; treat 1 px as 1 dp.

| Role | Value |
|---|---|
| Screen background | #000000 |
| Card / raised | #111111, #161616, #1C1C1C |
| Chip / input fill | #242424 |
| Lines, unselected borders | #2A2A2A, #333333, #3A3A3A |
| Text | #FFFFFF; secondary #C8C8C8; muted #A6A6A6; placeholder and disabled #7A7A7A |
| Ink on light fills | #0A0A0A |
| Primary (lime) | #A3F43F; selected fill #141A0A; soft lime #E4FBB8 |
| Amber (needs attention, "from your answer", Applied) | #F7A940; highlight on the resume #FFE2C2 |
| Blue (Saved) | #5AA9F8 |
| Paige cheeks | #FF6A2B |
| Sign-in story slides | blue #5AA9F8, amber #F7A940, lime #A3F43F full-bleed |

Type: Space Mono (headlines, labels, numbers; 161 uses) and Space Grotesk (body; 47 uses). Both from
Google Fonts. Headlines are Space Mono 28 px, letter-spacing about -0.8 px, set on two lines
("Start with / your resume"). Common sizes 12, 13, 14, 15, 16. Shapes: pills 999 px, cards 16–24 px,
the phone 50 px. Primary button: lime fill, #0A0A0A text, pill. Disabled primary: #222 fill, #7A7A7A
text, and it shows a toast that says why when tapped.

Status chips (`SS` in the logic): Saved blue outline, Applied amber outline, Interview lime fill,
Offer amber fill, Rejected grey.

Paige appears on sign-in (three poses), job paste, quick question, tailoring (wobble), Applications
and the paywall. She bobs with a sine of about 5 px. Under reduced motion she must stand still.

## Not in the MVP — decided by the owner on 2026-10-10

Do not build these, and do not raise them again as gaps, open questions, or review findings
(`docs/PRD.md` 6.1.2):

1. No consent notice screen and no 18+ check.
2. No light theme. The app is dark only.
3. No "Continue with Apple". Sign-in is Google only, even though the prototype draws an Apple button.
4. No follow-up reminders. Drop the "Follow-up reminders" toggle the prototype shows in Settings.
5. No offline states, and no error states beyond the five the prototype draws (sign-in cancelled,
   unreadable file, not a job post, tailoring failed, payment failed). No separate 200% text frames.

## Navigation

- Top bar per screen from `T`: back, or close on Exported and the paywall; an optional title and an
  optional right action (Save / Done).
- Step bar `1 Profile · 2 Job · 3 Tailor` on the setup and tailoring screens. Done steps show
  "✓ Profile" in lime.
- Bottom tabs on Applications and Profile only: Applications, a centre "+" (new job), Profile.
- One toast at a time, 3.2 s, with an optional action (Undo, Retry).
- Bottom sheets: application status, delete account, payment failed.

## Screens and states

The index groups match the left-hand chips in the prototype.

### Onboarding

| Screen | States | Key copy and behaviour |
|---|---|---|
| Sign in (`signin`) | stories 1–3 auto-advance every 5 s (tap left/right to step, hold to pause); signing in; sign-in cancelled toast "Sign-in cancelled. Nothing was saved." + Retry | Stories: "Upload your resume", "Paste the job you want", "Get a resume made for it". Button "Continue with Google". Build Google only; the drawn Apple button is cut. Footer: Terms, Privacy, "Your first tailored resume is free." A new user gets 1 free credit at sign-in. |
| Upload (`upload`) | default | "Start with your resume". Upload resume (PDF or DOCX, up to 5 MB), Paste as text, Fill in myself. "Never shared." |
| Unreadable file (`uploadError`) | image-only PDF | "We couldn't read that file", three tips, Choose another file, Paste as text |
| Paste resume (`pasteResume`) | default | "Paste your resume", Read text |
| Fill in myself (`manual`) | default | Full name, Email (from Google), Phone, City, Current or last job title, Company; Continue |
| Reading (`reading`) | progress over 3.5 s | Rows: Contact details, Work experience, Education, Skills, Achievements, each Reading… then a count. "Takes about 20 seconds". No back. |
| Review profile (`review`) | needs fix; fixed | "Your profile", completeness % (90, 96 when fixed, +4 with LinkedIn). Required banner "End date missing for Data Analyst Intern" → Add date. Optional "Add your LinkedIn". Cards: Contact, Experience · 3, Skills · N, Education. CTA "Looks right, continue" is disabled until required items are fixed. |

### Job and tailor

| Screen | States | Key copy and behaviour |
|---|---|---|
| Add job (`job`) | empty; has text; detected "Associate Analyst · Northwind GCC" + character count; not a job post; analyzing (2.2 s) | "Paste the job you want". Paste from clipboard, Use a link, Clear, Analyze job. Under 200 characters or not a job post shows "This doesn't look like a job post". |
| Import link (`jobLink`) | default | "Import from a link", Job link field, Import job; toast "Imported from …" |
| Job analyzed (`jobResult`) | with credits; no credits | Title, company, place. "Your match 61 Now → 92 Up to, after tailoring". "We only use what's backed by your experience." "What they screen for · 8": have chips (lime dot) and missing chips (amber outline). "Missing skills are only added if you confirm you have them." Must-haves with a reason each; one marked "!" "Not clear in your resume. We'll ask you next." CTA "Tailor my resume", note "Uses 1 credit · you have N". With 0 credits the CTA opens the paywall and returns here. |
| Quick question (`question`) | none picked; picked | "One quick question: Have you presented to senior leaders?" Options Yes, regularly / A few times / Not yet. Yes or few shows a detail field. Continue; Skip this. |
| Tailoring (`tailoring`) | progress over 4 s | Rows: Matching 8 keywords, Rewriting bullets, Adding your example (or Checking must-haves), Fitting to 1 page. No back. |
| Tailoring failed (`tailorFail`) | default | "Something went wrong on our side. Your credit wasn't used." Try again, Go back |
| Tailored resume (`tailored`) | Resume tab; Changes tab | "Your resume is ready", match 92 (84 without the answer). Resume tab: a 1-page preview with changed text highlighted (keyword lime, from your answer amber). Changes tab: each change with area, kind (New, Rewritten, Added, Reordered), before struck through, after, and source chip "Your resume" (lime) or "Your answer" (amber). Edit, Export PDF. |
| Edit resume (`editResume`) | default | Title "Resume", Save. "Still fits on 1 page". Summary, bullets per role with source tag, Skills line. |
| Exported (`exported`) | paid credit used; free credit used; not marked; marked Applied (+ Undo toast) | "Resume exported", file `Name_Company_Role.pdf`, Share, Open. Credits line "N left, was N+1. Credits never expire." or "Free resume used. 0 credits left. Nothing was charged." "Saved to Applications: …". "Did you apply? ✓ Mark as Applied". "Get prep questions — Soon", "Write a cover letter — Soon". Go to Applications. |

### App

| Screen | States | Key copy and behaviour |
|---|---|---|
| Applications (`home`) | empty; list | Header with credit count (opens Credits) and avatar (opens Profile). Empty: "No applications yet", Add a job. List: "Tailored resumes N", a stacked bar by status with a legend, then rows (initial disc, title, "company · NN match", status chip). |
| Application (`appDetail`) | default; status sheet | Status with date or "Not applied yet", Change. File card with Share, Open. Next steps: "Re-tailor with profile changes · 1 credit", "View job analysis", prep questions and cover letter "Soon". |
| Profile (`profile`) | default | Name, "Finance analyst · 4 yrs · Pune", % complete. Rows: Contact, Summary, Experience 3, Education 1, Skills N, Achievements 6, LinkedIn & links (amber "Add" tag when empty). "Built from <file> · Replace". Gear opens Settings. |
| Experience (`experience`) | default | Role list, + Add role |
| Edit role (`editRole`) | edit; add; current-job toggle | Job title, Company, Start, End, "I work here now", What you did (bullets), + Add bullet, Delete this role; Save |
| Contact (`editContact`) | default | Full name, Phone, City, Email (from Google, read-only), LinkedIn (optional, amber border when empty), Portfolio (optional); Save |
| Skills (`skills`) | default | Add field, removable chips; Done |
| List edit (`listEdit`) | Summary; Education; Achievements | One shared editor with a note per section; Save |
| Settings (`settings`) | default; delete sheet | Account (email, "Signed in with Google"), Credits & purchases, Resume: page size A4/Letter, file name format (Name_Company_Role, Name_Role, Name_Resume). Notifications: Product updates (the drawn Follow-up reminders toggle is cut). Privacy & help: Download my data (toast: emailed within 24 h), Help & feedback, Delete account, Sign out. "Terms · Privacy · v1.0.0". Delete sheet: states what is lost, "Type DELETE to confirm". |
| Credits (`credits`) | default | "N credits left. They never expire." Buy more. History ledger (+5 lime, −1 white). "Receipts go to your Google email". |
| Paywall (`paywall`) | out of credits; buying; payment failed sheet | "Keep tailoring for every job". "1 credit = 1 tailored resume. Credits never expire." Packs: 5 for ₹199 (₹40 each), 15 for ₹449 (₹30, Best value, preselected), 40 for ₹999 (₹25). "Buy 15 credits · ₹449". "One-time payment · Restore purchases". Fail sheet: "You weren't charged." |

## Sample data

Priya Deshmukh, Pune; roles at Infosys, Tata Digital, Bajaj Finserv; target job Associate Analyst,
Northwind GCC. Use it for mock data and screenshot fixtures. The full strings are in `JD`,
`RESUME_TXT`, `ROLES`, `SKILLS` and `LISTS` in the logic block.
