package com.tailormyresume.app.credits

import com.tailormyresume.app.auth.AuthOutcome
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.auth.FirebaseUser
import com.tailormyresume.app.auth.LocalDataWiper
import com.tailormyresume.app.auth.api
import com.tailormyresume.app.auth.jsonResponse
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.signOutCleaner
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.time.Clock
import kotlin.time.Instant

internal class GuestIdentity : FirebaseSessionClient, FirebaseUidProvider {
    var owner = "guest-one"
    override fun uid() = owner
    override fun isAnonymous() = true
    override suspend fun resumeGuest() = AuthOutcome.Success(FirebaseUser(owner, "", "", AccountMode.ANONYMOUS))
    override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = error("Google cannot switch a guest")
    override fun signOut() = Unit
}

internal class TrialEvidence : GuestEvidence {
    var hash: String? = null
    var reads = 0
    var signal = "0123456789abcdef"
    var beforeToken: suspend () -> Unit = {}
    override suspend fun continuity(): String = signal.also { reads++ }
    override suspend fun token(requestHash: String): String {
        hash = requestHash
        beforeToken()
        return "synthetic-integrity-test-token"
    }
}

internal class TrialHarness {
    val server = MockWebServer().apply { start() }
    val identity = GuestIdentity()
    val evidence = TrialEvidence()
    val store = TestMockStateStore()
    val sessions = TestSessionRepository()
    val api = server.api()
    val wallet = WalletSource(api, identity)
    val calls = mutableListOf<RecordedRequest>()
    var policyVersion = "guest-local-v1"
    var status = "AVAILABLE"
    var free = 0
    var instant = Instant.parse("2026-10-17T00:00:00Z")
    var expiresAt = "2026-10-18T00:00:00Z"
    var policyFailure: MockResponse? = null
    var initialize: (RecordedRequest) -> MockResponse = {
        free = 2
        jsonResponse(200, trialResponse())
    }
    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                synchronized(calls) { calls += request }
                return when (request.path) {
                    "/v1/me" -> jsonResponse(200, """{"user":{"id":"guest-one","createdAt":"2026-10-17T00:00:00Z"}}""")
                    "/v1/tailormyresume/wallet" -> jsonResponse(200, """{"wallet":${walletBody()}}""")
                    "/v1/tailormyresume/credits/policy" -> policyFailure ?: jsonResponse(200, policyBody())
                    "/v1/tailormyresume/credits/grant-challenges" -> jsonResponse(201, CHALLENGE)
                    "/v1/tailormyresume/credits/initialize" -> initialize(request)
                    else -> jsonResponse(404, """{"error":{"code":"NOT_FOUND"}}""")
                }
            }
        }
    }
    fun gateway() = RemoteGuestTrialGateway(
        identity, identity, api, TailorMyResumeApiConfig(server.url("/").toString()), evidence, store,
        sessions, wallet, TestCreditsRepository(), server.signOutCleaner(), LocalDataWiper.None,
        object : Clock {
            override fun now() = instant
        },
    )
    fun requests(path: String) = synchronized(calls) { calls.filter { it.path?.endsWith(path) == true }.map { it.body.clone().readUtf8() } }
    fun walletBody() = """{"credits":$free,"freeCredits":$free,"purchasedCredits":0,"analysesLeftToday":20,"day":"2026-10-17","resetsAt":"2026-10-17T18:30:00Z"}"""
    fun trialResponse() = """{"wallet":${walletBody()},"grants":[{"id":"33333333-3333-4333-8333-333333333333","kind":"GUEST_TRIAL","amount":2,"createdAt":"2026-10-17T00:00:00Z","policyVersion":"guest-local-v1"}],"trial":{"status":"GRANTED","reason":"ELIGIBLE"},"replayed":false}"""
    fun policyBody() = """{"policy":{"version":"$policyVersion","effectiveAt":"2026-10-16T00:00:00Z","expiresAt":"$expiresAt","accountMode":"ANONYMOUS","guestTrial":{"status":"$status","reason":"ELIGIBLE","amount":2,"operation":"GUEST_TRIAL","integrity":"PLAY_INTEGRITY_STANDARD","continuity":"ANDROID_ID","bindingVersion":1}}}"""
    companion object {
        const val REQUEST_HASH = "nrHgWZfUo-R3ouNur9XjOKmtKapV6l6RbGsjuUeBSSY"
        const val CHALLENGE = """{"challengeId":"22222222-2222-4222-8222-222222222222","challenge":"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8","expiresAt":"2026-10-17T00:05:00Z","bindingVersion":1,"requestHash":"$REQUEST_HASH"}"""
    }
}
