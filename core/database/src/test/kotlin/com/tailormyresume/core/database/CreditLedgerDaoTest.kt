package com.tailormyresume.core.database

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.model.CreditLedgerEntity
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class CreditLedgerDaoTest {

    private lateinit var database: TmrDatabase

    @Before
    fun open() {
        database = createInMemoryTmrDatabase(RuntimeEnvironment.getApplication())
    }

    @After
    fun close() = database.close()

    @Test
    fun balanceSumsTheLedgerAndEntriesComeNewestFirst() = runBlocking {
        val dao = database.creditLedgerDao()
        dao.insert(entry(CreditLedgerKind.FREE_GRANT, 1, 1))
        dao.insert(entry(CreditLedgerKind.SPEND, -1, 2, applicationId = "a"))
        dao.insert(entry(CreditLedgerKind.PURCHASE, 5, 3, productId = "application_pack_5"))

        assertThat(dao.observeBalance().first()).isEqualTo(5)
        assertThat(dao.observeLedger().first().map { it.createdAt.toEpochMilliseconds() }).containsExactly(3L, 2L, 1L).inOrder()
    }

    @Test
    fun anEmptyLedgerHasABalanceOfZero() = runBlocking {
        assertThat(database.creditLedgerDao().observeBalance().first()).isEqualTo(0)
    }

    @Test
    fun clearAllTablesEmptiesTheLedger() = runBlocking {
        database.creditLedgerDao().insert(entry(CreditLedgerKind.FREE_GRANT, 1, 1))

        database.clearAllTables()

        assertThat(database.creditLedgerDao().observeLedger().first()).isEmpty()
    }

    private fun entry(
        kind: CreditLedgerKind,
        amount: Int,
        at: Long,
        applicationId: String? = null,
        productId: String? = null,
    ) = CreditLedgerEntity(
        kind = kind,
        amount = amount,
        applicationId = applicationId,
        productId = productId,
        createdAt = Instant.fromEpochMilliseconds(at),
    )
}
