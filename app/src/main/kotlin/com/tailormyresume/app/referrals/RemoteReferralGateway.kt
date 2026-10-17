package com.tailormyresume.app.referrals

import com.tailormyresume.app.BuildConfig
import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.credits.GuestEvidence
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.ReferralGateway
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralUiState
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.ActivationObservationRequest
import com.tailormyresume.core.network.dto.GrantChallengeRequest
import com.tailormyresume.core.network.dto.ReferralAttachRequest
import com.tailormyresume.core.network.dto.ReferralCodeRequest
import com.tailormyresume.core.network.dto.ReferralOfferDto
import com.tailormyresume.core.network.dto.ReferralReadResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class RemoteReferralGateway @Inject constructor(
    private val api: TailorMyResumeApi,
    private val config: TailorMyResumeApiConfig,
    private val uids: FirebaseUidProvider,
    private val firebase: FirebaseSessionClient,
    private val guard: AccountOperationGuard,
    private val store: PrivateReferralStore,
    private val evidence: GuestEvidence,
    private val wallet: WalletSource,
    private val credits: CreditsRepository,
    private val clock: Clock,
) : ReferralGateway {
    private val mutable = MutableStateFlow<ReferralUiState>(ReferralUiState.Loading)
    override val state: StateFlow<ReferralUiState> = mutable
    private val mutex = Mutex()
    private var shown: ReferralOfferDto? = null
    private var shownOwner: String? = null
    private var readSucceeded = false
    private var lastError: ApiError? = null
    private data class Owner(val ticket: AccountOperationGuard.Ticket, val provider: String, val epoch: Long)

    private suspend fun owner(): Owner {
        val ticket = guard.capture()
        val user = checkNotNull(firebase.currentUser())
        check(user.uid == ticket.owner)
        return Owner(ticket, user.mode.name + ":" + user.googleSubject?.let(com.tailormyresume.app.auth.GoogleCredentialIdentity::digestSubject).orEmpty(), store.epoch())
    }
    private suspend fun check(owner: Owner) {
        guard.check(owner.ticket)
        val user = checkNotNull(firebase.currentUser())
        if (user.mode.name + ":" + user.googleSubject?.let(com.tailormyresume.app.auth.GoogleCredentialIdentity::digestSubject).orEmpty() != owner.provider || owner.epoch != store.epoch()) throw CancellationException("Referral account changed")
    }
    private suspend fun read(owner: Owner): ReferralJournal {
        check(owner)
        val uid = checkNotNull(owner.ticket.owner)
        val stored = store.read(uid, config.baseUrl)
        check(owner)
        return stored ?: ReferralJournal(uid, config.baseUrl, owner.provider, owner.ticket.generation)
    }
    private suspend fun write(owner: Owner, value: ReferralJournal) {
        check(owner)
        store.write(value.copy(provider = owner.provider, generation = owner.ticket.generation), owner.epoch) { guard.check(owner.ticket) }
        check(owner)
    }
    private fun content() = mutable.value as? ReferralUiState.Content ?: ReferralUiState.Content()
    private suspend fun run(action: suspend (Owner) -> Unit) = mutex.withLock {
        val owner = owner()
        if (shownOwner != owner.ticket.owner) { shown = null; mutable.value = ReferralUiState.Loading }
        shownOwner = owner.ticket.owner
        try {
            mutable.value = content().copy(busy = true)
            action(owner)
        } catch (cancelled: CancellationException) {
            mutable.value = ReferralUiState.Loading
            throw cancelled
        } catch (failure: Exception) {
            check(owner)
            mutable.value = content().copy(busy = false, message = ReferralMessage.Retry)
        }
    }

    override suspend fun refresh() = run { refresh(it) }

    private suspend fun refresh(owner: Owner, message: ReferralMessage = ReferralMessage.None) {
        val journal = read(owner)
        readSucceeded = false
        val response = apiResult { api.referrals() }
        check(owner)
        if (response.isFailure) {
            mutable.value = ReferralUiState.Content(candidate = journal.code, guest = uids.isAnonymous(), reviewRequired = journal.reviewFrom != null,
                message = if (journal.phase == "READ_ONLY") ReferralMessage.ReadOnly else ReferralMessage.Unavailable)
            return
        }
        readSucceeded = true
        val result = response.getOrThrow()
        if (result.attachment?.status == "ATTACHED") {
            write(owner, journal.copy(code = null, requestId = null, revision = null, payloadDigest = null, challengeId = null, retryAt = null, phase = "ATTACHED", reviewFrom = null))
            show(owner, result, read(owner), ReferralMessage.Applied)
        } else {
            show(owner, result, journal, if (journal.phase == "READ_ONLY") ReferralMessage.ReadOnly else message)
        }
        if (!uids.isAnonymous()) {
            wallet.refreshOrCached()
            check(owner)
            credits.refresh()
            check(owner)
        }
    }
    private suspend fun show(owner: Owner, result: ReferralReadResponse, journal: ReferralJournal, message: ReferralMessage) {
        check(owner)
        shown = result.offer
        val available = usable(result.offer)
        mutable.value = ReferralUiState.Content(
            code = result.code?.value, candidate = journal.code, reward = result.offer.rewardAmount?.takeIf { available },
            creditedCount = result.stats.creditedCount, earnedCredits = result.stats.earnedCredits,
            canIssue = result.offer.codeIssuance in setOf("AVAILABLE", "ACTIVATION_REQUIRED"), guest = uids.isAnonymous(), attached = result.attachment?.status == "ATTACHED",
            canAttach = available && result.offer.attachmentEligibility == "ELIGIBLE" && journal.phase != "READ_ONLY" && journal.reviewFrom == null,
            deadline = result.offer.deadline, reviewRequired = journal.reviewFrom != null,
            message = if (message != ReferralMessage.None) message else if (!available) ReferralMessage.Unavailable else if (result.offer.attachmentEligibility == "NOT_ELIGIBLE") ReferralMessage.Ineligible else if (result.offer.attachmentEligibility == "EXPIRED") ReferralMessage.Expired else ReferralMessage.None,
            retryAfterSeconds = remaining(journal),
        )
    }
    private fun usable(offer: ReferralOfferDto): Boolean = offer.status == "AVAILABLE" && offer.version != null &&
        offer.rewardAmount?.let { it in 1..1_000_000 } == true && offer.effectiveAt?.let { Instant.parse(it) <= clock.now() } == true &&
        offer.expiresAt?.let { Instant.parse(it) > clock.now() } == true

    override suspend fun save(code: String, replace: Boolean) = run { owner ->
        val normalized = canonicalReferralCode(code, if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "staging") "staging" else "production")
        val current = read(owner)
        when {
            current.phase in setOf("READ_ONLY", "ATTACHED", "SENT") -> refresh(owner)
            normalized == null -> mutable.value = content().copy(busy = false, message = ReferralMessage.Invalid)
            current.code != null && current.code != normalized && !replace -> mutable.value = content().copy(busy = false, message = ReferralMessage.ReplaceRequired)
            else -> {
                write(owner, current.copy(code = normalized, requestId = null, revision = null, payloadDigest = null, challengeId = null, phase = "CANDIDATE", retryAt = null))
                refresh(owner, ReferralMessage.Saved)
            }
        }
    }
    override suspend fun clear() = run { owner ->
        var current = read(owner)
        if (current.phase == "READ_ONLY") { refresh(owner); return@run }
        if (current.phase == "SENT") {
            refresh(owner)
            current = read(owner)
            if (!readSucceeded || current.phase == "ATTACHED") return@run
        }
        write(owner, current.copy(code = null, requestId = null, revision = null, payloadDigest = null, challengeId = null, retryAt = null, reviewFrom = null, phase = "CANDIDATE"))
        refresh(owner)
    }
    override suspend fun issue() = run { owner ->
        var current = read(owner)
        if (uids.isAnonymous()) { refresh(owner); return@run }
        if (shown?.codeIssuance == "ACTIVATION_REQUIRED") {
            current = observe(owner, current)
            refresh(owner)
        }
        if (shown?.codeIssuance != "AVAILABLE") { refresh(owner); return@run }
        current = current.copy(issueRequestId = current.issueRequestId ?: UUID.randomUUID().toString())
        write(owner, current)
        val result = apiResult { api.referralCode(ReferralCodeRequest(checkNotNull(current.issueRequestId))) }
        check(owner)
        if (result.isFailure) { fail(owner, current, result.exceptionOrNull()); return@run }
        refresh(owner)
    }
    private suspend fun observe(owner: Owner, current: ReferralJournal): ReferralJournal {
        val journal = current.copy(observationId = current.observationId ?: UUID.randomUUID().toString())
        write(owner, journal)
        apiResult { api.observeActivation(ActivationObservationRequest(checkNotNull(journal.observationId))) }.getOrThrow()
        check(owner)
        return journal
    }

    override suspend fun apply() = run { owner ->
        var current = read(owner)
        if (uids.isAnonymous() || current.reviewFrom != null) { refresh(owner); return@run }
        val before = shown
        refresh(owner)
        current = read(owner)
        if (!readSucceeded || current.code == null || current.phase in setOf("ATTACHED", "READ_ONLY", "REQUIRES_NEW") || remaining(current) != null) return@run
        if (shown?.codeIssuance == "ACTIVATION_REQUIRED") { current = observe(owner, current); refresh(owner) }
        val offer = shown ?: return@run
        if (!content().canAttach) return@run
        if (before?.version != offer.version) {
            mutable.value = content().copy(message = ReferralMessage.PolicyChanged)
            return@run
        }
        if (current.phase == "SENT" && current.challengeId != null && current.requestId != null && current.revision != null) {
            val recovered = send(owner, current, "committed-recovery")
            if (recovered || lastError !in setOf(ApiError.IntegrityChallengeInvalid, ApiError.IntegrityRejected)) return@run
            current = read(owner)
            if (current.phase == "READ_ONLY" || content().message == ReferralMessage.PolicyChanged || remaining(current) != null) return@run
        }
        val revision = checkNotNull(offer.version)
        if (current.revision != null && current.revision != revision) {
            write(owner, current.copy(revision = revision, phase = "PREPARED", challengeId = null))
            mutable.value = content().copy(message = ReferralMessage.PolicyChanged)
            return@run
        }
        current = current.copy(requestId = current.requestId ?: UUID.randomUUID().toString(), revision = revision,
            payloadDigest = referralDigest(checkNotNull(current.code)), phase = "PREPARED", challengeId = null)
        write(owner, current)
        val challenge = apiResult { api.referralChallenge(GrantChallengeRequest("REFERRAL_ATTACH", checkNotNull(current.requestId), revision, checkNotNull(current.payloadDigest))) }
        check(owner)
        if (challenge.isFailure) { fail(owner, current, challenge.exceptionOrNull()); return@run }
        val proof = challenge.getOrThrow()
        check(proof.bindingVersion == 1 && Instant.parse(proof.expiresAt) > clock.now())
        current = current.copy(challengeId = proof.challengeId)
        write(owner, current)
        val token = evidence.token(proof.requestHash)
        check(owner)
        current = current.copy(phase = "SENT")
        write(owner, current)
        send(owner, current, token)
    }
    private suspend fun send(owner: Owner, journal: ReferralJournal, token: String): Boolean {
        val response = apiResult {
            val result = api.attachReferral(ReferralAttachRequest(checkNotNull(journal.requestId), checkNotNull(journal.code), "MANUAL", checkNotNull(journal.revision), null, checkNotNull(journal.challengeId), token))
            if (!result.isSuccessful) throw retrofit2.HttpException(result)
            checkNotNull(result.body())
        }
        check(owner)
        if (response.isSuccess) {
            check(response.getOrThrow().attachment.status == "ATTACHED")
            write(owner, journal.copy(code = null, requestId = null, revision = null, payloadDigest = null, challengeId = null, retryAt = null, phase = "ATTACHED"))
            refresh(owner, ReferralMessage.Applied)
            return true
        }
        fail(owner, journal, response.exceptionOrNull())
        return false
    }
    private suspend fun fail(owner: Owner, journal: ReferralJournal, failure: Throwable?) {
        val error = (failure as? ApiException)?.error
        lastError = error
        if (error in setOf(ApiError.ReferralCodeInvalid, ApiError.InvalidInput, ApiError.ReferralSelf, ApiError.ReferralIneligible, ApiError.ReferralWindowExpired, ApiError.IdempotencyConflict)) {
            write(owner, journal.copy(phase = "REQUIRES_NEW"))
        }
        val message = when (error) {
            ApiError.ReferralAlreadyAttached -> {
                write(owner, journal.copy(code = null, requestId = null, revision = null, payloadDigest = null, challengeId = null, retryAt = null, phase = "READ_ONLY"))
                refresh(owner)
                return
            }
            ApiError.PolicyChanged -> { refresh(owner); ReferralMessage.PolicyChanged }
            ApiError.ReferralCodeInvalid, ApiError.InvalidInput -> ReferralMessage.Invalid
            ApiError.ReferralSelf -> ReferralMessage.Self
            ApiError.ReferralIneligible -> ReferralMessage.Ineligible
            ApiError.ReferralWindowExpired -> ReferralMessage.Expired
            ApiError.IdempotencyConflict -> ReferralMessage.Conflict
            ApiError.AccountDeleted -> { store.eraseFor(journal.owner, journal.environment); mutable.value = ReferralUiState.Loading; return }
            is ApiError.RateLimited -> {
                val seconds = error.retryAfterSeconds?.takeIf { it > 0 } ?: 60
                write(owner, journal.copy(retryAt = clock.now().toEpochMilliseconds() + seconds.toLong() * 1000))
                ReferralMessage.Retry
            }
            else -> ReferralMessage.Retry
        }
        mutable.value = content().copy(busy = false, message = message, retryAfterSeconds = remaining(read(owner)))
    }
    private fun remaining(journal: ReferralJournal): Int? = journal.retryAt?.let {
        ((it - clock.now().toEpochMilliseconds() + 999) / 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt().takeIf { seconds -> seconds > 0 }
    }
    override suspend fun acceptTransfer() = run { owner ->
        val current = read(owner)
        refresh(owner)
        if (shown?.attachmentEligibility == "ELIGIBLE" && current.reviewFrom != null) {
            write(owner, current.copy(reviewFrom = null, provider = owner.provider, requestId = null, phase = "CANDIDATE"))
            refresh(owner, ReferralMessage.Saved)
        }
    }
    fun invalidate() { shown = null; shownOwner = null; mutable.value = ReferralUiState.Loading }
}
