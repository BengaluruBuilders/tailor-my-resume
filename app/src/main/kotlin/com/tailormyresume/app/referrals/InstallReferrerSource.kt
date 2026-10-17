package com.tailormyresume.app.referrals

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

sealed interface InstallReferrerResult {
    data class Terminal(val value: String?) : InstallReferrerResult
    data object Retry : InstallReferrerResult
}

fun interface InstallReferrerSource {
    suspend fun read(): InstallReferrerResult
}

class OfficialInstallReferrerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) : InstallReferrerSource {
    override suspend fun read(): InstallReferrerResult = withContext(io) {
        val client = InstallReferrerClient.newBuilder(context).build()
        try {
            val response = withTimeoutOrNull(5_000) {
                suspendCancellableCoroutine { continuation ->
                    client.startConnection(object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            if (continuation.isActive) continuation.resume(responseCode)
                        }
                        override fun onInstallReferrerServiceDisconnected() {
                            if (continuation.isActive) continuation.resume(InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED)
                        }
                    })
                }
            }
            when (response) {
                InstallReferrerClient.InstallReferrerResponse.OK -> InstallReferrerResult.Terminal(client.installReferrer.installReferrer)
                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> InstallReferrerResult.Terminal(null)
                else -> InstallReferrerResult.Retry
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            InstallReferrerResult.Retry
        } finally {
            client.endConnection()
        }
    }
}
