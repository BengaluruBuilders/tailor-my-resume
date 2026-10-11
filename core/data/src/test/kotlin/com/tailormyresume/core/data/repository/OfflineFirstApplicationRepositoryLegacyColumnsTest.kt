package com.tailormyresume.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.model.asEntity
import com.tailormyresume.core.data.model.testApplication
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
class OfflineFirstApplicationRepositoryLegacyColumnsTest {

    private lateinit var database: TmrDatabase

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun closeDatabase() = database.close()

    private val migratedRow = testApplication.asEntity().copy(notes = "no reply yet", legacyStatus = "NO_RESPONSE")

    private fun repository() = OfflineFirstApplicationRepository(
        jobApplicationDao = database.jobApplicationDao(),
        clock = Clock.System,
        cleanup = ApplicationCleanup {},
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun upsertingTheObservedModelKeepsStoredNotesAndLegacyStatus() = runTest {
        database.jobApplicationDao().upsertApplication(migratedRow)
        val repository = repository()
        val observed = checkNotNull(repository.observeApplication(migratedRow.id).first())

        repository.upsertApplication(observed.copy(location = "Pune"))

        val stored = checkNotNull(database.jobApplicationDao().getApplication(migratedRow.id))
        assertThat(stored.location).isEqualTo("Pune")
        assertThat(stored.notes).isEqualTo("no reply yet")
        assertThat(stored.legacyStatus).isEqualTo("NO_RESPONSE")
    }

    @Test
    fun upsertingAModelBuiltWithoutLegacyFieldsStillKeepsTheStoredOnes() = runTest {
        database.jobApplicationDao().upsertApplication(migratedRow)

        repository().upsertApplication(testApplication.copy(location = "Goa"))

        val stored = checkNotNull(database.jobApplicationDao().getApplication(migratedRow.id))
        assertThat(stored.notes).isEqualTo("no reply yet")
        assertThat(stored.legacyStatus).isEqualTo("NO_RESPONSE")
    }

    @Test
    fun reinsertingAfterDeleteRestoresTheLegacyFieldsCarriedByTheModel() = runTest {
        database.jobApplicationDao().upsertApplication(migratedRow)
        val repository = repository()
        val observed = checkNotNull(repository.observeApplication(migratedRow.id).first())
        repository.deleteApplicationRow(migratedRow.id)

        repository.upsertApplication(observed)

        val stored = checkNotNull(database.jobApplicationDao().getApplication(migratedRow.id))
        assertThat(stored.notes).isEqualTo("no reply yet")
        assertThat(stored.legacyStatus).isEqualTo("NO_RESPONSE")
    }
}
