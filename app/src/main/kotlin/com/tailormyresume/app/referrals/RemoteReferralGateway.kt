package com.tailormyresume.app.referrals

import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.credits.GuestEvidence
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.ReferralEntryState
import com.tailormyresume.core.domain.ReferralGateway
import com.tailormyresume.core.domain.ReferralMessage
import com.tailormyresume.core.domain.ReferralShare
import com.tailormyresume.core.domain.ReferralUiState
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    @com.tailormyresume.core.common.network.di.ApplicationScope private val scope: kotlinx.coroutines.CoroutineScope,
    private val links: ReferralLinks = ReferralLinks(),
    private val installReferrer: InstallReferrerSource = InstallReferrerSource { InstallReferrerResult.Terminal(null) },
    private val foreground: com.tailormyresume.app.auth.ForegroundActivity = com.tailormyresume.app.auth.ForegroundActivity { null },
    private val googleAccounts: dagger.Lazy<com.tailormyresume.app.auth.GoogleAccountCoordinator>? = null,
) : ReferralGateway {
    private val mutable = MutableStateFlow<ReferralUiState>(ReferralUiState.Loading)
    private val mutex = Mutex()
    private val entryMutable = MutableStateFlow(ReferralEntryState())
    override val entry: StateFlow<ReferralEntryState> = entryMutable
    private val routes = MutableStateFlow(0L)
    override val routeRequests: StateFlow<Long> = routes
    private var disclosedCode: String? = null
    private var automatic: Automatic? = null
    private var collecting = false
    private var foregroundEpoch = 0L
    private data class Automatic(val owner: Owner, val code: String)
    internal data class GoogleContinuation(val code: String, val owner: String?, val epoch: Long)

    private var shown: ReferralOfferDto? = null
    private var shownOwner: String? = null
    private var shownGeneration = -1L
    override val state: StateFlow<ReferralUiState> = combine(mutable, guard.generations) { value, generation ->
        if (shownOwner == uids.uid() && generation == shownGeneration) value else ReferralUiState.Loading
    }.stateIn(scope, SharingStarted.Eagerly, ReferralUiState.Loading)
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
        if (stored != null && stored.provider != owner.provider && stored.requestId != null) {
            val marker = ReferralJournal(uid, config.baseUrl, owner.provider, owner.ticket.generation, phase = "READ_ONLY")
            write(owner, marker)
            return marker
        }
        return stored ?: ReferralJournal(uid, config.baseUrl, owner.provider, owner.ticket.generation)
    }
    private suspend fun write(owner: Owner, value: ReferralJournal) {
        check(owner)
        store.write(value.copy(provider = owner.provider, generation = owner.ticket.generation), owner.epoch) { guard.check(owner.ticket) }
        check(owner)
    }
    private fun content() = mutable.value as? ReferralUiState.Content ?: ReferralUiState.Content()
    private suspend fun run(action: suspend (Owner) -> Unit) = mutex.withLock { execute(action) }

    private suspend fun execute(action: suspend (Owner) -> Unit) {
        val owner = owner()
        if (shownOwner != owner.ticket.owner) {
            shown = null
            mutable.value = ReferralUiState.Loading
        }
        shownOwner = owner.ticket.owner
        shownGeneration = owner.ticket.generation
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

    override suspend fun refresh() {
        if (firebase.currentUser() == null) {
            entryMutable.value = ReferralEntryState(store.inbox(config.baseUrl))
            return
        }
        run { refresh(it) }
    }

    fun receiveView(action: String?, raw: String?) {
        scope.launch {
            try {
                captureView(action, raw)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                automatic = null
            }
        }
    }

    suspend fun captureView(action: String?, raw: String?) {
        if (action != android.content.Intent.ACTION_VIEW) return
        val code = links.parseUrl(raw) ?: return
        val actionEpoch = foregroundEpoch
        val epoch = store.epoch()
        mutex.withLock {
            if (epoch != store.epoch()) return
            store.capture(code, config.baseUrl, epoch)
        }
        val user = firebase.initializedUser()
        val accountReady = user == null || uids.isAnonymous() || googleAccounts?.get()?.prepareReferralEntry() != false
        mutex.withLock {
            if (epoch != store.epoch()) return@withLock
            if (user == null) {
                entryMutable.value = ReferralEntryState(store.inbox(config.baseUrl))
            } else {
                execute { owner ->
                    var candidate = bind(owner)
                    refresh(owner)
                    if (accountReady && !uids.isAnonymous() && candidate.source == "LINK" && shown?.codeIssuance == "ACTIVATION_REQUIRED") {
                        observe(owner, candidate)
                        refresh(owner)
                        candidate = read(owner)
                    }
                    if (accountReady && actionEpoch == foregroundEpoch && !uids.isAnonymous() && candidate.code == code && candidate.source == "LINK" && candidate.requestId == null && candidate.reviewFrom == null && candidate.phase == "CANDIDATE" && !candidate.requiresConfirmation) {
                        automatic = Automatic(owner, code)
                        routes.value++
                    }
                }
            }
        }
    }

    suspend fun foregroundEntry() {
        if (!links.enabled || collecting) return
        collecting = true
        val epoch = store.epoch()
        try {
            if (!store.beginCollection(epoch)) return
            val result = installReferrer.read()
            if (result !is InstallReferrerResult.Terminal) return
            mutex.withLock {
                if (epoch != store.epoch()) return@withLock
                store.capture(result.value?.let(links::parseReferrer), config.baseUrl, epoch, terminalReferrer = true)
                val user = firebase.initializedUser()
                if (epoch != store.epoch()) return@withLock
                if (user == null) {
                    entryMutable.value = ReferralEntryState(store.inbox(config.baseUrl))
                } else {
                    execute { owner ->
                        bind(owner)
                        refresh(owner)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (epoch == store.epoch()) entryMutable.value = entryMutable.value.copy(reward = null, version = null)
        } finally {
            collecting = false
        }
    }

    fun leaveForeground() {
        foregroundEpoch++
        automatic = null
    }

    internal suspend fun entered() = run { owner ->
        bind(owner)
        refresh(owner)
    }

    private suspend fun bind(owner: Owner): ReferralJournal {
        val current = read(owner)
        val bound = store.bindInbox(current, owner.epoch) { guard.check(owner.ticket) }
        check(owner)
        return bound
    }

    internal suspend fun googleStarting(): GoogleContinuation? = mutex.withLock {
        val uid = firebase.currentUser()?.uid
        val current = if (uid == null) null else store.read(uid, config.baseUrl)
        val code = current?.takeIf { it.source == "LINK" && it.reviewFrom == null && it.requestId == null && it.phase == "CANDIDATE" }?.code
            ?: if (uid == null) store.inbox(config.baseUrl) else null
        code?.takeIf { it == disclosedCode }?.let { GoogleContinuation(it, uid, store.epoch()) }
    }

    internal suspend fun googleContinued(continuation: GoogleContinuation?) {
        if (continuation == null) return
        mutex.withLock {
            val user = firebase.currentUser() ?: return@withLock
            if (uids.isAnonymous() || (continuation.owner != null && (continuation.owner != user.uid || continuation.epoch != store.epoch()))) return@withLock
            execute { owner ->
                val current = bind(owner)
                refresh(owner)
                if (current.code == continuation.code && current.source == "LINK" && current.requestId == null && current.reviewFrom == null && current.phase == "CANDIDATE" && !current.requiresConfirmation) {
                    automatic = Automatic(owner, continuation.code)
                    routes.value++
                }
            }
        }
    }

    override suspend fun displayed(code: String, version: String?) {
        disclosedCode = code
        val permission = automatic ?: return
        if (version == null || permission.code != code || foreground.current() == null) return
        automatic = null
        run { owner ->
            check(permission.owner)
            if (permission.owner != owner || shown?.version != version || content().candidate != code) return@run
            apply(owner, automaticAttempt = true)
        }
    }

    override suspend fun clearEntry() {
        automatic = null
        disclosedCode = null
        if (firebase.currentUser() == null) {
            mutex.withLock {
                store.dismissInbox()
                entryMutable.value = ReferralEntryState()
            }
        } else {
            clear()
        }
    }

    override suspend fun share(): ReferralShare? {
        var result: ReferralShare? = null
        run { owner ->
            val previous = content()
            refresh(owner)
            val value = if (readSucceeded) content() else previous.copy(reward = null, offerVersion = null)
            val code = value.code ?: return@run
            result = ReferralShare(code, value.shareUrl, value.reward?.takeIf { readSucceeded }, value.offerVersion?.takeIf { readSucceeded })
        }
        return result
    }

    private suspend fun refresh(owner: Owner, message: ReferralMessage = ReferralMessage.None) {
        val journal = read(owner)
        readSucceeded = false
        val response = apiResult { api.referrals() }
        check(owner)
        if (response.isFailure) {
            entryMutable.value = ReferralEntryState(journal.code?.takeIf { journal.source == "LINK" && journal.reviewFrom == null })
            mutable.value = ReferralUiState.Content(
                candidateSource = journal.source,
                candidate = journal.code,
                guest = uids.isAnonymous(),
                reviewRequired = journal.reviewFrom != null,
                attached = journal.phase == "ATTACHED",
                message = if (journal.phase == "ATTACHED") {
                    ReferralMessage.Applied
                } else if (journal.phase == "READ_ONLY") {
                    ReferralMessage.ReadOnly
                } else {
                    ReferralMessage.Unavailable
                },
            )
            return
        }
        readSucceeded = true
        val result = response.getOrThrow()
        if (result.attachment?.status == "ATTACHED") {
            automatic = null
            store.consumeInstallation(owner.epoch)
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
        entryMutable.value = ReferralEntryState(journal.code?.takeIf { journal.source == "LINK" && journal.reviewFrom == null }, result.offer.rewardAmount?.takeIf { usable(result.offer) }, result.offer.version?.takeIf { usable(result.offer) }, result.offer.deadline)
        val available = usable(result.offer)
        mutable.value = ReferralUiState.Content(
            code = result.code?.value, shareUrl = result.code?.value?.let { links.shareUrl(result.shareUrl, it) }, offerVersion = result.offer.version,
            candidateSource = journal.source, candidate = journal.code, reward = result.offer.rewardAmount?.takeIf { available },
            creditedCount = result.stats.creditedCount, earnedCredits = result.stats.earnedCredits,
            canIssue = result.offer.codeIssuance in setOf("AVAILABLE", "ACTIVATION_REQUIRED"), guest = uids.isAnonymous(), attached = result.attachment?.status == "ATTACHED",
            canAttach = available && result.offer.attachmentEligibility == "ELIGIBLE" && journal.phase !in setOf("READ_ONLY", "REQUIRES_NEW") && journal.reviewFrom == null,
            deadline = result.offer.deadline, reviewRequired = journal.reviewFrom != null,
            message = if (journal.requiresConfirmation) {
                ReferralMessage.PolicyChanged
            } else if (message != ReferralMessage.None) {
                message
            } else if (!available) {
                ReferralMessage.Unavailable
            } else if (result.offer.attachmentEligibility == "NOT_ELIGIBLE") {
                ReferralMessage.Ineligible
            } else if (result.offer.attachmentEligibility == "EXPIRED") {
                ReferralMessage.Expired
            } else {
                ReferralMessage.None
            },
            retryAfterSeconds = remaining(journal),
        )
    }
    private fun usable(offer: ReferralOfferDto): Boolean = offer.status == "AVAILABLE" && offer.version != null &&
        offer.rewardAmount?.let { it in 1..1_000_000 } == true && offer.effectiveAt?.let { Instant.parse(it) <= clock.now() } == true &&
        offer.expiresAt?.let { Instant.parse(it) > clock.now() } == true

    override suspend fun save(code: String, replace: Boolean) = run { owner ->
        val normalized = canonicalReferralCode(code)
        automatic = null
        val current = bind(owner)
        when {
            current.phase in setOf("READ_ONLY", "ATTACHED", "SENT") -> refresh(owner)
            normalized == null -> mutable.value = content().copy(busy = false, message = ReferralMessage.Invalid)
            current.code != null && current.code != normalized && !replace -> mutable.value = content().copy(busy = false, message = ReferralMessage.ReplaceRequired)
            current.code == normalized -> refresh(owner, ReferralMessage.Saved)
            current.requestId != null && !replace -> mutable.value = content().copy(busy = false, message = ReferralMessage.Conflict)
            else -> {
                write(owner, current.copy(code = normalized, source = "MANUAL", requiresConfirmation = false, requestId = null, revision = null, payloadDigest = null, challengeId = null, phase = "CANDIDATE", retryAt = null))
                refresh(owner, ReferralMessage.Saved)
            }
        }
    }
    override suspend fun clear() = run { owner ->
        automatic = null
        disclosedCode = null
        store.consumeInstallation(owner.epoch)
        var current = read(owner)
        if (current.phase == "READ_ONLY") {
            refresh(owner)
            return@run
        }
        if (current.phase == "SENT") {
            refresh(owner)
            current = read(owner)
            if (!readSucceeded || current.phase == "ATTACHED") return@run
        }
        write(owner, current.copy(code = null, requiresConfirmation = false, requestId = null, revision = null, payloadDigest = null, challengeId = null, retryAt = null, reviewFrom = null, phase = "CANDIDATE"))
        refresh(owner)
    }
    override suspend fun issue() = run { owner ->
        var current = read(owner)
        if (uids.isAnonymous()) {
            refresh(owner)
            return@run
        }
        if (shown?.codeIssuance == "ACTIVATION_REQUIRED") {
            current = observe(owner, current)
            refresh(owner)
        }
        if (shown?.codeIssuance != "AVAILABLE") {
            refresh(owner)
            return@run
        }
        current = current.copy(issueRequestId = current.issueRequestId ?: UUID.randomUUID().toString())
        write(owner, current)
        val result = apiResult { api.referralCode(ReferralCodeRequest(checkNotNull(current.issueRequestId))) }
        check(owner)
        if (result.isFailure) {
            fail(owner, current, result.exceptionOrNull())
            return@run
        }
        refresh(owner)
    }
    private suspend fun observe(owner: Owner, current: ReferralJournal): ReferralJournal {
        val journal = current.copy(observationId = current.observationId ?: UUID.randomUUID().toString())
        write(owner, journal)
        apiResult { api.observeActivation(ActivationObservationRequest(checkNotNull(journal.observationId))) }.getOrThrow()
        check(owner)
        return journal
    }

    override suspend fun apply() = run { owner -> apply(owner) }

    private suspend fun apply(owner: Owner, automaticAttempt: Boolean = false) {
        automatic = null
        var current = read(owner)
        if (uids.isAnonymous() || current.reviewFrom != null || (automaticAttempt && current.requiresConfirmation)) {
            refresh(owner)
            return
        }
        val before = shown
        refresh(owner)
        current = read(owner)
        if (!readSucceeded || current.code == null || current.phase in setOf("ATTACHED", "READ_ONLY", "REQUIRES_NEW") || remaining(current) != null) return
        if (shown?.codeIssuance == "ACTIVATION_REQUIRED") {
            current = observe(owner, current)
            refresh(owner)
        }
        val offer = shown ?: return
        if (!content().canAttach) return
        if (before?.version != offer.version) {
            write(owner, current.copy(requiresConfirmation = true))
            mutable.value = content().copy(message = ReferralMessage.PolicyChanged)
            return
        }
        if (current.phase == "SENT" && current.challengeId != null && current.requestId != null && current.revision != null) {
            val recovered = send(owner, current, "committed-recovery")
            if (recovered || lastError !in setOf(ApiError.IntegrityChallengeInvalid, ApiError.IntegrityRejected)) return
            current = read(owner)
            if (current.phase == "READ_ONLY" || content().message == ReferralMessage.PolicyChanged || remaining(current) != null) return
        }
        val revision = checkNotNull(offer.version)
        if (current.revision != null && current.revision != revision && !current.requiresConfirmation) {
            write(owner, current.copy(requiresConfirmation = true, phase = "PREPARED", challengeId = null))
            mutable.value = content().copy(message = ReferralMessage.PolicyChanged)
            return
        }
        current = current.copy(
            requestId = current.requestId ?: UUID.randomUUID().toString(),
            requiresConfirmation = false,
            revision = revision,
            payloadDigest = referralDigest(checkNotNull(current.code), current.source),
            phase = "PREPARED",
            challengeId = null,
        )
        write(owner, current)
        if (current.source == "LINK" && foreground.current() == null) {
            mutable.value = content().copy(busy = false, message = ReferralMessage.Retry)
            return
        }
        val challenge = apiResult { api.referralChallenge(GrantChallengeRequest("REFERRAL_ATTACH", checkNotNull(current.requestId), revision, checkNotNull(current.payloadDigest))) }
        check(owner)
        if (challenge.isFailure) {
            fail(owner, current, challenge.exceptionOrNull())
            return
        }
        val proof = challenge.getOrThrow()
        check(proof.bindingVersion == 1 && Instant.parse(proof.expiresAt) > clock.now())
        current = current.copy(challengeId = proof.challengeId)
        write(owner, current)
        val token = if (current.source == "LINK") {
            evidence.foregroundToken(proof.requestHash) { foreground.current() != null }
        } else {
            evidence.token(proof.requestHash)
        }
        check(owner)
        if (current.source == "LINK" && foreground.current() == null) throw CancellationException("Referral left foreground")
        current = current.copy(phase = "SENT")
        write(owner, current)
        send(owner, current, token)
    }
    private suspend fun send(owner: Owner, journal: ReferralJournal, token: String): Boolean {
        val response = apiResult {
            val result = api.attachReferral(ReferralAttachRequest(checkNotNull(journal.requestId), checkNotNull(journal.code), journal.source, checkNotNull(journal.revision), null, checkNotNull(journal.challengeId), token))
            if (!result.isSuccessful) throw retrofit2.HttpException(result)
            checkNotNull(result.body())
        }
        check(owner)
        if (response.isSuccess) {
            store.consumeInstallation(owner.epoch)
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
                write(owner, ReferralJournal(journal.owner, journal.environment, owner.provider, owner.ticket.generation, phase = "READ_ONLY"))
                refresh(owner)
                return
            }
            ApiError.PolicyChanged -> {
                write(owner, journal.copy(requiresConfirmation = true))
                refresh(owner)
                ReferralMessage.PolicyChanged
            }
            ApiError.ReferralCodeInvalid, ApiError.InvalidInput -> ReferralMessage.Invalid
            ApiError.ReferralSelf -> ReferralMessage.Self
            ApiError.ReferralIneligible -> ReferralMessage.Ineligible
            ApiError.ReferralWindowExpired -> ReferralMessage.Expired
            ApiError.IdempotencyConflict -> ReferralMessage.Conflict
            ApiError.AccountDeleted -> {
                store.eraseFor(journal.owner, journal.environment)
                mutable.value = ReferralUiState.Loading
                return
            }
            is ApiError.RateLimited, is ApiError.IntegrityUnavailable, is ApiError.PromotionUnavailable, is ApiError.IdentityUnavailable -> {
                val advertised = when (error) {
                    is ApiError.RateLimited -> error.retryAfterSeconds
                    is ApiError.IntegrityUnavailable -> error.retryAfterSeconds
                    is ApiError.PromotionUnavailable -> error.retryAfterSeconds
                    is ApiError.IdentityUnavailable -> error.retryAfterSeconds
                    else -> null
                }
                val seconds = advertised?.takeIf { it > 0 } ?: 60
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
    fun invalidate() {
        automatic = null
        disclosedCode = null
        entryMutable.value = ReferralEntryState()
        shown = null
        shownOwner = null
        mutable.value = ReferralUiState.Loading
    }
}
