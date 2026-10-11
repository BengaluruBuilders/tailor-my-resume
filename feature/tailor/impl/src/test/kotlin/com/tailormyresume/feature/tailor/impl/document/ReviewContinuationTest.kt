package com.tailormyresume.feature.tailor.impl.document

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.feature.tailor.impl.ReviewSection
import com.tailormyresume.feature.tailor.impl.TailorInputs
import com.tailormyresume.feature.tailor.impl.TailorUiState
import com.tailormyresume.feature.tailor.impl.buildTailorUiState
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testEntry
import com.tailormyresume.feature.tailor.impl.testProfile
import org.junit.Test

class ReviewContinuationTest {

    private val first = testBullet("b1", entryId = "W-01", original = "Built a tool", proposed = "Developed a tool", decision = BulletDecision.ACCEPTED)
    private val second = testBullet("b2", entryId = "W-02", original = "Wrote SQL", proposed = "Wrote SQL reports", decision = BulletDecision.ACCEPTED)

    private fun role(id: String, bullet: com.tailormyresume.core.model.TailoredBullet, title: String = "Intern") = testEntry(
        id,
        EntryCategory.EXPERIENCE,
        bullets = listOf(EvidenceBullet(bullet.sourceIds.first(), bullet.originalText)),
        title = title,
        organization = "Acme",
        startDate = "2024",
        endDate = "2025",
    )

    private fun reviewEntries(vararg entries: com.tailormyresume.core.model.ProfileEntry) =
        (
            buildTailorUiState(
                TailorInputs(
                    application = testApplication(listOf(first, second)).copy(
                        tailoredResume = TailoredResume(listOf(first, second), entryIds = entries.map { it.id }),
                    ),
                    profile = testProfile(entries.toList()),
                    editedBulletIds = emptySet(),
                ),
            ) as TailorUiState.Success
            ).sections.filterIsInstance<ReviewSection.Entries>().flatMap { it.entries }

    @Test
    fun continuationEntriesShareOneHeaderInTheReviewLikeTheExport() {
        val profileEntries = arrayOf(role("W-01", first), role("W-02", second))

        val review = reviewEntries(*profileEntries)

        assertThat(review).hasSize(1)
        assertThat(review.single().title).isEqualTo("Intern")
        assertThat(review.single().bullets.map { it.bullet.id }).containsExactly("b1", "b2").inOrder()
        val export = ResumeDocumentAssembler(TestResumeHeadings)
            .assemble(testProfile(profileEntries.toList()), TailoredResume(listOf(first, second), listOf("W-01", "W-02")))
            .sections.flatMap { it.entries }
        assertThat(review.map { it.title to it.bullets.size }).isEqualTo(export.map { it.title to it.bullets.size })
    }

    @Test
    fun differentRolesStayApartInTheReview() {
        val review = reviewEntries(role("W-01", first), role("W-02", second, title = "Analyst"))

        assertThat(review.map { it.title }).containsExactly("Intern", "Analyst").inOrder()
    }
}
