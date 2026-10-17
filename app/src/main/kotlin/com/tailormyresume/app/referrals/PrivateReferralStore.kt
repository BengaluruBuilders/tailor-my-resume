package com.tailormyresume.app.referrals

import android.content.Context
import android.util.AtomicFile
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class ReferralJournal(
    val owner: String,
    val environment: String,
    val provider: String,
    val generation: Long,
    val code: String? = null,
    val requestId: String? = null,
    val revision: String? = null,
    val payloadDigest: String? = null,
    val phase: String = "CANDIDATE",
    val challengeId: String? = null,
    val retryAt: Long? = null,
    val reviewFrom: String? = null,
    val issueRequestId: String? = null,
    val observationId: String? = null,
)

@Singleton
class PrivateReferralStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }
    private val erased = mutableSetOf<Pair<String, String>>()
    private var epoch = 0L
    fun epoch(): Long = synchronized(this) { epoch }
    private fun directory() = File(context.filesDir, "manual-referrals").also { it.mkdirs() }
    private fun file(owner: String, environment: String): AtomicFile {
        val digest = MessageDigest.getInstance("SHA-256").digest("$environment\u0000$owner".toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory(), "$digest.json"))
    }
    internal suspend fun read(owner: String, environment: String): ReferralJournal? = withContext(io) {
        mutex.withLock {
            val target = file(owner, environment)
            if (!target.baseFile.exists()) null else target.openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) }
                .takeIf { it.owner == owner && it.environment == environment }
        }
    }
    internal suspend fun write(value: ReferralJournal, expectedEpoch: Long, checkOwner: () -> Unit) = withContext(io) {
        mutex.withLock {
            checkOwner()
            check(expectedEpoch == epoch() && (value.environment to value.owner) !in erased)
            val target = file(value.owner, value.environment)
            val stream = target.startWrite()
            try {
                stream.write(json.encodeToString(ReferralJournal.serializer(), value).toByteArray())
                checkOwner()
                check(expectedEpoch == epoch())
                target.finishWrite(stream)
            } catch (failure: Exception) {
                target.failWrite(stream)
                throw failure
            }
        }
    }
    internal suspend fun clear(owner: String, environment: String) = withContext(io) {
        mutex.withLock { file(owner, environment).delete() }
    }
    suspend fun clearAll() {
        synchronized(this) { epoch++ }
        withContext(io) { mutex.withLock { directory().listFiles().orEmpty().forEach { it.delete() } } }
    }
    suspend fun eraseFor(owner: String, environment: String) {
        synchronized(this) { epoch++ }
        withContext(io) {
            mutex.withLock {
                erased.add(environment to owner)
                directory().listFiles().orEmpty().filter { it.extension == "json" }.forEach { target ->
                    val value = AtomicFile(target).openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) }
                    if (value.environment == environment && (value.owner == owner || value.reviewFrom == owner)) AtomicFile(target).delete()
                }
            }
        }
    }
}
