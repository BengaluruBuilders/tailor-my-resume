# TailorMyResume revamp plan — 2026-10-10

Author: the read-only planning scout. The inputs are:
- the app at `main` e387b855;
- PR #255 (prototype, PRD 6.1.2, Constitution ledger), read from `../hirehop-wt/scout` at 21ee644d;
- `apps-backend` `origin/main` 59a8abb.

The sources of truth, in order:
1. `docs/CONSTITUTION.md`.
2. `docs/PRD.md` 6.1.2.
3. `design/prototype-2026-10-10/README.md`.
4. The logic block of `Prototype.dc.html`: `index()`, `fresh()`, `returning()`, `renderVals()` and `T`.
5. `AGENTS.md`, `docs/BACKEND_CONTRACT.md` and `docs/ARCHITECTURE.md`.

The old design folders are never a build source: `design/avvio-canvas`, `design/jade-restyle` and `design/claude-design`.

The machine-readable issue list is `issues.json`, which has 50 issues. Each issue has `key`, `repo`, `title`, `body_markdown`, `labels`, `wave`, `lane`, `depends_on_titles`, plus `depends_on_keys`, `routing`, `security_review` and `files`. The filing agent must create the issues in the order of the array. Each issue's dependencies always come earlier in the array.

## 0. Orchestrator decisions built into this plan

| # | Decision | Effect in this plan |
|---|---|---|
| D4 | Constitution I.2 wins and is not amended. Each change on the Changes tab has its own Undo. Export needs one explicit "Accept changes" action. Until the user takes it, Export PDF stays disabled. This is the only accept control: there are no per-bullet toggles. | W2-G (`AcceptChangesUseCase`, the export guard), W4-C1 (UI), W1-G (ledger note). `TailorViewModelTest` and `UpdateBulletDecisionUseCaseTest` stay as the I.2 gates. |
| D5 | The prototype's "61 → 92 match" is built as keyword coverage, computed per PRD 4.2 and labelled as keyword coverage, for example "Keywords matched 61% → 92%". It is never called a match, fit, ATS or job-likelihood score (I.3). | W2-F (`KeywordCoverage`), W2-D (`TmrCoverageDelta`), and the copy in W4-A1 (story 3 sticker), W4-B2, W4-C1 (paper badge) and W4-D1 (row meta "NN% keywords"). |

The scout made these decisions inside the boundary it was given. Each is explained where it is used.
- **S1.** Keep all six feature modules and their names. `feature:analysis` hosts Job, Import link, Job analyzed and Quick question. `feature:settings` also hosts Credits and the Paywall. Renaming a module means editing `.github/workflows/build.yml`, which is CI config (in-harness) and a merge hotspot. One module per W4 lane gives six file-disjoint lanes.
- **S2.** Delete `core:ui`. It holds only Avvio composables. This is the one module deletion (W1-F, CI config).
- **S3.** Keep the offline engine in `core:domain`: the parser, gap matcher, tailor and lexicons. It is the demo flavour's backend, and it carries the I.1 gate (`OfflineFabricationGuardTest`).
- **S4.** The truth rule for quick-question answers (section 2.3). The rule is stricter than the prototype drawing, because Constitution I.1 wins.
- **S5.** `PrototypeFixtures` (the screenshot fixtures) lands early in W2-E, because the W4 screenshot tests need it. W6-B unifies it with the demo sample data. Wave 6 still owns the demo flavour and the debug menu.

## 1. Keep-or-delete table

The verdicts are:
- **keep**: unchanged or small edits.
- **rewrite**: the file or module stays, but its content is replaced.
- **delete**: removed.

The wave that acts is in brackets.

### 1.1 Gradle modules

| Item | Path | Verdict | Reason |
|---|---|---|---|
| `:app` | `app/` | rewrite (W3, W5, W6) | The navigation shell changes completely: a setup flow with a step bar, and tabs Applications, + and Profile. The prod sources for Google sign-in, Play Billing and the backend client must survive. The demo and debug source sets are rewritten in W6. |
| `:core:common` | `core/common` | keep | Dispatchers, the result type and scopes (II.4). |
| `:core:data` | `core/data` | keep, prune (W1-D), extend (W2-E) | Repositories over Room and DataStore. The cover letter, prep plan and connectivity code is deleted. Credits, resume settings and the new profile and application fields are added. |
| `:core:database` | `core/database` | keep, migrate to v3 (W2-E) | Room is the on-device source of truth (contract D1). V.6 requires a migration. |
| `:core:designsystem` | `core/designsystem` | rewrite (W1-E delete, W2 build) | The Paige design system replaces Avvio and Jade. It is dark only. |
| `:core:domain` | `core/domain` | keep, prune (W1-D), extend (W2-F, W2-G) | It holds the fabrication guard and truth rules (I.1, must survive), tailoring, analysis, sign-in and payment interfaces (II.6), and the offline demo engine (S3). |
| `:core:model` | `core/model` | keep, prune and extend | Pure Kotlin models (II.1). |
| `:core:navigation` | `core/navigation` | keep | Navigator and navigation state, which the new shell reuses. |
| `:core:network` | `core/network` | keep, extend (W5-N) | Must survive. The one HTTP stack (I.5). Contract v2 DTOs are added. |
| `:core:screenshot` | `core/screenshot` | keep (W2-A adds the 374 x 834 device) | Roborazzi helpers (Constitution ledger). |
| `:core:testing` | `core/testing` | keep, prune (W1-D), extend (W2-E) | Fakes (IV.2) and contract tests. `PrototypeFixtures` is added. |
| `:core:ui` | `core/ui` | delete (W1-F) | Only Avvio composables: the status sheet, coverage meter, provenance chips, empty state and delete dialog. The new equivalents live in `core:designsystem`. |
| `:feature:onboarding:api/impl` | `feature/onboarding` | rewrite (W1-B, W4-A) | Sign in, Upload, Unreadable file, Paste, Manual, Reading and Review profile. Keep `ContentResolverResumeTextSource`, the on-device PDF and DOCX text reader (contract D2). |
| `:feature:analysis:api/impl` | `feature/analysis` | rewrite (W1-C, W4-B) | Becomes the Job area: Add job, Import link, Job analyzed and Quick question. The name is kept (S1). |
| `:feature:tailor:api/impl` | `feature/tailor` | rewrite (W1-A, W4-C) | Tailoring, failed, tailored, edit, export and exported. Keep the PDF renderer, `TailorViewModel` and its test (I.2 gate). |
| `:feature:applications:api/impl` | `feature/applications` | rewrite (W1-C, W4-D) | Applications home, detail and status sheet. |
| `:feature:profile:api/impl` | `feature/profile` | rewrite (W1-B, W4-E) | Profile overview and its editors. |
| `:feature:settings:api/impl` | `feature/settings` | rewrite (W1-C, W4-F) | Settings, the delete sheet, Credits and the Paywall. Money and accounts sit in one lane, reviewed in-harness. |
| `build-logic` | `build-logic/convention` | keep | Convention plugins (III.5), Kover (80% on domain and data), the screenshot plugin. |

### 1.2 Screens and routes (NavKeys at e387b855)

| Item | Path | Verdict | Reason |
|---|---|---|---|
| `WelcomeNavKey` / Welcome | `feature/onboarding` | delete (W1-B, W1-D) | The prototype starts at Sign in. |
| `SignInNavKey` / Sign in | `feature/onboarding` | rewrite (W4-A1) | Story slides, Google only, the cancelled toast. The gateway must survive. |
| `ConsentNavKey` / Consent (read-only too) | `feature/onboarding` | delete | Cut: no consent screen and no 18+ check. |
| `PasteJobDescriptionNavKey` | `feature/onboarding` | delete; the job screen is rebuilt as `JobNavKey` in `feature/analysis` (W4-B1) | The job comes after the profile in the new flow. |
| `ImportResumeNavKey` / Import resume | `feature/onboarding` | rewrite as `UploadNavKey` plus `UploadErrorNavKey`, `PasteResumeNavKey`, `ManualProfileNavKey`, `ReadingNavKey` (W3-A, W4-A2) | Prototype screens `upload`, `uploadError`, `pasteResume`, `manual` and `reading`. |
| `ConfirmFactsNavKey` / Confirm facts | `feature/onboarding` | delete; replaced by `ReviewProfileNavKey` (W4-A3) | One profile review replaces fact-by-fact confirmation. |
| `GuidedProfileFormNavKey` / Guided form | `feature/profile` | delete | Replaced by the 6-field "Fill in myself". |
| `FactEvidenceNavKey` / Evidence path | `feature/profile` | delete | Replaced by the one quick question. |
| `FactEditorNavKey` / Fact editor | `feature/profile` | delete; replaced by `EditRoleNavKey`, `EditContactNavKey`, `SkillsNavKey`, `ListEditNavKey(section)` (W4-E) | The editors are drawn per section. |
| `ProfileNavKey` / Profile | `feature/profile` | rewrite (W4-E1) | Profile tab. |
| `AnalysisNavKey` / Gap analysis | `feature/analysis` | rewrite as `JobResultNavKey(applicationId)` (W4-B2) | Job analyzed. |
| (new) `JobNavKey`, `JobLinkNavKey`, `QuickQuestionNavKey` | `feature/analysis/api` | add (W3-A) | Prototype screens `job`, `jobLink` and `question`. |
| `TailorNavKey` / Tailor review | `feature/tailor` | rewrite as `TailoringNavKey`, `TailorFailedNavKey`, `TailoredNavKey(tab)`, `EditResumeNavKey` (W3-A, W4-C1) | Prototype screens `tailoring`, `tailorFail`, `tailored` and `editResume`. |
| `BulletReviewNavKey` / Bullet review sheet | `feature/tailor` | delete | D4: no per-bullet accept. |
| `ExportPreviewNavKey` / Export preview | `feature/tailor` | delete | The Resume tab is the preview. Export goes straight to Exported. |
| `ExportedNavKey` / Exported | `feature/tailor` | rewrite (W4-C2) | Exported, with the four states. |
| `ShareLastExportNavKey` | `feature/tailor` | delete | Share is on the file card. |
| `PackPurchaseNavKey` / Pack purchase | `feature/tailor` | delete; replaced by `PaywallNavKey(returnTo)` in `feature/settings` (W4-F2) | Paywall with 3 packs. |
| `CreditsNavKey` / Credits | `feature/tailor` | move to `feature/settings`, rewrite (W4-F2) | The Credits ledger. |
| `CoverLetterNavKey`, `PrepQuestionsNavKey` | `feature/tailor` | delete | Cut. Only "Soon" rows remain. |
| `ApplicationsNavKey` / Applications | `feature/applications` | rewrite (W4-D1) | Home: empty state and list. |
| `ApplicationDetailNavKey` / Application workspace | `feature/applications` | rewrite (W4-D1) | Detail plus the status sheet. Notes and prep tasks are deleted. |
| `SettingsNavKey` / Settings | `feature/settings` | rewrite (W4-F1) | It is no longer a top-level tab. It opens from the Profile gear. |
| `YourDataNavKey` / Your data | `feature/settings` | delete | A Settings row and a toast replace it. |
| `DeleteAccountNavKey`, `AccountDeletedNavKey` | `feature/settings` | delete; a sheet on Settings replaces them (W4-F1) | Delete sheet with "Type DELETE". |
| `TOP_LEVEL_NAV_ITEMS` (Applications, Profile, Settings) | `app/.../TopLevelNavItem.kt` | rewrite (W3-A) | The tabs are Applications, + and Profile. |

### 1.3 `core:designsystem` and `core:ui` components

| Item | Path | Verdict | Reason |
|---|---|---|---|
| `theme/Color.kt`, `Palette.kt` | `core/designsystem/.../theme` | delete Palette; rewrite Color (W1-E trims, W2-A rebuilds) | Paige tokens, dark only. |
| `theme/Type.kt` | same | rewrite (W2-A) | Space Mono and Space Grotesk. |
| `theme/Shape.kt`, `Spacing.kt` | same | rewrite (W2-A) | Radii 999/32/24/20/18/16 and the prototype spacing. |
| `theme/Motion.kt`, `component/TmrMotion.kt` | same | keep plus the `idle` spec (W2-A) | AGENTS.md motion registers. `reduced` must make Paige still. |
| `theme/Theme.kt` | same | rewrite: dark only (W1-E) | No light theme. |
| `theme/Elevation.kt`, `theme/Background.kt` | same | delete | The look is flat: surfaces #111, #161616 and #1C1C1C. |
| `res/font/*manrope*`, `*archivo_black*`, `fonts-licenses/*` | same | delete (W1-E); add Space Mono and Space Grotesk with OFL (W2-A) | This is the old Avvio type. |
| `icon/TmrIcons.kt` | same | rewrite (W2-A) | Prototype icon set. |
| `TmrPreviewTheme.kt` | component | rewrite (W2-A) | Dark only. |
| `TmrDock.kt` | component | delete in W3-A | `TmrTabBar` (W2-B) replaces it. `:app` uses it until W3. |
| `Button.kt`, `TmrIconButton.kt` | component | delete (W1-E); new `TmrPrimaryButton`, `TmrSecondaryButton`, `TmrTextButton`, top-bar icon buttons (W2-B, W2-C) | New pill styles. A disabled button still reports taps, so the screen can say why. |
| `TmrTextField.kt` | component | delete; new `TmrTextField`, `TmrTextArea`, `TmrLabeledField` (W2-C) | New style. |
| `TmrToast.kt`, `TmrSnackbar.kt` | component | delete; new `TmrToastHost` (W2-B) | One white toast at the top. |
| `TmrBottomSheet.kt`, `TmrSheet.kt` | component | delete; new `TmrBottomSheet` (W2-B) | #141414 sheet with top radius 32. |
| `TmrStepProgress.kt` | component | delete; new `TmrStepBar` (W2-B) | Profile, Job, Tailor pills. |
| `TmrScreen.kt`, `TmrInnerHeader.kt`, `TmrHomeHeader.kt`, `TmrHeaderBackdrop.kt`, `TmrHeaderCollapseState.kt`, `TmrBottomActionBar.kt` | component | delete; new `TmrTopBar` and the screen scaffold (W2-B) | Driven by the `T` table. |
| `TmrStatusChip.kt`, `TmrApplicationStatusChip.kt`, `TmrApplicationStatusKind.kt`, `TmrStatusKind.kt`, `TmrStatusDisc.kt` | component | delete; new `TmrStatusChip` (W2-D) | The `SS` colours. |
| `TmrProvenanceChip.kt`, `TmrProvenanceKind.kt`, `TmrEvidenceMark.kt` | component | delete; new `TmrSourceChip` (W2-D) | Two sources: "Your resume" and "Your answer". |
| `TmrTags.kt`, `TmrPillRow.kt`, `TmrFilterChip.kt` | component | delete; new `TmrTag`, `TmrKeywordChip`, `TmrSticker` (W2-C, W2-D) | New chip styles. |
| `TmrCard.kt`, `TmrDivider.kt`, `TmrSectionLabel.kt`, `TmrHeadline.kt` | component | delete; new versions (W2-C, W2-D) | New look. |
| `TmrMonogram.kt` | component | delete; new `TmrInitialDisc` (W2-D) | Initial disc on application rows and the avatar. |
| `TmrPaperColors.kt` | component | delete; new `TmrResumePaper` (W2-D) | One-page white preview with highlight spans. |
| `TmrCoverageBlock.kt`, `TmrMetrics.kt` | component | delete; new `TmrCoverageDelta` (W2-D, D5) | "Keywords matched now → up to". |
| `TmrSpotIllustration.kt`, `TmrSpotKind.kt`, `illustration/TmrIllustration.kt`, `illustration/TmrCharacterIllustration.kt` | component | delete; new `TmrPaige` (W2-C) | Paige replaces the old characters. |
| `TmrOfflineBanner.kt` | component | delete | No offline states. |
| `TmrErrorCallout.kt` | component | delete; the inline notice in W2-D covers "not a job post" and the required banner | Only the drawn error states remain. |
| `TmrCheckbox.kt`, `TmrConfirmDialog.kt`, `TmrIconConfirmDialog.kt` | component | delete | No dialogs or checkboxes are drawn. Toggles and sheets replace them. |
| `Background.kt`, `LoadingWheel.kt`, `TmrDesignValues.kt`, `TmrPrimitives.kt` | component | delete | Avvio internals. Progress rows replace the loading wheel. |
| `core/ui/*`: `ApplicationStatusChip`, `FactIdTag`, `FactProvenanceChip`, `KeywordCoverageMeter`, `RequirementStatusDisc`, `EmptyState`, `KeywordCoverageBar`, `FactSourceProvenance`, `ApplicationStatusKindMapper`, `ProvenanceSummaryRow`, `MatchStatusKindMapper`, `StatusLabels`, `ApplicationStatusSheet`, `DeleteItemDialog` | `core/ui` | delete (W1-F) | Old UI. W2 builds the new equivalents in `core:designsystem` (II.5). |

### 1.4 Core logic (selected; everything not listed follows its package verdict)

| Item | Path | Verdict | Reason |
|---|---|---|---|
| `FabricationGuard`, `offline/OfflineFabricationGuard`, the lexicons, `NumberClaims`, `ScaleClaims`, `OwnershipVerbs`, `EvidenceIndex`, `TokenSubset`, `AliasRewriter` | `core/domain` | keep, extend (W2-G) | Constitution I.1, must survive. It now also covers USER_ANSWER facts, the summary and the skills. |
| `TailorResumeUseCase`, `UpdateBulletDecisionUseCase` | `core/domain` | keep, extend (W2-G) | These are the I.1 and I.2 gates. `AcceptChangesUseCase` is added (D4). |
| `ResumeTailor`, `JobAnalysisSource`, `AnalyzeJobUseCase`, `GapMatcher`, `ResumeTextParser`, `ProposeJobLabelUseCase`, `KeywordsStatedIn`, `LocalAnalyzer` | `core/domain` | keep | II.6 interfaces. Detected "title · company" before analysis. Keyword coverage input. |
| `SignInGateway`, `SignInResult`, `SignInOutcome`, `SignInFailureReason`, `FirebaseUidProvider` | `core/domain` | keep | Google sign-in must survive. |
| `PaymentGateway`, `Purchase*`, `ApplicationPack`, `CreditSpend` | `core/domain` | keep, rewrite the pack list (W2-G) | Play Billing must survive. Packs are 5, 15 and 40. |
| `account/*` (`DeleteAccountUseCase`, wipe, `ExportAccountDataUseCase`, `AccountDataArchiveWriter`) | `core/domain` | keep | Delete account. Download my data uses the interim on-device archive (R6). |
| `coverletter/*`, `prep/*`, `ConsentUploader`, `UpgradedMatch`, `AddUserStatedFact(s)UseCase` (keep only if W2-G reuses it for answers) | `core/domain` | delete (W1-D) | Cut features. |
| `CoverLetterRepository`, `PrepPlanRepository`, `connectivity/*` | `core/data` | delete (W1-D) | Cut. No offline states. |
| `UsageAllowance`, `StoredUsageAllowance` | `core/data` | rewrite as `CreditsRepository` (W2-E) | Credit ledger. |
| `ContentReportRepository`, `PendingReportQueue` | `core/data` | keep, no UI | The Play AI-generated content policy (PRD 10.4, Google Play item 2) asks for an in-app report action. The prototype draws none. See R7. |
| `ExportHistoryRepository`, `TailoringReviewStateRepository`, `SessionRepository`, `PendingAccountWipe`, `ApplicationCleanup` | `core/data` | keep | Exports, the accept and undo state, the session, and deletion. |
| DTOs `CoverLetter.kt`, `Prep.kt`, `ConsentRequest`/`ConsentDto` | `core/network` | delete (W1-D) | Cut routes. |
| prod `GuardedCoverLetterSource`, `GuardedPrepQuestionSource`, `RemoteCoverLetterSource`, `RemotePrepQuestionSource`, `ConsentRevoker` | `app/src/prod` | delete (W1-D) | Cut. |
| prod `auth/*` (Credential Manager, Firebase, `SignOutCleaner`, `LocalDataWiper`, `SessionExpiryHandler`), `billing/*`, `ai/Guarded*` (job analysis, resume tailor), `RemoteResumeTextParser`, `network/NetworkConfigModule` | `app/src/prod` | keep (W5-N extends them) | Must survive: Google sign-in, Play Billing with purchase verification, and the backend client. |
| `ConnectivityBindings` (demo and prod) | `app/src/*/di` | delete (W1-D) | No offline states. |

### 1.5 Debug and dev tools

| Item | Path | Verdict | Reason |
|---|---|---|---|
| "TailorMyResume dev" launcher: `DebugScenarioActivity`, `DebugScenarioMenu`, `DebugMenuViewModel`, `DebugScenarioTarget` | `app/src/debug` | rewrite (W6-C) | It mirrors the prototype `index()` chips and the New user and Returning user presets. QA uses it to reach every state. |
| `DebugPreviewLifecycle`, `DebugPreviewLifecycleObserver`, `main/ai/DebugPreviewMode` | `app/src` | keep | Forced-state preview plumbing. |
| "Switch offline" debug action, `DebugOverridableConnectivityMonitor` | `app/src/debug`, `core/data` | delete (W1-D) | No offline states. |
| `DebugScenario` enum (19 values) | `core/model` | rewrite (W6-C) | Values become the prototype state names. |
| `SampleDataController`, `OfflineSampleDataController`, `SampleDataSet` | `core/domain/sample` | rewrite (W6-B) | Priya Deshmukh and Northwind GCC: the fresh and returning data. |
| `MockStateStore`, `DataStoreMockStateStore`, `MockLatency`, `DelayMockLatency` | `core/data/mock` | keep | Demo state persistence and timing (reading 3.5 s, analyzing 2.2 s, tailoring 4 s). |
| `ForcedPaymentScenario`, `MockPackCatalogue`, `OfflinePaymentGateway` | `core/domain/offline` | rewrite (W6-A) | Packs 5, 15 and 40. Forced "payment failed". |
| `prototype` entry in `.claude/launch.json` (from #255) | `.claude/launch.json` | keep | QA compares against the prototype on port 8767. |
| `tools/ci/verify-local.sh`, `check-constitution.sh`, `scan-secrets.sh`, `tools/setup.sh`, `tools/pre-push` | `tools/` | keep | Constitution gates. |
| Contract tests for deleted repositories (`CoverLetterRepositoryContractTest`, `PrepPlanRepositoryContractTest`, `ConnectivityMonitorContractTest`) | `core/testing` | delete (W1-D) | Their subjects are deleted. |
| Stale nested worktree `.claude/worktrees/sync` | `.claude/worktrees/sync` | out of scope | Not part of the build. Flagged for the orchestrator's worktree cleanup. |

## 2. Screen map

Every state that `index()`, `fresh()` or `returning()` can render, with its sheets, toasts and "Soon" rows.
- **Module column.** It gives the module's short name; all live under `feature/*/impl`.
- **Composable column.** It names the route composable. The screen composable is stateless (II.2).
- **NavKey column.** W3-A creates every key.
- **Backend column.** It lists the routes under `/v1/tailormyresume/` unless the row says otherwise.

### 2.1 States

| Prototype state (`index()` label or render branch) | Screen id | Module | Composable / key components | NavKey | Backend route(s) |
|---|---|---|---|---|---|
| Sign in, story 1/2/3 (auto 5 s, tap, hold) | `signin` | onboarding | `SignInRoute` → `SignInScreen`; `TmrStoryBars`, `TmrPaige(signin1..3)`, `TmrPrimaryButton` (Google) | `SignInNavKey` | Credential Manager + Firebase, then `GET /v1/me`, `GET /wallet` (free credit) |
| Signing in | `signin` (signingIn) | onboarding | same; label "Signing in…" | same | same |
| Sign-in cancelled (toast + Retry) | `signin` + toast | onboarding | `TmrToastHost` | same | none |
| Upload resume | `upload` | onboarding | `UploadScreen`; `TmrHeroCard(Blue)`, `TmrSticker` PDF and DOCX, `TmrUploadCard` | `UploadNavKey` | none (system picker; on-device text, contract D2) |
| Unreadable file | `uploadError` | onboarding | `UploadErrorScreen`; `TmrFileCard`, tips list | `UploadErrorNavKey` | none (`NoTextLayer` on device; a parse failure also lands here, R5) |
| Paste resume | `pasteResume` | onboarding | `PasteResumeScreen`; `TmrTextArea` | `PasteResumeNavKey` | none until Read text |
| Fill in myself | `manual` | onboarding | `ManualProfileScreen`; 6 × `TmrLabeledField` | `ManualProfileNavKey` | none (local profile) |
| Reading 0 → 100% | `reading` | onboarding | `ReadingScreen`; `TmrProgressRows` | `ReadingNavKey` | `POST /resume/parse` (v2 fields: summary, location, links, current) |
| Review profile, needs fix (90%) | `review` | onboarding | `ReviewProfileScreen`; required banner, `TmrCard` sections, disabled CTA plus toast "Add the missing end date first" | `ReviewProfileNavKey` | none |
| Review profile, fixed (96%; 100% with LinkedIn) | `review` (fixed) | onboarding | same | same | none |
| Add job, empty | `job` | analysis | `JobScreen`; `TmrHeroCard(Amber)`, `TmrTextArea`, Paste from clipboard, Use a link | `JobNavKey` | none |
| Add job, has text with detected label and character count | `job` | analysis | same; `ProposeJobLabelUseCase` | same | none |
| Not a job post | `job` (jdError) | analysis | same; inline notice | same | client < 200 chars, or `POST /analyses` → 422 `NOT_A_JOB_POST` |
| Analyzing 0 → 100% | `job` (analyzing) | analysis | same; "Reading the job… N%" | same | `POST /analyses` (v2) |
| Import link (+ toast "Imported from <host>") | `jobLink` | analysis | `JobLinkScreen`; `TmrLabeledField` | `JobLinkNavKey` | `POST /job-imports` (new) |
| Job analyzed, with credits | `jobResult` | analysis | `JobResultScreen`; `TmrCoverageDelta` (D5), `TmrKeywordChip`, must-have rows, note "Uses 1 credit · you have N" | `JobResultNavKey(applicationId)` | data from `POST /analyses`; balance from `GET /wallet` |
| Job analyzed, no credits (CTA → paywall, then back to Quick question) | `jobResult` | analysis | same; note "No credits left · get more to continue" | same | `GET /wallet` |
| Quick question, none picked (toast "Pick an answer, or skip") | `question` | analysis | `QuickQuestionScreen`; `TmrPaige(question)`, `TmrChoiceRow` × 3 | `QuickQuestionNavKey(applicationId)` | question from `POST /analyses` `question` |
| Quick question, picked (detail field for Yes or A few) | `question` | analysis | same; `TmrTextArea` | same | answer sent in `POST /tailorings` `answer` |
| Tailoring 0 → 100% | `tailoring` | tailor | `TailoringScreen`; `TmrHeroCard(Lime)`, `TmrProgressRows`, `TmrPaige(tailoring)` | `TailoringNavKey(applicationId)` | `POST /tailorings`, then `GET /tailorings/{id}` (1 credit on success) |
| Tailoring failed | `tailorFail` | tailor | `TailorFailedScreen`; `TmrPaige(sad)` | `TailorFailedNavKey` | failed job; credit released |
| Tailored resume, Resume tab (92%, or 84% without the answer) | `tailored` (tab resume) | tailor | `TailoredScreen`; `TmrSegmentedTabs`, `TmrResumePaper`, Export PDF disabled until accepted (D4) | `TailoredNavKey(applicationId)` | none (result stored locally) |
| Tailored resume, Changes tab with per-change Undo and Accept changes (D4) | `tailored` (tab changes) | tailor | same; change cards with `TmrSourceChip`, `TmrTag(kind)`, Undo, the Accept changes control | same | none |
| Edit resume (+ toast "Changes saved") | `editResume` | tailor | `EditResumeScreen`; `TmrTextArea`s with source tags | `EditResumeNavKey` | none |
| Exported, paid credit used | `exported` | tailor | `ExportedScreen`; `TmrFileCard`, credits line | `ExportedNavKey(applicationId)` | none (on-device PDF, contract D12) |
| Exported, free credit used | `exported` | tailor | same; "Free resume used. 0 credits left. Nothing was charged." | same | none |
| Exported, not marked: "Did you apply? ✓ Mark as Applied" | `exported` | tailor | same | same | none |
| Exported, marked Applied + toast "Applied, marked on <date>" with Undo | `exported` | tailor | same; status chip, "Marked on", Change (status sheet) | same | none |
| Exported "Soon" rows: Get prep questions, Write a cover letter (toast "Coming soon") | `exported` | tailor | `TmrSoonRow` × 2 | same | none |
| Applications, empty | `home` | applications | `ApplicationsScreen`; `TmrPaige(home)`, Add a job; `TmrTabBar` | `ApplicationsNavKey` | `GET /wallet` (credit count) |
| Applications, list (returning) | `home` | applications | same; `TmrStatusBar`, rows with `TmrInitialDisc` and `TmrStatusChip`, meta "<company> · NN% keywords" (D5) | same | `GET /wallet` |
| Application | `appDetail` | applications | `ApplicationDetailScreen`; `TmrFileCard`, next steps, 2 × `TmrSoonRow` | `ApplicationDetailNavKey(id)` | re-tailor goes to Quick question or the paywall (`GET /wallet`) |
| Status sheet (+ toast "Status set to X") | `appDetail` + sheet status | applications | `TmrBottomSheet` + 5 × `TmrChoiceRow` | same | none (local, Room) |
| Profile | `profile` | profile | `ProfileScreen`; `TmrListRow`s, `TmrTag(Add)`, "Built from … · Replace"; `TmrTabBar` | `ProfileNavKey` | none |
| Experience | `experience` | profile | `ExperienceScreen` | `ExperienceNavKey` | none |
| Edit role: edit, add, "I work here now" (toasts "Role saved", "Role deleted") | `editRole` | profile | `EditRoleScreen`; `TmrToggle` | `EditRoleNavKey(entryId?)` | none |
| Contact (toast "Contact saved") | `editContact` | profile | `EditContactScreen`; read-only email, amber LinkedIn border | `EditContactNavKey` | none |
| Skills (toast "Skills saved") | `skills` | profile | `SkillsScreen`; removable chips | `SkillsNavKey` | none |
| List edit: Summary, Education, Achievements (toast "Saved") | `listEdit` | profile | `ListEditScreen(section)` | `ListEditNavKey(section)` | none |
| Settings (toggle Product updates only) | `settings` | settings | `SettingsScreen`; `TmrListRow`, `TmrToggle` | `SettingsNavKey` | `GET /wallet` |
| Download my data toast | `settings` + toast | settings | interim: on-device archive and share sheet (R6) | same | `GET /me/export` today; `POST /me/data-export` when B12 ships |
| Help & feedback | `settings` | settings | email intent | same | none |
| Sign out (toast "Signed out") | `settings` → `signin` | settings | root to Sign in | same | none (Firebase sign-out, local wipe) |
| Delete account sheet (Type DELETE; toast "Account deleted") | `settings` + sheet delete | settings | `TmrBottomSheet`, `TmrPaige(sad)` | same | `DELETE /v1/me` |
| Credits | `credits` | settings | `CreditsScreen`; ledger rows | `CreditsNavKey` | `GET /wallet`, `GET /credits` (new) |
| Paywall, out of credits | `paywall` | settings | `PaywallScreen`; `TmrHeroCard(Amber)`, 3 × pack `TmrChoiceRow`, Best value | `PaywallNavKey(returnTo?)` | `GET /packs`, Play `ProductDetails` |
| Paywall, buying ("Processing…"), success toast "N credits added" | `paywall` (paying) | settings | same | same | Play Billing, then `POST /purchases` |
| Restore purchases (toast "No previous purchases found") | `paywall` | settings | same | same | Play `queryPurchasesAsync`, then `POST /purchases` per token |
| Payment failed sheet | `paywall` + sheet payFail | settings | `TmrBottomSheet` | same | none (Play error) |

The prototype shows the toasts "Opening share options…" and "Opening PDF…" only as stand-ins. The app opens the share sheet or the PDF viewer directly.

### 2.2 Chrome from the `T` table (W3-A)

| Screen | Step bar | Left | Title | Right action |
|---|---|---|---|---|
| upload | 1 | back if the stack is not empty | — | — |
| uploadError, pasteResume, manual, review | 1 | back | — | — |
| reading | 1 | none (noBack) | — | — |
| job | 2 | back if the stack is not empty | — | — |
| jobLink, jobResult | 2 | back | — | — |
| question, tailorFail, tailored | 3 | back | — | — |
| tailoring | 3 | none (noBack) | — | — |
| editResume | — | back | Resume | Save → back, toast "Changes saved" |
| exported | — | close → root Applications | — | — |
| appDetail | — | back | Application | — |
| experience, editContact, skills, listEdit | — | back | Profile | editContact Save; skills Done; listEdit Save |
| editRole | — | back | Experience | Save |
| settings | — | back | Profile | — |
| credits | — | back | Settings | — |
| paywall | — | close → back | — | — |
| signin, home, profile | no top bar (signin is full bleed; home and profile have their own headers and the tab bar) | | | |

The step labels come from `steps`:
- A done step reads "✓ Profile" in lime on #1C1C1C.
- The current step reads "N Name" in #0A0A0A on lime.
- A later step reads in #A6A6A6.

The back fallback is in `back()`. When the stack is empty, back goes to `home` if the user has applications or a fixed profile. Otherwise it goes to `upload`.

### 2.3 Truth rule for the quick-question answer (S4)

The prototype shows an "Added" Infosys bullet from an answer whose detail ("presented the monthly variance report to the CFO") does not name Infosys. Attaching it to Infosys would add an employer claim the candidate never made. That is a critical fabrication in PRD 4.1 item 3 and is forbidden by Constitution I.1.

The rule (W2-G on the device, B5b on the server):
- An answer-backed bullet may sit under experience entry E only when the detail text names E's organisation or title.
- Otherwise the answer can support only the summary and the skills line, and the Changes tab shows "Summary · New · Your answer".

With the prototype answer, the app therefore shows no Infosys "Added" card. The Changes count is 3 instead of 4. This is recorded as R3.

## 3. Design tokens and the Paige component

The phone screen is 374 x 834 dp (1 px = 1 dp). The app is dark only.

### 3.1 Colour (`TmrTheme.colors`)

| Token | Value | Use |
|---|---|---|
| `background` | #000000 | Every screen except Sign in |
| `surface` | #111111 | Cards, option rows |
| `surfaceRaised` | #161616 | Raised cards |
| `surfaceHigh` | #1C1C1C | Top-bar buttons, step pills, done steps |
| `sheet` | #141414 | Bottom sheet |
| `sheetOption` | #1A1A1A | Unselected option in a sheet |
| `uploadCard` | #0D0D0D | Upload card fill |
| `fill` | #242424 | Chips, inputs |
| `disabledFill` | #222222 | Disabled primary button |
| `line` | #2A2A2A | Unselected option border, lines |
| `lineStrong` | #333333 | Toggle off track |
| `lineHigher` | #3A3A3A | Dashed upload border, sheet handle, unselected border in sheets |
| `tabDivider` | #1A1A1A | Tab bar top line |
| `text` | #FFFFFF | Primary text |
| `textSecondary` | #C8C8C8 | Body copy |
| `textMuted` | #A6A6A6 | Labels, later steps |
| `textDisabled` | #7A7A7A | Placeholder, disabled, pending rows |
| `ink` | #0A0A0A | Text on light fills, Paige outline |
| `lime` | #A3F43F | Primary, selected, "Your resume", Interview |
| `limeSelected` | #141A0A | Selected option fill |
| `limeSoft` | #E4FBB8 | Keyword highlight on the paper |
| `amber` | #F7A940 | Needs attention, "Your answer", Applied, Offer fill, job hero |
| `amberHighlight` | #FFE2C2 | Answer highlight on the paper |
| `blue` | #5AA9F8 | Saved, resume and profile hero |
| `cheek` | #FF6A2B | Paige cheeks, DOCX sticker |
| `paper` | #FFFFFF | Resume paper, toast, Paige page |
| `paperFold` | #DCDCD5 | Paige folded corner |
| `segOffer` | #E9E8E4 | Offer segment in the status bar |
| `segRejected` | #555555 | Rejected segment |
| `rejectedBorder` | #444444 | Rejected chip border |
| `scrim` | #000000 at 65% | Sheet scrim |

The story slides use blue, amber and lime, full bleed. Their ink is #0A0A0A, and so is the status-bar ink on Sign in.

Status chips (`SS`):

| Status | Text | Fill | Border |
|---|---|---|---|
| Saved | #5AA9F8 | none | #5AA9F8 |
| Applied | #F7A940 | none | #F7A940 |
| Interview | #0A0A0A | #A3F43F | #A3F43F |
| Offer | #0A0A0A | #F7A940 | #F7A940 |
| Rejected | #A6A6A6 | none | #444444 |

### 3.2 Type (`TmrTheme.typography`)

Both families are bundled as TTF with their OFL licences: Space Mono 400 and 700, and Space Grotesk 400, 500, 600 and 700.

| Token | Family | Size / line height / letter spacing | Use (prototype count) |
|---|---|---|---|
| `headline` | Space Mono 400 | 28 / 1.1 / −1.0 (README "about −0.8") | Two-line hero headlines (22) |
| `headlineSmall` | Space Mono 400 | 26 / 1.1 / −1.0 | Sheet titles |
| `title` | Space Mono 400 | 22 / 1.15 / −0.6 | Section heads |
| `display` | Space Mono 700 | 34 / 1.0 / −1.5 | Coverage numbers, credit count |
| `displayLarge` | Space Mono 700 | 60 / 1.0 / −3 | Credits screen count |
| `label` | Space Mono 400 | 12 / — / 0.6, uppercase | Step pills, section labels, titles (34) |
| `labelWide` | Space Mono 400 | 12 / — / 0.8, uppercase | Hero labels |
| `button` | Space Mono 400 | 13 | Pills, top-bar action (18) |
| `mono15`, `mono14` | Space Mono 400 | 15, 14 | Rows, numbers |
| `body` | Space Grotesk 400 | 15 / 1.45 | Body (18) |
| `bodyLarge` | Space Grotesk 400 | 16 | Inputs, rows (10) |
| `bodySmall` | Space Grotesk 400 | 14 / 1.5 | Explanations |
| `caption` | Space Grotesk 400 | 13 | Meta lines |
| `strong` | Space Grotesk 600 and 700 | 15, 14 | Card titles, stickers |
| `paper*` | Space Grotesk | 7 to 9 | The resume paper preview (fixed scale) |

### 3.3 Shape (`TmrTheme.shapes`)

| Token | Radius (dp) | Use |
|---|---|---|
| `pill` | 999 | Buttons, chips, step pills, stickers (74 uses) |
| `hero` | 32 | Hero card, top of the bottom sheet |
| `cardLarge` | 24 | Upload card, big cards |
| `card` | 20 | Default card (30 uses) |
| `cardSmall` | 16 | Rows, Paige page (22 uses) |
| `toast` | 18 | Toast |
| `paperCorner` | 12 to 14 | Paper preview, job post card |
| `bar` | 2 | Story bars, status bar |

### 3.4 Spacing and sizes (`TmrTheme.spacing`)

- **Screen gutter.** 14 on setup screens.
- **Card padding.** 14x16 (the most common), 16, 12x16, 10x16, and 22 in the hero.
- **Gaps.** 2, 3, 4, 6, 8, 10, 12 (the most common), and 14.
- **Touch targets.** The top-bar buttons are 44 dp. The primary button height is 56 (34 uses), and secondary buttons are 52 or 48.
- **Tab bar.** Items are 54 high. The centre "+" is a 56 dp disc. Padding is 8/24/28.
- **Hero card.** About 290 high on Upload, with padding 22/22/0.
- **Toast.** It sits 96 dp from the top, with 14 on each side and a minimum height of 44. Its action pill is 40 high.
- **Sheet.** Padding 12/18/40, with a 44 x 4 handle.

### 3.5 Motion (`TmrTheme.motion`)

- **`proof` and `hop`.** Both stay. `hop` marks gap closed, exported, pack purchased, and the first fact confirmed (reused for Mark as Applied).
- **`idle` (new, W2-A).** Ambient loops:
  - Paige bob: translateY = 5 dp × sin(t / 420 ms + phase), so the period is about 2.64 s, with a per-screen phase of 0 to 4.
  - Tailoring wobble: rotate 4° × sin(t / 300 ms) and translateY 6 dp × sin(t / 210 ms).
  - Story bars: 5 s per slide.
  - Spinner: one turn per 900 ms.
  
  The logic block sets these timings (`bob`, `bt`, `tg.pT`, `spin`, `storyDuration`).
- **Timed progress.** Reading takes 3.5 s and analyzing 2.2 s. Tailoring takes 4 s in the demo. In prod it follows the real job.
- **Reduced motion.** When `TmrTheme.motion.reduced` is true, nothing moves:
  - Paige is a still at phase 0 with her pose.
  - The wobble stops.
  - The story slides do not auto-advance, but taps still step.
  - The spinner becomes a static ring.
  
  The state change stays visible (AGENTS.md).

### 3.6 Paige (`TmrPaige`, W2-C)

- **Page.** 124 x 154 dp, white #FFFFFF, a 2.5 dp #0A0A0A outline, radius 16. The folded top-right corner is 30 x 30 in #DCDCD5 with a 2.5 dp outline on its left and bottom edges and radius 0/12/0/10.
- **Eyes.** Two #0A0A0A pills, 48 dp from the top and 26 dp apart. Each pose sets their width, height and offset (`eyeW`, `eyeH`, `eyeT`). The default is 14 x 19.
- **Happy face.** Cheeks are #FF6A2B ovals, 16 x 9, at top 74, inset 16. The mouth is a U-shaped stroke of 2.5 dp at top 72, 22 x 12 by default (`mouthW`, `mouthH`).
- **Sad face.** No cheeks. The mouth is an inverted arc, 20 x 10, at top 78.
- **Text lines.** Two #0A0A0A lines of 4 dp at the bottom (inset 22, the second at 60% width).
- **Ticks.** Three strokes above the head (5 x 18–20, rotated −35°, 0° and 35°).
- **Arms.** Two white circles of 22 dp with a 2.5 dp outline at the sides (top 92, offset −16).
- **Poses** (the transform comes from the logic):

| Pose | Transform | Eyes | Mouth | Ticks | Arms | Sad |
|---|---|---|---|---|---|---|
| signin1 | rotate 4°, lift −46 | 14 x 19 | 22 x 12 | yes | yes | no |
| signin2 | x 56, rotate −8°, lift 10 | 11 x 15, x −4 | 16 x 8 | yes | yes | no |
| signin3 | x −30, rotate −5°, lift 16 | 18 x 6 | 30 x 15 | yes | yes | no |
| upload | rotate 4° + bob | default | default | yes | yes | no |
| job | rotate −6° + bob | 11 x 15, x −4 | 16 x 8 | no | yes | no |
| question | rotate 6° + bob | 13 x 17, y −3 | 14 x 8 | no | yes | no |
| tailoring | wobble | 16 x 6 | 20 x 10 | yes | no | no |
| fail | rotate −5° | 11 x 13, y 4 | — | no | yes | yes |
| home | rotate 5° + bob | default | default | yes | yes | no |
| paywall | rotate 5° + bob | default | default | no | yes | no |
| delete | rotate −4° | 11 x 13, y 4 | — | no | no | yes |

- **Motion.** The bob, wobble and story lift apply only through `graphicsLayer`, as `translationY` and `rotationZ`. Under reduced motion the static transform stays and the time-based part is 0.
- **Accessibility.** Paige is decorative: `contentDescription = null`.

## 4. Backend contract changes (contract v2)

Today's facts (`apps-backend` at 59a8abb):
- **Auth.** Firebase ID token plus `X-App-Id: tailormyresume`.
- **Storage.** The server stores no profile, application or JD (D1).
- **Wallet.** Two counters, `free_credits` and `purchased_credits`. There is no history.
- **Credit spend.** A credit is spent by `POST /applications/:id/unlock`.
- **Daily allowances.** 3 analyses and 1 free tailoring per day.
- **Consent gate.** Every AI route needs consents `ai-processing` and `age-18-plus` for notice `2026-10-b`.
- **Packs.** One pack, `application_pack_5`.
- **Analysis.** No numeric score, no reasons, no question.
- **Migrations.** Drizzle, forward only. There are no down migrations.
- **Health.** `GET /health` (not `/v1/health`).

The app consumes these routes from the prod source set. The demo flavour fakes them in W6-A.

| # | Route | Current shape | New shape | Migration (reversible?) | App side that consumes it | Issue |
|---|---|---|---|---|---|---|
| C1 | `GET /packs`, `POST /purchases` (credit packs 5, 15, 40) | `packs:[{productId:"application_pack_5",credits:5,creditsExpire:false}]` | Three entries: `application_pack_5` (5), `application_pack_15` (15), `application_pack_40` (40). Prices stay in Play Console (₹199, ₹449, ₹999). The purchase response is unchanged. | None (config). Play Console products are an owner step (W5-PLAY). | `RemotePaymentGateway`, `PaywallViewModel` | B2, W5-N2 |
| C2 | `GET /credits` (credit ledger, new) | none; only `GET /purchases` | `{credits:{balance, entries:[{id, kind: WELCOME\|PURCHASE\|TAILORING\|REFUND\|MIGRATION, amount, applicationId\|null, productId\|null, createdAt}]}}`, newest first, ≤ 100. The app joins `applicationId` with local Room data for the "Data Analyst / Halden Retail · 4 Oct" labels, and uses the Play price for "₹199 · 5 Oct". | New table `tailormyresume_credit_ledger` plus a backfill of one MIGRATION row per wallet. Additive. It is reversible only by a new drop-table migration, because drizzle has no down migrations. No existing data is changed. | `CreditsRepository` (prod), `CreditsViewModel` | B3, W5-N2 |
| C3 | `GET /wallet` | `{freeCredits, purchasedCredits, analysesLeftToday, freeTailoringsLeftToday, day, resetsAt, unlockedApplicationIds}` | `{credits, freeCredits, purchasedCredits, analysesLeftToday, day, resetsAt}`. The wallet is still created with 1 free credit on first read, plus a WELCOME ledger row. The app reads it right after sign-in. | None | Applications header, Job analyzed note, Settings, Paywall label | B3, B4 |
| C4 | `POST /tailorings`, `GET /tailorings/{id}` (when a credit is spent) | Checks for unlock, a credit or a free daily tailoring. Never debits. `section` regenerations. | Reserves 1 credit at start (402 `NO_CREDIT` at 0). On SUCCEEDED it debits 1 and writes a TAILORING ledger row in one transaction. On FAILED or INTERRUPTED it releases the credit ("Your credit wasn't used"). Each new `requestId` costs 1, including a re-tailor of the same application. `section` is removed. | Column `credit_reserved boolean default false` on the tailorings table. Additive and reversible by a drop-column migration. | `TailoringViewModel`, the re-tailor gate in `ApplicationDetailViewModel` | B4, W5-N1 |
| C5 | `POST /applications/{id}/unlock` | Spends 1 credit per application at first export | Removed. The `tailormyresume_unlocks` table stays for one release, then a later migration drops it. | None now. A later drop is irreversible (data loss of the unlock history), so export it first. | none (export is free once tailored) | B4 |
| C6 | Daily allowances | 3 analyses per day (429 `ALLOWANCE_EXHAUSTED`) and 1 free tailoring per day | The analysis cap rises to 20 as an abuse limit. Free tailorings are removed (the welcome credit replaces them). | None (config). | The analysis failure toast (R5) | B4 |
| C7 | `POST /analyses` (quick question) | `{generationId, job, matches, allowance}` | Adds `question: {requirementId, text, why, options:[YES_REGULARLY, A_FEW_TIMES, NOT_YET]} \| null`. At most one question: the first MUST_HAVE that is GAP or PARTIAL, of type EXPERIENCE or SOFT_SKILL. | None | `QuickQuestionViewModel` | B5a, W5-N1 |
| C8 | `POST /tailorings` (quick-question answer as a fact source) | No answer input. `source: IMPORTED\|USER_STATED\|USER_EDITED`. | Request `answer: {requirementId, choice: YES_REGULARLY\|A_FEW_TIMES\|NOT_YET\|SKIPPED, detail ≤ 400 \| null} \| null`. YES or A_FEW adds the fact `ans-<reqId>` with source `USER_ANSWER`, plus a bullet `ans-<reqId>-b1` when there is detail. The section 2.3 truth rule binds where it may be cited. The result change source shows "Your answer". | None (stateless; the tailoring row keeps its 24 h purge) | `TailorResumeUseCase`, Changes tab source chips | B5b, W2-G, W5-N1 |
| C9 | `POST /tailorings` result (summary and skills) | `bullets[]` only | Adds `summary: {text, sourceIds, verification} \| null` and `skills: {ordered[], added[]}`. `added` holds only user-stated or answer-grounded skills. The facts payload gains `summary: {id, text}`. | None | The Resume tab paper, the Changes tab "Summary · New" and "Skills · Reordered" | B8, W5-N1 |
| C10 | `POST /analyses` (reasons, location, not a job post) | No reasons, no location. No classifier: < 20 words is `INVALID_INPUT`. | Adds `job.location`, `matches[].reason` (≤ 120 chars, from cited evidence only), and error 422 `NOT_A_JOB_POST` (the claim is released). | None | `JobViewModel` (not a job post), `JobResultScreen` must-have rows | B6, W5-N1 |
| C11 | `POST /resume/parse` (one profile review) | `profile:{fullName,email,phone,headline,skills,entries[ref,category,title,organization,startDate,endDate,bullets]}` | Adds `summary`, `location`, `links:[{kind: LINKEDIN\|PORTFOLIO\|OTHER, url}]` and `entries[].current: boolean`. The review-once state lives on the device only (`reviewedAt` in Room); the server needs no flag. The required gap rule (end date missing and not current) runs on the device. | None | `ReadingViewModel`, `ReviewProfileViewModel` | B7, W5-N1 |
| C12 | `POST /job-imports` (import from a job link, new) | none (PRD 6.3 said no scraping; 6.1.2 wins) | `{url}` (https only) → `{jobText, sourceHost}`. 422 `JOB_IMPORT_FAILED` for under 200 characters or a blocked fetch. SSRF guards are in B9. The limit is 20 imports per day. | None. The claims table may need a new `kind` value: additive, reversible by a follow-up migration. | `JobLinkViewModel` | B9, W5-N1 |
| C13 | Export file name and page size | No server export (contract D12) | **No backend change.** The device builds the PDF. The file name follows `ResumeSettings.fileNameFormat` (default `Name_Company_Role`). Words are joined with "-" and parts with "_", as in `Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf`. The page size is A4 or Letter from `ResumeSettings`. Both are DataStore on the device. | Room v3 stores `exportFileName` on the application (W2-E). | `ExportFileName`, `ResumePdfRenderer`, Settings rows | W2-F, W4-C2, W4-F1 |
| C14 | Dropped consent fields | The AI routes need `ai-processing` and `age-18-plus` at `noticeVersion` "2026-10-b". The app posts `POST /v1/me/consents` (`ConsentRequest {purpose, granted, policyVersion}`). | The gate is removed from the AI routes. The app stops calling `POST /v1/me/consents` and deletes its DTOs (W1-D). The core route and the `consents` table stay for history and appear in `GET /me/export`. `noticeVersion` is removed from the TMR config. | None. Nothing is deleted, so the change is reversible by restoring the gate. | none | B1, W1-D |
| C15 | `POST /prep-questions`, `POST /cover-letters` | Live | Removed | None | none (deleted in W1-D) | B10 |
| C16 | Download my data | `GET /me/export` (synchronous JSON, no profile) | Target: `POST /me/data-export {device}` → 202. The server emails the merged JSON to the Google email and keeps nothing. **Blocked**: the backend has no email provider. Interim: the on-device archive goes through the share sheet (R6). | None | `SettingsViewModel` | B12 (revamp-blocker) |
| C17 | `DELETE /v1/me` | `202 {deletion:{status}}`; cascades all TMR tables | Unchanged. It must also cascade the new ledger table (B3 test). | None | Delete sheet | B3, W4-F1 |

### 4.1 Staging test identity

There is none. The server accepts only a real Firebase ID token for the TMR project. A grep of `src/`, `.github`, `Dockerfile`, `fly.toml` and `docs/` finds no bypass, no test user, no token script and no emulator support. B11 adds a script with no runtime bypass:
1. The owner runs it with the service account.
2. It creates a synthetic user `tmr-e2e-<n>`.
3. It mints a custom token and exchanges it for an ID token and a refresh token.
4. It writes them to a git-ignored file.

Until the owner runs it, the end-to-end API tests are blocked. File it as `revamp-blocker`.

## 5. Waves, lanes and the issue list

The rules:
- Within a wave, the lanes have disjoint file sets.
- At most 3 implementation lanes run at once (owner policy). Start the lanes in the order listed.
- Issues inside one lane run in sequence.
- An issue that depends on an issue in another lane waits for that merge.

**Batching.** W4 issues are already batched by screen area: each holds 4 to 9 drawn states and is one PR. W2-B, W2-C and W2-D each batch 5 to 9 components.

### W1: delete. The build stays green after every merge.

| Lane | Issues | File set |
|---|---|---|
| W1-A | W1-A | `feature/tailor/impl/**` |
| W1-B | W1-B | `feature/onboarding/impl/**`, `feature/profile/impl/**` |
| W1-C | W1-C | `feature/analysis/impl/**`, `feature/applications/impl/**`, `feature/settings/impl/**` |
| W1-D | W1-D (after A, B, C) | `core/{model,domain,data,network,testing}/**`, `app/src/**`, `feature/*/api/**` |
| W1-E | W1-E (after A, B, C; parallel to D) | `core/designsystem/**` |
| W1-F | W1-F (after C, D, E) | `core/ui/**`, `settings.gradle.kts`, `app/build.gradle.kts`, `feature/settings/impl/build.gradle.kts`, `.github/workflows/build.yml` |
| W1-G | W1-G (any time) | `docs/CONSTITUTION.md` |

Run order: A, B and C in parallel, then D and E in parallel, then F. G is a docs PR the orchestrator can merge at any point.

Each deleting lane replaces removed entries with `BasicText` placeholders. That keeps every remaining NavKey resolvable and needs no new design-system dependency.

### W2: new design system, plus the data and domain contract

| Lane | Issues | File set |
|---|---|---|
| W2-A | W2-A | `core/designsystem/.../theme/**`, `res/font/**`, `fonts-licenses/**`, `icon/**`, `TmrPreviewTheme.kt`, `core/screenshot/**`, `docs/DESIGN_SYSTEM.md`, `AGENTS.md` |
| W2-B | W2-B (after W2-A) | `core/designsystem/.../component/chrome/**`, its tests, its baselines, `strings_chrome.xml` |
| W2-C | W2-C (after W2-A) | `.../component/hero/**`, `.../component/input/**`, their tests, `strings_input.xml` |
| W2-D | W2-D (after W2-A) | `.../component/content/**`, its tests, `strings_content.xml` |
| W2-E | W2-E | `core/model/**`, `core/database/**`, `core/data/**`, `core/testing/**` |
| W2-F | W2-F (after W2-E) | `core/domain` packages `coverage`, `profile`, `onboarding`, `export`, and `di/DomainModule.kt` |
| W2-G | W2-G (after W2-E) | `core/domain` files `FabricationGuard`, `OfflineFabricationGuard`, `EvidenceIndex`, `EvidenceSources`, `TailorResumeUseCase`, `UpdateBulletDecisionUseCase`, `AcceptChangesUseCase`, `PaymentGateway`, `ApplicationPack`, `CreditSpend` |

Start W2-A and W2-E together. Then run B, C and D in parallel, with F and G in parallel. Each component lane uses its own string resource file and screenshot-name prefix, so the files stay disjoint inside one module.

### W3: navigation shell

| Lane | Issues | File set |
|---|---|---|
| W3-A | W3-A, then W3-B | `app/src/main/**`, `core/navigation/**`, `feature/*/api/**`, `feature/*/impl/**/navigation/*EntryProvider.kt`, `TmrDock.kt` (delete), `docs/ARCHITECTURE.md` section 6 |

W3 is one lane because it touches every entry provider. It is the only wave that edits all feature modules at once, so the W4 lanes never touch `:app` or another module.

### W4: flows. Six file-disjoint lanes, one per feature impl module; run at most 3 at once.

| Lane | Issues (in order) | File set | Suggested start group |
|---|---|---|---|
| W4-A onboarding | W4-A1 → W4-A2 → W4-A3 | `feature/onboarding/impl/**` | 1 |
| W4-B job | W4-B1 → W4-B2 | `feature/analysis/impl/**` | 1 |
| W4-C tailor | W4-C1 → W4-C2 | `feature/tailor/impl/**` | 1 |
| W4-D applications | W4-D1 | `feature/applications/impl/**` | 2 |
| W4-E profile | W4-E1 → W4-E2 | `feature/profile/impl/**` | 2 |
| W4-F settings and money | W4-F1 → W4-F2 | `feature/settings/impl/**` | 2 |

Screens open screens in other modules only through the `api` NavKeys created in W3-A. Examples: Review opens Edit role and Contact; Job analyzed opens the Paywall; the Applications header opens Credits.

### W5: backend, plus the app-side contract client

The app-side lanes:

| Lane | Issues | File set | Can start |
|---|---|---|---|
| W5-DOC | W5-DOC | `docs/BACKEND_CONTRACT.md` | at once, in parallel with W1 |
| W5-N | W5-N1 → W5-N2 | `core/network/**`, `app/src/prod/**` | after W2-G and W5-DOC; in parallel with W3 and W4 (disjoint files) |
| W5-OWNER | W5-PLAY | Play Console | at once (owner step, revamp-blocker) |

The `apps-backend` lanes. Each file is owned by one lane; a dependency serializes any shared file.

| Lane | Issues (in order) | File set | Can start in parallel with app W1–W3? |
|---|---|---|---|
| BE-A money | B2 → B3 → B4 | `config.ts`, `wallet.ts`, `purchases.ts`, new `ledger.ts`, `schema.ts`, `drizzle/**`, `export.ts`, `claims.ts`, the credit part of `tailoring.ts`, the unlock route in `routes.ts` | yes (after W5-DOC) |
| BE-B analysis | B6 → B5a | `analyses.ts`, `src/core/errors.ts` | yes |
| BE-C parse | B7 | `resume-parse.ts` | yes |
| BE-D tailoring | B8, then B5b (after B4, B8 and B5a) | `tailoring-pipeline.ts`, `facts.ts`, the `tailoring.ts` request schema (after B4) | B8 yes; B5b waits for B4 |
| BE-E routes and cleanup | B1, B9 (after B6), B10 (after B9 and app W1-D), B13 (last) | `consent.ts`, new `job-imports.ts`, `routes.ts` (except the unlock removal in B4), `prep-questions.ts`, `cover-letters.ts`, `docs/architecture.md`, `docs/compliance.md` | B1 and B9 yes |
| BE-F identity | B11 | `scripts/mint-test-token.ts`, `.gitignore`, the architecture.md test-identity paragraph (B13 merges after) | yes |
| BE-G email export | B12 | `export.ts` (after B3), `src/core/email/**` | blocked on the owner's email provider |

After each backend merge, confirm the deploy as the orchestrator prompt requires:
- `GET /health` on the Fly app answers.
- The changed route answers.

### W6: mock backend and sample data

| Lane | Issues | File set | Can start |
|---|---|---|---|
| W6-A | W6-A | `core/domain/.../offline/*` (payment, analysis, tailor, import, sign-in fakes), `app/src/demo/**` | after W2-F, W2-G and W5-DOC; may run during W4 |
| W6-B | W6-B | `core/domain/.../sample/**`, `core/testing/.../data/**` | after W2-E; may run during W3 and W4 |
| W6-C | W6-C | `app/src/debug/**`, `core/model/.../DebugScenario.kt` | after W6-A, W6-B and all of W4 |
| W6-D | W6-D | missing screenshot tests in `feature/*/impl/src/test/**`, `docs/MOCK_BACKEND.md` | after W6-C |

### Issue count

| Wave | App | Backend |
|---|---|---|
| W1 | 7 | 0 |
| W2 | 7 | 0 |
| W3 | 2 | 0 |
| W4 | 12 | 0 |
| W5 | 4 (W5-DOC, W5-N1, W5-N2, W5-PLAY) | 14 (B1–B13, counting B5a and B5b) |
| W6 | 4 | 0 |
| Total | 36 | 14 |

Routing:
- `in-harness-sonnet`: auth, secrets, payments, migrations, customer data, CI config, and the I.1 and I.2 truth code.
- `minimax-impl`: standard and UI code.
- `owner`: steps no agent can take.

### Ordered issue table
| Key | Repo | Title | Wave | Lane | Depends on | Routing | Security review |
|---|---|---|---|---|---|---|---|
| W1-A | app | chore(tailor): delete cover letter, prep questions, DOCX, export preview, section regeneration and the old tailor screens | W1 | W1-A | none | minimax-impl | no |
| W1-B | app | chore(onboarding,profile): delete welcome, consent, fact-by-fact confirmation, guided form, evidence path and the old profile screens | W1 | W1-B | none | minimax-impl | no |
| W1-C | app | chore(analysis,applications,settings): delete the share card, requirement sheets, notes, Your data and Account deleted screens and the old screens | W1 | W1-C | none | minimax-impl | no |
| W1-D | app | chore(core,app): remove consent, cover letter, prep, connectivity and other dead features from the domain, data, network and app layers | W1 | W1-D | W1-A, W1-B, W1-C | in-harness-sonnet | yes (touches sign-out and account wipe wiring) |
| W1-E | app | chore(designsystem): delete the Avvio components, the light theme and the Manrope and Archivo Black fonts | W1 | W1-E | W1-A, W1-B, W1-C | minimax-impl | no |
| W1-F | app | build: delete the core:ui module | W1 | W1-F | W1-C, W1-D, W1-E | in-harness-sonnet | no |
| W1-G | app | docs(constitution): point the screenshot ledger at the 2026-10-10 prototype | W1 | W1-G | none | in-harness-sonnet | no |
| W2-A | app | feat(designsystem): dark-only Paige tokens with Space Mono and Space Grotesk | W2 | W2-A | W1-E | minimax-impl | no |
| W2-B | app | feat(designsystem): screen chrome — top bar, step bar, tab bar, toast host and bottom sheet | W2 | W2-B | W2-A | minimax-impl | no |
| W2-C | app | feat(designsystem): Paige, hero card, buttons, choice rows, toggle and text fields | W2 | W2-C | W2-A | minimax-impl | no |
| W2-D | app | feat(designsystem): rows, chips, progress rows, status bar, resume paper and file card | W2 | W2-D | W2-A | minimax-impl | no |
| W2-E | app | feat(data): profile, application, credits and resume-settings data for the simplified flow (Room v3) | W2 | W2-E | W1-D | in-harness-sonnet | no |
| W2-F | app | feat(domain): keyword coverage, profile completeness, required gaps, start destination and export file names | W2 | W2-F | W2-E | minimax-impl | no |
| W2-G | app | feat(domain): quick-answer facts, summary and skills in the fabrication guard, and one Accept changes decision | W2 | W2-G | W2-E | in-harness-sonnet | yes (credits and packs) |
| W3-A | app | feat(app): navigation shell — setup flow with step bar, Applications/+/Profile tabs, back and close rules from T | W3 | W3-A | W2-B, W2-C, W2-D, W2-F | minimax-impl | no |
| W3-B | app | feat(app): start destination and session gate | W3 | W3-A | W3-A | in-harness-sonnet | yes (accounts) |
| W4-A1 | app | feat(onboarding): sign-in stories, Google sign-in and the sign-in cancelled toast | W4 | W4-A | W3-B | in-harness-sonnet | yes (accounts) |
| W4-A2 | app | feat(onboarding): upload, unreadable file, paste resume, fill in myself and reading | W4 | W4-A | W4-A1 | minimax-impl | no |
| W4-A3 | app | feat(onboarding): review profile once — required fix, LinkedIn and completeness | W4 | W4-A | W4-A2 | minimax-impl | no |
| W4-B1 | app | feat(job): add job, not a job post, analyzing and import from a link | W4 | W4-B | W3-A | minimax-impl | no |
| W4-B2 | app | feat(job): job analysis with keyword coverage and the quick question | W4 | W4-B | W4-B1 | minimax-impl | no |
| W4-C1 | app | feat(tailor): tailoring, tailoring failed, and the tailored resume with Changes, Undo and Accept changes | W4 | W4-C | W3-A, W2-G | in-harness-sonnet | no |
| W4-C2 | app | feat(tailor): edit resume, one-page PDF export with page size and file-name format, and the exported screen | W4 | W4-C | W4-C1 | minimax-impl | no |
| W4-D1 | app | feat(applications): Applications home, application detail and the status sheet | W4 | W4-D | W3-A | minimax-impl | no |
| W4-E1 | app | feat(profile): profile overview, experience list and role editor | W4 | W4-E | W3-A | minimax-impl | no |
| W4-E2 | app | feat(profile): contact, skills and list editors for summary, education and achievements | W4 | W4-E | W4-E1 | minimax-impl | no |
| W4-F1 | app | feat(settings): settings, download my data, sign out and the delete account sheet | W4 | W4-F | W3-B | in-harness-sonnet | yes (accounts, deletion, data export) |
| W4-F2 | app | feat(credits): credits ledger, paywall with 5, 15 and 40 packs, payment failed sheet and restore | W4 | W4-F | W4-F1, W2-E, W2-G | in-harness-sonnet | yes (money) |
| W5-DOC | app | docs(contract): backend contract v2 for the simplified MVP | W5 | W5-DOC | none | in-harness-sonnet | no |
| W5-N1 | app | feat(network): contract v2 DTOs and remote sources for parse, analysis, quick question, tailoring and link import | W5 | W5-N | W5-DOC, W2-G | in-harness-sonnet | no |
| W5-N2 | app | feat(billing): three packs, the credit ledger and spend-per-tailoring in the prod flavour | W5 | W5-N | W5-N1, W2-E | in-harness-sonnet | yes (money) |
| W5-PLAY | app | chore(release): create the 15 and 40 credit products in Play Console and set pack prices | W5 | W5-OWNER | none | owner | yes (money) |
| B1 | backend | feat(tailormyresume): drop the consent gate from the AI routes | W5 | BE-E | W5-DOC | in-harness-sonnet | no |
| B2 | backend | feat(tailormyresume): credit packs of 5, 15 and 40 | W5 | BE-A | W5-DOC | in-harness-sonnet | yes (money) |
| B3 | backend | feat(tailormyresume): credit ledger table and GET /v1/tailormyresume/credits | W5 | BE-A | B2 | in-harness-sonnet | yes (money, data export) |
| B4 | backend | feat(tailormyresume): spend one credit per successful tailoring; drop unlock and the daily free tailoring | W5 | BE-A | B3 | in-harness-sonnet | yes (money) |
| B6 | backend | feat(tailormyresume): must-have reasons, job location and NOT_A_JOB_POST in analyses | W5 | BE-B | W5-DOC | minimax-impl | no |
| B5a | backend | feat(tailormyresume): one quick question in the analysis answer | W5 | BE-B | B6 | minimax-impl | no |
| B7 | backend | feat(tailormyresume): summary, location, links and current-role flag in resume parse | W5 | BE-C | W5-DOC | minimax-impl | no |
| B8 | backend | feat(tailormyresume): tailoring returns a verified summary and skills order | W5 | BE-D | W5-DOC | in-harness-sonnet | no |
| B5b | backend | feat(tailormyresume): the quick-question answer as a tailoring fact source | W5 | BE-D | B4, B8, B5a | in-harness-sonnet | no |
| B9 | backend | feat(tailormyresume): import a job post from a link | W5 | BE-E | B6, B1 | minimax-impl | no (owner policy; SSRF guards are acceptance criteria, see PLAN R4) |
| B10 | backend | chore(tailormyresume): delete the prep-questions and cover-letters routes | W5 | BE-E | B9, W1-D | minimax-impl | no |
| B11 | backend | chore: staging test identity for API testing without Google sign-in on a device | W5 | BE-F | none | in-harness-sonnet | yes (accounts) |
| B12 | backend | feat(tailormyresume): email the data export to the Google account email | W5 | BE-G | B3 | in-harness-sonnet | yes (data export) |
| B13 | backend | docs: TailorMyResume section of architecture.md and compliance.md for contract v2 | W5 | BE-E | B1, B4, B5b, B7, B9, B10 | minimax-impl | no |
| W6-A | app | feat(demo): offline fakes for contract v2 — packs, ledger, quick question, link import and the five failures | W6 | W6-A | W2-F, W2-G, W5-DOC | minimax-impl | no |
| W6-B | app | feat(demo): Priya Deshmukh and Northwind GCC sample data for New user and Returning user | W6 | W6-B | W2-E | minimax-impl | no |
| W6-C | app | feat(debug): developer menu mirrors the prototype index | W6 | W6-C | W6-A, W6-B, W4-A3, W4-B2, W4-C2, W4-D1, W4-E2, W4-F2 | minimax-impl | no |
| W6-D | app | test: one screenshot baseline per prototype state at 374 x 834 and docs/MOCK_BACKEND.md for v2 | W6 | W6-D | W6-C | minimax-impl | no |

## 6. Risks and unknowns

### 6.1 Constitution (Article VII)

- **No amendment of a rule or gate is forced.** D4 keeps I.2 and D5 keeps I.3. I.1 stays because of the strict answer rule (section 2.3). I.5 is unchanged: the backend only, prod only, one HTTP stack. Consent S4 was already struck in #255.
- **A recommended Article VII.1 edit (W1-G).** The Roborazzi ledger row says "the designs in `design/claude-design` are the acceptance criterion". That points at the old design. Change the wording to the prototype and record D4 and D5 in a ledger row. The rule texts of Articles I to VI do not change.
- **Gate names that must survive the deletions.** These are I.1 `OfflineFabricationGuardTest` and `TailorResumeUseCaseTest`, and I.2 `UpdateBulletDecisionUseCaseTest` and `TailorViewModelTest`. W1-A keeps `TailorViewModel` and its test on purpose. If a lane deletes one of these, VII.3 requires a rule-text change in the same PR. Do not do that.
- **III.3.** A lint baseline may only shrink, and a new `lint-baseline.xml` file is an amendment. The new feature code must be lint-clean. The baselines that exist today (`app`, `core/navigation`, `feature/analysis/impl`, `feature/tailor/impl`) can only lose entries.
- **The Kover ledger.** `koverVerify` needs 80% line coverage on `core:domain` and `core:data`. W1-D deletes code and its tests together. W2-E, W2-F and W2-G add tests with the code. Watch the ratio in W1-D, because deleting well-tested cover-letter and prep code can lower it.
- **The ViewModel test rule.** "Every ViewModel test class must exist for a ViewModel that exists". Delete a ViewModel and its test together. Create each new ViewModel with its test (IV.1).

### 6.2 Gradle module-graph hazards when deleting

- **`core:ui` removal (W1-F).** It must edit `settings.gradle.kts`, `app/build.gradle.kts`, `feature/settings/impl/build.gradle.kts` and `.github/workflows/build.yml`, which lists `:core:ui:testDebugUnitTest`. A leftover task name fails CI. That is why W1-F is in-harness, which the CI-config routing also requires.
- **No other module is removed or renamed (S1).** `build.yml` hard-codes the task lists of every feature and core module (lines 183–194 and 250–261). A rename would need a CI edit in each wave.
- **The order of the delete lanes.** W1-A, B and C edit only `impl` modules. `TmrApp`, the debug targets and the `api` modules still reference the old NavKeys, so the old keys stay until W1-D. W1-D and W1-E must not start before A, B and C merge. Otherwise they delete classes a lane still compiles against.
- **`feature:settings:impl` depends on `core:ui`.** W1-C removes the last imports. W1-F removes the dependency line.
- **Configuration cache and isolated projects are on (V.4).** A deleted module that a convention plugin or the root `build.gradle.kts` still names fails at configuration time. Grep `build-logic` and the root build file in W1-F.
- **The `api` modules.** `feature/settings/api` has `api(projects.core.model)`. The other `api` modules depend on `core:model` through the plugin. Adding keys with arguments (`applicationId`, `section`) needs `@Serializable` types from `core:model` only (II.1).

### 6.3 Roborazzi baselines

- **Deleted in W1.** 511 PNG baselines:
  - 166 tailor, 114 onboarding, 73 profile, 54 settings, 48 applications and 42 analysis (all in W1-A, W1-B and W1-C);
  - 14 designsystem (W1-E).
  
  Their screenshot test classes go with them.
- **Recorded fresh.** Each W2 component issue records its own baselines. Each W4 issue records one baseline per drawn state, on the new 374 x 834 `Prototype` device (W2-A), with `PrototypeFixtures` data. W6-D audits against section 2 so that no state lacks a baseline.
- **Not re-recorded for motion.** AGENTS.md: "A settled frame must not change." A baseline captures the settled frame, with Paige at phase 0. Record progress screens at fixed percentages (0, 40, 100), using `TestClock` or virtual time, never wall time.
- **Determinism.** The shared ThreadedRenderer race in Robolectric 4.16 is fixed in `captureScreenHh` (memory note). Keep the new device inside that helper.
- **Stale copies.** `.claude/worktrees/sync` holds old copies of the baselines. It is not tracked by the build, but a careless `git add -A` in a lane worktree could pick it up. Tell the lane agents to stage paths explicitly.

### 6.4 The dependencyGuard baseline

- The baselines are `app/dependencies/demoReleaseRuntimeClasspath.txt` (160 lines) and `prodReleaseRuntimeClasspath.txt` (202 lines).
- **W1.** It probably does not change the classpath. DOCX export uses JDK zip only, and the connectivity code uses platform APIs. If W1-D removes the last user of a library, update the baseline in the same PR with `./gradlew dependencyGuardBaseline` and give the reason (V.1).
- **W2-A.** Bundled TTF fonts add no dependency. Do not add `ui-text-google-fonts`: downloadable fonts would be a new release dependency, and a provider fetch conflicts with the I.5 spirit.
- **W5-N.** It must not add libraries. OkHttp, Retrofit, kotlinx.serialization, Play Billing, Credential Manager and Firebase Auth are already in the prod baseline.

### 6.5 Product and contract risks

| # | Risk | Default the plan takes | Who decides |
|---|---|---|---|
| R1 | D5 changes visible copy in several places: the story 3 sticker, the Job analyzed numbers, the paper badge and the home row meta. "92 match" becomes "92%" plus "Keywords matched" / "keywords". | Built as D5 says. Every W4 copy check fails on the word "match" used as a score. | done (D5) |
| R2 | D4 adds one control the prototype does not draw: Accept changes, with Export disabled until the user takes it. | One full-width control above the Edit and Export row on both tabs, styled like the secondary pill. The disabled Export toast says "Accept the changes first". | done (D4); styling in W4-C1 review |
| R3 | The truth rule for answers (section 2.3) removes the prototype's Infosys "Added" card when the answer does not name the employer. The Changes count is then 3, not 4. | The strict rule (I.1 wins). | owner may revisit only by naming the role in the question UI (not drawn) |
| R4 | `POST /job-imports` is a server-side fetch of a user URL, an SSRF risk. Owner policy sends to security review only accounts, money, deletion and data export. | minimax-impl. The SSRF test list is in the acceptance criteria. The scout recommends the orchestrator add a security review anyway. | orchestrator |
| R5 | Failures that the prototype does not draw: an analysis error or allowance hit, a parse error, a job-import error, and a PENDING Play purchase. Extra error states are banned. | Map each one to a drawn state or the drawn toast pattern: <ul><li>An analysis failure shows the toast plus Retry (the same pattern as "Sign-in cancelled").</li><li>A parse failure goes to "Unreadable file".</li><li>An import failure goes to "Not a job post".</li><li>The analysis cap rises to 20 per day, so users rarely hit it.</li></ul> | done (scout) |
| R6 | Download my data: the prototype says "We'll email your data … within 24 hours". The backend has no email provider and holds no profile (D1). | Interim: the existing on-device archive (`ExportAccountDataUseCase` plus `server.json`) goes through the share sheet. The toast copy then differs from the prototype. B12 carries the emailed version as a `revamp-blocker`: the owner picks a provider and adds the secret. | owner (provider) |
| R7 | The Google Play AI-generated content policy (PRD 10.4 item 2) expects an in-app "report" action. The prototype draws none. | Keep `ContentReportRepository` and the backend route, with no UI. "Help & feedback" (email) is the only drawn path. Flag it before the Play submission. | owner, before release |
| R8 | The prototype's Paige bob, wobble and story timing need an ambient-loop spec. AGENTS.md allows two registers only and "no duration scale". | Add `TmrTheme.motion.idle` as one named spec, not a scale. W2-A updates the AGENTS.md Motion section. Not a Constitution change. | orchestrator (AGENTS.md edit) |
| R9 | "Fitting to 1 page" and "Still fits on 1 page" have no drawn overflow state. | The PDF renderer steps the body text down (10.5 → 9.5 pt) and line spacing until it fits. The prototype data must fit at the first step. If a longer real profile overflows, the export is 2 pages and the indicator hides. QA tracks it. | QA |
| R10 | A pending Play purchase, or a user cancel, on the paywall. | A cancel shows nothing. Pending shows a toast "Payment is pending. Credits arrive when it completes." The payment failed sheet is only for a real failure. | done (scout) |
| R11 | There is no staging test identity (section 4.1). | B11 script. Running it is an owner step. Until then, the end-to-end API steps use the emulator's Google account only. | owner |
| R12 | Play Console must have `application_pack_15` and `application_pack_40` at ₹449 and ₹999, and `_5` at ₹199. | W5-PLAY, `revamp-blocker`. The demo flavour shows these prices from `MockPackCatalogue`. | owner |
| R13 | The backend credit model changes (unlock is removed). The app on main still calls unlock until W4 and W5-N land. Staging only, no public release. | Accept a short break on staging between B4 and W5-N2. Or merge B4 after W5-N2 is ready, which is the preferred order if the e2e run is near. | orchestrator |
| R14 | The Room v3 migration maps NO_RESPONSE to APPLIED and drops the notes from the domain model; the notes column stays in Room. | The migration is additive. The column is ignored, not dropped, which avoids a table rebuild. | done |
| R15 | Data minimisation. The ledger keeps `applicationId` and `productId` only. Job titles and company names stay on the device, so a reinstall shows generic labels. | Accept (contract D1). | done |

### 6.6 Hot files to watch during parallel work

| File | Owned by | Others must not edit |
|---|---|---|
| `app/src/main/**` | W3-A, then W3-B | every W4 lane |
| `feature/*/api/**` | W1-D, then W3-A | W4 lanes (they only use keys) |
| `core/domain/.../di/DomainModule.kt` | W2-F | W2-G |
| `.github/workflows/build.yml` | W1-F | everyone else |
| apps-backend `routes.ts` | BE-E (B9, B10) and B4 (the unlock removal only) | — |
| apps-backend `src/core/errors.ts` | B6 (adds `NOT_A_JOB_POST` and `JOB_IMPORT_FAILED`) | B9 uses them |
| apps-backend `tailoring.ts` | B4, then B5b | B8 (pipeline file only) |
| `docs/ARCHITECTURE.md` | W1-F (section 2), W3-A (section 6), W6-C (debug menu) | — |

No issue uses `git stash` across worktrees, as the memory note on the shared stash stack requires.
