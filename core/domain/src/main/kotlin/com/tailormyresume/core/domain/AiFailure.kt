package com.tailormyresume.core.domain

enum class AiFailure {
    Network,
    Timeout,
    NoCredit,
    AllowanceExhausted,
    InvalidInput,
    AccountDeleted,
    RateLimited,
    AnalysisInProgress,
    QuotaExceeded,
    SignInRequired,
    NotAJobPost,
    JobImportFailed,
    Unavailable,
}

class AiException(val failure: AiFailure, val retryAfterSeconds: Int? = null) : Exception(failure.name)

fun Throwable.isAiFailure(failure: AiFailure): Boolean = (this as? AiException)?.failure == failure
