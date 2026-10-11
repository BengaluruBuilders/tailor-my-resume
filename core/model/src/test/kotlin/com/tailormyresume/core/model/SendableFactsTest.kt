package com.tailormyresume.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SendableFactsTest {
    private fun entry(id: String, source: FactSource, bullets: Int) = ProfileEntry(
        id = id,
        category = EntryCategory.EXPERIENCE,
        title = "t",
        organization = "o",
        startDate = "",
        endDate = "",
        bullets = List(bullets) { EvidenceBullet("$id-b$it", "text $it") },
        source = source,
        isConfirmed = true,
    )

    private fun profile(vararg entries: ProfileEntry) = CandidateProfile(
        fullName = "n",
        email = "e",
        phone = "",
        headline = "",
        skills = listOf("SQL"),
        entries = entries.toList(),
    )

    @Test
    fun answerEntriesAreNeverSendableAndDoNotUseUpTheWindow() {
        val answers = entry("ans", FactSource.USER_ANSWER, 15)
        val imported = (1..14).map { entry("e$it", FactSource.IMPORTED, 15) }

        val sendable = profile(answers, *imported.toTypedArray()).sendableFacts()

        assertEquals(false, sendable.entries.any { it.id == "ans" })
        assertEquals(ProfileLimits.MAX_BULLETS_IN_TOTAL, sendable.entries.sumOf { it.bullets.size })
    }

    @Test
    fun evidenceIdsOfSendableFactsExcludeAnswerBulletsAndKeepSkills() {
        val sendable = profile(entry("ans", FactSource.USER_ANSWER, 1), entry("e1", FactSource.USER_STATED, 1)).sendableFacts()

        assertEquals(setOf("e1", "e1-b0", "skill:SQL"), sendable.evidenceIds())
    }
}
