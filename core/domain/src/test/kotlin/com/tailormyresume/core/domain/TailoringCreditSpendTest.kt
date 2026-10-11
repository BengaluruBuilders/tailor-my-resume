package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class TailoringCreditSpendTest {
    private val clock = FixedClock(Instant.fromEpochSeconds(2_000_000_000))
    private val ledger = mutableListOf<CreditLedgerEntry>()

    private suspend fun <T> spend(tailoring: suspend () -> T): T =
        TailoringCreditSpend.forSuccess("app-7", clock, { ledger += it }, tailoring)

    @Test
    fun successSpendsExactlyOneCreditForApplication() = runTest {
        val result = spend { "tailored" }

        assertThat(result).isEqualTo("tailored")
        assertThat(ledger).containsExactly(
            CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "app-7", null, clock.instant),
        )
    }

    @Test
    fun failureSpendsNothing() = runTest {
        val outcome = runCatching { spend { error("unavailable") } }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(ledger).isEmpty()
    }

    @Test
    fun interruptionSpendsNothing() = runTest {
        val outcome = runCatching { spend { throw CancellationException("left screen") } }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        assertThat(ledger).isEmpty()
    }
}
