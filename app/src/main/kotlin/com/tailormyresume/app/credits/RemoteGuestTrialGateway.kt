package com.tailormyresume.app.credits

import com.tailormyresume.app.auth.AuthOutcome
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.auth.FirebaseUser
import com.tailormyresume.app.auth.LocalDataWiper
import com.tailormyresume.app.auth.SignOutCleaner
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GuestTrialGateway
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.CreditPolicyDto
import com.tailormyresume.core.network.dto.GrantChallengeRequest
import com.tailormyresume.core.network.dto.GuestInitializeRequest
import com.tailormyresume.core.network.dto.TrialContinuityDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Singleton
class RemoteGuestTrialGateway @Inject constructor(
    private val firebase: FirebaseSessionClient,
    private val uids: FirebaseUidProvider,
    private val api: TailorMyResumeApi,
    private val config: TailorMyResumeApiConfig,
    private val evidence: GuestEvidence,
    private val store: MockStateStore,
    private val sessions: SessionRepository,
    private val wallet: WalletSource,
    private val credits: CreditsRepository,
    private val cleaner: SignOutCleaner,
    private val wiper: LocalDataWiper,
    private val clock: Clock,
    private val referrals: com.tailormyresume.app.referrals.RemoteReferralGateway? = null,
) : GuestTrialGateway {
    private val mutex = Mutex()
    private var actor: FirebaseUser? = null
    private var actorGeneration: Int? = null
    private var policy: CreditPolicyDto? = null
    private var liveActor: String? = null
    private var retryAt: Instant? = null

    override suspend fun prepare(): GuestTrialOffer = mutex.withLock {
        try {
            val user = (firebase.resumeGuest() as? AuthOutcome.Success)?.value
                ?: return@withLock GuestTrialOffer.Unavailable(canContinue = false)
            if (actor?.uid != user.uid) retryAt = null
            actor = user
            liveActor = null
            val generation = wallet.generation()
            actorGeneration = generation
            apiResult { api.me() }.getOrThrow()
            ensureOwner(user.uid, generation)
            liveActor = user.uid
            referrals?.entered()
            wallet.refreshOrCached()
            refreshPolicy(user.uid, generation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            policy = null
            unavailable(failure)
        }
    }

    override suspend fun claim(): GuestTrialResult = mutex.withLock {
        val user = checkNotNull(actor)
        val generation = checkNotNull(actorGeneration)
        ensureOwner(user.uid, generation)
        val remaining = retryAt?.minus(clock.now())?.inWholeSeconds ?: 0
        if (remaining > 0) return@withLock GuestTrialResult.Refresh(GuestTrialOffer.Unavailable(remaining.toInt()))
        val displayed = policy ?: return@withLock GuestTrialResult.Refresh(GuestTrialOffer.Unavailable())
        if (displayed.expiresAt?.let(Instant::parse)?.let { it <= clock.now() } != false) {
            return@withLock GuestTrialResult.Refresh(refreshPolicy(user.uid, generation, changed = true))
        }
        if (displayed.toOffer() !is GuestTrialOffer.Available) return@withLock GuestTrialResult.Refresh(displayed.toOffer())
        try {
            initialize(user.uid, generation, displayed)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val error = (failure as? ApiException)?.error
            val offer = if (error == ApiError.PolicyChanged || error == ApiError.IntegrityChallengeInvalid) {
                refreshPolicy(user.uid, generation, changed = true)
            } else {
                unavailable(failure)
            }
            wallet.refreshOrCached()
            GuestTrialResult.Refresh(offer)
        }
    }

    private suspend fun initialize(uid: String, generation: Int, displayed: CreditPolicyDto): GuestTrialResult {
        val value = evidence.continuity()
        ensureOwner(uid, generation)
        val digest = guestPayloadDigest(value)
        val key = "guestTrial.${config.baseUrl}.$uid"
        val pending = store.read(key)?.split(":", limit = 2)
        val requestId = pending?.first() ?: UUID.randomUUID().toString()
        check(pending == null || pending.getOrNull(1) == digest) { "Trial request has changed" }
        store.write(key, "$requestId:$digest")
        val version = checkNotNull(displayed.version)
        val challenge = apiResult { api.grantChallenge(GrantChallengeRequest("GUEST_TRIAL", requestId, version, digest)) }.getOrThrow()
        ensureOwner(uid, generation)
        check(challenge.bindingVersion == 1 && Instant.parse(challenge.expiresAt) > clock.now())
        val token = evidence.token(challenge.requestHash)
        ensureOwner(uid, generation)
        val response = apiResult {
            api.initializeGuest(GuestInitializeRequest(requestId, version, TrialContinuityDto("ANDROID_ID", value), challenge.challengeId, token))
        }.getOrThrow()
        ensureOwner(uid, generation)
        check(wallet.update(response.wallet, generation))
        credits.refresh()
        ensureOwner(uid, generation)
        return when (response.trial.status) {
            "GRANTED" -> GuestTrialResult.Granted(response.grants.single().amount)
            "ALREADY_GRANTED" -> GuestTrialResult.Existing
            "NOT_ELIGIBLE" -> GuestTrialResult.NotEligible
            else -> error("Unsupported trial result")
        }
    }

    override suspend fun continueAsGuest() = mutex.withLock {
        val user = checkNotNull(actor)
        val generation = checkNotNull(actorGeneration)
        ensureOwner(user.uid, generation)
        if (liveActor != user.uid) error("Guest account is not available")
        withContext(NonCancellable) {
            val previous = sessions.lastAccountId()
            if (previous != null && previous != user.uid) {
                val job = sessions.observeKeptJobDescription().first()
                cleaner.clear()
                wiper.wipeAll()
                job?.let { sessions.keepJobDescription(it) }
            }
            if (uids.uid() != user.uid || !firebase.isAnonymous()) throw CancellationException("Guest session changed")
            sessions.saveLastAccountId(user.uid)
            sessions.saveAccount(SignInAccount(user.uid, "", "", AccountMode.ANONYMOUS))
        }
        wallet.refreshOrCached()
        credits.refresh()
    }

    private suspend fun refreshPolicy(uid: String, generation: Int, changed: Boolean = false): GuestTrialOffer {
        val current = apiResult { api.creditPolicy().policy }.getOrThrow()
        ensureOwner(uid, generation)
        policy = current
        return current.toOffer(changed)
    }

    private fun CreditPolicyDto.toOffer(changed: Boolean = false): GuestTrialOffer = when {
        accountMode != "ANONYMOUS" -> GuestTrialOffer.NotEligible
        guestTrial.status == "ALREADY_GRANTED" -> GuestTrialOffer.Existing
        guestTrial.status == "NOT_ELIGIBLE" -> GuestTrialOffer.NotEligible
        guestTrial.status == "AVAILABLE" && guestTrial.amount != null && (guestTrial.amount ?: 0) > 0 &&
            version != null && effectiveAt?.let(Instant::parse)?.let { it <= clock.now() } == true &&
            expiresAt?.let(Instant::parse)?.let { it > clock.now() } == true && guestTrial.operation == "GUEST_TRIAL" && guestTrial.bindingVersion == 1 &&
            guestTrial.integrity == "PLAY_INTEGRITY_STANDARD" && guestTrial.continuity == "ANDROID_ID" ->
            GuestTrialOffer.Available(checkNotNull(guestTrial.amount), changed)
        else -> GuestTrialOffer.Unavailable()
    }

    private fun unavailable(failure: Exception): GuestTrialOffer.Unavailable {
        val apiError = (failure as? ApiException)?.error
        if (apiError in setOf(ApiError.AccountDeleted, ApiError.Unauthenticated, ApiError.InvalidToken, ApiError.CrossAppToken)) liveActor = null
        val seconds = when (val error = apiError) {
            is ApiError.RateLimited -> error.retryAfterSeconds ?: 60
            is ApiError.IntegrityUnavailable -> error.retryAfterSeconds ?: 60
            is ApiError.PromotionUnavailable -> error.retryAfterSeconds ?: 60
            else -> null
        }
        if (seconds != null) retryAt = clock.now() + seconds.coerceAtLeast(1).seconds
        return GuestTrialOffer.Unavailable(seconds, canContinue = liveActor != null && liveActor == uids.uid() && firebase.isAnonymous())
    }

    private fun ensureOwner(uid: String, generation: Int) {
        if (uids.uid() != uid || !firebase.isAnonymous() || wallet.generation() != generation) {
            throw CancellationException("Guest session changed")
        }
    }
}
