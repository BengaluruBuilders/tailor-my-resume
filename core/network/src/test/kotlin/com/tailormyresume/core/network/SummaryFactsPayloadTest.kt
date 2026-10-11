package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.network.dto.ProfileFactsDto
import com.tailormyresume.core.network.mapper.toFactsDto
import org.junit.Test

class SummaryFactsPayloadTest {
    private val json = tailormyresumeJson()
    private val idRule = Regex("^[A-Za-z0-9:_.-]{1,64}$")

    private fun entry(id: String, bulletId: String) = ProfileEntry(
        id, EntryCategory.EXPERIENCE, "Analyst", "Org", "2024", "Present",
        listOf(EvidenceBullet(bulletId, "Did a thing")), FactSource.IMPORTED, isConfirmed = true,
    )

    private fun profile(summary: String, entries: List<ProfileEntry> = emptyList()) = CandidateProfile(
        fullName = "Priya Deshmukh",
        email = "priya@example.com",
        phone = "+91 98123 45610",
        headline = "Data analyst",
        skills = listOf("SQL"),
        entries = entries,
        city = "Pune",
        linkedinUrl = "https://www.linkedin.com/in/priya",
        portfolioUrl = "https://priya.example.com",
        summary = summary,
    )

    @Test
    fun summaryIdAndOmissionRules() {
        val sent = profile("Data analyst with 2 years in retail operations.").toFactsDto().summary

        assertThat(sent?.text).isEqualTo("Data analyst with 2 years in retail operations.")
        assertThat(sent?.id).matches(idRule.toPattern())
        assertThat(sent?.id?.startsWith("ans-")).isFalse()
        assertThat(profile("   ").toFactsDto().summary).isNull()
        assertThat(profile("x".repeat(1_001)).toFactsDto().summary).isNull()
        assertThat(profile("x".repeat(1_000)).toFactsDto().summary).isNotNull()
    }

    @Test
    fun summaryIdCollidesWithNoEntryOrBulletId() {
        val clashing = listOf(entry("summary", "summary-1"), entry("E2", "summary-2"))

        val id = profile("A summary.", clashing).toFactsDto().summary?.id

        assertThat(id).isNotIn(listOf("summary", "summary-1", "summary-2", "E2"))
        assertThat(id).matches(idRule.toPattern())
    }

    @Test
    fun payloadCarriesNoPersonalDataOrLinks() {
        val wire = json.encodeToString(
            ProfileFactsDto.serializer(),
            profile("Data analyst.", listOf(entry("E1", "E1-1"))).toFactsDto(),
        )

        listOf("Priya", "priya@example.com", "98123", "Pune", "linkedin", "priya.example.com", "USER_ANSWER", "ans-")
            .forEach { assertThat(wire).doesNotContain(it) }
        assertThat(wire).contains(""""summary":{"id":"summary","text":"Data analyst."}""")
    }

    @Test
    fun blankSummaryLeavesNoKey() {
        val wire = json.encodeToString(ProfileFactsDto.serializer(), profile("").toFactsDto())

        assertThat(wire).doesNotContain("summary")
    }
}
