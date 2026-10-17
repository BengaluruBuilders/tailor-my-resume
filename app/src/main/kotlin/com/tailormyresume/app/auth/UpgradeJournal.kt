package com.tailormyresume.app.auth

import android.content.Context
import android.util.AtomicFile
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
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
    val retryAtMillis: Long? = null,
    val profileChoiceConfirmed: Boolean = false,
)

@Singleton
class PrivateUpgradeStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val json = Json { encodeDefaults = true }
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val erased = mutableSetOf<Pair<String, String>>()
    private fun directory() = File(context.filesDir, "account-upgrades").also { it.mkdirs() }
    private fun file(value: UpgradeJournal): AtomicFile {
        val key = value.environment + "\u0000" + value.prepareKey
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory(), "$digest.json"))
    }
    private fun values(): List<UpgradeJournal> = directory().listFiles().orEmpty()
        .filter { it.extension == "json" }.sortedByDescending { it.lastModified() }.map { target ->
            AtomicFile(target).openRead().use { json.decodeFromString(UpgradeJournal.serializer(), it.reader().readText()) }
        }

    suspend fun read(): UpgradeJournal? = withContext(io) { mutex.withLock { values().firstOrNull() } }

    suspend fun readFor(owner: String, environment: String): UpgradeJournal? = withContext(io) {
        mutex.withLock {
            val candidates = values().filter { it.environment == environment }
            candidates.firstOrNull { it.source == owner || it.destination == owner }
                ?: candidates.firstOrNull { it.destination == null && it.phase in setOf("SWITCHING", "DIRECT_SWITCHING") }
        }
    }

    suspend fun write(value: UpgradeJournal) = withContext(io) {
        mutex.withLock {
            check((value.environment to value.source) !in erased && (value.destination == null || (value.environment to value.destination) !in erased))
            val atomic = file(value)
            val stream = atomic.startWrite()
            try {
                stream.write(json.encodeToString(UpgradeJournal.serializer(), value).toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (failure: Exception) {
                atomic.failWrite(stream)
                throw failure
            }
        }
    }

    suspend fun clear(value: UpgradeJournal) = withContext(io) { mutex.withLock { file(value).delete() } }

    suspend fun eraseFor(owner: String, environment: String) = withContext(io) {
        mutex.withLock {
            erased.add(environment to owner)
            values().filter { it.environment == environment && (it.source == owner || it.destination == owner) }
                .forEach { file(it).delete() }
        }
    }
}
