package com.tailormyresume.app.referrals

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.app.auth.AuthOutcome
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.auth.FirebaseUser
import com.tailormyresume.app.auth.api
import com.tailormyresume.app.auth.errorResponse
import com.tailormyresume.app.auth.jsonResponse
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.credits.GuestEvidence
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class RemoteReferralGatewayTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer().apply { start() }
    private val api = server.api()
    private val fence = LocalWorkFence()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private var uid = "invitee"
    private var guest = false
    private var version = "referral-local-v1"
    private var available = true
    private var codeAvailable = true
    private var ownCode: String? = null
    private var continuityMode = "NONE"
    private var attached = false
    private var getFailure = false
    private var postFailure: String? = null
    private var postStatus = 409
    private var time = Instant.parse("2026-10-17T00:00:00Z")
    private val paths = mutableListOf<Pair<String, String>>()
    private var signalReads = 0
    private var tokenCalls = 0
    private var tokenHash: String? = null
    private var beforeToken: suspend () -> Unit = {}
    private val identity = object : FirebaseUidProvider, FirebaseSessionClient {
        override fun uid() = uid
        override fun isAnonymous() = guest
        override suspend fun currentUser() = FirebaseUser(uid, "", "", if (guest) AccountMode.ANONYMOUS else AccountMode.GOOGLE, if (guest) null else "provider-$uid")
        override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = error("No Google flow in fixture")
        override fun signOut() = Unit
    }
    private val evidence = object : GuestEvidence {
        override suspend fun continuity(): String {
            signalReads++
            error("Referral must use null")
        }
        override suspend fun token(requestHash: String): String {
            tokenCalls++
            tokenHash = requestHash
            beforeToken()
            return "synthetic-token"
        }
    }
    private val clock = object : Clock {
        override fun now() = time
    }
    private lateinit var store: PrivateReferralStore
    private fun createStore() = PrivateReferralStore(context, UnconfinedTestDispatcher()).also { store = it }
    private fun client(journal: PrivateReferralStore = createStore()) = RemoteReferralGateway(
        api, TailorMyResumeApiConfig(server.url("/").toString()), identity, identity,
        AccountOperationGuard(identity, fence), journal, evidence, WalletSource(api, identity), TestCreditsRepository(), clock, scope,
    )
    private fun content(client: RemoteReferralGateway) = client.state.value as ReferralUiState.Content
    private fun offer() = """{"status":"${if (available) "AVAILABLE" else "UNAVAILABLE"}","reason":"ELIGIBLE","version":"$version","effectiveAt":"2026-10-16T00:00:00Z","expiresAt":"2026-10-18T00:00:00Z","rewardAmount":13,"continuityMode":"$continuityMode","codeIssuance":"${if (codeAvailable) "AVAILABLE" else "UNAVAILABLE"}","attachmentEligibility":"${if (attached) "ATTACHED" else "ELIGIBLE"}","deadline":"2026-10-18T00:00:00Z"}"""
    private fun receipt() = """{"id":"33333333-3333-4333-8333-333333333333","status":"ATTACHED","source":"MANUAL","attachedAt":"2026-10-17T00:00:00Z","reward":{"recipient":"INVITER","amount":13,"status":"GRANTED","policyVersion":"referral-local-v1"}}"""
    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = request.body.clone().readUtf8()
                synchronized(paths) { paths += request.path.orEmpty() to body }
                return when (request.path) {
                    "/v1/tailormyresume/referrals/me" -> if (getFailure) {
                        errorResponse(503, "PROMOTION_UNAVAILABLE")
                    } else {
                        jsonResponse(
                            200,
                            """{"code":${ownCode?.let { """{"value":"$it","createdAt":"2026-10-17T00:00:00Z"}""" } ?: "null"},"shareUrl":null,"offer":${offer()},"attachment":${if (attached) receipt() else "null"},"stats":{"creditedCount":2,"earnedCredits":26}}""",
                        )
                    }
                    "/v1/tailormyresume/referrals/code" -> { ownCode = CODE; jsonResponse(201, """{"code":{"value":"$CODE","createdAt":"2026-10-17T00:00:00Z"},"shareUrl":null,"offer":${offer()},"replayed":false}""") }
                    "/v1/tailormyresume/referrals/challenges" -> jsonResponse(201, """{"challengeId":"22222222-2222-4222-8222-222222222222","challenge":"opaque","expiresAt":"2026-10-17T00:05:00Z","bindingVersion":1,"requestHash":"$HASH"}""")
                    "/v1/tailormyresume/referrals/attach" -> postFailure?.let { errorResponse(postStatus, it).setHeader("Retry-After", "11") } ?: run {
                        attached = true
                        jsonResponse(201, """{"attachment":${receipt()},"reward":${Json.parseToJsonElement(receipt()).jsonObject.getValue("reward")},"replayed":false}""")
                    }
                    "/v1/tailormyresume/wallet" -> jsonResponse(200, """{"wallet":{"credits":7,"freeCredits":7,"purchasedCredits":0,"analysesLeftToday":20,"day":"2026-10-17","resetsAt":"2026-10-17T18:30:00Z"}}""")
                    else -> errorResponse(404, "NOT_FOUND")
                }
            }
        }
    }

    @After fun close() {
        scope.cancel()
        server.shutdown()
        File(context.filesDir, "manual-referrals").deleteRecursively()
    }
    private fun requests(suffix: String) = synchronized(paths) { paths.filter { it.first.endsWith(suffix) }.map { it.second } }

    @Test fun optionalAndNoneUseNullAndPassiveReadsNeverCollectEvidence() = runTest {
        for (mode in listOf("NONE", "OPTIONAL")) {
            attached = false
            ownCode = null
            continuityMode = mode
            val freshStore = createStore()
            freshStore.clearAll()
            val client = client(freshStore)
            client.refresh()
            client.save(CODE)
            client.issue()
            assertThat(content(client).code).isEqualTo(CODE)
            assertThat(tokenCalls).isEqualTo(if (mode == "NONE") 0 else 1)
            client.apply()
            assertThat(content(client).attached).isTrue()
            assertThat(content(client).candidate).isNull()
            assertThat(content(client).reward).isEqualTo(13)
        }
        assertThat(signalReads).isEqualTo(0)
        assertThat(tokenCalls).isEqualTo(2)
        assertThat(tokenHash).isEqualTo(HASH)
        requests("/attach").forEach { assertThat(Json.parseToJsonElement(it).jsonObject.getValue("continuity")).isEqualTo(JsonNull) }
        requests("/challenges").forEach { assertThat(Json.parseToJsonElement(it).jsonObject.getValue("operation").jsonPrimitive.content).isEqualTo("REFERRAL_ATTACH") }
    }

    @Test fun guestCandidateSurvivesProcessAndSameUidGoogleUpgradeAndFirstCandidateWins() = runTest {
        guest = true
        val client = client()
        client.save(CODE)
        client.save(OTHER)
        assertThat(content(client).candidate).isEqualTo(CODE)
        assertThat(content(client).message).isEqualTo(ReferralMessage.ReplaceRequired)
        client.apply()
        client.issue()
        assertThat(requests("/attach")).isEmpty()
        assertThat(tokenCalls).isEqualTo(0)
        guest = false
        val recovered = client(createStore())
        recovered.refresh()
        assertThat(content(recovered).candidate).isEqualTo(CODE)
        val pending = checkNotNull(store.read(uid, server.url("/").toString()))
        assertThat(pending.environment).isEqualTo(server.url("/").toString())
        recovered.apply()
        val sent = Json.parseToJsonElement(requests("/attach").single()).jsonObject
        assertThat(sent.getValue("code").jsonPrimitive.content).isEqualTo(CODE)
        assertThat(sent.getValue("source").jsonPrimitive.content).isEqualTo("MANUAL")
        assertThat(content(recovered).attached).isTrue()
    }

    @Test fun unavailableDoesNotCollectAndInvalidCodeDoesNotMutate() = runTest {
        available = false
        val client = client()
        client.save(CODE)
        client.apply()
        client.save("https://example.invalid")
        assertThat(content(client).message).isEqualTo(ReferralMessage.Invalid)
        assertThat(tokenCalls).isEqualTo(0)
        assertThat(requests("/attach")).isEmpty()
    }

    @Test fun lostResponseThenDetachedGetClearsAllPrivateAttemptEvidence() = runTest {
        postFailure = "INTERNAL_ERROR"
        postStatus = 500
        val client = client()
        client.save(CODE)
        client.apply()
        val prior = checkNotNull(store.read(uid, server.url("/").toString()))
        assertThat(prior.phase).isEqualTo("SENT")
        attached = true
        available = false
        val recovered = client(createStore())
        recovered.refresh()
        assertThat(content(recovered).attached).isTrue()
        val cleared = checkNotNull(store.read(uid, server.url("/").toString()))
        assertThat(cleared.code).isNull()
        assertThat(cleared.requestId).isNull()
        assertThat(cleared.challengeId).isNull()
        assertThat(cleared.payloadDigest).isNull()
        assertThat(tokenCalls).isEqualTo(1)
        assertThat(requests("/attach")).hasSize(1)
    }

    @Test fun alreadyAttachedWithGetFailureKeepsOnlyReadMarkerAndContradictionNeverRetries() = runTest {
        postFailure = "REFERRAL_ALREADY_ATTACHED"
        val client = client()
        client.save(CODE)
        beforeToken = { getFailure = true }
        client.apply()
        assertThat(content(client).message).isEqualTo(ReferralMessage.ReadOnly)
        val marker = checkNotNull(store.read(uid, server.url("/").toString()))
        assertThat(marker.code).isNull()
        assertThat(marker.requestId).isNull()
        assertThat(marker.challengeId).isNull()
        getFailure = false
        client.refresh()
        client.apply()
        client.save(OTHER, true)
        assertThat(content(client).message).isEqualTo(ReferralMessage.ReadOnly)
        assertThat(requests("/attach")).hasSize(1)
    }

    @Test fun changedPolicyNeedsAnotherExplicitConfirmationBeforeFreshEvidence() = runTest {
        val client = client()
        client.save(CODE)
        version = "referral-local-v2"
        client.apply()
        assertThat(content(client).message).isEqualTo(ReferralMessage.PolicyChanged)
        assertThat(tokenCalls).isEqualTo(0)
        client.apply()
        assertThat(requests("/attach")).hasSize(1)
    }

    @Test fun ownerSwitchDuringEvidenceCannotSendOrRestoreOldState() = runTest {
        val client = client()
        client.save(CODE)
        beforeToken = {
            uid = "other"
            fence.invalidate()
        }
        try {
            client.apply()
        } catch (_: CancellationException) { }
        assertThat(requests("/attach")).isEmpty()
        assertThat(client.state.value).isEqualTo(ReferralUiState.Loading)
        val other = client(createStore())
        other.refresh()
        assertThat(content(other).candidate).isNull()
    }

    @Test fun erasureDuringEvidenceCannotRestoreJournal() = runTest {
        val client = client()
        client.save(CODE)
        beforeToken = {
            store.eraseFor(uid, server.url("/").toString())
            fence.invalidate()
        }
        try {
            client.apply()
        } catch (_: CancellationException) { }
        assertThat(store.read(uid, server.url("/").toString())).isNull()
        assertThat(requests("/attach")).isEmpty()
    }

    @Test fun retryDeadlineAndOriginalRequestKeySurviveRecreation() = runTest {
        postFailure = "RATE_LIMITED"
        postStatus = 429
        val client = client()
        client.save(CODE)
        client.apply()
        val pending = checkNotNull(store.read(uid, server.url("/").toString()))
        val recovered = client(createStore())
        recovered.refresh()
        recovered.apply()
        assertThat(requests("/attach")).hasSize(1)
        assertThat(content(recovered).retryAfterSeconds).isEqualTo(11)
        time = Instant.parse("2026-10-17T00:00:12Z")
        postFailure = null
        recovered.apply()
        assertThat(requests("/attach")).hasSize(2)
        requests("/attach").forEach { assertThat(Json.parseToJsonElement(it).jsonObject.getValue("requestId").jsonPrimitive.content).isEqualTo(pending.requestId) }
    }

    @Test fun explicitReplaceClearAndPrivatePersistenceNeverStoreTokenOrSignal() = runTest {
        val client = client()
        client.save(CODE)
        client.save(OTHER, true)
        assertThat(content(client).candidate).isEqualTo(OTHER)
        postFailure = "REFERRAL_SELF"
        client.apply()
        val text = File(context.filesDir, "manual-referrals").listFiles().orEmpty().joinToString { it.readText() }
        assertThat(text).doesNotContain("synthetic-token")
        assertThat(text).doesNotContain("integrityToken")
        client.clear()
        assertThat(content(client).candidate).isNull()
        store.clearAll()
        assertThat(store.read(uid, server.url("/").toString())).isNull()
    }

    @Test fun conflictCandidateRequiresExplicitRebindAndCannotReplaceDestinationCandidate() = runTest {
        val original = client()
        guest = true
        original.save(CODE)
        store.transferForReview(uid, "destination", server.url("/").toString(), "GOOGLE", fence.ticket()) { }
        uid = "destination"
        guest = false
        val destination = client(store)
        destination.refresh()
        assertThat(content(destination).reviewRequired).isTrue()
        destination.apply()
        assertThat(requests("/attach")).isEmpty()
        destination.acceptTransfer()
        destination.apply()
        assertThat(requests("/attach")).hasSize(1)
    }

    @Test fun missingConfigurationRetainsForeignSyntacticCandidateWithoutEvidenceOrMutation() = runTest {
        available = false; codeAvailable = false; guest = true
        val client = client()
        val foreign = "foreign-app.other-environment.abcdefghijklmnopqrstuvwxyz234567"
        client.save(foreign); client.apply(); client.issue()
        guest = false
        val restored = client(createStore())
        restored.refresh(); restored.apply(); restored.issue()
        assertThat(content(restored).candidate).isEqualTo(foreign)
        assertThat(requests("/attach")).isEmpty()
        assertThat(requests("/code")).isEmpty()
        assertThat(requests("/challenges")).isEmpty()
        assertThat(tokenCalls).isEqualTo(0)
        assertThat(signalReads).isEqualTo(0)
    }
    @Test fun serverRejectsForeignScopeWithoutChangingFixedDestinationOrClaimingApplied() = runTest {
        postFailure = "REFERRAL_CODE_INVALID"; postStatus = 400
        val client = client()
        val foreign = "foreign-app.other-environment.abcdefghijklmnopqrstuvwxyz234567"
        client.save(foreign); client.apply()
        assertThat(content(client).attached).isFalse()
        assertThat(content(client).message).isEqualTo(ReferralMessage.Invalid)
        assertThat(content(client).candidate).isEqualTo(foreign)
        assertThat(Json.parseToJsonElement(requests("/attach").single()).jsonObject.getValue("code").jsonPrimitive.content).isEqualTo(foreign)
        assertThat(paths.map { it.first }.all { it.startsWith("/v1/tailormyresume/") }).isTrue()
    }
    companion object {
        const val CODE = "resume-demo.preview-2.abcdefghijklmnopqrstuvwxyz234567"
        const val OTHER = "resume-demo.preview-2.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val HASH = "L079-sjpEzA28TrNZOp8Lk7Wn8K17fWOPToCDN-Zl6c"
    }
}
