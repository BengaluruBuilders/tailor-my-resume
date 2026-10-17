package com.tailormyresume.app.auth

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.credits.GooglePromotionClient
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.account.LocalWorkCheckpoint
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GoogleAccountGateway
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.UpgradeCompleteRequest
import com.tailormyresume.core.network.dto.UpgradeIntentRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAccountCoordinator @Inject constructor(
    private val firebase: FirebaseSessionClient,
    private val credentials: GoogleCredentialSource,
    private val api: TailorMyResumeApi,
    private val config: TailorMyResumeApiConfig,
    private val sessions: SessionRepository,
    private val journals: PrivateUpgradeStore,
    private val checkpoints: LocalWorkCheckpoint,
    private val fence: LocalWorkFence,
    private val pendingWipe: PendingAccountWipe,
    private val cleaner: SignOutCleaner,
    private val promotions: GooglePromotionClient,
    private val wallet: WalletSource,
    private val uids: FirebaseUidProvider,
    private val clock: kotlin.time.Clock,
    @ApplicationScope private val scope: CoroutineScope,
    private val referrals: com.tailormyresume.app.referrals.PrivateReferralStore? = null,
) : GoogleAccountGateway, AppStartTask {
    private val mutable = MutableStateFlow<GoogleAccountState>(GoogleAccountState.Idle)
    override val state: StateFlow<GoogleAccountState> = mutable
    private val mutex = Mutex()

    override suspend fun signIn(): SignInResult = mutex.withLock {
        mutable.value = GoogleAccountState.Busy
        try {
            checkLive()
            if (firebase.hasAnonymousSession()) linkGuest() else directSignIn()
        } catch (cancelled: CancellationException) {
            if (!switched(ownJournal()) && pendingWipe.state() == PendingWipeState.NONE) fence.release()
            mutable.value = GoogleAccountState.Retry(canCancel = false)
            throw cancelled
        } catch (failure: Exception) {
            recoverable(failure)
            SignInResult.Failed(SignInFailureReason.ProviderUnavailable)
        }
    }

    override suspend fun open() = mutex.withLock { safely { reconcile() } }

    override suspend fun claim() = mutex.withLock {
        safely("CLAIM") {
            checkLive()
            if (firebase.hasAnonymousSession()) {
                linkGuest()
            } else {
                val journal = ownJournal() ?: newJournal(checkNotNull(uids.uid()), "DIRECT_GOOGLE").also { journals.write(it) }
                if (journal.recentAuthRecovery != null) {
                    mutable.value = GoogleAccountState.Retry(canCancel = true)
                } else {
                    mutable.value = promotions.claim(journal)
                }
            }
        }
    }

    private suspend fun linkGuest(): SignInResult {
        val user = checkNotNull(firebase.currentUser())
        check(sessions.lastAccountId()?.let { it == user.uid } != false)
        try {
            fence.freeze()
        } catch (unsaved: IllegalStateException) {
            mutable.value = GoogleAccountState.SaveEdits
            return SignInResult.AwaitingChoice
        }
        checkpoints.capture(user.uid, config.baseUrl)
        var journal = ownJournal()?.takeIf { it.source == user.uid && it.path == "GUEST_LINK" && it.proof != null }
            ?: newJournal(user.uid, "GUEST_LINK")
        journals.write(journal)
        val prepared = apiResult { api.prepareUpgrade(UpgradeIntentRequest(journal.prepareKey, checkNotNull(journal.proof))) }
        val failure = prepared.exceptionOrNull()
        if (failure != null && !preparationUnavailable(failure)) throw failure
        journal = journal.copy(intentId = prepared.getOrNull()?.intentId, phase = "LINKING")
        journals.write(journal)
        val token = credential() ?: return cancelBeforeSwitch()
        val outcome = firebase.linkGoogle(token)
        checkLive()
        if (outcome is AuthOutcome.Failure && outcome.failure == AuthFailure.CredentialConflict) {
            if (journal.intentId == null) {
                fence.release()
                mutable.value = GoogleAccountState.Retry(canCancel = true)
                return SignInResult.AwaitingChoice
            }
            val snapshot = checkNotNull(checkpoints.read(user.uid, config.baseUrl))
            journals.write(journal.copy(phase = "CONFLICT"))
            mutable.value = GoogleAccountState.Conflict(snapshot.profile != null, snapshot.applications.map { it.id to "${it.jobTitle} · ${it.company}" })
            fence.release()
            return SignInResult.AwaitingChoice
        }
        val linked = (outcome as? AuthOutcome.Success)?.value ?: firebase.currentUser()?.takeIf { it.uid == user.uid && it.mode == AccountMode.GOOGLE }
        if (linked == null) {
            fence.release()
            throw IllegalStateException("Google linking is unavailable")
        }
        check(linked.uid == user.uid)
        journal = journal.copy(phase = "LINKED")
        journals.write(journal)
        publish(linked)
        fence.release()
        mutable.value = promotions.prepare(journal)
        return SignInResult.SignedIn(linked.account())
    }

    private suspend fun directSignIn(): SignInResult {
        val previous = sessions.observeAccount().first()
        try {
            fence.freeze()
        } catch (unsaved: IllegalStateException) {
            mutable.value = GoogleAccountState.SaveEdits
            return SignInResult.AwaitingChoice
        }
        val previousId = previous?.id ?: sessions.lastAccountId()
        previousId?.let { checkpoints.capture(it, config.baseUrl) }
        var preparing = newJournal(previousId.orEmpty(), "DIRECT_GOOGLE").copy(phase = "DIRECT_SWITCHING")
        journals.write(preparing)
        val token = credential() ?: return cancelBeforeSwitch()
        preparing = preparing.copy(destinationGoogleDigest = GoogleCredentialIdentity.digest(token))
        journals.write(preparing)
        fence.invalidate()
        val outcome = firebase.signIn(token)
        val user = (outcome as? AuthOutcome.Success)?.value ?: run {
            fence.release()
            return (outcome as AuthOutcome.Failure).failure.toSignInResult()
        }
        checkLive()
        val journal = preparing.copy(source = user.uid, phase = "DIRECT_LINKED", destination = user.uid)
        journals.write(journal)
        apiResult { api.me() }.getOrThrow()
        if (previousId != user.uid) {
            referrals?.clearAll()
            cleaner.clear()
            checkpoints.restore(user.uid, config.baseUrl)
        }
        journals.write(journal.copy(phase = "LINKED"))
        publish(user)
        fence.release()
        mutable.value = promotions.prepare(journal)
        return SignInResult.SignedIn(user.account())
    }

    override suspend fun continueConflict(useProfile: Boolean, applications: Set<String>) = mutex.withLock {
        safely {
            checkLive()
            var journal = checkNotNull(ownJournal())
            check(journal.phase == "CONFLICT" && firebase.hasAnonymousSession() && uids.uid() == journal.source)
            fence.freeze()
            val snapshot = checkpoints.capture(journal.source, config.baseUrl)
            check(applications.all { id -> snapshot.applications.any { it.id == id } })
            journal = journal.copy(useProfile = useProfile, applications = applications, phase = "SWITCHING")
            journals.write(journal)
            val token = credential()
            if (token == null) {
                journals.write(journal.copy(phase = "CONFLICT"))
                fence.release()
                mutable.value = GoogleAccountState.Conflict(snapshot.profile != null, snapshot.applications.map { it.id to "${it.jobTitle} · ${it.company}" })
                return@safely
            }
            journal = journal.copy(destinationGoogleDigest = GoogleCredentialIdentity.digest(token))
            journals.write(journal)
            fence.invalidate()
            val outcome = firebase.signIn(token)
            val user = (outcome as? AuthOutcome.Success)?.value ?: throw IllegalStateException("Google sign-in is unavailable")
            check(user.uid != journal.source)
            journal = journal.copy(destination = user.uid, phase = "SWITCHED")
            journals.write(journal)
            completeConflict(journal, user)
        }
    }

    private suspend fun completeConflict(original: UpgradeJournal, user: FirebaseUser) {
        var journal = original
        check(journal.destination == user.uid)
        check(journal.destinationGoogleDigest == null || journal.destinationGoogleDigest == user.googleSubject?.let(GoogleCredentialIdentity::digestSubject))
        checkLive()
        val result = apiResult {
            api.completeUpgrade(UpgradeCompleteRequest(journal.completionKey, checkNotNull(journal.intentId), checkNotNull(journal.proof), "PRESERVE_LOCAL_WORK"))
        }.getOrThrow()
        checkLive()
        check(uids.uid() == user.uid)
        check(
            result.completion.kind == "EXISTING_ACCOUNT" && result.completion.creditsTransferred == 0 &&
                !result.completion.purchasesTransferred && !result.completion.runningJobsTransferred,
        )
        journal = journal.copy(phase = "AUTHORIZED")
        journals.write(journal)
        val destinationProfile = checkpoints.read(user.uid, config.baseUrl)?.profile
        if (journal.useProfile && destinationProfile != null && !journal.profileChoiceConfirmed) {
            val guestProfile = checkpoints.read(journal.source, config.baseUrl)?.profile
            mutable.value = GoogleAccountState.ProfileChoice(guestProfile?.fullName.orEmpty(), destinationProfile.fullName)
            return
        }
        withContext(NonCancellable) {
            val candidate = referrals?.read(journal.source, config.baseUrl)
            referrals?.clearAll()
            if (candidate?.code != null) {
                referrals?.write(candidate.copy(owner = user.uid, provider = user.mode.name, generation = fence.ticket(), requestId = null,
                    revision = null, payloadDigest = null, challengeId = null, phase = "CANDIDATE", retryAt = null, reviewFrom = journal.source), checkNotNull(referrals).epoch()) {
                    check(uids.uid() == user.uid)
                }
            }
            cleaner.clear()
            checkpoints.restore(user.uid, config.baseUrl)
            checkLive()
            check(uids.uid() == user.uid)
            checkpoints.importSelected(journal.completionKey, journal.source, user.uid, config.baseUrl, journal.useProfile, journal.applications)
            journals.write(journal.copy(phase = "IMPORTED"))
            publish(user)
            wallet.refreshOrCached()
            journals.clear(journal)
        }
        fence.release()
        mutable.value = GoogleAccountState.Complete(null, null, preservedWork = true)
    }

    override suspend fun chooseProfile(useGuest: Boolean) = mutex.withLock {
        safely {
            val journal = checkNotNull(ownJournal())
            check(journal.phase == "AUTHORIZED")
            val user = checkNotNull(firebase.currentUser())
            check(user.uid == journal.destination)
            val confirmed = journal.copy(useProfile = useGuest, profileChoiceConfirmed = true)
            journals.write(confirmed)
            completeConflict(confirmed, user)
        }
    }

    private suspend fun reconcile(recoveringAuth: Boolean = false) {
        checkLive()
        val user = firebase.currentUser() ?: run {
            mutable.value = GoogleAccountState.Idle
            return
        }
        val journal = ownJournal()
        if (journal?.recentAuthRecovery != null && !recoveringAuth) {
            mutable.value = GoogleAccountState.Retry(canCancel = !switched(journal))
            return
        }
        if (journal != null && journal.phase in setOf("DIRECT_SWITCHING", "DIRECT_LINKED") && user.mode == AccountMode.GOOGLE) {
            fence.freeze()
            apiResult { api.me() }.getOrThrow()
            referrals?.clearAll()
            cleaner.clear()
            checkpoints.restore(user.uid, config.baseUrl)
            val linked = journal.copy(source = user.uid, destination = user.uid, phase = "LINKED")
            journals.write(linked)
            publish(user)
            fence.release()
            mutable.value = promotions.prepare(linked)
            return
        }
        if (journal != null && journal.phase in setOf("SWITCHING", "SWITCHED", "AUTHORIZED", "IMPORTED", "DIRECT_SWITCHING", "DIRECT_LINKED") && user.uid != journal.source) {
            check(journal.destination == null || journal.destination == user.uid)
            fence.freeze()
            val bound = journal.copy(destination = user.uid, phase = if (journal.phase == "SWITCHING") "SWITCHED" else journal.phase)
            journals.write(bound)
            completeConflict(bound, user)
            return
        }
        if (user.mode == AccountMode.ANONYMOUS) {
            if (journal?.phase == "CONFLICT") {
                val snapshot = checkNotNull(checkpoints.read(user.uid, config.baseUrl))
                mutable.value = GoogleAccountState.Conflict(snapshot.profile != null, snapshot.applications.map { it.id to "${it.jobTitle} · ${it.company}" })
                return
            }
            val policy = apiResult { api.creditPolicy().policy }.getOrNull()
            val signup = policy?.signupBonus
            mutable.value = GoogleAccountState.Offer(signup?.amount?.takeIf { signup.status == "AVAILABLE" && it > 0 }, null, false, google = false)
        } else {
            if (sessions.observeAccount().first()?.id != user.uid) {
                fence.freeze()
                sessions.lastAccountId()?.let { checkpoints.capture(it, config.baseUrl) }
                cleaner.clear()
                checkpoints.restore(user.uid, config.baseUrl)
            }
            if (journal?.phase == "LINKING") firebase.refreshGoogle()
            val pending = journal ?: newJournal(user.uid, "DIRECT_GOOGLE").copy(phase = "LINKED").also { journals.write(it) }
            publish(user)
            fence.release()
            mutable.value = promotions.prepare(pending)
        }
    }

    override suspend fun retry() = mutex.withLock {
        val journal = ownJournal()
        safely(protectedJournal = journal) {
            if (journal?.recentAuthRecovery == null) reconcile() else recoverRecentAuth(journal)
        }
    }

    private suspend fun recoverRecentAuth(journal: UpgradeJournal) {
        val recovery = checkNotNull(journal.recentAuthRecovery)
        val generation = fence.ticket()
        checkRecoveryOwner(recovery, generation)
        val token = credential() ?: run {
            mutable.value = GoogleAccountState.Retry(canCancel = !switched(journal))
            return
        }
        checkRecoveryOwner(recovery, generation)
        check(GoogleCredentialIdentity.digest(token) == recovery.googleDigest)
        val result = firebase.reauthenticateGoogle(token, recovery.uid, recovery.googleDigest)
        check(result is AuthOutcome.Success)
        checkRecoveryOwner(recovery, generation)
        check(result.value.uid == recovery.uid && result.value.googleSubject?.let(GoogleCredentialIdentity::digestSubject) == recovery.googleDigest)
        if (recovery.operation == "CLAIM") {
            mutable.value = promotions.resumeClaim(journal)
        } else {
            reconcile(recoveringAuth = true)
        }
        checkRecoveryOwner(recovery, generation)
        ownJournal()?.takeIf { it.recentAuthRecovery == recovery }?.let { journals.write(it.copy(recentAuthRecovery = null)) }
    }

    private suspend fun checkRecoveryOwner(recovery: RecentGoogleAuthRecovery, generation: Long) {
        checkLive()
        check(fence.ticket() == generation)
        val user = checkNotNull(firebase.currentUser())
        check(uids.uid() == recovery.uid && user.uid == recovery.uid && user.mode == AccountMode.GOOGLE)
        check(user.googleSubject?.let(GoogleCredentialIdentity::digestSubject) == recovery.googleDigest)
    }

    private fun switched(journal: UpgradeJournal?): Boolean = journal?.phase in setOf("SWITCHING", "SWITCHED", "AUTHORIZED", "IMPORTED", "DIRECT_SWITCHING", "DIRECT_LINKED")

    override suspend fun cancel() = mutex.withLock {
        val journal = ownJournal()
        check(journal == null || journal.phase !in setOf("SWITCHING", "SWITCHED", "AUTHORIZED", "IMPORTED", "DIRECT_SWITCHING", "DIRECT_LINKED"))
        if (firebase.hasAnonymousSession() && journal != null) journals.clear(journal)
        fence.release()
        mutable.value = GoogleAccountState.Idle
    }

    override suspend fun dismiss() = mutex.withLock {
        val journal = ownJournal()
        if (journal?.phase in setOf("SWITCHING", "SWITCHED", "AUTHORIZED", "IMPORTED", "DIRECT_SWITCHING", "DIRECT_LINKED")) return@withLock
        fence.release()
        mutable.value = GoogleAccountState.Idle
    }

    override fun start() {
        scope.launch {
            if (journals.read() != null) open()
        }
    }

    private suspend fun credential(): String? = when (val result = credentials.idToken()) {
        is AuthOutcome.Success -> result.value
        is AuthOutcome.Failure -> if (result.failure == AuthFailure.Cancelled) null else error("Google sign-in is unavailable")
    }

    private suspend fun cancelBeforeSwitch(): SignInResult {
        ownJournal()?.let { journals.clear(it) }
        fence.release()
        mutable.value = GoogleAccountState.Idle
        return SignInResult.Cancelled
    }

    private suspend fun ownJournal(): UpgradeJournal? {
        val uid = uids.uid() ?: return null
        val journal = journals.readFor(uid, config.baseUrl, firebase.currentUser()?.googleSubject?.let(GoogleCredentialIdentity::digestSubject)) ?: return null
        return journal.takeIf { it.source == uid || it.destination == uid || (it.destination == null && it.phase in setOf("SWITCHING", "DIRECT_SWITCHING")) }
    }

    private suspend fun publish(user: FirebaseUser) {
        checkLive()
        check(uids.uid() == user.uid)
        sessions.saveLastAccountId(user.uid)
        sessions.saveAccount(user.account())
    }

    private suspend fun checkLive() {
        check(pendingWipe.state() == PendingWipeState.NONE) { "Account deletion must finish first" }
    }

    private suspend fun safely(operation: String = "RECONCILE", protectedJournal: UpgradeJournal? = null, action: suspend () -> Unit) {
        mutable.value = GoogleAccountState.Busy
        try {
            action()
        } catch (cancelled: CancellationException) {
            if (!switched(ownJournal() ?: protectedJournal) && pendingWipe.state() == PendingWipeState.NONE) fence.release()
            mutable.value = GoogleAccountState.Retry(canCancel = false)
            throw cancelled
        } catch (failure: Exception) {
            recoverable(failure, operation, protectedJournal)
        }
    }

    private fun preparationUnavailable(failure: Throwable): Boolean = when ((failure as? ApiException)?.error) {
        is ApiError.IdentityUnavailable, is ApiError.PromotionUnavailable, is ApiError.RateLimited,
        ApiError.Offline, ApiError.Timeout, ApiError.InternalError,
        -> true
        else -> false
    }

    private suspend fun recoverable(failure: Exception, operation: String = "RECONCILE", protectedJournal: UpgradeJournal? = null) {
        val journal = ownJournal() ?: protectedJournal
        val switched = switched(journal)
        if ((failure as? ApiException)?.error == ApiError.RecentGoogleAuthRequired && journal != null) {
            val user = checkNotNull(firebase.currentUser())
            check(user.mode == AccountMode.GOOGLE && user.uid == uids.uid())
            val recovery = journal.recentAuthRecovery ?: RecentGoogleAuthRecovery(user.uid, GoogleCredentialIdentity.digestSubject(checkNotNull(user.googleSubject)), operation)
            journals.write(journal.copy(recentAuthRecovery = recovery))
        }
        if (!switched && pendingWipe.state() == PendingWipeState.NONE) fence.release()
        val after = when (val error = (failure as? ApiException)?.error) {
            is ApiError.IdentityUnavailable -> error.retryAfterSeconds
            is ApiError.IntegrityUnavailable -> error.retryAfterSeconds
            is ApiError.PromotionUnavailable -> error.retryAfterSeconds
            is ApiError.RateLimited -> error.retryAfterSeconds
            else -> null
        }
        if (after != null && journal != null) journals.write(journal.copy(retryAtMillis = clock.now().toEpochMilliseconds() + after.coerceAtLeast(1).toLong() * 1000))
        if ((failure as? ApiException)?.error == ApiError.PolicyChanged && journal != null && !switched) {
            mutable.value = promotions.prepare(journal, changed = true)
        } else {
            mutable.value = GoogleAccountState.Retry(canCancel = !switched, retryAfterSeconds = after)
        }
    }

    private fun newJournal(uid: String, path: String): UpgradeJournal = UpgradeJournal(
        environment = config.baseUrl,
        source = uid,
        prepareKey = newId(),
        proof = if (path == "GUEST_LINK") Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }) else null,
        completionKey = newId(),
        observationKey = newId(),
        path = path,
    )

    private fun newId() = UUID.randomUUID().toString()
    private fun FirebaseUser.account() = SignInAccount(uid, displayName.ifBlank { email }, email, AccountMode.GOOGLE)
}
