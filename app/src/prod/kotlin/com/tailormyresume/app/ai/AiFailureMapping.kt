package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.apiResult

internal fun ApiError.toAiFailure(): AiFailure = when (this) {
    ApiError.NoCredit -> AiFailure.NoCredit
    ApiError.AllowanceExhausted -> AiFailure.AllowanceExhausted
    ApiError.InvalidInput, ApiError.PayloadTooLarge -> AiFailure.InvalidInput
    ApiError.AccountDeleted -> AiFailure.AccountDeleted
    is ApiError.RateLimited -> AiFailure.RateLimited
    ApiError.AnalysisInProgress -> AiFailure.AnalysisInProgress
    ApiError.NotAJobPost -> AiFailure.NotAJobPost
    ApiError.JobImportFailed -> AiFailure.JobImportFailed
    ApiError.QuotaExceeded, ApiError.BudgetExceeded -> AiFailure.QuotaExceeded
    ApiError.Unauthenticated, ApiError.InvalidToken -> AiFailure.SignInRequired
    ApiError.Offline -> AiFailure.Network
    ApiError.Timeout -> AiFailure.Timeout
    else -> AiFailure.Unavailable
}

internal fun <T> Result<T>.orAiFailure(): T = getOrElse { failure ->
    val error = (failure as? ApiException)?.error
    throw AiException(error?.toAiFailure() ?: AiFailure.Unavailable, (error as? ApiError.RateLimited)?.retryAfterSeconds)
}

internal suspend fun <T> remoteAi(block: suspend () -> T): T = apiResult(block).orAiFailure()
