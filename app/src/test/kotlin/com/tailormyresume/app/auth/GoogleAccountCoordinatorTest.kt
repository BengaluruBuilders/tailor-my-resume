package com.tailormyresume.app.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.app.billing.walletJson
import com.tailormyresume.app.credits.GooglePromotionClient
import com.tailormyresume.app.credits.TrialEvidence
import com.tailormyresume.core.data.account.LocalWorkCheckpoint
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Base64
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class GoogleAccountCoordinatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, TmrDatabase::class.java).allowMainThreadQueries().build()
    private val server = MockWebServer().apply { start() }
    private val api = server.api()
    private val store = TestMockStateStore()
    private val sessions = TestSessionRepository()
    private val pending = TestPendingAccountWipe()
    private val fence = LocalWorkFence()
    private val firebase = Identity()
    private val credentials = ScriptedCredentials(AuthOutcome.Success(token("google-B")))
    private val environment = server.url("/").toString()
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-17T00:01:00Z")
    }
    private var prepare: () -> MockResponse = { jsonResponse(201, """{"intentId":"intent","createdAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-18T00:00:00Z","state":"PREPARED"}""") }
    private var complete: () -> MockResponse = { jsonResponse(200, COMPLETE) }
    private var observation: () -> MockResponse = { errorResponse(503, "IDENTITY_UNAVAILABLE") }
    private var challenge: () -> MockResponse = { jsonResponse(201, com.tailormyresume.app.credits.TrialHarness.CHALLENGE) }
    private var activation: () -> MockResponse = { jsonResponse(200, fixture("guestOneToSix")) }
    private val calls = mutableListOf<Pair<String?, String>>()

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(calls) { calls += request.path to request.body.clone().readUtf8() }
                return when (request.path) {
                    "/v1/tailormyresume/account/upgrade-intents" -> prepare()
                    "/v1/tailormyresume/account/upgrade-intents/complete" -> complete()
                    "/v1/tailormyresume/account/activation-observations" -> observation()
                    "/v1/tailormyresume/credits/policy" -> jsonResponse(200, policy())
                    "/v1/tailormyresume/account/activation-challenges" -> challenge()
                    "/v1/tailormyresume/account/activate" -> activation()
                    "/v1/tailormyresume/wallet" -> jsonResponse(200, """{"wallet":${walletJson()}}""")
                    "/v1/me" -> jsonResponse(200, ME_BODY)
                    else -> errorResponse(404, "NOT_FOUND")
                }
            }
        }
    }

    @After fun close() {
        db.close()
        server.shutdown()
        listOf("account-work", "account-upgrades", "exports").forEach { File(context.filesDir, it).deleteRecursively() }
    }

    @Test fun navigationCancellationAfterDirectPublicationExposesRetryAndPreservesWork() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.user = FirebaseUser("A", "A", "a@example.com", googleSubject = "google-B")
        var navigating = true
        val navigation = object : com.tailormyresume.core.data.repository.SessionRepository by sessions {
            override suspend fun saveAccount(account: SignInAccount) {
                sessions.saveAccount(account)
                if (navigating) {
                    navigating = false
                    currentCoroutineContext().cancel(CancellationException("Sign-in screen removed"))
                }
            }
        }
        val coordinator = coordinator(checkpoints, journals, navigation)
        val operation = launch { coordinator.signIn() }
        operation.join()
        assertThat(operation.isCancelled).isTrue()
        assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Retry(canCancel = true))
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("B")
        assertThat(journals.read()?.phase).isEqualTo("LINKED")
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
        fence.check(fence.ticket())
        availableObservation()
        coordinator.retry()
        assertThat(coordinator.state.value).isInstanceOf(GoogleAccountState.Offer::class.java)
        assertThat(firebase.switches).isEqualTo(1)
        assertThat(requests("/activate")).isEmpty()
    }

    @Test fun navigationCancellationAfterSameUidLinkReleasesFenceWithoutLosingJournalOrWork() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.conflict = false
        var navigating = true
        val navigation = object : com.tailormyresume.core.data.repository.SessionRepository by sessions {
            override suspend fun saveAccount(account: SignInAccount) {
                sessions.saveAccount(account)
                if (navigating) {
                    navigating = false
                    currentCoroutineContext().cancel(CancellationException("Sign-in screen removed"))
                }
            }
        }
        val coordinator = coordinator(checkpoints, journals, navigation)
        val operation = launch { coordinator.signIn() }
        operation.join()
        assertThat(operation.isCancelled).isTrue()
        assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Retry(canCancel = true))
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
        assertThat(journals.read()?.phase).isEqualTo("LINKED")
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        fence.check(fence.ticket())
        assertThat(firebase.switches).isEqualTo(0)
        assertThat(requests("/activate")).isEmpty()
    }

    @Test fun cancellationDuringConflictCompletionKeepsFenceAndRecoverableJournal() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        val original = switchedJournal()
        journals.write(original)
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        fence.freeze()
        firebase.beforeCurrent = {
            firebase.beforeCurrent = {}
            currentCoroutineContext().cancel(CancellationException("Account screen removed"))
        }
        val coordinator = coordinator(checkpoints, journals)
        val operation = launch { coordinator.open() }
        operation.join()
        assertThat(operation.isCancelled).isTrue()
        assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Retry(canCancel = false))
        assertThat(journals.read()).isEqualTo(original)
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
        assertThat(runCatching { fence.check(fence.ticket()) }.isFailure).isTrue()
    }

    @Test fun conflictCheckpointsBeforeAnyGoogleSwitchAndCancellationKeepsGuestWork() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        val coordinator = coordinator(checkpoints, journals)
        assertThat(coordinator.signIn()).isEqualTo(SignInResult.AwaitingChoice)
        assertThat(coordinator.state.value).isInstanceOf(GoogleAccountState.Conflict::class.java)
        assertThat(firebase.switches).isEqualTo(0)
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
        coordinator.cancel()
        assertThat(firebase.uid()).isEqualTo("A")
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        assertThat(journals.read()).isNull()
    }

    @Test fun sameUidLinkRetainsLocalIdsAndPendingJobWhileUnavailableOffersRemainRetryable() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.conflict = false
        store.write("tailoring.request.unfinished.local-app", "legitimate-job")
        coordinator(checkpoints, journals).signIn()
        assertThat(firebase.uid()).isEqualTo("A")
        assertThat(sessions.observeAccount().first()?.mode).isEqualTo(AccountMode.GOOGLE)
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        assertThat(store.read("tailoring.request.unfinished.local-app")).isEqualTo("legitimate-job")
        assertThat(journals.read()?.phase).isEqualTo("LINKED")
        assertThat(firebase.switches).isEqualTo(0)
    }

    @Test fun unavailablePrelinkServiceDoesNotBlockGenuineSameUidGoogleLogin() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.conflict = false
        prepare = { errorResponse(503, "IDENTITY_UNAVAILABLE") }
        coordinator(checkpoints, journals).signIn()
        assertThat(firebase.user.mode).isEqualTo(AccountMode.GOOGLE)
        assertThat(firebase.uid()).isEqualTo("A")
        assertThat(sessions.observeAccount().first()?.mode).isEqualTo(AccountMode.GOOGLE)
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        assertThat(journals.read()?.intentId).isNull()
    }

    @Test fun processDeathAfterFirebaseSwitchResumesOnlyBoundDestinationAndImportsOnce() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(journal().copy(phase = "SWITCHING", destinationGoogleDigest = GoogleCredentialIdentity.digestSubject("google-B")))
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Complete(null, null, preservedWork = true))
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("B")
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
        assertThat(journals.read()).isNull()
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
    }

    @Test fun pendingDeletionDuringServerCompletionPreventsLocalImportAndPublication() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(journal().copy(phase = "SWITCHED", destination = "B", destinationGoogleDigest = GoogleCredentialIdentity.digestSubject("google-B")))
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = {
            pending.current = com.tailormyresume.core.data.repository.PendingWipeState.REQUESTED
            jsonResponse(200, COMPLETE)
        }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        assertThat(coordinator.state.value).isInstanceOf(GoogleAccountState.Retry::class.java)
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
        assertThat(db.localHandoffDao().find("completion")).isNull()
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
    }

    @Test fun linkedRecentAuthRetryReauthenticatesSameUserWithoutSwitchingOrReplacingJournal() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.user = FirebaseUser("A", "", "", googleSubject = "google-B")
        val original = journal().copy(phase = "LINKED", signupVersion = "original-signup", baseVersion = "original-base", retryAtMillis = 1)
        journals.write(original)
        observation = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        val rejected = checkNotNull(journals.read())
        assertThat(rejected.copy(recentAuthRecovery = null)).isEqualTo(original)
        assertThat(rejected.recentAuthRecovery?.operation).isEqualTo("RECONCILE")
        assertThat(credentials.requests).isEqualTo(0)
        availableObservation()
        coordinator.retry()
        assertThat(firebase.authEvents).containsExactly("reauth:A", "refresh:A").inOrder()
        assertThat(firebase.switches).isEqualTo(0)
        assertThat(journals.read()).isEqualTo(original)
        assertThat(coordinator.state.value).isInstanceOf(GoogleAccountState.Offer::class.java)
        assertThat(db.profileDao().readProfile()?.fullName).isEqualTo("Guest")
    }

    @Test fun switchedRecoverySurvivesProcessDeathAndRetriesExactCompletionOnlyAfterExplicitCredentials() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        val original = switchedJournal()
        journals.write(original)
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        coordinator(checkpoints, journals).open()
        val rejected = checkNotNull(journals.read())
        assertThat(rejected.copy(recentAuthRecovery = null)).isEqualTo(original)
        val restartedStore = PrivateUpgradeStore(context, io)
        val restarted = coordinator(checkpoints, restartedStore)
        restarted.open()
        assertThat(requests("/complete")).hasSize(1)
        assertThat(credentials.requests).isEqualTo(0)
        complete = { jsonResponse(200, COMPLETE) }
        restarted.retry()
        assertThat(firebase.authEvents).containsExactly("reauth:B", "refresh:B").inOrder()
        assertThat(firebase.switches).isEqualTo(0)
        assertThat(requests("/complete").distinct()).hasSize(1)
        assertThat(requests("/complete")).hasSize(2)
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("B")
        assertThat(db.localHandoffDao().find("completion")).isNotNull()
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
        assertThat(restartedStore.read()).isNull()
    }

    @Test fun cancellationWrongSubjectAndProviderOutageRetainSwitchedRecoveryAndNeverSignIn() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(switchedJournal())
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        val rejected = journals.read()
        for (outcome in listOf(AuthOutcome.Failure(AuthFailure.Cancelled), AuthOutcome.Success(token("google-C")), AuthOutcome.Failure(AuthFailure.Offline))) {
            credentials.outcome = outcome
            coordinator.retry()
            assertThat(journals.read()).isEqualTo(rejected)
            assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Retry(canCancel = false))
            assertThat(firebase.authEvents).isEmpty()
            assertThat(firebase.switches).isEqualTo(0)
            assertThat(firebase.uid()).isEqualTo("B")
            assertThat(db.localHandoffDao().find("completion")).isNull()
            assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
            assertThat(runCatching { fence.check(fence.ticket()) }.isFailure).isTrue()
        }
        credentials.outcome = AuthOutcome.Success(token("google-B"))
        firebase.reauthFailure = AuthFailure.Offline
        coordinator.retry()
        assertThat(journals.read()).isEqualTo(rejected)
        assertThat(requests("/complete")).hasSize(1)
    }

    @Test fun claimFreshnessFailureResumesOriginalRequestAfterRestartAndCommittedReplayStaysLookupFree() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.user = FirebaseUser("A", "", "", googleSubject = "google-B")
        journals.write(journal().copy(phase = "LINKED"))
        availableObservation()
        val first = coordinator(checkpoints, journals)
        first.open()
        activation = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        first.claim()
        val rejected = checkNotNull(journals.read())
        assertThat(rejected.recentAuthRecovery?.operation).isEqualTo("CLAIM")
        val observations = requests("activation-observations").size
        activation = { jsonResponse(200, fixture("guestOneToSix").replace("\"replayed\":false", "\"replayed\":true")) }
        val restarted = coordinator(checkpoints, PrivateUpgradeStore(context, io))
        restarted.open()
        restarted.retry()
        val bodies = requests("/activate").map { Json.parseToJsonElement(it).jsonObject }
        assertThat(bodies).hasSize(2)
        assertThat(bodies[1].filterKeys { it != "integrityToken" }).isEqualTo(bodies[0].filterKeys { it != "integrityToken" })
        assertThat(requests("activation-observations")).hasSize(observations)
        assertThat(requests("activation-challenges")).hasSize(1)
        assertThat(journals.read()?.attemptCommitted).isTrue()
        assertThat(journals.read()?.recentAuthRecovery).isNull()
        assertThat(firebase.authEvents).containsExactly("reauth:A", "refresh:A").inOrder()
        assertThat(firebase.switches).isEqualTo(0)
    }

    @Test fun deletionDuringReauthenticationErasesPrivateRecoveryAndRejectsLateCompletion() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(switchedJournal())
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        val erased = checkNotNull(journals.read())
        firebase.beforeReauth = {
            pending.current = com.tailormyresume.core.data.repository.PendingWipeState.REQUESTED
            fence.freezeForErasure()
            journals.eraseFor("B", environment)
            checkpoints.erase("A", environment)
        }
        coordinator.retry()
        assertThat(requests("/complete")).hasSize(1)
        assertThat(journals.read()).isNull()
        assertThat(runCatching { journals.write(erased) }.isFailure).isTrue()
        assertThat(db.localHandoffDao().find("completion")).isNull()
        assertThat(runCatching { fence.check(fence.ticket()) }.isFailure).isTrue()
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
    }

    @Test fun expiredConflictCapabilityAfterFreshAuthenticationKeepsCheckpointWithoutImport() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(switchedJournal())
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        val rejected = journals.read()
        complete = { errorResponse(409, "UPGRADE_INTENT_EXPIRED") }
        coordinator.retry()
        assertThat(journals.read()).isEqualTo(rejected)
        assertThat(db.localHandoffDao().find("completion")).isNull()
        assertThat(checkpoints.read("A", environment)?.profile?.fullName).isEqualTo("Guest")
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
    }

    @Test fun differentFirebaseAccountDuringFreshCredentialsCannotAdoptPrivateHandoffOrReleaseItsFence() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        checkpoints.capture("A", environment)
        journals.write(switchedJournal())
        firebase.user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
        complete = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        val rejected = journals.read()
        firebase.beforeReauth = { firebase.user = FirebaseUser("C", "C", "c@example.com", googleSubject = "google-C") }
        coordinator.retry()
        assertThat(requests("/complete")).hasSize(1)
        assertThat(journals.read()).isEqualTo(rejected)
        assertThat(journals.readFor("C", environment, GoogleCredentialIdentity.digestSubject("google-C"))).isNull()
        assertThat(db.localHandoffDao().find("completion")).isNull()
        assertThat(sessions.observeAccount().first()?.id).isEqualTo("A")
        assertThat(runCatching { fence.check(fence.ticket()) }.isFailure).isTrue()
        assertThat(coordinator.state.value).isEqualTo(GoogleAccountState.Retry(canCancel = false))
    }

    @Test fun delayedPartialReceiptRecoveryRetainsRevisionsActionBindingAndRateDeadline() = runTest {
        val io = UnconfinedTestDispatcher(testScheduler)
        val checkpoints = LocalWorkCheckpoint(context, db, store, io)
        val journals = PrivateUpgradeStore(context, io)
        guest()
        firebase.user = FirebaseUser("A", "", "", googleSubject = "google-B")
        val original = journal().copy(
            phase = "LINKED", path = "DIRECT_GOOGLE", proof = null, intentId = null,
            requestId = "committed-partial-request", challengeId = "committed-challenge",
            signupVersion = "original-signup-receipt", baseVersion = "original-base-revision",
            requestedBase = true, continuityDigest = "original-continuity", attemptCommitted = true,
            retryAtMillis = clock.now().toEpochMilliseconds() + 300_000,
        )
        journals.write(original)
        observation = { errorResponse(403, "RECENT_GOOGLE_AUTH_REQUIRED") }
        val coordinator = coordinator(checkpoints, journals)
        coordinator.open()
        availableObservation("directUnused")
        coordinator.retry()
        assertThat(journals.read()).isEqualTo(original)
        assertThat(requests("/activate")).isEmpty()
        assertThat(requests("activation-challenges")).isEmpty()
        val offer = coordinator.state.value as GoogleAccountState.Offer
        assertThat(offer.canClaim).isFalse()
        assertThat(offer.retryAfterSeconds).isGreaterThan(0)
        assertThat(firebase.switches).isEqualTo(0)
    }

    private fun switchedJournal() = journal().copy(phase = "SWITCHED", destination = "B", destinationGoogleDigest = GoogleCredentialIdentity.digestSubject("google-B"))

    private fun availableObservation(example: String = "guestOneToSix") {
        observation = {
            val result = Json.parseToJsonElement(fixture(example)).jsonObject
            jsonResponse(200, JsonObject(result.filterKeys { it in setOf("wallet", "activation", "grants", "referralEligibility") }).toString())
        }
    }

    private fun requests(suffix: String): List<String> = synchronized(calls) { calls.filter { it.first?.endsWith(suffix) == true }.map { it.second } }

    private fun fixture(name: String): String {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }.first {
            File(it, "docs/testing/earn-credits/2026-10-17/02-google-signup/fixtures/responses.json").isFile
        }
        return Json.parseToJsonElement(File(root, "docs/testing/earn-credits/2026-10-17/02-google-signup/fixtures/responses.json").readText())
            .jsonObject.getValue("examples").jsonObject.getValue(name).toString()
    }

    private fun policy() = """{"policy":{"version":"guest-local-v1","effectiveAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-19T00:00:00Z","accountMode":"GOOGLE","guestTrial":{"status":"NOT_ELIGIBLE","reason":"ACCOUNT_MODE_INELIGIBLE","amount":null,"operation":"GUEST_TRIAL","integrity":"PLAY_INTEGRITY_STANDARD","continuity":"ANDROID_ID","bindingVersion":1},"signupBonus":${fixture("signupPolicyNewDirect")}}}"""

    private suspend fun guest() {
        db.profileDao().upsertProfile(ProfileEntity(fullName = "Guest", email = "", phone = "", headline = "", skills = emptyList()))
        sessions.saveLastAccountId("A")
        sessions.saveAccount(SignInAccount("A", "", "", AccountMode.ANONYMOUS))
    }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        checkpoints: LocalWorkCheckpoint,
        journals: PrivateUpgradeStore,
        sessionRepository: com.tailormyresume.core.data.repository.SessionRepository = sessions,
    ): GoogleAccountCoordinator {
        val wallet = WalletSource(api, firebase)
        return GoogleAccountCoordinator(
            firebase, credentials, api, TailorMyResumeApiConfig(environment), sessionRepository, journals,
            checkpoints, fence, pending, server.signOutCleaner(),
            GooglePromotionClient(
                api,
                TrialEvidence(),
                journals,
                wallet,
                TestCreditsRepository(),
                firebase,
                clock,
            ),
            wallet, firebase, clock, backgroundScope,
        )
    }

    private fun journal() = UpgradeJournal(environment, "A", "prepare", "proof", intentId = "intent", completionKey = "completion", observationKey = "observation")

    private class Identity : FirebaseSessionClient, FirebaseUidProvider {
        var user = FirebaseUser("A", "", "", AccountMode.ANONYMOUS)
        var conflict = true
        var switches = 0
        val authEvents = mutableListOf<String>()
        var reauthFailure: AuthFailure? = null
        var beforeReauth: suspend () -> Unit = {}
        override fun uid() = user.uid
        override fun isAnonymous() = user.mode == AccountMode.ANONYMOUS
        var beforeCurrent: suspend () -> Unit = {}
        override suspend fun currentUser(): FirebaseUser {
            beforeCurrent()
            return user
        }
        override suspend fun linkGoogle(googleIdToken: String): AuthOutcome<FirebaseUser> = if (conflict) {
            AuthOutcome.Failure(AuthFailure.CredentialConflict)
        } else {
            user = user.copy(mode = AccountMode.GOOGLE, googleSubject = "google-B")
            AuthOutcome.Success(user)
        }
        override suspend fun refreshGoogle() = AuthOutcome.Success(user)
        override suspend fun reauthenticateGoogle(googleIdToken: String, expectedUid: String, expectedGoogleDigest: String): AuthOutcome<FirebaseUser> {
            assertThat(expectedUid).isEqualTo(user.uid)
            assertThat(expectedGoogleDigest).isEqualTo(GoogleCredentialIdentity.digestSubject(checkNotNull(user.googleSubject)))
            assertThat(GoogleCredentialIdentity.digest(googleIdToken)).isEqualTo(expectedGoogleDigest)
            authEvents += "reauth:$expectedUid"
            beforeReauth()
            reauthFailure?.let { return AuthOutcome.Failure(it) }
            authEvents += "refresh:$expectedUid"
            return AuthOutcome.Success(user)
        }
        override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> {
            switches++
            user = FirebaseUser("B", "B", "b@example.com", googleSubject = "google-B")
            return AuthOutcome.Success(user)
        }
        override fun signOut() = Unit
    }

    private companion object {
        fun token(subject: String): String = "header." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"$subject"}""".toByteArray()) + ".signature"
        val COMPLETE = """{"wallet":${walletJson()},"completion":{"kind":"EXISTING_ACCOUNT","disposition":"PRESERVE_LOCAL_WORK","creditsTransferred":0,"purchasesTransferred":false,"runningJobsTransferred":false},"replayed":false}"""
    }
}
