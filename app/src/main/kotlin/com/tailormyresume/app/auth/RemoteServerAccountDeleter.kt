package com.tailormyresume.app.auth

import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import javax.inject.Inject

class RemoteServerAccountDeleter @Inject constructor(private val api: TailorMyResumeApi) : ServerAccountDeleter {
    override val deletesRemoteData = true

    override suspend fun delete(): Result<Unit> = apiResult { api.deleteMe() }
        .map { }
        .recoverCatching { failure ->
            if ((failure as? ApiException)?.error == ApiError.AccountDeleted) Unit else throw failure
        }

    override fun mayHaveReachedServer(failure: Throwable): Boolean = when (val error = (failure as? ApiException)?.error) {
        ApiError.Timeout, ApiError.Offline, ApiError.InternalError, ApiError.HttpError, ApiError.AiProviderError -> true
        is ApiError.Unknown -> error.httpStatus >= SERVER_ERROR_FLOOR || error.httpStatus == 0
        else -> false
    }

    override suspend fun isClosed(): Result<Boolean> = apiResult { api.me() }.fold(
        onSuccess = { Result.success(false) },
        onFailure = { failure ->
            if ((failure as? ApiException)?.error == ApiError.AccountDeleted) Result.success(true) else Result.failure(failure)
        },
    )

    private companion object {
        const val SERVER_ERROR_FLOOR = 500
    }
}
