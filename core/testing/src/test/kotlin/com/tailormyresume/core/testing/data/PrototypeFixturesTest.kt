package com.tailormyresume.core.testing.data

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.EntryCategory
import org.junit.Test

class PrototypeFixturesTest {

    @Test
    fun freshAndReturningMatchThePrototype() {
        val fresh = PrototypeFixtures.fresh()
        val returning = PrototypeFixtures.returning()

        listOf(fresh, returning).forEach { scenario ->
            val profile = scenario.profile
            assertThat(profile.fullName).isEqualTo("Priya Deshmukh")
            assertThat(profile.email).isEqualTo("priya.deshmukh@gmail.com")
            assertThat(profile.phone).isEqualTo("+91 98200 41736")
            assertThat(profile.city).isEqualTo("Pune")
            assertThat(profile.linkedinUrl).isEmpty()
            assertThat(profile.skills).containsExactly(
                "SQL", "Excel", "Power BI", "Python", "Forecasting", "Tableau", "Financial modelling",
                "Data cleaning", "Dashboards", "Pandas", "Reconciliation", "Month-end close", "Looker",
                "A/B testing", "Budgeting", "Reporting", "Presenting", "Stakeholder updates",
            ).inOrder()
            assertThat(profile.summary)
                .isEqualTo("Finance analyst with 4 years of SQL, Power BI and Excel work across banking and retail.")

            val roles = profile.entries.filter { it.category == EntryCategory.EXPERIENCE }
            assertThat(roles.map { Triple(it.title, it.organization, it.startDate) }).containsExactly(
                Triple("Business Analyst", "Infosys", "Jul 2022"),
                Triple("Data Analyst Intern", "Tata Digital", "Jun 2021"),
                Triple("Finance Associate", "Bajaj Finserv", "Jun 2020"),
            ).inOrder()
            assertThat(roles[0].endDate).isEqualTo("Present")
            assertThat(roles[2].endDate).isEqualTo("May 2021")
            assertThat(roles[0].bullets.map { it.text }).containsExactly(
                "Built dashboards in Power BI for monthly reports, cut prep time 40%.",
                "Wrote SQL pipelines over 20M+ rows of transaction data.",
            ).inOrder()
            assertThat(roles[1].bullets.map { it.text }).containsExactly(
                "Built forecasting models in Python for category demand.",
                "Automated weekly Excel reports for 5 business teams.",
            ).inOrder()
            assertThat(roles[2].bullets.map { it.text })
                .containsExactly("Reconciled ledgers and prepared month-end close packs.")

            assertThat(profile.entries.filter { it.category == EntryCategory.EDUCATION }.map { it.title })
                .containsExactly("B.Com, Finance · Symbiosis College, Pune · 2020")
            assertThat(profile.entries.filter { it.category == EntryCategory.ACHIEVEMENT }.map { it.title }).containsExactly(
                "Cut monthly reporting prep time by 40%",
                "Built 12 dashboards used by 3 regional teams",
                "Automated reports for 5 business teams",
                "Top performer award, Infosys 2023",
                "Led intern onboarding for 6 hires",
                "Closed books 2 days faster each month",
            ).inOrder()
        }

        assertThat(fresh.profile.entries.first { it.title == "Data Analyst Intern" }.endDate).isEmpty()
        assertThat(returning.profile.entries.first { it.title == "Data Analyst Intern" }.endDate).isEqualTo("May 2022")
        assertThat(fresh.applications).isEmpty()
        assertThat(fresh.ledger).isEmpty()

        assertThat(returning.applications.map { it.id to it.status }).containsExactly(
            "nw" to ApplicationStatus.APPLIED,
            "kb" to ApplicationStatus.INTERVIEW,
            "hr" to ApplicationStatus.SAVED,
        ).inOrder()
        assertThat(returning.applications.first { it.id == "nw" }.appliedOn.toString()).isEqualTo("2026-10-08T09:00:00Z")
        assertThat(returning.applications.first { it.id == "kb" }.appliedOn.toString()).isEqualTo("2026-10-06T09:00:00Z")
        assertThat(returning.applications.first { it.id == "hr" }.appliedOn).isNull()
        assertThat(returning.ledger.map { Triple(it.kind, it.amount, it.createdAt.toString()) }).containsExactly(
            Triple(CreditLedgerKind.FREE_GRANT, 1, "2026-10-02T09:00:00Z"),
            Triple(CreditLedgerKind.SPEND, -1, "2026-10-04T09:00:00Z"),
            Triple(CreditLedgerKind.PURCHASE, 5, "2026-10-05T09:00:00Z"),
            Triple(CreditLedgerKind.SPEND, -1, "2026-10-06T09:00:00Z"),
            Triple(CreditLedgerKind.SPEND, -1, "2026-10-08T09:00:00Z"),
        ).inOrder()
        assertThat(returning.ledger.single { it.kind == CreditLedgerKind.PURCHASE }.productId).isEqualTo("application_pack_5")
        assertThat(returning.ledger.sumOf { it.amount }).isEqualTo(3)
    }
}
