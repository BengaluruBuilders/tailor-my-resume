# TailorMyResume prototype — architecture spec

This spec binds every implementation agent. The reference is Now in Android (NiA) at
`/Users/aibuilders/Documents/dev/products/_ref/nowinandroid` (commit a49ed25). Read it locally. Copy its
patterns, build logic, and versions. Do not fetch it from the web.

Product context: `docs/PRD.md` in the repo. The prototype covers PRD features F1, F2, F3, F4 (PDF only), F5.
The prototype has no backend. All "AI" steps run as deterministic, offline Kotlin code behind interfaces, so a
backend implementation can replace them later (dependency inversion). Sign-in, payment, and the account state
are mock implementations. `docs/MOCK_BACKEND.md` describes them.

The current look follows `docs/AVVIO_REDESIGN.md`. `docs/DESIGN_SYSTEM.md` holds the tokens and components.

## 1. Coding standards

- Follow NiA: Kotlin, Jetpack Compose, Material 3, Hilt, Room, Coroutines/Flow, Navigation 3, unidirectional
  data flow, `UiState` sealed interfaces, `StateFlow` from `ViewModel` via `stateIn(WhileSubscribed(5_000))`,
  `collectAsStateWithLifecycle`, stateless screen composables with a stateful `...Route`/entry wrapper.
- SOLID: one responsibility per class; depend on interfaces; small focused interfaces; no god classes; no god
  functions (keep functions under ~40 lines).
- **No code comments.** No KDoc, no license headers, no section dividers, no commented-out code. Use names.
  The only allowed comment names an external ground (bug ID, spec line). This rule is enforced by a hook.
- No `!!`. No `GlobalScope`. Inject dispatchers with the `@Dispatcher(IO)` qualifier as NiA does.
- Tests: JUnit4 + Truth + Turbine + kotlinx-coroutines-test, as in NiA. Use test fakes from `core:testing`,
  not mocking libraries.
- Package root: `com.tailormyresume`. Namespaces: `com.tailormyresume.core.model`, `com.tailormyresume.feature.profile.impl`, etc.
  Application ID: `com.tailormyresume.app`.
- Convention plugin IDs: `tailormyresume.android.application`, `tailormyresume.android.application.compose`,
  `tailormyresume.android.library`, `tailormyresume.android.library.compose`, `tailormyresume.android.feature.api`,
  `tailormyresume.android.feature.impl`, `tailormyresume.android.room`, `tailormyresume.hilt`, `tailormyresume.jvm.library`.
- minSdk 26, compileSdk/targetSdk 36. JDK 17 toolchain (host has JDK 17 only).

## 2. Modules

| Module | Type | Content |
|---|---|---|
| `:app` | application | `TailorMyResumeApplication` (@HiltAndroidApp), `MainActivity`, `TmrApp`, `AppViewModel`, `AppRootState`, the three top-level destinations, Nav3 wiring. The `debug` source set holds the developer menu |
| `:core:model` | jvm library | Pure Kotlin data models (section 3) |
| `:core:common` | android library | `Dispatcher` qualifier, `TmrDispatchers`, dispatchers + application-scope DI modules, `Result` wrapper |
| `:core:designsystem` | library compose | `TmrTheme` with the Avvio-inspired tokens (colour, type, shape, spacing, elevation, motion). `TmrScreen` and the headers `TmrHomeHeader` and `TmrInnerHeader`. `TmrDock`, `TmrBottomActionBar`, sheets, dialogs, cards, chips, and text fields. `docs/DESIGN_SYSTEM.md` lists every component |
| `:core:navigation` | library | NiA `Navigator` and `NavigationState` pattern. `PendingNavigation` holds keys to push when the main root opens |
| `:core:database` | library + room + hilt | Room DB, entities, DAOs, type converters. It holds the profile and the applications |
| `:core:data` | library + hilt | Repository interfaces and offline-first implementations. `SessionRepository` and `ExportHistoryRepository` store their state in `MockStateStore` (DataStore). `ConnectivityMonitor` reports the network state |
| `:core:domain` | library + hilt | JD analysis, gap match, tailoring, fabrication guard, resume text parser, cover letter, prep questions, fact validation, use cases. Gateways: `SignInGateway`, `PaymentGateway`, `AccountDataExporter`, `SampleDataController` |
| `:core:network` | library + hilt | The one HTTP stack (I.5): OkHttp, Retrofit, kotlinx.serialization. `TailorMyResumeApi` (one function per route of `docs/BACKEND_CONTRACT.md`), the DTOs and their pure mappers to `core:model`, the `X-App-Id` and bearer interceptors with one 401 retry, the sealed `ApiError`, `apiResult`, and `IdTokenProvider`. Only `prod` bindings use it: `:app` adds it as `prodImplementation`, and no other module declares a network library |
| `:core:testing` | library | Test repositories and gateways, contract tests, `MainDispatcherRule`, test data |
| `:core:screenshot` | library | Roborazzi screenshot helpers and test devices |
| `:feature:onboarding:{api,impl}` | feature | Welcome, Paste JD, Sign in, Consent, Import resume, Confirm your facts (Flow 1, S1 to S6) |
| `:feature:analysis:{api,impl}` | feature | Gap analysis and save as application (S7) |
| `:feature:tailor:{api,impl}` | feature | Tailored resume review, bullet review, cover letter, prep questions, export preview, application pack, exported, credits (S8 to S15) |
| `:feature:profile:{api,impl}` | feature | Profile, fact editor, guided form, evidence path (S16 to S19). Top-level |
| `:feature:applications:{api,impl}` | feature | Application list and workspace (S20, S21). Top-level, start destination of the main root |
| `:feature:settings:{api,impl}` | feature | Settings, your data, delete account (S22 to S24). Top-level |

`docs/DESIGN_SYSTEM.md` and `design/02-screens.md` list the screens of each module.

Dependency rules: features depend on `core:*` and on other features' `api` only. `core:domain` depends on
`core:data` and `core:model`. `core:model` depends on nothing Android. `core:network` depends on `core:model` only,
and only `:app` (as `prodImplementation`) and remote bindings of the `prod` flavour depend on it.

### Flavours

`:app` has one flavour dimension, `backend`, with two flavours. `demo` binds the on-device implementations of
`docs/MOCK_BACKEND.md`, keeps the developer menu and sample data, and has no `INTERNET` permission and no
network classes. `prod` binds the remote implementations through the files in `app/src/prod/kotlin/.../di`
and declares `INTERNET`. Room stays the local source of truth in both. In `prod` these are remote:
sign-in and account deletion, resume parsing, job analysis, tailoring, prep questions, the cover letter,
payment and the wallet, content reports (saved locally, then posted), and the data export (local files plus
`server.json`). Profile, applications, and review state are local in both flavours.

Prod sign-in (`app/src/prod/.../auth`) uses Firebase Auth and Credential Manager without the google-services
plugin. It reads four Gradle properties (in `gradle.properties` or `~/.gradle/gradle.properties`, never in git):
`tailormyresumeWebClientId`, `tailormyresumeFirebaseApiKey`, `tailormyresumeFirebaseAppId`, and `tailormyresumeFirebaseProjectId`. Each
defaults to empty. If one is empty, sign-in shows the sign-in-failed state and the app still starts.

## 3. Shared models (`:core:model`, package `com.tailormyresume.core.model`)

Use these names and shapes exactly. Timestamps use `kotlin.time.Instant` (Kotlin 2.3, stable).

```kotlin
enum class EntryCategory { EDUCATION, EXPERIENCE, PROJECT, CERTIFICATION, ACHIEVEMENT }
enum class FactSource { IMPORTED, USER_STATED, USER_EDITED }

data class EvidenceBullet(val id: String, val text: String)

data class ProfileEntry(
    val id: String,
    val category: EntryCategory,
    val title: String,
    val organization: String,
    val startDate: String,
    val endDate: String,
    val bullets: List<EvidenceBullet>,
    val source: FactSource,
    val isConfirmed: Boolean,
)

data class CandidateProfile(
    val fullName: String,
    val email: String,
    val phone: String,
    val headline: String,
    val skills: List<String>,
    val entries: List<ProfileEntry>,
)

enum class RequirementType { SKILL, TOOL, EXPERIENCE, EDUCATION, SOFT_SKILL }
enum class RequirementPriority { MUST_HAVE, NICE_TO_HAVE }

data class JobRequirement(
    val id: String,
    val text: String,
    val type: RequirementType,
    val priority: RequirementPriority,
    val keywords: List<String>,
)

data class JobDescription(
    val title: String,
    val company: String,
    val rawText: String,
    val requirements: List<JobRequirement>,
)

enum class MatchStatus { MET, PARTIAL, GAP }

data class RequirementMatch(
    val requirement: JobRequirement,
    val status: MatchStatus,
    val evidenceIds: List<String>,
)

data class KeywordCoverage(val covered: Int, val total: Int)

data class GapAnalysis(
    val matches: List<RequirementMatch>,
    val keywordCoverage: KeywordCoverage,
)

enum class EditType { REWORD, REORDER, SHORTEN, EMPHASISE, MERGE }
enum class BulletDecision { PENDING, ACCEPTED, REJECTED }

sealed interface GuardrailViolation {
    data object MissingSource : GuardrailViolation
    data class UnsupportedNumber(val value: String) : GuardrailViolation
    data class UnsupportedTerm(val term: String) : GuardrailViolation
    data class VerbEscalation(val from: String, val to: String) : GuardrailViolation
    data class UnsupportedScaleClaim(val phrase: String) : GuardrailViolation
}

data class TailoredBullet(
    val id: String,
    val entryId: String,
    val originalText: String,
    val proposedText: String,
    val sourceIds: List<String>,
    val editTypes: List<EditType>,
    val keywordsUsed: List<String>,
    val violations: List<GuardrailViolation>,
    val decision: BulletDecision,
)

data class TailoredResume(val bullets: List<TailoredBullet>)

enum class ApplicationStatus { SAVED, APPLIED, INTERVIEW, OFFER, REJECTED, NO_RESPONSE }

data class JobApplication(
    val id: String,
    val job: JobDescription,
    val status: ApplicationStatus,
    val notes: String,
    val gapAnalysis: GapAnalysis?,
    val tailoredResume: TailoredResume?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

## 4. Repository interfaces (`:core:data`, package `com.tailormyresume.core.data.repository`)

```kotlin
interface ProfileRepository {
    fun observeProfile(): Flow<CandidateProfile?>
    suspend fun saveProfile(profile: CandidateProfile)
    suspend fun clearProfile()
}

interface ApplicationRepository {
    fun observeApplications(): Flow<List<JobApplication>>
    fun observeApplication(id: String): Flow<JobApplication?>
    suspend fun upsertApplication(application: JobApplication)
    suspend fun updateStatus(id: String, status: ApplicationStatus)
    suspend fun updateNotes(id: String, notes: String)
    suspend fun deleteApplication(id: String)
}
```

`SessionRepository` and `ExportHistoryRepository` are also in `:core:data`. `docs/MOCK_BACKEND.md` gives their
signatures.

Test fakes in `:core:testing`: `TestProfileRepository`, `TestApplicationRepository` (backed by
`MutableSharedFlow`/`MutableStateFlow`, like NiA's test repositories), plus `MainDispatcherRule` and
`com.tailormyresume.core.testing.data` sample objects (`sampleProfile`, `sampleApplication`).

## 5. Domain interfaces (`:core:domain`, package `com.tailormyresume.core.domain`)

```kotlin
interface JobDescriptionAnalyzer { fun analyze(rawText: String): JobDescription }
interface GapMatcher { fun match(profile: CandidateProfile, job: JobDescription): GapAnalysis }
interface ResumeTailor { fun tailor(profile: CandidateProfile, job: JobDescription, gap: GapAnalysis): TailoredResume }
interface FabricationGuard { fun check(proposedText: String, sources: List<EvidenceBullet>, profile: CandidateProfile): List<GuardrailViolation> }
interface ResumeTextParser { fun parse(rawText: String): CandidateProfile }
```

Use cases (each a class with `operator fun invoke`, injected with `@Inject constructor`):
`AnalyzeJobUseCase` (profile + raw JD -> `JobDescription` + `GapAnalysis`),
`TailorResumeUseCase` (tailor, then run the guard on every bullet; a bullet with violations falls back to the
original text with an empty `editTypes`), `CreateApplicationUseCase`.

The gateways and the use cases of the account, payment, sample data, and onboarding flows are in
`docs/MOCK_BACKEND.md`.

Offline implementations use a keyword dictionary (skills, tools, degrees, soft skills) plus alias
normalisation (`"js" -> "javascript"`, `"k8s" -> "kubernetes"`). Hilt `@Binds` modules bind interfaces to
the offline implementations.

## 6. Navigation

Copy NiA Navigation 3: `NavKey` objects/data classes in each feature `api` module; `EntryProvider`
extension in each `impl` module; `core:navigation` `Navigator`/`NavigationState`.

### Shell

`AppViewModel` maps the start destination to `AppRootState`. `TmrApp` shows one of two roots and switches when the
"onboarding complete" flag or the account of `SessionRepository` changes. Both roots render the same shell
(`TmrShell`) over one back stack in `NavigationState`; the root only picks the first key and owns a `ViewModelStore`
(`RootViewModelStores`). The app clears the store of a root when it leaves the root.

| Root | First screen |
|---|---|
| First run (`TmrFirstRunRoot`) | Sign in. The setup flow continues from Review profile to Add job inside the same stack |
| Main (`TmrMainRoot`) | Applications |

To open the main root with screens on top of Applications, call `PendingNavigation.set(...)` just before
`SessionRepository.markOnboardingComplete()`. `TmrMainRoot` consumes the keys when it starts.
`docs/REDESIGN.md` section 6 has the earlier navigation contract.

Rules, all in `core:navigation` `Navigator` and the app `ScreenChrome` table:

- **Back.** The top entry pops. With an empty stack, back goes to Applications if the user has an application or a
  reviewed profile, else to Upload. Sign in and the target itself have no fallback, so back leaves the app.
  The top bar Back and system back use the same `goBack()`.
- **Root.** `root(key)` clears the stack to one key. A tab tap, Go to Applications, Close on Exported, the
  Applications avatar and sign-out or delete-account completion (`navigateToSignIn()`) all use it. Back from the
  rooted key does not return to the previous flow.
- **Close.** Close on Exported is root Applications. Close on Paywall is back.
- **Tab bar.** `TmrTabBar` (Applications, +, Profile) shows only on the Applications and Profile screens. The centre +
  pushes Add job (`Navigator.newApp()`). Settings opens from the Profile gear and is not a tab.
- **Chrome.** `chromeFor(key, entriesBelow)` gives each screen its step bar, leading control, title and right action.
  Upload and Add job show Back only when entries lie below them. An entry may register the right action with
  `RegisterChromeAction`; otherwise Save and Done go back and show the toast "Changes saved".
- **Hosts.** `TmrShell` holds one toast host and one bottom-sheet host above the screens. An entry shows a toast or
  a sheet through `LocalTmrToast` and `LocalTmrSheetHost`; a new one replaces the current one.

### NavKeys

Each key is registered with one `entry<...>` in the `impl` module of its feature. The 27 keys below are one per
prototype screen. W4 replaces each placeholder entry with the real screen.

| Key | Module | Screen |
|---|---|---|
| `SignInNavKey` | `feature/onboarding/api` | Sign in |
| `UploadNavKey` | `feature/onboarding/api` | Upload resume |
| `UploadErrorNavKey` | `feature/onboarding/api` | Unreadable file |
| `PasteResumeNavKey` | `feature/onboarding/api` | Paste resume |
| `ManualProfileNavKey` | `feature/onboarding/api` | Fill in myself |
| `ReadingNavKey` | `feature/onboarding/api` | Reading |
| `ReviewProfileNavKey` | `feature/onboarding/api` | Review profile |
| `JobNavKey` | `feature/analysis/api` | Add job |
| `JobLinkNavKey` | `feature/analysis/api` | Import link |
| `JobResultNavKey(applicationId)` | `feature/analysis/api` | Job analyzed |
| `QuickQuestionNavKey(applicationId)` | `feature/analysis/api` | Quick question |
| `TailoringNavKey(applicationId)` | `feature/tailor/api` | Tailoring |
| `TailorFailedNavKey(applicationId)` | `feature/tailor/api` | Tailoring failed |
| `TailoredNavKey(applicationId)` | `feature/tailor/api` | Tailored resume |
| `EditResumeNavKey(applicationId)` | `feature/tailor/api` | Edit resume |
| `ExportedNavKey(applicationId)` | `feature/tailor/api` | Exported |
| `ApplicationsNavKey` | `feature/applications/api` | Applications (tab) |
| `ApplicationDetailNavKey(applicationId)` | `feature/applications/api` | Application |
| `ProfileNavKey` | `feature/profile/api` | Profile (tab) |
| `ExperienceNavKey` | `feature/profile/api` | Experience |
| `EditRoleNavKey(entryId?)` | `feature/profile/api` | Edit role; null adds a role |
| `EditContactNavKey` | `feature/profile/api` | Contact |
| `SkillsNavKey` | `feature/profile/api` | Skills |
| `ListEditNavKey(section)` | `feature/profile/api` | Summary, Education or Achievements (`ProfileListSection`) |
| `SettingsNavKey` | `feature/settings/api` | Settings |
| `CreditsNavKey` | `feature/settings/api` | Credits |
| `PaywallNavKey(returnToApplicationId?)` | `feature/settings/api` | Paywall; after a purchase it replaces itself with Quick question when the id is set, else goes back |

Every key carries an optional `DebugScenario` from `core:model`, defaulting to `DebugScenario.DEFAULT`. That is the
only mechanism for forcing a screen into a loading, empty, offline, error or partial state. A screenshot test can
navigate straight to a forced state without touching feature business logic.

### Debug developer menu

The `debug` source set of `:app` holds the developer menu. Release builds cannot reach it.
It adds a second launcher entry, "TailorMyResume dev" (`DebugScenarioActivity`). The menu can:

- load the sample data and reset the app data (`SampleDataController`);
- switch the mock connectivity between online and offline;
- open any screen of the list above with a chosen `DebugScenario`.

### Mock backend state

`MockStateStore` keeps the session, credits, purchases, and export history in a DataStore
Preferences file. The state survives a process restart. Room keeps the profile and the applications.

### Repositories in features

A ViewModel injects the interface of a repository or a gateway. It never builds display data inline. The
list below shows what each feature injects today.

| Feature | Injects |
|---|---|
| `onboarding` | `SessionRepository`, `ProfileRepository`, `SignInGateway`, `UsageAllowance`, `ConnectivityMonitor`, `NextOnboardingStepUseCase`, `ProposeJobLabelUseCase`, `DiscardJobDraftsUseCase` |
| `analysis` | `SessionRepository`, `ProfileRepository`, `PrepPlanRepository`, `ContentReportRepository`, `UsageAllowance`, `PaymentGateway`, `AnalyzeJobUseCase`, `AddUserStatedFactUseCase`, `CreateApplicationUseCase` |
| `tailor` | `ApplicationRepository`, `ProfileRepository`, `TailoringReviewStateRepository`, `ContentReportRepository`, `ExportHistoryRepository`, `PaymentGateway`, `ConnectivityMonitor` |
| `profile` | `ProfileRepository`, `AddUserStatedFactsUseCase`, `FactIdAllocator`, `ConnectivityMonitor` |
| `applications` | `ApplicationRepository`, `ProfileRepository`, `ExportHistoryRepository`, `PrepPlanRepository`, `ContentReportRepository` |
| `settings` | `SignInGateway`, `SessionRepository`, `ApplicationRepository`, `ExportHistoryRepository`, `DeleteAccountUseCase`, `ExportAccountDataUseCase` |

Gap analysis consumes the daily analysis, and Paste JD only checks it. `docs/MOCK_BACKEND.md` gives the rule.
Sign out keeps the credits. Account deletion closes the credit account, so the next sign-in starts with one
free credit.

### Fonts

The UI uses bundled Manrope (variable) for text and fact IDs, and Archivo Black for uppercase
headlines (`TmrHeadline`). `TmrFontFamilies` in `core:designsystem` defines both families. Font licenses are in `core/designsystem/fonts-licenses/`.

## 7. Build and host rules

- Gradle wrapper version: `gradle-9.8.0`. NiA at commit `a49ed25` uses 9.7.1, so the
  wrapper is no longer identical to the reference. Put `org.gradle.workers.max=3` in
  `gradle.properties`. Other agents build at the same time on this shared CI host.
- Run `./gradlew --stop` when you finish.
- Add NiA production tooling only in the order that the adoption ledger in `docs/CONSTITUTION.md` gives.
  Spotless with ktlint has no license header step.
- Verify before you push: `tools/ci/verify-local.sh` must pass.
