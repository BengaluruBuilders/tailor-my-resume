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
    val source: String = "MANUAL",
    val requestId: String? = null,
    val revision: String? = null,
    val requiresConfirmation: Boolean = false,
    val payloadDigest: String? = null,
    val phase: String = "CANDIDATE",
    val challengeId: String? = null,
    val retryAt: Long? = null,
    val reviewFrom: String? = null,
    val issueRequestId: String? = null,
    val observationId: String? = null,
)

@Serializable
internal data class ReferralInstallation(
    val environment: String? = null,
    val code: String? = null,
    val consumed: Boolean = false,
    val attempts: Int = 0,
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
            if (!target.baseFile.exists()) {
                null
            } else {
                target.openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) }
                    .takeIf { it.owner == owner && it.environment == environment }
            }
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
    internal suspend fun transferForReview(source: String, destination: String, environment: String, provider: String, generation: Long, checkOwner: () -> Unit) = withContext(io) {
        mutex.withLock {
            checkOwner()
            val sourceFile = file(source, environment)
            val destinationFile = file(destination, environment)
            val current = if (destinationFile.baseFile.exists()) destinationFile.openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) } else null
            val candidate = if (sourceFile.baseFile.exists()) sourceFile.openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) } else null
            if (current == null && candidate?.code != null) {
                val review = ReferralJournal(destination, environment, provider, generation, code = candidate.code, source = candidate.source, reviewFrom = source)
                val stream = destinationFile.startWrite()
                try {
                    stream.write(json.encodeToString(ReferralJournal.serializer(), review).toByteArray())
                    checkOwner()
                    destinationFile.finishWrite(stream)
                } catch (failure: Exception) {
                    destinationFile.failWrite(stream)
                    throw failure
                }
            }
            synchronized(this@PrivateReferralStore) { epoch++ }
            directory().listFiles().orEmpty().filter { it.name != destinationFile.baseFile.name }.forEach { it.delete() }
        }
    }
    suspend fun clearAll(preserveInbox: Boolean = false) {
        synchronized(this) { epoch++ }
        withContext(io) {
            mutex.withLock {
                directory().listFiles().orEmpty().forEach { it.delete() }
                val installation = installation()
                saveInstallation(if (preserveInbox) installation else installation.copy(code = null, environment = null, consumed = true))
            }
        }
    }
    suspend fun eraseFor(owner: String, environment: String) {
        synchronized(this) { epoch++ }
        withContext(io) {
            mutex.withLock {
                erased.add(environment to owner)
                saveInstallation(installation().copy(code = null, environment = null, consumed = true))
                directory().listFiles().orEmpty().filter { it.extension == "json" }.forEach { target ->
                    val value = AtomicFile(target).openRead().use { json.decodeFromString<ReferralJournal>(it.reader().readText()) }
                    if (value.environment == environment && (value.owner == owner || value.reviewFrom == owner)) AtomicFile(target).delete()
                }
            }
        }
    }
    private fun installationFile() = AtomicFile(File(context.filesDir, "referral-installation.json"))
    private fun installation(): ReferralInstallation = installationFile().let { target ->
        if (target.baseFile.exists()) target.openRead().use { json.decodeFromString<ReferralInstallation>(it.reader().readText()) } else ReferralInstallation()
    }
    private fun saveInstallation(value: ReferralInstallation) {
        val target = installationFile()
        val stream = target.startWrite()
        try {
            stream.write(json.encodeToString(ReferralInstallation.serializer(), value).toByteArray())
            target.finishWrite(stream)
        } catch (failure: Exception) {
            target.failWrite(stream)
            throw failure
        }
    }
    internal suspend fun inbox(environment: String): String? = withContext(io) {
        mutex.withLock { installation().takeIf { it.environment == environment }?.code }
    }
    internal suspend fun capture(code: String?, environment: String, expectedEpoch: Long, terminalReferrer: Boolean = false): Boolean = withContext(io) {
        mutex.withLock {
            check(expectedEpoch == epoch())
            val current = installation()
            if (terminalReferrer && current.consumed) return@withLock false
            val next = current.copy(
                code = current.code ?: code,
                environment = if (current.code == null && code != null) environment else current.environment,
                consumed = current.consumed || terminalReferrer,
            )
            saveInstallation(next)
            code != null && next.code == code && next.environment == environment
        }
    }
    internal suspend fun consumeInstallation(expectedEpoch: Long = epoch()) = withContext(io) {
        mutex.withLock {
            check(expectedEpoch == epoch())
            saveInstallation(installation().copy(consumed = true))
        }
    }
    internal suspend fun beginCollection(expectedEpoch: Long): Boolean = withContext(io) {
        mutex.withLock {
            check(expectedEpoch == epoch())
            val current = installation()
            if (current.consumed || current.attempts >= 3) return@withLock false
            saveInstallation(current.copy(attempts = current.attempts + 1))
            true
        }
    }
    internal suspend fun bindInbox(value: ReferralJournal, expectedEpoch: Long, checkOwner: () -> Unit): ReferralJournal = withContext(io) {
        mutex.withLock {
            checkOwner()
            check(expectedEpoch == epoch() && (value.environment to value.owner) !in erased)
            val inbox = installation()
            if (inbox.environment != value.environment || inbox.code == null) return@withLock value
            val bound = if (value.code == null && value.phase == "CANDIDATE") value.copy(code = inbox.code, source = "LINK") else value
            val target = file(bound.owner, bound.environment)
            val stream = target.startWrite()
            try {
                stream.write(json.encodeToString(ReferralJournal.serializer(), bound).toByteArray())
                checkOwner()
                check(expectedEpoch == epoch())
                target.finishWrite(stream)
            } catch (failure: Exception) {
                target.failWrite(stream)
                throw failure
            }
            saveInstallation(inbox.copy(code = null, environment = null))
            bound
        }
    }
    internal suspend fun dismissInbox() = withContext(io) {
        mutex.withLock { saveInstallation(installation().copy(code = null, environment = null, consumed = true)) }
    }

}
