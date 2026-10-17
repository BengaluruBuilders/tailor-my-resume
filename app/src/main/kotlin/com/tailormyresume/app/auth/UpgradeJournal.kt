package com.tailormyresume.app.auth

import android.content.Context
import android.util.AtomicFile
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UpgradeJournal(
    val environment: String,
    val source: String,
    val prepareKey: String,
    val proof: String?,
    val intentId: String? = null,
    val phase: String = "PREPARING",
    val destination: String? = null,
    val useProfile: Boolean = true,
    val applications: Set<String> = emptySet(),
    val completionKey: String,
    val observationKey: String,
    val path: String = "GUEST_LINK",
    val requestId: String? = null,
    val signupVersion: String? = null,
    val baseVersion: String? = null,
    val requestedBase: Boolean = false,
    val continuityDigest: String? = null,
    val challengeId: String? = null,
    val attemptCommitted: Boolean = false,
)

@Singleton
class PrivateUpgradeStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val json = Json { encodeDefaults = true }
    private fun file() = AtomicFile(File(context.filesDir, "account-upgrade.json"))

    suspend fun read(): UpgradeJournal? = withContext(io) {
        if (!file().baseFile.exists()) null else file().openRead().use {
            json.decodeFromString(UpgradeJournal.serializer(), it.reader().readText())
        }
    }

    suspend fun write(value: UpgradeJournal) = withContext(io) {
        val atomic = file()
        val stream = atomic.startWrite()
        try {
            stream.write(json.encodeToString(UpgradeJournal.serializer(), value).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        }
    }

    suspend fun clear() = withContext(io) { file().delete() }
}
