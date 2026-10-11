package com.tailormyresume.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.testing.repository.CreditsRepositoryContractTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstCreditsRepositoryTest : CreditsRepositoryContractTest() {

    private lateinit var database: TmrDatabase

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun closeDatabase() = database.close()

    override fun createCreditsRepository(): CreditsRepository =
        OfflineFirstCreditsRepository(database.creditLedgerDao(), UnconfinedTestDispatcher())
}
