package com.tailormyresume.app.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.auth.jsonResponse
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.core.model.AccountMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Test

class RemoteGuestTrialGatewayTest {
    private val fixture = TrialHarness()

    @After fun close() {
        fixture.server.shutdown()
    }

    @Test fun sameUidSessionGenerationChangeRejectsThePreparedOffer() = runTest {
        val subject = fixture.gateway()
        subject.prepare()
        fixture.wallet.clear()
        assertThat(runCatching { subject.claim() }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(fixture.requests("grant-challenges")).isEmpty()
        assertThat(runCatching { subject.continueAsGuest() }.isFailure).isTrue()
    }

    @Test fun expiredDisplayedPolicyRefreshesBeforeReadingTheSignalOrIssuingAChallenge() = runTest {
        val subject = fixture.gateway()
        subject.prepare()
        fixture.instant = kotlin.time.Instant.parse("2026-10-19T00:00:00Z")
        fixture.expiresAt = "2026-10-20T00:00:00Z"
        fixture.policyVersion = "guest-local-v2"
        assertThat(subject.claim()).isEqualTo(GuestTrialResult.Refresh(GuestTrialOffer.Available(2, changed = true)))
        assertThat(fixture.requests("grant-challenges")).isEmpty()
        assertThat(fixture.evidence.reads).isEqualTo(0)
    }

    @Test fun rateLimitedTrialRespectsRetryAfterBeforeAnotherEvidenceRequest() = runTest {
        fixture.initialize = { jsonResponse(429, """{"error":{"code":"RATE_LIMITED"}}""").setHeader("Retry-After", "60") }
        val subject = fixture.gateway()
        subject.prepare()
        subject.claim()
        assertThat(subject.claim()).isEqualTo(GuestTrialResult.Refresh(GuestTrialOffer.Unavailable(60)))
        assertThat(fixture.requests("grant-challenges")).hasSize(1)
        assertThat(fixture.evidence.reads).isEqualTo(1)
    }

    @Test fun deletedAccountCannotPublishAGuestSessionThroughContinue() = runTest {
        fixture.policyFailure = jsonResponse(409, """{"error":{"code":"ACCOUNT_DELETED"}}""")
        val subject = fixture.gateway()
        assertThat(subject.prepare()).isEqualTo(GuestTrialOffer.Unavailable(canContinue = false))
        assertThat(runCatching { subject.continueAsGuest() }.isFailure).isTrue()
        assertThat(fixture.sessions.observeAccount().first()).isNull()
    }

    @Test fun explicitTrialBindsTheCanonicalPayloadAndPublishesAuthoritativeWallet() = runTest {
        val subject = fixture.gateway()
        assertThat(subject.prepare()).isEqualTo(GuestTrialOffer.Available(2))
        assertThat(fixture.evidence.reads).isEqualTo(0)
        assertThat(subject.claim()).isEqualTo(GuestTrialResult.Granted(2))
        assertThat(fixture.wallet.cached?.credits).isEqualTo(2)
        assertThat(fixture.evidence.hash).isEqualTo(TrialHarness.REQUEST_HASH)
        val challenge = fixture.requests("grant-challenges").single()
        assertThat(challenge).contains(guestPayloadDigest("0123456789abcdef"))
        val request = Json.parseToJsonElement(fixture.requests("initialize").single()).jsonObject
        assertThat(request.keys).containsExactly("requestId", "policyVersion", "continuity", "challengeId", "integrityToken")
        assertThat(fixture.sessions.observeAccount().first()).isNull()
        subject.continueAsGuest()
        assertThat(fixture.sessions.observeAccount().first()?.mode).isEqualTo(AccountMode.ANONYMOUS)
    }

    @Test fun restartAfterLostReplyUsesTheSameBusinessKeyWithFreshEvidence() = runTest {
        fixture.initialize = { jsonResponse(500, """{"error":{"code":"INTERNAL_ERROR"}}""") }
        val first = fixture.gateway()
        first.prepare()
        assertThat(first.claim()).isInstanceOf(GuestTrialResult.Refresh::class.java)
        fixture.initialize = {
            fixture.free = 2
            jsonResponse(200, fixture.trialResponse())
        }
        val restarted = fixture.gateway()
        restarted.prepare()
        restarted.claim()
        val bodies = fixture.requests("initialize").map { Json.parseToJsonElement(it).jsonObject }
        assertThat(bodies).hasSize(2)
        assertThat(bodies[0]["requestId"]).isEqualTo(bodies[1]["requestId"])
        assertThat(fixture.requests("grant-challenges")).hasSize(2)
    }

    @Test fun policyChangesRequireReviewAndKeepTheStableOperationKey() = runTest {
        fixture.initialize = {
            fixture.policyVersion = "guest-local-v2"
            jsonResponse(409, """{"error":{"code":"POLICY_CHANGED"}}""")
        }
        val subject = fixture.gateway()
        subject.prepare()
        assertThat(subject.claim()).isEqualTo(GuestTrialResult.Refresh(GuestTrialOffer.Available(2, changed = true)))
        fixture.initialize = {
            fixture.free = 2
            jsonResponse(200, fixture.trialResponse())
        }
        subject.claim()
        val bodies = fixture.requests("initialize").map { Json.parseToJsonElement(it).jsonObject }
        assertThat(bodies[0]["requestId"]).isEqualTo(bodies[1]["requestId"])
        assertThat(bodies[1]["policyVersion"].toString()).isEqualTo("\"guest-local-v2\"")
    }

    @Test fun ownerChangeDuringEvidenceCannotPostOrPublishTheOldWallet() = runTest {
        val subject = fixture.gateway()
        subject.prepare()
        fixture.evidence.beforeToken = { fixture.identity.owner = "another-owner" }
        assertThat(runCatching { subject.claim() }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(fixture.requests("initialize")).isEmpty()
        assertThat(fixture.wallet.cached).isNull()
        assertThat(runCatching { subject.continueAsGuest() }.isFailure).isTrue()
    }

    @Test fun cancellationKeepsTheBusinessKeyWithoutPublishingASession() = runTest {
        val entered = CompletableDeferred<Unit>()
        fixture.evidence.beforeToken = {
            entered.complete(Unit)
            CompletableDeferred<Unit>().await()
        }
        val subject = fixture.gateway()
        subject.prepare()
        val active = async { subject.claim() }
        entered.await()
        active.cancel(CancellationException("route closed"))
        active.join()
        assertThat(fixture.sessions.observeAccount().first()).isNull()
        fixture.evidence.beforeToken = {}
        val restarted = fixture.gateway()
        restarted.prepare()
        restarted.claim()
        val bodies = fixture.requests("grant-challenges").map { Json.parseToJsonElement(it).jsonObject }
        assertThat(bodies[0]["requestId"]).isEqualTo(bodies[1]["requestId"])
    }

    @Test fun unavailablePolicyKeepsExistingCreditsAndAllowsGuestWork() = runTest {
        fixture.free = 7
        fixture.policyFailure = jsonResponse(503, """{"error":{"code":"PROMOTION_UNAVAILABLE"}}""").setHeader("Retry-After", "60")
        val subject = fixture.gateway()
        assertThat(subject.prepare()).isEqualTo(GuestTrialOffer.Unavailable(60))
        assertThat(fixture.wallet.cached?.credits).isEqualTo(7)
        subject.continueAsGuest()
        assertThat(fixture.sessions.observeAccount().first()?.id).isEqualTo("guest-one")
        assertThat(fixture.evidence.reads).isEqualTo(0)
    }

    @Test fun recordedTrialAtZeroDoesNotCollectEvidenceOrRequestAnotherGrant() = runTest {
        fixture.status = "ALREADY_GRANTED"
        val subject = fixture.gateway()
        assertThat(subject.prepare()).isEqualTo(GuestTrialOffer.Existing)
        assertThat(subject.claim()).isEqualTo(GuestTrialResult.Refresh(GuestTrialOffer.Existing))
        assertThat(fixture.requests("grant-challenges")).isEmpty()
        assertThat(fixture.wallet.cached?.credits).isEqualTo(0)
    }

    @Test fun invalidSignalNeverUsesAnEmptyOrSharedTrialIdentity() = runTest {
        fixture.evidence.signal = "0000000000000000"
        val subject = fixture.gateway()
        subject.prepare()
        assertThat(subject.claim()).isInstanceOf(GuestTrialResult.Refresh::class.java)
        assertThat(fixture.requests("grant-challenges")).isEmpty()
    }
}
