package com.tailormyresume.core.network

import kotlinx.serialization.Serializable

sealed interface ApiError {
    data object InvalidInput : ApiError
    data object AppIdInvalid : ApiError
    data object AppNotFound : ApiError
    data object Unauthenticated : ApiError
    data object InvalidToken : ApiError
    data object CrossAppToken : ApiError
    data object Forbidden : ApiError
    data object NotFound : ApiError
    data object AccountDeleted : ApiError
    data object PayloadTooLarge : ApiError
    data class RateLimited(val retryAfterSeconds: Int?) : ApiError
    data object QuotaExceeded : ApiError
    data object BudgetExceeded : ApiError
    data object AiProviderError : ApiError
    data object MethodNotAllowed : ApiError
    data object HttpError : ApiError
    data object InternalError : ApiError
    data object NoCredit : ApiError
    data object PurchasePending : ApiError
    data object PurchaseInvalid : ApiError
    data object AllowanceExhausted : ApiError
    data object AnalysisInProgress : ApiError
    data object NotAJobPost : ApiError
    data object JobImportFailed : ApiError
    data object PlayUnavailable : ApiError
    data object Offline : ApiError
    data object Timeout : ApiError
    data class Unknown(val httpStatus: Int) : ApiError
}

class ApiException(val error: ApiError) : Exception(error.toString())

@Serializable
internal data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
internal data class ErrorBody(val code: String? = null, val message: String? = null)

internal fun apiErrorOf(httpStatus: Int, code: String?, retryAfterSeconds: Int?): ApiError = when (code) {
    "INVALID_INPUT" -> ApiError.InvalidInput
    "APP_ID_INVALID" -> ApiError.AppIdInvalid
    "APP_NOT_FOUND" -> ApiError.AppNotFound
    "UNAUTHENTICATED" -> ApiError.Unauthenticated
    "INVALID_TOKEN" -> ApiError.InvalidToken
    "CROSS_APP_TOKEN" -> ApiError.CrossAppToken
    "FORBIDDEN" -> ApiError.Forbidden
    "NOT_FOUND" -> ApiError.NotFound
    "ACCOUNT_DELETED" -> ApiError.AccountDeleted
    "PAYLOAD_TOO_LARGE" -> ApiError.PayloadTooLarge
    "RATE_LIMITED" -> ApiError.RateLimited(retryAfterSeconds)
    "QUOTA_EXCEEDED" -> ApiError.QuotaExceeded
    "BUDGET_EXCEEDED" -> ApiError.BudgetExceeded
    "AI_PROVIDER_ERROR" -> ApiError.AiProviderError
    "METHOD_NOT_ALLOWED" -> ApiError.MethodNotAllowed
    "HTTP_ERROR" -> ApiError.HttpError
    "INTERNAL_ERROR" -> ApiError.InternalError
    "NO_CREDIT" -> ApiError.NoCredit
    "PURCHASE_PENDING" -> ApiError.PurchasePending
    "PURCHASE_INVALID" -> ApiError.PurchaseInvalid
    "ALLOWANCE_EXHAUSTED" -> ApiError.AllowanceExhausted
    "ANALYSIS_IN_PROGRESS" -> ApiError.AnalysisInProgress
    "NOT_A_JOB_POST" -> ApiError.NotAJobPost
    "JOB_IMPORT_FAILED" -> ApiError.JobImportFailed
    "PLAY_UNAVAILABLE" -> ApiError.PlayUnavailable
    else -> when (httpStatus) {
        HTTP_PAYMENT_REQUIRED -> ApiError.NoCredit
        HTTP_TOO_MANY_REQUESTS -> ApiError.RateLimited(retryAfterSeconds)
        HTTP_UNAUTHORIZED -> ApiError.Unauthenticated
        else -> ApiError.Unknown(httpStatus)
    }
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_PAYMENT_REQUIRED = 402
private const val HTTP_TOO_MANY_REQUESTS = 429
