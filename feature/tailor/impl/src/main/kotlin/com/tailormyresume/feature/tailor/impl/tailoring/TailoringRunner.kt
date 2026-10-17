package com.tailormyresume.feature.tailor.impl.tailoring

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.TailoringCreditSpend
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock

sealed interface TailoringResult {
    data object Success : TailoringResult

    data class Failure(val cause: Throwable) : TailoringResult
}

class TailoringRunner @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val creditsRepository: CreditsRepository,
    private val tailorResume: TailorResumeUseCase,
    private val clock: Clock,
    @Dispatcher(TmrDispatchers.Default) private val dispatcher: CoroutineDispatcher,
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) {
    suspend operator fun invoke(applicationId: String, runId: String): TailoringResult = try {
        val ticket = workFence.ticket()
        workFence.check(ticket)
        val application = checkNotNull(applicationRepository.observeApplication(applicationId).first())
        val profile = checkNotNull(profileRepository.observeProfile().first())
        val gap = checkNotNull(application.gapAnalysis)
        val runKey = TailoringCreditSpend.runKey(runId)
        val alreadySpent = creditsRepository.observeLedger().first().any {
            it.kind == CreditLedgerKind.SPEND && it.productId == runKey
        }
        val storedForThisRun = application.tailoredResume?.runId == runId
        if (storedForThisRun || alreadySpent) {
            if (storedForThisRun) tailorResume.resultStored(applicationId, runId)
            if (!alreadySpent) creditsRepository.record(TailoringCreditSpend.spendEntry(applicationId, runId, clock))
            return TailoringResult.Success
        }
        TailoringCreditSpend.forSuccess(
            applicationId = applicationId,
            runId = runId,
            clock = clock,
            record = creditsRepository::record,
            tailoring = {
                val tailored = withContext(dispatcher) {
                    tailorResume(profile, application.job, gap, applicationId, runId, quickAnswer = application.quickAnswer)
                }
                workFence.check(ticket)
                applicationRepository.upsertApplication(
                    application.copy(
                        tailoredResume = tailored.copy(runId = runId),
                        keywordCoverage = KeywordCoverageCalculator.compute(
                            gap.matches,
                            application.quickAnswer,
                            tailored.withPendingAccepted(),
                            profile,
                        ),
                        changesAcceptedAt = null,
                        updatedAt = clock.now(),
                    ),
                )
                tailorResume.resultStored(applicationId, runId)
            },
        )
        TailoringResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        TailoringResult.Failure(failure)
    }
}
