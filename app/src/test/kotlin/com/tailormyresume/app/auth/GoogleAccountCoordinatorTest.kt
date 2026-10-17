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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
    private var prepare: () -> MockResponse = { jsonResponse(201, """{"intentId":"intent","createdAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-18T00:00:00Z","state":"PREPARED"}""") }
    private var complete: () -> MockResponse = { jsonResponse(200, COMPLETE) }

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/v1/tailormyresume/account/upgrade-intents" -> prepare()
                "/v1/tailormyresume/account/upgrade-intents/complete" -> complete()
                "/v1/tailormyresume/account/activation-observations" -> errorResponse(503, "IDENTITY_UNAVAILABLE")
                "/v1/tailormyresume/wallet" -> jsonResponse(200, """{"wallet":${walletJson()}}""")
                "/v1/me" -> jsonResponse(200, ME_BODY)
                else -> errorResponse(404, "NOT_FOUND")
            }
        }
    }

    @After fun close() {
        db.close()
        server.shutdown()
        listOf("account-work", "account-upgrades", "exports").forEach { File(context.filesDir, it).deleteRecursively() }
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

    private suspend fun guest() {
        db.profileDao().upsertProfile(ProfileEntity(fullName = "Guest", email = "", phone = "", headline = "", skills = emptyList()))
        sessions.saveLastAccountId("A")
        sessions.saveAccount(SignInAccount("A", "", "", AccountMode.ANONYMOUS))
    }

    private fun kotlinx.coroutines.test.TestScope.coordinator(checkpoints: LocalWorkCheckpoint, journals: PrivateUpgradeStore): GoogleAccountCoordinator {
        val wallet = WalletSource(api, firebase)
        return GoogleAccountCoordinator(
            firebase, credentials, api, TailorMyResumeApiConfig(environment), sessions, journals,
            checkpoints, fence, pending, server.signOutCleaner(),
            GooglePromotionClient(
                api,
                TrialEvidence(),
                journals,
                wallet,
                TestCreditsRepository(),
                firebase,
                Clock.System,
            ),
            wallet, firebase, Clock.System, backgroundScope,
        )
    }

    private fun journal() = UpgradeJournal(environment, "A", "prepare", "proof", intentId = "intent", completionKey = "completion", observationKey = "observation")

    private class Identity : FirebaseSessionClient, FirebaseUidProvider {
        var user = FirebaseUser("A", "", "", AccountMode.ANONYMOUS)
        var conflict = true
        var switches = 0
        override fun uid() = user.uid
        override fun isAnonymous() = user.mode == AccountMode.ANONYMOUS
        override suspend fun currentUser() = user
        override suspend fun linkGoogle(googleIdToken: String): AuthOutcome<FirebaseUser> = if (conflict) {
            AuthOutcome.Failure(AuthFailure.CredentialConflict)
        } else {
            user = user.copy(mode = AccountMode.GOOGLE, googleSubject = "google-B")
            AuthOutcome.Success(user)
        }
        override suspend fun refreshGoogle() = AuthOutcome.Success(user)
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
