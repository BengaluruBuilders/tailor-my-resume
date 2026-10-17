package com.tailormyresume.feature.settings.impl.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import org.junit.Test
import java.time.ZoneOffset
import kotlin.time.Instant

class CreditsHistoryMapperTest {

    private fun rows(
        entries: List<CreditLedgerEntry> = returningScenario.ledger,
        applications: List<com.tailormyresume.core.model.JobApplication> = returningScenario.applications,
    ) = toHistoryRows(entries, applications, ZoneOffset.UTC)

    @Test
    fun guestTrialKeepsItsOriginalServerIdAndActualAmount() {
        val entry = CreditLedgerEntry(CreditLedgerKind.GUEST_TRIAL, 8, null, null, Instant.parse("2026-10-17T00:00:00Z"), id = "original-server-id")
        val row = toHistoryRows(listOf(entry), emptyList(), java.time.ZoneId.of("UTC")).single()
        assertThat(row.key).isEqualTo("original-server-id")
        assertThat(row.title).isEqualTo(HistoryTitle.GuestTrial)
        assertThat(row.amountText).isEqualTo("+8")
    }

    @Test
    fun rowsAreNewestFirstWithPrototypeLabels() {
        val rows = rows()

        assertThat(rows.map { it.title }).containsExactly(
            HistoryTitle.Tailored("Associate Analyst"),
            HistoryTitle.Tailored("Business Analyst"),
            HistoryTitle.BoughtCredits(5),
            HistoryTitle.Tailored("Data Analyst"),
            HistoryTitle.FreeFirstResume,
        ).inOrder()
        assertThat(rows.map { it.detail }).containsExactly(
            HistoryDetail.Text("Northwind GCC"),
            HistoryDetail.Text("Kestrel Bank"),
            HistoryDetail.None,
            HistoryDetail.Text("Halden Retail"),
            HistoryDetail.Welcome,
        ).inOrder()
        assertThat(rows.map { it.date }).containsExactly("8 Oct", "6 Oct", "5 Oct", "4 Oct", "2 Oct").inOrder()
    }

    @Test
    fun amountsCarryTheirSign() {
        assertThat(rows().map { it.amountText }).containsExactly("−1", "−1", "+5", "−1", "+1").inOrder()
    }

    @Test
    fun spendForAGoneApplicationIsGeneric() {
        val rows = rows(applications = emptyList())

        assertThat(rows.first().title).isEqualTo(HistoryTitle.TailoredResume)
        assertThat(rows.first().detail).isEqualTo(HistoryDetail.None)
    }

    @Test
    fun purchaseRowNeverShowsTodaysPlayPrice() {
        val entry = CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, ApplicationPack.APPLICATION_PACK_FIVE, Instant.parse("2026-10-05T09:00:00Z"))

        val row = rows(entries = listOf(entry)).single()

        assertThat(row.title).isEqualTo(HistoryTitle.BoughtCredits(5))
        assertThat(row.detail).isEqualTo(HistoryDetail.None)
    }

    @Test
    fun refundAndMigrationAreGeneric() {
        val at = Instant.parse("2026-10-09T09:00:00Z")
        val rows = rows(
            entries = listOf(
                CreditLedgerEntry(CreditLedgerKind.REFUND, 1, null, null, at),
                CreditLedgerEntry(CreditLedgerKind.MIGRATION, 2, null, null, at),
            ),
        )

        assertThat(rows.map { it.title }).containsExactly(HistoryTitle.Refund, HistoryTitle.CarriedOver)
        assertThat(rows.map { it.amountText }).containsExactly("+1", "+2")
    }

    @Test
    fun dateUsesTheGivenZone() {
        val lateUtc = CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-01T22:30:00Z"))

        val utc = toHistoryRows(listOf(lateUtc), emptyList(), ZoneOffset.UTC).single().date
        val ist = toHistoryRows(listOf(lateUtc), emptyList(), ZoneOffset.ofHoursMinutes(5, 30)).single().date

        assertThat(utc).isEqualTo("1 Oct")
        assertThat(ist).isEqualTo("2 Oct")
    }

    @Test
    fun rowKeysStayUniqueForIdenticalEntries() {
        val at = Instant.parse("2026-10-09T09:00:00Z")
        val twin = CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "nw", null, at)

        val keys = rows(entries = listOf(twin, twin, twin)).map { it.key }

        assertThat(keys.toSet()).hasSize(3)
    }
}
