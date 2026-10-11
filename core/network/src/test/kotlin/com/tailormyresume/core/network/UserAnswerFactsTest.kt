package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.network.mapper.toFactsDto
import org.junit.Test

class UserAnswerFactsTest {

    private fun entry(id: String, source: FactSource) = ProfileEntry(
        id, EntryCategory.EXPERIENCE, "Title", "Org", "2024", "2025",
        listOf(EvidenceBullet("$id-1", "Did a thing")), source, isConfirmed = true,
    )

    @Test
    fun userAnswerFactsAreNeverSentToTheBackend() {
        val profile = CandidateProfile(
            "Priya",
            "p@example.com",
            "+91",
            "Headline",
            listOf("SQL"),
            listOf(entry("E1", FactSource.IMPORTED), entry("E2", FactSource.USER_ANSWER), entry("E3", FactSource.USER_STATED)),
        )

        val sent = profile.toFactsDto().entries

        assertThat(sent.map { it.id }).containsExactly("E1", "E3").inOrder()
        assertThat(sent.map { it.source }).containsNoneIn(listOf(FactSource.USER_ANSWER))
    }

    @Test
    fun userAnswerFactsDoNotConsumeTheEntryLimit() {
        val answers = (1..3).map { entry("A$it", FactSource.USER_ANSWER) }
        val imported = (1..ProfileLimits.MAX_ENTRIES).map { entry("I$it", FactSource.IMPORTED) }
        val profile = CandidateProfile("Priya", "p@example.com", "+91", "Headline", emptyList(), answers + imported)

        assertThat(profile.toFactsDto().entries).hasSize(ProfileLimits.MAX_ENTRIES)
    }
}
