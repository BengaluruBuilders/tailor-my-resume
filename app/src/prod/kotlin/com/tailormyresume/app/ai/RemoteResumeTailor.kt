package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.sendableFacts
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.TailoringDto
import com.tailormyresume.core.network.dto.TailoringFailureCode
import com.tailormyresume.core.network.dto.TailoringResultDto
import com.tailormyresume.core.network.dto.TailoringStartRequest
import com.tailormyresume.core.network.dto.TailoringStatus
import com.tailormyresume.core.network.mapper.toDto
import com.tailormyresume.core.network.mapper.toFactsDto
import com.tailormyresume.core.network.mapper.toTailoredBullet
import kotlinx.coroutines.delay
import javax.inject.Inject

class RemoteResumeTailor @Inject constructor(
    private val api: TailorMyResumeApi,
    private val pending: PendingTailoringIds,
) : ResumeTailor {
    override suspend fun tailor(
        profile: CandidateProfile,
        job: JobDescription,
        gap: GapAnalysis,
        applicationId: String,
        section: EntryCategory?,
    ): TailoredResume {
        val request = TailoringStartRequest(
            requestId = pending.idFor(applicationId, section),
            applicationId = applicationId,
            job = job.toDto(),
            matches = gap.matches.map { it.toDto() },
            profile = profile.toFactsDto(),
            section = section,
        )
        try {
            val finished = awaitFinished(start(request))
            pending.clear(applicationId, section)
            val result = finished.result?.takeIf { finished.status == TailoringStatus.SUCCEEDED }
                ?: throw AiException(finished.failureCode.toAiFailure())
            return result.toTailoredResume(profile)
        } catch (failure: AiException) {
            if (failure.failure !in RESUMABLE) pending.clear(applicationId, section)
            throw failure
        }
    }

    private suspend fun start(request: TailoringStartRequest): TailoringDto {
        var attempt = 1
        while (true) {
            val result = apiResult { api.startTailoring(request) }
            val error = (result.exceptionOrNull() as? ApiException)?.error
            if (error !is ApiError.RateLimited || attempt >= START_ATTEMPTS) return result.orAiFailure().tailoring
            val advised = error.retryAfterSeconds
            if (advised != null && advised > MAX_RETRY_AFTER_SECONDS) return result.orAiFailure().tailoring
            attempt++
            val waitSeconds = advised?.takeIf { it >= MIN_RETRY_AFTER_SECONDS }
            delay((waitSeconds ?: DEFAULT_RETRY_AFTER_SECONDS) * MILLIS_PER_SECOND)
        }
    }

    private suspend fun awaitFinished(started: TailoringDto): TailoringDto {
        var current = started
        var waited = 0L
        var step = FIRST_POLL_MILLIS
        while (current.status == TailoringStatus.RUNNING) {
            if (waited >= GIVE_UP_AFTER_MILLIS) throw AiException(AiFailure.Timeout)
            delay(step)
            waited += step
            current = remoteAi { api.tailoring(current.id) }.tailoring
            step = minOf(step + POLL_BACKOFF_MILLIS, MAX_POLL_MILLIS)
        }
        return current
    }

    private fun TailoringFailureCode?.toAiFailure(): AiFailure = when (this) {
        TailoringFailureCode.QUOTA_EXCEEDED, TailoringFailureCode.BUDGET_EXCEEDED -> AiFailure.QuotaExceeded
        else -> AiFailure.Unavailable
    }

    private fun TailoringResultDto.toTailoredResume(profile: CandidateProfile): TailoredResume {
        val sourceText = profile.sendableFacts().entries.flatMap { it.bullets }.associate { it.id to it.text }
        return TailoredResume(
            bullets = bullets.map { bullet ->
                bullet.toTailoredBullet(bullet.sourceIds.firstNotNullOfOrNull(sourceText::get).orEmpty())
                    .copy(generationId = generationId)
            },
        )
    }

    private companion object {
        val RESUMABLE = setOf(AiFailure.Network, AiFailure.Timeout, AiFailure.RateLimited, AiFailure.Unavailable)
        const val START_ATTEMPTS = 3
        const val DEFAULT_RETRY_AFTER_SECONDS = 10
        const val MIN_RETRY_AFTER_SECONDS = 1
        const val MAX_RETRY_AFTER_SECONDS = 60
        const val MILLIS_PER_SECOND = 1_000L
        const val FIRST_POLL_MILLIS = 2_000L
        const val POLL_BACKOFF_MILLIS = 1_000L
        const val MAX_POLL_MILLIS = 5_000L

        // docs/BACKEND_CONTRACT.md line 4.4 rule 5: the server fails a RUNNING job after 5 minutes; 30 s of slack
        const val GIVE_UP_AFTER_MILLIS = 330_000L
    }
}
