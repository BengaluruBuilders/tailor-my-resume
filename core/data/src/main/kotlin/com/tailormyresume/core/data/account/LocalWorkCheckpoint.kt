package com.tailormyresume.core.data.account

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.database.model.LocalHandoffEntity
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.database.model.ProfileEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkSnapshot(
    val owner: String,
    val environment: String,
    val profile: ProfileEntity?,
    val entries: List<ProfileEntryEntity>,
    val applications: List<JobApplicationEntity>,
    val local: Map<String, String>,
)

@Singleton
class LocalWorkCheckpoint @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TmrDatabase,
    private val store: MockStateStore,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) {
    private val json = Json { encodeDefaults = true }

    suspend fun capture(owner: String, environment: String): WorkSnapshot = withContext(io) {
        val snapshot = database.withTransaction {
            val apps = database.jobApplicationDao().readAll()
            val keys = LOCAL_KEYS + apps.flatMap { app ->
                listOf("tailor.edited.${app.id}", "tailor.regenerations.${app.id}", "reports.content.${app.id}", "prep.plan.${app.id}")
            }
            val local = keys.mapNotNull { key -> store.read(key)?.let { key to it } }.toMap()
            WorkSnapshot(owner, environment, database.profileDao().readProfile(), database.profileDao().readEntries(), apps, local)
        }
        val target = folder(owner, environment)
        val files = File(target, "exports").also { it.mkdirs() }
        File(context.filesDir, "exports").takeIf { it.exists() }?.copyRecursively(files, overwrite = true)
        atomicWrite(File(target, "snapshot.json"), json.encodeToString(WorkSnapshot.serializer(), snapshot))
        snapshot
    }

    suspend fun read(owner: String, environment: String): WorkSnapshot? = withContext(io) {
        val file = File(folder(owner, environment), "snapshot.json")
        if (!file.exists()) null else json.decodeFromString(WorkSnapshot.serializer(), AtomicFile(file).openRead().use { it.reader().readText() })
            .also { check(it.owner == owner && it.environment == environment) }
    }

    suspend fun restore(owner: String, environment: String) = withContext(io) {
        val snapshot = read(owner, environment)
        database.withTransaction {
            database.profileDao().deleteProfile()
            database.jobApplicationDao().deleteAll()
            database.creditLedgerDao().clear()
            if (snapshot != null) {
                snapshot.profile?.let { database.profileDao().replaceProfile(it, snapshot.entries) }
                snapshot.applications.forEach { database.jobApplicationDao().upsertApplication(it) }
            }
        }
        clearLocalKeys()
        snapshot?.local?.forEach { (key, value) -> store.write(key, value) }
        File(context.filesDir, "exports").deleteRecursively()
        File(folder(owner, environment), "exports").takeIf { it.exists() }
            ?.copyRecursively(File(context.filesDir, "exports"), overwrite = true)
    }

    suspend fun importSelected(
        handoff: String,
        source: String,
        destination: String,
        environment: String,
        useProfile: Boolean,
        applicationIds: Set<String>,
    ) = withContext(io) {
        val snapshot = checkNotNull(read(source, environment))
        val selected = snapshot.applications.filter { it.id in applicationIds }
        val mapping = selected.associate { it.id to importedId(handoff, it.id) }
        stageFiles(snapshot, mapping)
        database.withTransaction {
            val previous = database.localHandoffDao().find(handoff)
            if (previous == null) {
                if (useProfile) snapshot.profile?.let { database.profileDao().replaceProfile(it, snapshot.entries) }
                selected.forEach { app ->
                    database.jobApplicationDao().upsertApplication(detached(app, mapping.getValue(app.id)))
                }
                database.localHandoffDao().insert(LocalHandoffEntity(handoff, destination))
            } else {
                check(previous.destination == destination)
            }
        }
        mapping.forEach { (old, new) ->
            listOf("tailor.edited.", "tailor.regenerations.").forEach { prefix ->
                snapshot.local[prefix + old]?.let { store.write(prefix + new, it) }
            }
        }
        if (store.read("session.keptJobDescription") == null) {
            snapshot.local["session.keptJobDescription"]?.let { store.write("session.keptJobDescription", it) }
        }
        if (useProfile) snapshot.local["session.onboardingComplete"]?.let { store.write("session.onboardingComplete", it) }
    }

    suspend fun erase(owner: String, environment: String) = withContext(io) {
        folder(owner, environment).deleteRecursively()
    }

    private fun stageFiles(snapshot: WorkSnapshot, mapping: Map<String, String>) {
        val root = File(folder(snapshot.owner, snapshot.environment), "exports")
        mapping.forEach { (old, new) ->
            val original = File(root, segment(old))
            if (original.exists()) original.copyRecursively(File(context.filesDir, "exports/${segment(new)}"), overwrite = true)
        }
    }

    private suspend fun clearLocalKeys() {
        LOCAL_KEYS.forEach { store.remove(it) }
        listOf("tailor.", "reports.content.", "prep.plan.", "tailoring.request.").forEach { store.removeWithPrefix(it) }
    }

    private fun folder(owner: String, environment: String): File =
        File(context.filesDir, "account-work/${digest(environment + "\u0000" + owner)}").also { it.mkdirs() }

    private fun segment(id: String): String = URLEncoder.encode(id, Charsets.UTF_8.name()).replace(".", "%2E").ifEmpty { "%" }

    private fun importedId(handoff: String, id: String): String = "local-${digest(handoff + "\u0000" + id).take(48)}"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun atomicWrite(file: File, value: String) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(value.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        }
    }

    private fun detached(app: JobApplicationEntity, id: String): JobApplicationEntity = app.copy(
        id = id,
        gapAnalysis = app.gapAnalysis?.copy(generationId = null),
        tailoredResume = app.tailoredResume?.copy(runId = null, bullets = app.tailoredResume.bullets.map { it.copy(generationId = null) }),
    )

    private companion object {
        val LOCAL_KEYS = listOf("session.keptJobDescription", "session.onboardingComplete", "resume.settings", "exports.history")
    }
}
