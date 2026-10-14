package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.StoredResumeSettingsRepository
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.database.model.CreditLedgerEntity
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ResumeSettings
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
class LocalDataWiperSimplifiedFlowTest {
    private lateinit var database: TmrDatabase
    private val store = TestMockStateStore()

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(RuntimeEnvironment.getApplication())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun wipeAllEmptiesTheCreditLedgerAndResetsResumeSettings() = runTest {
        database.creditLedgerDao().insert(
            CreditLedgerEntity(
                kind = CreditLedgerKind.PURCHASE,
                amount = 5,
                applicationId = null,
                productId = "application_pack_5",
                createdAt = Instant.fromEpochMilliseconds(1),
            ),
        )
        val settings = StoredResumeSettingsRepository(store)
        settings.update { it.copy(pageSize = PageSize.LETTER, productUpdates = true) }

        RoomLocalDataWiper(database, store, ExportedFiles { }, UnconfinedTestDispatcher()).wipeAll()

        assertThat(database.creditLedgerDao().observeLedger().first()).isEmpty()
        assertThat(settings.observeSettings().first()).isEqualTo(ResumeSettings())
    }
}
