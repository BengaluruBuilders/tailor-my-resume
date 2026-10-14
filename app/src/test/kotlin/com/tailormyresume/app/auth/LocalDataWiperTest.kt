package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class LocalDataWiperTest {
    private lateinit var database: TmrDatabase
    private val store = TestMockStateStore()
    private var exportFilesDeleted = false

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(RuntimeEnvironment.getApplication())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun wipeAllEmptiesRoomStoreAndExportDirectories() = runTest {
        database.profileDao().replaceProfile(
            ProfileEntity(fullName = "Priya", email = "p@example.com", phone = "1", headline = "h", skills = listOf("SQL")),
            emptyList(),
        )
        database.jobApplicationDao().upsertApplication(application("app-1"))
        store.write("exports.history", "[]")
        store.write("coverletter.app-1", "letter")
        store.write("session.account", "{}")

        RoomLocalDataWiper(database, store, ExportedFiles { exportFilesDeleted = true }, UnconfinedTestDispatcher()).wipeAll()

        assertThat(database.profileDao().observePopulatedProfile().first()).isNull()
        assertThat(database.jobApplicationDao().observeApplications().first()).isEmpty()
        assertThat(store.read("exports.history")).isNull()
        assertThat(store.read("coverletter.app-1")).isNull()
        assertThat(store.read("session.account")).isNull()
        assertThat(exportFilesDeleted).isTrue()
    }

    private fun application(id: String) = JobApplicationEntity(
        id = id,
        jobTitle = "Analyst",
        company = "Northwind",
        rawText = "text",
        requirements = emptyList(),
        status = ApplicationStatus.SAVED,
        notes = "",
        gapAnalysis = null,
        tailoredResume = null,
        createdAt = Instant.fromEpochMilliseconds(1),
        updatedAt = Instant.fromEpochMilliseconds(1),
    )
}
