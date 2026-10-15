# Mock backend

The mock backend is a set of interfaces with on-device implementations. The app binds remote
implementations through one Hilt binding each, and the on-device ones stay as fakes in `core.domain.offline`
for screenshot tests and the Dev console
(`app/src/main/kotlin/.../di`, Constitution I.5 as amended, `docs/BACKEND_CONTRACT.md`). No UI code changes.

Remote in the app: `SignInGateway`, `ResumeTextParser`, `AnalyzeJobUseCase`, `ResumeTailor`,
`PrepQuestionSource`, `CoverLetterSource`, `PaymentGateway`, `UsageAllowance`, `ContentReportRepository`,
`AccountDataExporter`, and account deletion. `RemoteContentReportRepository` saves the report locally, then
posts it; a post that fails with a retryable error stays in `PendingReportQueue` and is sent on the next
app start. `RemoteAccountDataExporter` adds `server.json`, or a placeholder when the fetch fails. Everything
else, including the sample data controller, stays on-device.

Read this file before you use a gateway, a repository, or a use case from the core modules.

## Changes for feature adoption

A feature must change these things. Each item names the API.

1. Sign in and Consent. Call `navigator.replace(NextKey)` (module `core:navigation`) instead of
   `navigate`, so these screens leave the back stack. On a root key `replace` acts like `navigate`.
2. Fact ids. Show `FactDisplayIds.forBulletId(bulletId, profile.entries)` for an evidence bullet and
   `FactDisplayIds.of(entry, profile.entries)` for an entry. Never show a raw bullet id or a raw entry id.
   Pass the title when you allocate an id: `factIdAllocator.nextId(category, existing, title)`.
   `AddUserStatedFactUseCase` ("I have this" on Gap analysis) allocates a `U-` id with
   `factIdAllocator.nextUncategorisedId(existing)`. An old entry with the id `user-stated` also shows as `U-01`.
3. User-stated facts. Use `AddUserStatedFactsUseCase`. It creates the profile when none exists and saves
   the facts as confirmed. Delete `UserFactWriter`'s bypass for the add path (the profile feature still
   owns contact, skills, and replace).
4. Daily allowance. Inject `UsageAllowance`. Show `observeAnalysesLeft()` and
   `observeFreeTailoringsLeft()`. Paste JD only checks that `observeAnalysesLeft()` is above zero. It never
   consumes. Gap analysis calls `consumeAnalysis()` once, when an analysis succeeds for a newly kept job
   description. It does not call it again when the analysis runs again for the same kept text, and it does not
   call it when the analysis fails. Call `consumeFreeTailoring()` when the person tailors with no credit.
   A `false` result means no allowance is left.
5. Prep plan. Inject `PrepPlanRepository`. Stop writing "Prep:" lines to the application notes.
   Gap analysis keeps its prep items and reports under the draft key `KeptJobDescription.draftKey`
   (`analysis-draft-<hash of the text>`) until the application exists. The item id is the requirement id.
   When the application is created, the items and reports move to the application id, and the draft is cleared.
   When Paste JD keeps a different job description, it calls `DiscardJobDraftsUseCase(previousKeptJob)`.
6. Report inaccurate content. Inject `ContentReportRepository`. Call `report(...)`. Use one thank-you
   copy in every feature. Hide the report action for an id in `observeReportedIds(...)`.
   Use the kind that names the item: `REQUIREMENT` (the id is the requirement id), `RESUME_BULLET`,
   `SECTION` (the id is the section key of the tailored resume), `COVER_LETTER`, `PREP_QUESTION`.
7. Tailoring review state. Inject `TailoringReviewStateRepository`. Delete `TailorSessionStore`.
   The keys in `MockStateStore` are the same, so no data is lost.
8. Export detail. Fill `ExportRecord.pageCount` and `templateName` when you record an export. Show
   "1 page, PDF, Plain" from them.
9. Create application. Pass the kept job: `createApplication(profile, analysis, kept)`. Kept company and
   role override the analyzer when they are not blank. Paste JD prefills its two fields from
   `ProposeJobLabelUseCase(rawText)`.
10. Deleting an application. `ApplicationRepository.deleteApplication` now also clears the prep plan, the
    reports, and the review state. Keep `ExportHistoryRepository.clearFor`.
11. Sign out and delete account. Sign out keeps the credits. `PaymentGateway.clearCredits()` closes the credit
    account. After an account deletion the next sign-in starts with the free credit. A screen must not
    cache the old credit count.
12. Share the last export. The workspace calls `navigator.navigateToShareLastExport(applicationId)` (module
    `feature:tailor:api`). The route reads the newest `ExportRecord` and opens the share sheet for its file. If the
    file is gone, it shows a message and goes back.

## Rules

1. Inject the interface. Never name a class that starts with `Offline` or `Stored` outside a Hilt module.
2. Every stateful mock is a singleton. All screens see the same state.
3. State lives in `MockStateStore`. It survives a process restart. Room holds the profile and the applications.
4. Core modules return typed data. The UI turns it into text from its own resources.
5. The mock waits for a short time on slow operations, so loading states show. Tests use `NoMockLatency`.

## Where state lives

| State | Store | Key |
|---|---|---|
| Signed-in account | `MockStateStore` | `session.account` |
| Uid of the last signed-in account (kept on sign-out; a different uid at sign-in wipes local data) | `MockStateStore` | `session.lastAccountId` |
| Consent record | `MockStateStore` | `session.consent` |
| Onboarding complete flag | `MockStateStore` | `session.onboardingComplete` |
| Kept job description | `MockStateStore` | `session.keptJobDescription` |
| Credits, pending packs, purchase history | `MockStateStore` | `payment.state` |
| Export history | `MockStateStore` | `exports.history` |
| Daily allowance | `MockStateStore` | `usage.allowance` |
| Prep plan of an application | `MockStateStore` | `prep.plan.<applicationId>` |
| Content reports of an application | `MockStateStore` | `reports.content.<applicationId>` |
| Regenerations used per section | `MockStateStore` | `tailor.regenerations.<applicationId>` |
| Hand-edited bullet ids | `MockStateStore` | `tailor.edited.<applicationId>` |
| Written cover letter of an application | `MockStateStore` | `coverletter.<applicationId>` |
| Profile and applications | Room | `hh-database` |

`MockStateStore` is a DataStore Preferences file named `mock_backend`. The Room schema did not change.

## Hilt modules

| Layer | Module | File |
|---|---|---|
| Data | `DataModule` | `core/data/src/main/kotlin/com/tailormyresume/core/data/di/DataModule.kt` |
| Domain | `DomainModule` | `core/domain/src/main/kotlin/com/tailormyresume/core/domain/di/DomainModule.kt` |

## Interfaces in `core:data`

### `MockStateStore`

Package `com.tailormyresume.core.data.mock`.

```kotlin
interface MockStateStore {
    fun observe(key: String): Flow<String?>
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
}
```

Helpers: `readValue`, `writeValue` and `observeValue` encode a `@Serializable` type as JSON. A value that does not decode reads as `null`.

- Mock: `DataStoreMockStateStore` (internal, `@Singleton`).
- Binding: `DataModule.bindsMockStateStore`. The `DataStore<Preferences>` comes from `DataModule.providesMockStateDataStore`.
- Test fake: `TestMockStateStore`. Contract: `MockStateStoreContractTest`.
- Real implementation: a feature module does not need this. Delete the mock when no state is stored on the device.

### `MockLatency`

```kotlin
enum class MockOperation { SIGN_IN, LOAD_PACKS, PURCHASE, RESTORE, EXPORT_DATA, DELETE_ACCOUNT_STEP }

interface MockLatency {
    suspend fun await(operation: MockOperation)
}
```

- Mock: `DelayMockLatency` (internal). It uses `delay`: 900 ms sign in, 350 ms packs, 1200 ms purchase, 800 ms restore, 700 ms data export, 900 ms for each account deletion step.
- Binding: `DataModule.bindsMockLatency`.
- Test: `NoMockLatency` (an `object`, waits for nothing).
- Real implementation: not needed. A real backend has its own delay.

### `SessionRepository`

Package `com.tailormyresume.core.data.repository`.

```kotlin
interface SessionRepository {
    fun observeAccount(): Flow<SignInAccount?>
    fun observeConsent(): Flow<ConsentRecord?>
    fun observeOnboardingComplete(): Flow<Boolean>
    fun observeKeptJobDescription(): Flow<KeptJobDescription?>
    suspend fun saveAccount(account: SignInAccount)
    suspend fun recordConsent(record: ConsentRecord)
    suspend fun markOnboardingComplete()
    suspend fun keepJobDescription(job: KeptJobDescription)
    suspend fun clearKeptJobDescription()
    suspend fun signOut()
    suspend fun clear()
}
```

Models are in `com.tailormyresume.core.model`:

```kotlin
data class SignInAccount(val id: String, val displayName: String, val email: String)
enum class ConsentPurpose { READ_AND_BUILD, KEEP_CONFIRMED_FACTS, AI_PROCESSING, AGE_18_PLUS }
data class ConsentRecord(val purposes: Set<ConsentPurpose>, val acceptedAt: Instant, val noticeVersion: String)
data class KeptJobDescription(val text: String, val company: String, val role: String) {
    val draftKey: String
}
```

`ConsentRecord.CURRENT_NOTICE_VERSION` is the version string to store. `SignInAccount` also has a typealias in `com.tailormyresume.core.domain`, so the old import still works.

`signOut()` removes the account and the kept job description. Every build type also runs `SignOutCleaner` on sign-out: it clears the consent and keeps the onboarding flag, so the next sign-in shows S4 before Applications. `clear()` removes the account, the consent, the onboarding flag, and the kept job description. Delete account and "Reset app data" use `clear()`. Neither method touches the profile, the applications, the credits, or the export history.

- Mock: `StoredSessionRepository` (internal, `@Singleton`).
- Binding: `DataModule.bindsSessionRepository`.
- Test fake: `TestSessionRepository` (extra setters: `sendAccount`, `sendConsent`, `sendOnboardingComplete`). Contract: `SessionRepositoryContractTest`.
- Real implementation: keep the account from the sign-in provider. Send the consent record to the server. Keep the flag on the device or on the server.

### `ExportHistoryRepository`

```kotlin
interface ExportHistoryRepository {
    fun observeExports(): Flow<List<ExportRecord>>
    fun observeExports(applicationId: String): Flow<List<ExportRecord>>
    suspend fun record(export: ExportRecord)
    suspend fun clearFor(applicationId: String)
    suspend fun clear()
}
```

```kotlin
enum class ExportFormat { PDF, DOCX }
enum class CreditKind { FREE, PURCHASED }
data class ExportRecord(
    val applicationId: String,
    val format: ExportFormat,
    val fileName: String,
    val exportedAt: Instant,
    val creditKind: CreditKind?,
    val pageCount: Int? = null,
    val templateName: String? = null,
)
```

`pageCount` and `templateName` are `null` when unknown. Records keep the order in which they were recorded. `creditKind` is `null` when no credit paid.
If a screen deletes an application, it must call `clearFor(applicationId)`. The workspace shows the "exported" state when `observeExports(applicationId)` is not empty.

- Mock: `StoredExportHistoryRepository` (internal, `@Singleton`).
- Binding: `DataModule.bindsExportHistoryRepository`.
- Test fake: `TestExportHistoryRepository` (extra setter: `sendExports`). Contract: `ExportHistoryRepositoryContractTest`.
- Real implementation: store the records on the server and on the device.

### `UsageAllowance`

```kotlin
interface UsageAllowance {
    fun observeAnalysesLeft(): Flow<Int>
    fun observeFreeTailoringsLeft(): Flow<Int>
    suspend fun consumeAnalysis(): Boolean
    suspend fun consumeFreeTailoring(): Boolean
    suspend fun clear()
    companion object { const val DAILY_ANALYSES = 3; const val DAILY_FREE_TAILORINGS = 1 }
}
object UsageDay { fun of(instant: Instant, zone: TimeZone = TimeZone.getDefault()): Long }
```

The free allowance of a local day: 3 job analyses and 1 free tailoring. The allowance resets on a new local day, judged with the injected `Clock` and the device time zone. `consume...` returns `false` and changes nothing when none is left. The interface does not know about credits. The caller calls `consumeFreeTailoring()` only when the person has no credit.

Who consumes. Paste JD does not consume. It reads `observeAnalysesLeft()` and refuses to continue at zero. Gap analysis consumes one analysis after the analysis succeeds for a job description that it has not counted yet. A failed analysis does not count. Running the analysis again for the same kept text does not count.

- Mock: `StoredUsageAllowance` (internal, `@Singleton`, key `usage.allowance`). Binding: `DataModule.bindsUsageAllowance`.
- Test fake: `TestUsageAllowance(clock)`. Contract: `UsageAllowanceContractTest` (`createUsageAllowance(clock: TestClock)`).
- Real implementation: count on the server.

### `PrepPlanRepository`

```kotlin
interface PrepPlanRepository {
    fun observeItems(applicationId: String): Flow<List<PrepPlanItem>>
    suspend fun add(applicationId: String, item: PrepPlanItem)
    suspend fun remove(applicationId: String, itemId: String)
    suspend fun setDone(applicationId: String, itemId: String, done: Boolean)
    suspend fun clearFor(applicationId: String)
}
data class PrepPlanItem(val id: String, val text: String, val done: Boolean = false)
```

Items keep the order in which they were added. `add` ignores an item whose id is already in the plan. `setDone` and `remove` ignore an unknown id.

- Mock: `StoredPrepPlanRepository`. Binding: `DataModule.bindsPrepPlanRepository`.
- Test fake: `TestPrepPlanRepository`. Contract: `PrepPlanRepositoryContractTest`.

### `ContentReportRepository`

```kotlin
interface ContentReportRepository {
    suspend fun report(report: ContentReport)
    fun observeReports(applicationId: String): Flow<List<ContentReport>>
    fun observeReportedIds(applicationId: String, kind: ReportedItemKind): Flow<Set<String>>
    suspend fun clearFor(applicationId: String)
}
enum class ReportedItemKind { REQUIREMENT, RESUME_BULLET, SECTION, COVER_LETTER, PREP_QUESTION }
data class ContentReport(applicationId: String, itemKind: ReportedItemKind, itemId: String, reportedAt: Instant)
```

The same item (kind and id) is stored once. The mock keeps the reports on the device only.

- Mock: `StoredContentReportRepository`. Binding: `DataModule.bindsContentReportRepository`.
- Test fake: `TestContentReportRepository`. Contract: `ContentReportRepositoryContractTest`.

### `DiscardJobDraftsUseCase`

Package `com.tailormyresume.core.domain`. `suspend operator fun invoke(job: KeptJobDescription)` clears the prep plan and
the reports that Gap analysis stored under `job.draftKey`. Paste JD calls it for the old kept job description when it
keeps a job description with a different text.

### `TailoringReviewStateRepository`

```kotlin
interface TailoringReviewStateRepository {
    fun observe(applicationId: String): Flow<TailoringReviewState>
    suspend fun recordRegeneration(applicationId: String, section: String)
    suspend fun markEdited(applicationId: String, bulletId: String)
    suspend fun clearEdited(applicationId: String, bulletIds: Collection<String>)
    suspend fun clearFor(applicationId: String)
}
data class TailoringReviewState(regenerationsBySection: Map<String, Int>, editedBulletIds: Set<String>) {
    val regenerationsUsed: Int
    fun regenerationsUsedIn(section: String): Int
}
```

Use `EntryCategory.name` as the `section`. The keys `tailor.regenerations.<id>` and `tailor.edited.<id>` are the keys that the tailor feature wrote before. A plain number in `tailor.regenerations.<id>` (the old format) reads as a total with an empty section name, so no data is lost.

- Mock: `StoredTailoringReviewStateRepository`. Binding: `DataModule.bindsTailoringReviewStateRepository`.
- Test fake: `TestTailoringReviewStateRepository`. Contract: `TailoringReviewStateRepositoryContractTest`.

`ApplicationRepository.deleteApplication(id)` (mock `OfflineFirstApplicationRepository`) also calls `clearFor(id)` on these three repositories. `DeleteAccountUseCase` deletes through it, so account deletion clears them too.

### `CoverLetterRepository`

```kotlin
interface CoverLetterRepository {
    fun observeLetter(applicationId: String): Flow<WrittenCoverLetter?>
    suspend fun save(applicationId: String, letter: WrittenCoverLetter)
    suspend fun clearFor(applicationId: String)
}
data class WrittenCoverLetter(paragraphs: List<WrittenParagraph>, writtenAt: Instant) { val wordCount: Int }
data class WrittenParagraph(text: String, isGreeting: Boolean = false, isUserEdited: Boolean = false)
```

The Cover letter screen saves the letter when it writes one and when the person saves an edit. When the screen opens
and a letter is stored, it shows that letter and not the offer. The workspace shows "<N> words · written <date>" and
the action "Open" while a letter is stored. `observeLetter` emits `null` when no letter is stored.
`ApplicationRepository.deleteApplication(id)` also calls `clearFor(id)` (through `StoredApplicationCleanup`), so account
deletion clears the letters. "Reset app data" clears the whole `MockStateStore`.

- Mock: `StoredCoverLetterRepository`. Binding: `DataModule.bindsCoverLetterRepository`.
- Test fake: `TestCoverLetterRepository`. Contract: `CoverLetterRepositoryContractTest`.

### `RequirementPhrase`

Package `com.tailormyresume.core.domain.prep`. `RequirementPhrase.of(text)` turns a requirement text into a phrase for use
inside a sentence. It trims the text, removes closing sentence punctuation, and removes a leading "Must have",
"Nice to have" or "Good to have" marker. Use it in every template that puts a requirement in a sentence. Text that quotes
the JD ("the JD asks for “...”") stays verbatim.

### `ConnectivityMonitor` and `MockConnectivityControl`

Package `com.tailormyresume.core.data.connectivity`.

```kotlin
interface ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}

interface MockConnectivityControl {
    fun setOnline(online: Boolean)
}
```

Only the debug developer menu may inject `MockConnectivityControl`.

- Mock: `OfflineConnectivityMonitor` (internal, `@Singleton`). It starts online.
- Binding: `DataModule.bindsConnectivityMonitor` and `DataModule.bindsMockConnectivityControl`. Both give the same instance.
- Test fake: `TestConnectivityMonitor(initiallyOnline = true)`. Contract: `ConnectivityMonitorContractTest`.
- Real implementation: wrap `ConnectivityManager` network callbacks. Do not bind `MockConnectivityControl`. Delete the developer-menu toggle.

## Interfaces in `core:domain`

### `SignInGateway`

Package `com.tailormyresume.core.domain`.

```kotlin
interface SignInGateway {
    suspend fun currentAccount(): SignInAccount?
    suspend fun signIn(): SignInResult
    suspend fun signOut()
}

sealed interface SignInResult {
    data class SignedIn(val account: SignInAccount) : SignInResult
    data object Cancelled : SignInResult
    data class Failed(val reason: SignInFailureReason) : SignInResult
}
enum class SignInFailureReason { NetworkUnavailable, ProviderUnavailable }
```

- Mock: `OfflineSignInGateway` (`@Singleton`, needs `MockStateStore`). A sign-in succeeds with `SignInAccount.localAccount` (Priya Deshmukh) and writes it to `SessionRepository`. `signOut()` calls `SessionRepository.signOut()` and keeps the credit state (`payment.state`). A signed-out person who signs in again keeps the credits. `signIn()` removes the credit state only when `clearCredits()` closed the account, so the next sign-in after an account deletion starts with the free credit.
- Test hooks: `withOutcome(SignInOutcome.Cancelled | Failed | SignedIn)` and `withAccount(account)`.
- Binding: `DomainModule.bindSignInGateway`.
- Test fake: `TestSignInGateway(sessionRepository = TestSessionRepository(), store = TestMockStateStore())` with the same hooks. Contract: `SignInGatewayContractTest`.
- Real implementation: run the Google sign-in flow. Save the account with `SessionRepository.saveAccount`. In `signOut()`, end the provider session and call `SessionRepository.signOut()`.

### `PaymentGateway`

```kotlin
interface PaymentGateway {
    suspend fun packs(): List<ApplicationPack>
    suspend fun purchase(packId: String): PurchaseResult
    suspend fun entitlement(): PurchaseEntitlement
    fun observeEntitlement(): Flow<PurchaseEntitlement>
    suspend fun purchaseHistory(): List<PurchaseRecord>
    fun observePurchaseHistory(): Flow<List<PurchaseRecord>>
    suspend fun restorePurchases(): PurchaseEntitlement
    suspend fun unlock(applicationId: String): CreditSpend
    suspend fun clearCredits(): PurchaseEntitlement
}
```

```kotlin
data class ApplicationPack(id, name, credits, priceInPaise, currencyCode, creditsExpire)
data class PurchaseEntitlement(freeCredits: Int, purchasedCredits: Int, pendingPackIds: List<String>)
enum class PurchaseState { PENDING, COMPLETED }
data class PurchaseRecord(packId: String, orderId: String, purchasedAt: Instant, state: PurchaseState)
sealed interface CreditSpend {
    data class Spent(val entitlement: PurchaseEntitlement, val kind: CreditKind = CreditKind.FREE) : CreditSpend
    data object NoCreditLeft : CreditSpend
}
```

Rules:

- `observeEntitlement()` is the one source for the credits pill. Use it on every screen that shows credits.
- `purchaseHistory()` lists the newest purchase first. Only completed and pending purchases are listed.
- `unlock(applicationId)` spends a free credit first, once per application. `Spent.kind` tells which kind paid, and is `null` for a repeat. Pass it to `ExportRecord.creditKind`.
- `clearCredits()` closes the credit account: zero credits, no pending pack, no history. A fresh install has one free credit. `clearCredits()` marks the account as closed. The next `SignInGateway.signIn()` after a closed account starts again with the free credit. `signOut()` alone keeps the credits.
- The pack ids are `ApplicationPack.APPLICATION_PACK_FIVE`. Never read prices or names from constants. Call `packs()`.
- The new members have default bodies so old test doubles still compile. A real implementation must override all of them.

Mock details:

- Class `OfflinePaymentGateway` (`@Singleton`). The catalogue is in `MockPackCatalogue` (same package `com.tailormyresume.core.domain.offline`). Only tests and the mock read `MockPackCatalogue`.
- Every purchase succeeds unless a hook says otherwise. Hooks: `withOutcome(packId, PurchaseOutcome)`, `withFailureReason(reason)`, `withFreeCredits(n)`. `withFreeCredits` only applies before the first saved state.
- A pending purchase gets one `PENDING` record. A later completed purchase of the same pack turns that record into `COMPLETED`.
- Order ids look like `mock-order-<id>`.
- Binding: `DomainModule.bindPaymentGateway`.
- Test fake: `TestPaymentGateway(store = TestMockStateStore(), clock = TestClock())` with the same hooks. Contract: `PaymentGatewayContractTest`.
- Real implementation: wrap Play Billing. Acknowledge every purchase. Return the order id from the purchase token. Put a server check behind `entitlement()`.

### `NextOnboardingStepUseCase` and `OnboardingStep`

Package `com.tailormyresume.core.domain.onboarding`.

```kotlin
class NextOnboardingStepUseCase {
    suspend operator fun invoke(): OnboardingStep
    fun observe(): Flow<OnboardingStep>
}

sealed interface OnboardingStep {
    data object SignIn : OnboardingStep
    data object Consent : OnboardingStep
    data object ImportResume : OnboardingStep
    data object ConfirmFacts : OnboardingStep
    data class GapAnalysis(val job: KeptJobDescription) : OnboardingStep
    data object PasteJobDescription : OnboardingStep
    data object Applications : OnboardingStep
}
```

The first rule that matches wins:

| # | Condition | Result |
|---|---|---|
| 1 | No signed-in account | `SignIn` |
| 2 | No consent record | `Consent` |
| 3 | The profile is missing or has no entry | `ImportResume` |
| 4 | The profile has no confirmed entry | `ConfirmFacts` |
| 5 | A job description is kept | `GapAnalysis(job)` |
| 6 | Onboarding is complete | `Applications` |
| 7 | Otherwise | `PasteJobDescription` |

The Confirm facts screen enables "Continue to my analysis" only when at least one entry is confirmed. This matches rule 4. The Confirm facts screen counts only the facts it lists. Skills are excluded because the screen cannot confirm them. Paste JD treats a text of fewer than 20 words as too short (`PASTE_JD_MIN_WORDS`). Design frame S2-05 shows that 18 words is too short.

### Fact counts

`CandidateProfile.factCounts()` (in `core:model`) gives `ProfileFactCounts(total, confirmed, userStated)`. `total` is skills plus entries. `confirmed` is skills plus confirmed entries that are not user-stated. `userStated` is confirmed user-stated entries. Profile, Your data, and Delete account use it. Confirm facts is the exception: it counts its own listed facts in `ConfirmFactsUiState`.

### `ObserveStartDestinationUseCase`

```kotlin
enum class StartDestination { Welcome, Applications }

class ObserveStartDestinationUseCase {
    operator fun invoke(): Flow<StartDestination>
}
```

`Applications` when an account is signed in, onboarding is complete, and a consent record exists. `Welcome` otherwise. `AppViewModel` maps the flow to the app root, so sign out switches to the first-run root. When the consent record is gone (after sign-out, or `ConsentRevoker` after `CONSENT_REQUIRED`), the root stays on the first-run flow, `NextOnboardingStepUseCase` returns `Consent`, and the main root returns after S4 records the consent.

### `DeleteAccountUseCase`

```kotlin
class DeleteAccountUseCase {
    suspend fun preview(): AccountDeletionCounts
    suspend operator fun invoke(onStep: suspend (AccountDeletionStep) -> Unit = {}): AccountDeletionResult
}
```

It deletes the profile and the export history, then the applications (and their prep plan, reports, and review state), then the credits. The screen opens a confirm dialog first; only Confirm calls the use case. Each of the three `AccountDeletionStep` values waits for `MockOperation.DELETE_ACCOUNT_STEP` (the constructor takes `MockLatency`), so the steps show on the screen. The last step signs out and clears the session; this part cannot be cancelled. `AccountDeletionCounts.profileFacts` is `CandidateProfile.factCounts().total`. If a step fails before the credits are cleared, it restores the applications, the profile, and the export history. The constructor gained `ExportHistoryRepository`, `SessionRepository`, and `SignInGateway`. Hilt supplies them.

### `AccountDataExporter` (export my data)

Package `com.tailormyresume.core.domain.account`.

```kotlin
interface AccountDataExporter {
    suspend fun export(data: AccountData): AccountDataArchive
}

data class AccountData(
    val generatedAt: Instant,
    val account: SignInAccount?,
    val consent: ConsentRecord?,
    val profile: CandidateProfile?,
    val applications: List<JobApplication>,
    val entitlement: PurchaseEntitlement,
    val purchases: List<PurchaseRecord>,
    val exports: List<ExportRecord>,
)

data class AccountDataArchive(val fileName: String, val file: java.io.File)
```

Use cases: `CollectAccountDataUseCase()` returns the `AccountData`. `ExportAccountDataUseCase()` collects the data and calls the exporter. It returns the `AccountDataArchive`.

- Mock: `OfflineAccountDataExporter`. It writes `tailormyresume-my-data.zip` to `cacheDir/data-exports` with five entries: `account.txt`, `profile.txt`, `applications.txt`, `purchases.txt`, and `my-data.json`. `my-data.json` holds a faithful copy of the account email and name (never the account id or any token), consent, the profile, the credit counts with `unlockedApplicationIds` and `pendingPackIds`, the purchases, and every application with its job description text, requirements, gap analysis, tailored resume, cover letter, prep plan and exports (`creditKind`, `pageCount`, `templateName`). `applications.txt` indents every line of a multi-line value.
- Binding: `DomainModule.bindAccountDataExporter`.
- Test fake: `TestAccountDataExporter` (records each `AccountData` in `exported`). Contract: `AccountDataExporterContractTest`.
- Real implementation: ask the server for the archive and save it to a file.

How the settings feature must adopt it:

1. Delete `SettingsDataExportWriter`. Keep `SettingsExportFileStore`, `SettingsFileProvider`, and `createSettingsShareIntent`.
2. Inject `ExportAccountDataUseCase` in `YourDataViewModel`. Call it when the data is ready to share.
3. Share `AccountDataArchive.file` with the FileProvider. The file sits in `cacheDir/data-exports`, so the existing `file_paths` entry may need that folder name. Check the provider paths.
4. Show the purchase list from `PaymentGateway.observePurchaseHistory()`. Remove the text "This build of TailorMyResume takes no payment".
5. Remove the fake step delays in `YourDataViewModel`. Show the real wait. The mock already waits 700 ms.

### `SampleDataController`

Package `com.tailormyresume.core.domain.sample`.

```kotlin
interface SampleDataController {
    suspend fun load()
    suspend fun reset()
    suspend fun keepSampleJobDescription()
    suspend fun clearSampleJobDescription()
}
```

`keepSampleJobDescription()` keeps the Northwind sample job description in the session. The developer menu uses it so that Gap analysis opens with a job. `clearSampleJobDescription()` removes it.

`load()` calls `reset()` first, then fills the app through the normal repositories and gateways:

- Account Priya Deshmukh, signed in. Consent recorded for all purposes. Onboarding complete.
- Profile: B.Tech Computer Science 2024, Data Operations Associate at Saffron Retail (Pune), Data Intern at Kiran Agro Exports, project "Placement Stats Dashboard", a hackathon. Every fact is confirmed.
- One purchase of `application_pack_5`. Two exports of the Northwind application. They spend the free credit and one purchased credit. Four credits are left.
- Four applications. Each one comes from the real `AnalyzeJobUseCase` and `TailorResumeUseCase`.

| Id | Role | Company | Status | Review |
|---|---|---|---|---|
| `sample-northwind-associate-analyst` | Associate Analyst | Northwind GCC | Applied | All bullets decided, exported as PDF and DOCX |
| `sample-paisa-ledger-data-analyst-intern` | Data Analyst Intern | Paisa Ledger | Interview | Half of the bullets decided |
| `sample-sahyadri-motors-graduate-engineer-trainee` | Graduate Engineer Trainee | Sahyadri Motors | Saved | No bullet decided |
| `sample-meridian-business-analyst` | Business Analyst | Meridian GCC | No response | No bullet decided |

`reset()` returns the app to a fresh install: no session, no profile, no application, no export, one free credit.

The dataset is one file: `core/domain/src/main/kotlin/com/tailormyresume/core/domain/sample/SampleDataSet.kt`.
The controller is in `core:domain`, not `core:data`, because it uses `AnalyzeJobUseCase`, `TailorResumeUseCase`, and `PaymentGateway`. `core:data` cannot see them.

- Mock: `OfflineSampleDataController`. It always uses the offline payment gateway and the offline analysis and tailoring sources. In every build type it never opens a Play purchase and never calls the backend.
- Binding: `DomainModule.provideSampleDataController` (`@Singleton`).
- `keepSampleJobDescription()` also signs in the sample candidate when the app has no account, consent, or profile. This lets the Gap analysis preview open with the Error and Daily limit scenarios.
- Test fake: `TestSampleDataController` (counts calls). Contract: `SampleDataControllerContractTest`.
- Real implementation: none. Remove the binding and the developer-menu entry in a production flavor.

## How a feature uses this

Inject the type. Hilt provides it. All calls below are `suspend` unless noted.

| Need | Inject | Call |
|---|---|---|
| Start destination | `ObserveStartDestinationUseCase` | `observeStartDestination().first()` gives `StartDestination.Welcome` or `StartDestination.Applications` |
| Next onboarding step | `NextOnboardingStepUseCase` | `nextOnboardingStep()` gives an `OnboardingStep`. Use `nextOnboardingStep.observe()` for a `Flow` |
| Keep the pasted job description | `SessionRepository` | `sessionRepository.keepJobDescription(KeptJobDescription(text, company, role))` |
| Read the kept job description | `SessionRepository` | `sessionRepository.observeKeptJobDescription().first()` |
| Sign in | `SignInGateway` | `when (val result = signInGateway.signIn()) { is SignInResult.SignedIn -> nextOnboardingStep(); SignInResult.Cancelled, is SignInResult.Failed -> show the state }` |
| Sign out | `SignInGateway` | `signInGateway.signOut()`. Then open Welcome |
| Record consent | `SessionRepository`, `Clock` | `sessionRepository.recordConsent(ConsentRecord(purposes, clock.now(), ConsentRecord.CURRENT_NOTICE_VERSION))`. Then `nextOnboardingStep()` |
| Read consent | `SessionRepository` | `sessionRepository.observeConsent()` |
| Mark onboarding complete | `SessionRepository` | `sessionRepository.markOnboardingComplete()`. Call it after the first application is created |
| Read credits | `PaymentGateway` | `paymentGateway.observeEntitlement()` gives a `Flow<PurchaseEntitlement>`. Use `totalCredits` for the pill |
| Show packs | `PaymentGateway` | `paymentGateway.packs()` |
| Buy a pack | `PaymentGateway` | `paymentGateway.purchase(pack.id)` gives `PurchaseResult.Completed`, `Pending`, `Cancelled`, or `Failed` |
| Show purchases | `PaymentGateway` | `paymentGateway.observePurchaseHistory()` |
| Check or count a daily analysis or free tailoring | `UsageAllowance` | `usageAllowance.observeAnalysesLeft()` (Paste JD), `consumeAnalysis()` (Gap analysis, on success), `consumeFreeTailoring()` |
| Discard the drafts of an old job description | `DiscardJobDraftsUseCase` | `discardJobDrafts(previousKeptJob)` |
| Add or tick a prep item | `PrepPlanRepository` | `prepPlanRepository.add(applicationId, PrepPlanItem(id, text))`, `setDone(...)` |
| Report inaccurate content | `ContentReportRepository` | `contentReportRepository.report(ContentReport(applicationId, kind, itemId, clock.now()))` |
| Add facts the person typed | `AddUserStatedFactsUseCase` | `addUserStatedFacts(listOf(FactDraft(...)))` gives `AddFactsOutcome` |
| Add the words for one requirement ("I have this") | `AddUserStatedFactUseCase` | `addUserStatedFact(requirement, statement)`. The fact gets a `U-` id |
| Propose company and role from a pasted job description | `ProposeJobLabelUseCase` | `proposeJobLabel(rawText)` gives `JobLabelProposal(role, company)` |
| Show a fact id | `FactDisplayIds` (object) | `FactDisplayIds.forBulletId(bulletId, entries)`, `FactDisplayIds.of(entry, entries)` |
| Spend a credit | `PaymentGateway` | `paymentGateway.unlock(applicationId)` gives `CreditSpend.Spent(entitlement, kind)` or `CreditSpend.NoCreditLeft` |
| Record an export | `ExportHistoryRepository` | After a `Spent` result: `exportHistoryRepository.record(ExportRecord(applicationId, format, fileName, clock.now(), spend.kind))` |
| Show the exported state | `ExportHistoryRepository` | `exportHistoryRepository.observeExports(applicationId)`. The state is "exported" when the list is not empty |
| Know if the device is online | `ConnectivityMonitor` | `connectivityMonitor.isOnline` (a `Flow<Boolean>`) |
| Export my data | `ExportAccountDataUseCase` | `exportAccountData()` gives an `AccountDataArchive` |
| Delete the account | `DeleteAccountUseCase` | `deleteAccount(onStep)` gives `AccountDeletionResult`. Then open Welcome |
| Load sample data | `SampleDataController` | `sampleDataController.load()` |
| Reset to a fresh install | `SampleDataController` | `sampleDataController.reset()` |

Names in the "Call" column are the injected property names. The class names are in the "Inject" column.

If a purchase is `Pending`, do not spend a credit. Show the pending state. `observeEntitlement()` lists the pack in `pendingPackIds`.

## Tests

Use the fakes from `core:testing`. Do not use a mocking library.

| Interface | Fake | Contract test |
|---|---|---|
| `MockStateStore` | `TestMockStateStore` | `MockStateStoreContractTest` |
| `SessionRepository` | `TestSessionRepository` | `SessionRepositoryContractTest` |
| `ExportHistoryRepository` | `TestExportHistoryRepository` | `ExportHistoryRepositoryContractTest` |
| `ConnectivityMonitor` | `TestConnectivityMonitor` | `ConnectivityMonitorContractTest` |
| `SignInGateway` | `TestSignInGateway` | `SignInGatewayContractTest` |
| `PaymentGateway` | `TestPaymentGateway` | `PaymentGatewayContractTest` |
| `AccountDataExporter` | `TestAccountDataExporter` | `AccountDataExporterContractTest` |
| `SampleDataController` | `TestSampleDataController` | `SampleDataControllerContractTest` |
| `UsageAllowance` | `TestUsageAllowance` | `UsageAllowanceContractTest` |
| `PrepPlanRepository` | `TestPrepPlanRepository` | `PrepPlanRepositoryContractTest` |
| `ContentReportRepository` | `TestContentReportRepository` | `ContentReportRepositoryContractTest` |
| `TailoringReviewStateRepository` | `TestTailoringReviewStateRepository` | `TailoringReviewStateRepositoryContractTest` |
| `MockLatency` | `NoMockLatency` | none |

Also in `core:testing`: `TestClock` and `TestIdGenerator`. To test a real implementation, extend the contract class and return your implementation from its `create...` method.

## Known limits

- The mock has one fixed account. Only an account deletion resets the credits. A real backend keeps credits per account.
- `OfflineSampleDataController.load()` waits for the purchase delay of the mock gateway (about 1.2 s).
- `DebugScenario` still forces loading, empty, error, and offline states in the ViewModels. The mock does not replace it.
