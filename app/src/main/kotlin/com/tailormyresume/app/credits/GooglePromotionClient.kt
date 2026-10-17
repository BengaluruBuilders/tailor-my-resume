package com.tailormyresume.app.credits

import com.tailormyresume.app.auth.PrivateUpgradeStore
import com.tailormyresume.app.auth.UpgradeJournal
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.GoogleAccountState
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.ActivationChallengeRequest
import com.tailormyresume.core.network.dto.ActivationObservationRequest
import com.tailormyresume.core.network.dto.CreditPolicyDto
import com.tailormyresume.core.network.dto.GoogleActivationRequest
import com.tailormyresume.core.network.dto.GoogleActivationResponse
import com.tailormyresume.core.network.dto.TrialContinuityDto
import com.tailormyresume.core.network.dto.UpgradeProofDto
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class GooglePromotionClient @Inject constructor(
    private val api: TailorMyResumeApi,
    private val evidence: GuestEvidence,
    private val journals: PrivateUpgradeStore,
    private val wallet: WalletSource,
    private val credits: CreditsRepository,
    private val uids: FirebaseUidProvider,
    private val clock: Clock,
) {
    private var displayed: CreditPolicyDto? = null

    suspend fun prepare(journal: UpgradeJournal, changed: Boolean = false): GoogleAccountState {
        val uid = uids.uid()
        val generation = wallet.generation()
        if (journal.requestId != null && journal.challengeId != null && !journal.attemptCommitted) {
            val replay = apiResult { api.activateGoogle(request(journal, signal(journal), "committed-recovery")) }
            ensureOwner(uid, generation)
            if (replay.isSuccess) return finish(journal, replay.getOrThrow(), generation)
            val error = (replay.exceptionOrNull() as? ApiException)?.error
            if (error !in setOf(ApiError.IntegrityChallengeInvalid, ApiError.IntegrityRejected, ApiError.PolicyChanged)) {
                throw checkNotNull(replay.exceptionOrNull())
            }
        }
        val observed = apiResult { api.observeActivation(ActivationObservationRequest(journal.observationKey)) }.getOrThrow()
        ensureOwner(uid, generation)
        check(wallet.update(observed.wallet, generation))
        val policy = apiResult { api.creditPolicy().policy }.getOrThrow()
        ensureOwner(uid, generation)
        displayed = policy
        val reconciled = if (observed.activation?.source == "GUEST_LINK" && journal.path != "GUEST_LINK") {
            journal.copy(path = "GUEST_LINK", proof = null, intentId = null, requestId = null, challengeId = null, requestedBase = false, baseVersion = null)
                .also { journals.write(it) }
        } else journal
        return offer(policy, reconciled, changed)
    }

    suspend fun claim(journal: UpgradeJournal): GoogleAccountState {
        val policy = displayed ?: return prepare(journal, changed = true)
        val signup = policy.signupBonus
        val base = signup.baseTrial
        val signupAvailable = signup.status == "AVAILABLE" && signup.version != null && validTime(signup.effectiveAt, signup.expiresAt)
        val baseAvailable = journal.path == "DIRECT_GOOGLE" && base?.status == "AVAILABLE" && base.version != null && validTime(policy.effectiveAt, policy.expiresAt)
        if (!signupAvailable && !baseAvailable) return prepare(journal, changed = true)
        val uid = uids.uid()
        val generation = wallet.generation()
        val wantsBase = journal.path == "DIRECT_GOOGLE" && (journal.requestedBase || baseAvailable)
        val continuity = if (wantsBase) evidence.continuity() else null
        val intent = if (journal.proof != null) journal.intentId else null
        val digest = activationDigest(journal.path, intent, continuity)
        check(journal.continuityDigest == null || journal.continuityDigest == activationDigest("DIRECT_GOOGLE", null, continuity))
        var pending = journal.copy(
            requestId = if (journal.attemptCommitted) UUID.randomUUID().toString() else journal.requestId ?: UUID.randomUUID().toString(),
            signupVersion = signup.grantPolicyVersion ?: signup.version,
            baseVersion = if (wantsBase) base?.grantPolicyVersion ?: base?.version else null,
            requestedBase = wantsBase,
            continuityDigest = if (wantsBase) activationDigest("DIRECT_GOOGLE", null, continuity) else null,
            challengeId = null,
            attemptCommitted = false,
        )
        journals.write(pending)
        ensureOwner(uid, generation)
        val challenge = apiResult {
            api.activationChallenge(ActivationChallengeRequest("GOOGLE_ACTIVATION", checkNotNull(pending.requestId), pending.signupVersion, pending.baseVersion, digest))
        }.getOrThrow()
        ensureOwner(uid, generation)
        check(challenge.bindingVersion == 1 && Instant.parse(challenge.expiresAt) > clock.now())
        pending = pending.copy(challengeId = challenge.challengeId)
        journals.write(pending)
        val token = evidence.token(challenge.requestHash)
        ensureOwner(uid, generation)
        val result = apiResult { api.activateGoogle(request(pending, continuity, token)) }.getOrThrow()
        ensureOwner(uid, generation)
        return finish(pending, result, generation)
    }

    private suspend fun finish(journal: UpgradeJournal, result: GoogleActivationResponse, generation: Int): GoogleAccountState {
        check(wallet.update(result.wallet, generation))
        journals.write(journal.copy(attemptCommitted = true, phase = "LINKED"))
        credits.refresh()
        return GoogleAccountState.Complete(
            result.grants.firstOrNull { it.kind == "GOOGLE_SIGNUP" && result.signupBonus.status == "GRANTED" && !result.replayed }?.amount,
            result.grants.firstOrNull { it.kind == "GUEST_TRIAL" && result.baseTrial.status == "GRANTED" && !result.replayed }?.amount,
        )
    }

    private fun request(journal: UpgradeJournal, continuity: String?, token: String) = GoogleActivationRequest(
        requestId = checkNotNull(journal.requestId),
        policyVersion = journal.signupVersion,
        trialPolicyVersion = journal.baseVersion,
        path = journal.path,
        upgradeIntent = journal.intentId?.let { id -> journal.proof?.let { UpgradeProofDto(id, it) } },
        continuity = continuity?.let { TrialContinuityDto("ANDROID_ID", it) },
        challengeId = checkNotNull(journal.challengeId),
        integrityToken = token,
    )

    private suspend fun signal(journal: UpgradeJournal): String? = if (journal.requestedBase) {
        evidence.continuity().also { check(activationDigest("DIRECT_GOOGLE", null, it) == journal.continuityDigest) }
    } else {
        null
    }

    private fun offer(policy: CreditPolicyDto, journal: UpgradeJournal, changed: Boolean): GoogleAccountState.Offer {
        val signup = policy.signupBonus
        val validSignup = signup.status == "AVAILABLE" && signup.operation == "GOOGLE_ACTIVATION" && signup.bindingVersion == 1 &&
            signup.integrity == "PLAY_INTEGRITY_STANDARD" && validTime(signup.effectiveAt, signup.expiresAt)
        val validBase = journal.path == "DIRECT_GOOGLE" && signup.baseTrial?.status == "AVAILABLE" && validTime(policy.effectiveAt, policy.expiresAt)
        val signupAmount = signup.amount?.takeIf { validSignup && it > 0 }
        val baseAmount = signup.baseTrial?.amount?.takeIf { validBase && it > 0 }
        return GoogleAccountState.Offer(signupAmount, baseAmount, signupAmount != null || baseAmount != null, changed)
    }

    private fun validTime(start: String?, end: String?): Boolean =
        start?.let(Instant::parse)?.let { it <= clock.now() } == true && end?.let(Instant::parse)?.let { it > clock.now() } == true

    private fun ensureOwner(uid: String?, generation: Int) {
        if (uid == null || uid != uids.uid() || uids.isAnonymous() || wallet.generation() != generation) {
            throw kotlinx.coroutines.CancellationException("Google session changed")
        }
    }
}
