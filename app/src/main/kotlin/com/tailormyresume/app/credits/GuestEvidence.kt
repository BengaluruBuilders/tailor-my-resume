package com.tailormyresume.app.credits

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.tailormyresume.app.BuildConfig
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface GuestEvidence {
    suspend fun continuity(): String
    suspend fun token(requestHash: String): String
}

class StandardGuestEvidence @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) : GuestEvidence {
    // Explicit guest/base continuity only; referral uses null: https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID
    @SuppressLint("HardwareIds")
    override suspend fun continuity(): String = withContext(io) {
        val value = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        requireNotNull(value?.takeIf(::usableAndroidId)) { "Trial verification is unavailable" }
    }

    override suspend fun token(requestHash: String): String = withContext(io) {
        val project = BuildConfig.TAILORMYRESUME_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER.toLongOrNull()
            ?.takeIf { it > 0 } ?: error("Trial verification is not configured")
        require(Regex("[A-Za-z0-9_-]{43}").matches(requestHash))
        val manager = IntegrityManagerFactory.createStandard(context)
        val provider = manager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder().setCloudProjectNumber(project).build(),
        ).awaitIntegrity()
        provider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build(),
        ).awaitIntegrity().token()
    }
}

internal fun usableAndroidId(value: String): Boolean =
    Regex("[0-9a-f]{16}").matches(value) && value !in setOf("0000000000000000", "9774d56d682e549c")

internal fun guestPayloadDigest(value: String): String {
    require(usableAndroidId(value))
    val bytes = "[\"TMR_GUEST_TRIAL_PAYLOAD_V1\",\"ANDROID_ID\",\"$value\"]".toByteArray(Charsets.UTF_8)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes))
}

private suspend fun <T> Task<T>.awaitIntegrity(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
}
