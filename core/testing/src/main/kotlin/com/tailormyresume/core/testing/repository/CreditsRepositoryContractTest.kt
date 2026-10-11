package com.tailormyresume.core.testing.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

abstract class CreditsRepositoryContractTest {

    protected abstract fun createCreditsRepository(): CreditsRepository

    @Test
    fun balanceIsThreeAndLedgerIsNewestFirst() = runTest {
        val repository = createCreditsRepository()
        listOf(
            entry(CreditLedgerKind.FREE_GRANT, 1, day = 2),
            entry(CreditLedgerKind.SPEND, -1, day = 4, applicationId = "hr"),
            entry(CreditLedgerKind.PURCHASE, 5, day = 5, productId = "application_pack_5"),
            entry(CreditLedgerKind.SPEND, -1, day = 6, applicationId = "kb"),
            entry(CreditLedgerKind.SPEND, -1, day = 8, applicationId = "nw"),
        ).forEach { repository.record(it) }

        repository.refresh()

        assertThat(repository.observeBalance().first()).isEqualTo(3)
        assertThat(repository.observeLedger().first().map { it.createdAt.toString() }).containsExactly(
            "2026-10-08T09:00:00Z",
            "2026-10-06T09:00:00Z",
            "2026-10-05T09:00:00Z",
            "2026-10-04T09:00:00Z",
            "2026-10-02T09:00:00Z",
        ).inOrder()
    }

    @Test
    fun anEmptyLedgerHasNoCredits() = runTest {
        val repository = createCreditsRepository()

        repository.refresh()

        assertThat(repository.observeBalance().first()).isEqualTo(0)
        assertThat(repository.observeLedger().first()).isEmpty()
    }

    @Test
    fun anEntryKeepsItsApplicationAndProduct() = runTest {
        val repository = createCreditsRepository()
        val purchase = entry(CreditLedgerKind.PURCHASE, 5, day = 5, productId = "application_pack_5")

        repository.record(purchase)

        assertThat(repository.observeLedger().first()).containsExactly(purchase)
    }

    @Test
    fun clearEmptiesTheLedger() = runTest {
        val repository = createCreditsRepository()
        repository.record(entry(CreditLedgerKind.FREE_GRANT, 1, day = 2))

        repository.clear()

        assertThat(repository.observeLedger().first()).isEmpty()
    }

    @Test
    fun removingAnApplicationDropsOnlyItsRows() = runTest {
        val repository = createCreditsRepository()
        val purchase = entry(CreditLedgerKind.PURCHASE, 5, day = 5, productId = "application_pack_5")
        repository.record(purchase)
        repository.record(entry(CreditLedgerKind.SPEND, -1, day = 6, applicationId = "kb"))

        repository.removeForApplication("kb")

        assertThat(repository.observeLedger().first()).containsExactly(purchase)
    }

    private fun entry(
        kind: CreditLedgerKind,
        amount: Int,
        day: Int,
        applicationId: String? = null,
        productId: String? = null,
    ) = CreditLedgerEntry(
        kind = kind,
        amount = amount,
        applicationId = applicationId,
        productId = productId,
        createdAt = Instant.parse("2026-10-%02dT09:00:00Z".format(day)),
    )
}
