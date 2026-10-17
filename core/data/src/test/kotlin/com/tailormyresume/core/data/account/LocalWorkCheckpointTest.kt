package com.tailormyresume.core.data.account

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.model.asEntity
import com.tailormyresume.core.data.model.testApplication
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocalWorkCheckpointTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = createInMemoryTmrDatabase(context)
    private val store = TestMockStateStore()

    @After fun close() {
        database.close()
        File(context.filesDir, "account-work").deleteRecursively()
        File(context.filesDir, "exports").deleteRecursively()
    }

    @Test fun handoffKeepsDestinationProfileDetachesCapabilitiesAndImportsOnceAfterRestart() = runTest {
        val checkpoint = LocalWorkCheckpoint(context, database, store, UnconfinedTestDispatcher(testScheduler))
        database.profileDao().upsertProfile(profile("Destination"))
        checkpoint.capture("B", "fixture")
        checkpoint.restore("A", "fixture")
        database.profileDao().upsertProfile(profile("Guest"))
        val original = testApplication.asEntity().copy(notes = "Guest notes", exportFileName = "resume.pdf")
        database.jobApplicationDao().upsertApplication(original)
        store.write("tailor.edited.${original.id}", "edited-bullet")
        store.write("tailoring.request.unfinished.${original.id}", "remote-request")
        store.write("reports.content.${original.id}", "source-report")
        store.write("session.keptJobDescription", "saved job draft")
        checkpoint.capture("A", "fixture")
        checkpoint.restore("B", "fixture")
        checkpoint.importSelected("handoff", "A", "B", "fixture", useProfile = false, setOf(original.id))
        val imported = database.jobApplicationDao().readAll().single()
        assertThat(imported.id).isNotEqualTo(original.id)
        assertThat(imported.notes).isEqualTo("Guest notes")
        assertThat(imported.gapAnalysis?.generationId).isNull()
        assertThat(imported.tailoredResume?.runId).isNull()
        assertThat(imported.tailoredResume?.bullets?.map { it.generationId }.orEmpty()).containsNoneOf("remote-generation", "source-generation")
        assertThat(database.profileDao().readProfile()?.fullName).isEqualTo("Destination")
        assertThat(store.read("tailor.edited.${imported.id}")).isEqualTo("edited-bullet")
        assertThat(store.read("tailoring.request.unfinished.${original.id}")).isNull()
        assertThat(store.read("reports.content.${imported.id}")).isNull()
        val restarted = LocalWorkCheckpoint(context, database, store, UnconfinedTestDispatcher(testScheduler))
        restarted.restore("B", "fixture")
        restarted.importSelected("handoff", "A", "B", "fixture", false, setOf(original.id))
        assertThat(database.jobApplicationDao().readAll()).containsExactly(imported)
        assertThat(restarted.read("A", "fixture")?.profile?.fullName).isEqualTo("Guest")
    }

    @Test fun accountCAndAnotherEnvironmentCannotLoadGuestSnapshotOrFiles() = runTest {
        val checkpoint = LocalWorkCheckpoint(context, database, store, UnconfinedTestDispatcher(testScheduler))
        database.profileDao().upsertProfile(profile("Guest"))
        checkpoint.capture("A", "fixture")
        assertThat(checkpoint.read("C", "fixture")).isNull()
        assertThat(checkpoint.read("A", "production")).isNull()
        checkpoint.restore("C", "fixture")
        assertThat(database.profileDao().readProfile()).isNull()
        assertThat(runCatching { checkpoint.importSelected("handoff", "A", "B", "fixture", true, emptySet()) }.isFailure).isTrue()
        checkpoint.erase("A", "fixture")
        assertThat(checkpoint.read("A", "fixture")).isNull()
    }

    @Test fun aFailedOrCancelledTransitionLeavesAnExactSourceCheckpoint() = runTest {
        val checkpoint = LocalWorkCheckpoint(context, database, store, UnconfinedTestDispatcher(testScheduler))
        database.profileDao().upsertProfile(profile("Guest"))
        val original = testApplication.asEntity().copy(notes = "Unchanged", changesAcceptedAt = testApplication.updatedAt)
        database.jobApplicationDao().upsertApplication(original)
        val captured = checkpoint.capture("A", "fixture")
        assertThat(checkpoint.read("A", "fixture")).isEqualTo(captured)
        assertThat(database.jobApplicationDao().readAll()).containsExactly(original)
        assertThat(database.profileDao().readProfile()?.fullName).isEqualTo("Guest")
    }

    private fun profile(name: String) = ProfileEntity(fullName = name, email = "", phone = "", headline = "", skills = emptyList())
}
