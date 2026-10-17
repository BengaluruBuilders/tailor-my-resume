package com.tailormyresume.app.credits

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.auth.PrivateUpgradeStore
import com.tailormyresume.app.auth.UpgradeJournal
import com.tailormyresume.app.auth.api
import com.tailormyresume.app.auth.jsonResponse
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class GooglePromotionClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer().apply { start() }
    private val api = server.api()
    private var owner = "google-A"
    private val uids = FirebaseUidProvider { owner }
    private val wallet = WalletSource(api, uids)
    private val evidence = TrialEvidence()
    private var now = Instant.parse("2026-10-17T00:01:00Z")
    private val clock = object : Clock { override fun now() = now }
    private val calls = mutableListOf<Pair<String?, String>>()
    private var signupCommitted = false
    private var signupAvailable = true
    private var baseCommitted = false
    private var signupAmount = 5
    private var activate: () -> MockResponse = { jsonResponse(200, example("directUnused")) }
    private val fixtures = fixtureResponses()

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(calls) { calls += request.path to request.body.clone().readUtf8() }
                return when (request.path) {
                    "/v1/tailormyresume/account/activation-observations" -> {
                        val direct = fixtures.getValue("directUnused").jsonObject
                        jsonResponse(200, JsonObject(direct.filterKeys { it in setOf("wallet", "activation", "grants", "referralEligibility") }).toString())
                    }
                    "/v1/tailormyresume/credits/policy" -> jsonResponse(200, policy())
                    "/v1/tailormyresume/account/activation-challenges" -> jsonResponse(201, TrialHarness.CHALLENGE)
                    "/v1/tailormyresume/account/activate" -> activate()
                    else -> jsonResponse(404, """{"error":{"code":"NOT_FOUND"}}""")
                }
            }
        }
    }

    @After fun close() {
        server.shutdown()
        File(context.filesDir, "account-upgrades").deleteRecursively()
    }

    @Test fun directSignupBindsBothActionsAndUsesServerAmounts() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val journal = journal()
        store.write(journal)
        val client = client(store)
        assertThat(client.prepare(journal)).isEqualTo(GoogleAccountState.Offer(5, 2, true))
        assertThat(client.claim(journal)).isEqualTo(GoogleAccountState.Complete(5, 2))
        val body = posted().single()
        assertThat(body["policyVersion"]?.jsonPrimitive?.content).isEqualTo("signup-local-v1")
        assertThat(body["trialPolicyVersion"]?.jsonPrimitive?.content).isEqualTo("guest-local-v1")
        assertThat(body["upgradeIntent"]).isEqualTo(JsonNull)
        assertThat(evidence.hash).isEqualTo(TrialHarness.REQUEST_HASH)
        assertThat(wallet.cached?.credits).isEqualTo(7)
    }

    @Test fun baseCanCommitWithNoSignupCampaignAndLaterSignupUsesOriginalBaseRevision() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val client = client(store)
        val journal = journal()
        store.write(journal)
        signupAvailable = false
        activate = { jsonResponse(200, example("directBaseSignupAbsent")) }
        assertThat(client.prepare(journal)).isEqualTo(GoogleAccountState.Offer(null, 2, true))
        assertThat(client.claim(journal)).isEqualTo(GoogleAccountState.Complete(null, 2))
        assertThat(posted().single()["policyVersion"]).isEqualTo(JsonNull)
        signupAvailable = true
        baseCommitted = true
        activate = { jsonResponse(200, example("signupRetryAfterBaseFirst")) }
        val pending = checkNotNull(store.read())
        client.prepare(pending)
        assertThat(client.claim(pending)).isEqualTo(GoogleAccountState.Complete(5, null))
        val bodies = posted()
        assertThat(bodies[0]["requestId"]).isNotEqualTo(bodies[1]["requestId"])
        assertThat(bodies[1]["trialPolicyVersion"]?.jsonPrimitive?.content).isEqualTo("guest-local-v1")
        assertThat(wallet.cached?.credits).isEqualTo(7)
    }

    @Test fun partialRateLimitDeadlineSurvivesRestartAndOnlyMissingGrantGetsANewKey() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        var client = client(store)
        val journal = journal()
        store.write(journal)
        activate = { jsonResponse(200, httpExample("baseRateLimited")).setHeader("Retry-After", "60") }
        client.prepare(journal)
        assertThat(client.claim(journal)).isEqualTo(GoogleAccountState.Complete(5, null, retryAfterSeconds = 60))
        val pending = checkNotNull(store.read())
        val tokenReads = evidence.reads
        client = client(store)
        assertThat(client.claim(pending)).isEqualTo(GoogleAccountState.Retry(true, 60))
        assertThat(evidence.reads).isEqualTo(tokenReads)
        assertThat(posted()).hasSize(1)
        now += 61.seconds
        signupCommitted = true
        signupAvailable = false
        activate = { jsonResponse(200, example("directBaseRetryAfterSignupPause")) }
        client.prepare(pending)
        assertThat(client.claim(pending)).isEqualTo(GoogleAccountState.Complete(null, 2))
        val bodies = posted()
        assertThat(bodies[0]["requestId"]).isNotEqualTo(bodies[1]["requestId"])
        assertThat(bodies[1]["policyVersion"]?.jsonPrimitive?.content).isEqualTo("signup-local-v1")
        assertThat(bodies[0]["continuity"]).isEqualTo(bodies[1]["continuity"])
        assertThat(wallet.cached?.credits).isEqualTo(5)
    }

    @Test fun lostCommittedReplyRecoversBeforeObservationPolicyOrNewIntegrityEvidence() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val original = journal()
        store.write(original)
        val first = client(store)
        first.prepare(original)
        activate = { jsonResponse(500, """{"error":{"code":"INTERNAL_ERROR"}}""") }
        assertThat(runCatching { first.claim(original) }.isFailure).isTrue()
        val observations = requests("activation-observations")
        activate = { jsonResponse(200, example("directUnused").replace("\"replayed\":false", "\"replayed\":true")) }
        val restarted = client(store)
        val pending = checkNotNull(store.read())
        restarted.prepare(pending)
        assertThat(requests("activation-observations")).isEqualTo(observations)
        assertThat(posted()).hasSize(2)
        assertThat(posted()[0]["requestId"]).isEqualTo(posted()[1]["requestId"])
        assertThat(posted()[1]["integrityToken"]?.jsonPrimitive?.content).isEqualTo("committed-recovery")
    }

    @Test fun staleOwnerDuringIntegrityCannotPostGrantOrApplyWallet() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val journal = journal()
        store.write(journal)
        val client = client(store)
        client.prepare(journal)
        evidence.beforeToken = { owner = "google-C" }
        assertThat(runCatching { client.claim(journal) }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(posted()).isEmpty()
        assertThat(wallet.cached).isNull()
    }

    @Test fun displayedAmountComesFromPolicyWithoutAClientSignupConstant() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        signupAmount = 13
        assertThat(client(store).prepare(journal())).isEqualTo(GoogleAccountState.Offer(13, 2, true))
        assertThat(evidence.reads).isEqualTo(0)
    }

    private fun client(store: PrivateUpgradeStore) = GooglePromotionClient(api, evidence, store, wallet, TestCreditsRepository(), uids, clock)

    private fun journal() = UpgradeJournal(server.url("/").toString(), owner, "prepare", null, completionKey = "complete", observationKey = "observe", path = "DIRECT_GOOGLE", phase = "LINKED")

    private fun posted() = synchronized(calls) { calls.filter { it.first?.endsWith("/activate") == true }.map { Json.parseToJsonElement(it.second).jsonObject } }
    private fun requests(path: String) = synchronized(calls) { calls.count { it.first?.endsWith(path) == true } }
    private fun example(name: String) = fixtures.getValue(name).toString()
    private fun httpExample(name: String) = fixtureRoot().getValue("httpExamples").jsonObject.getValue(name).jsonObject.getValue("body").toString()

    private fun policy(): String {
        val original = fixtures.getValue("signupPolicyNewDirect").jsonObject
        val base = original.getValue("baseTrial").jsonObject.toMutableMap().apply {
            if (baseCommitted) {
                put("status", JsonPrimitive("ALREADY_GRANTED"))
                put("version", JsonNull)
                put("grantPolicyVersion", JsonPrimitive("guest-local-v1"))
            }
        }
        val signup = original.toMutableMap().apply {
            put("baseTrial", JsonObject(base))
            put("amount", if (signupAvailable) JsonPrimitive(signupAmount) else JsonNull)
            put("status", JsonPrimitive(if (signupCommitted) "ALREADY_GRANTED" else if (signupAvailable) "AVAILABLE" else "UNAVAILABLE"))
            put("version", if (signupAvailable) JsonPrimitive("signup-local-v1") else JsonNull)
            put("grantPolicyVersion", if (signupCommitted) JsonPrimitive("signup-local-v1") else JsonNull)
        }
        return """{"policy":{"version":"guest-local-v1","effectiveAt":"2026-10-17T00:00:00Z","expiresAt":"2026-10-19T00:00:00Z","accountMode":"GOOGLE","guestTrial":{"status":"NOT_ELIGIBLE","reason":"ACCOUNT_MODE_INELIGIBLE","amount":null,"operation":"GUEST_TRIAL","integrity":"PLAY_INTEGRITY_STANDARD","continuity":"ANDROID_ID","bindingVersion":1},"signupBonus":${JsonObject(signup)}}} """
    }

    private fun fixtureResponses() = fixtureRoot().getValue("examples").jsonObject
    private fun fixtureRoot(): JsonObject {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }.first {
            File(it, "docs/testing/earn-credits/2026-10-17/02-google-signup/fixtures/responses.json").isFile
        }
        return Json.parseToJsonElement(File(root, "docs/testing/earn-credits/2026-10-17/02-google-signup/fixtures/responses.json").readText()).jsonObject
    }
}
