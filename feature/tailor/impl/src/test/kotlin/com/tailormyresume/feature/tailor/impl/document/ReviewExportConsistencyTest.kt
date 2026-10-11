package com.tailormyresume.feature.tailor.impl.document

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.feature.tailor.impl.ReviewSection
import com.tailormyresume.feature.tailor.impl.TailorInputs
import com.tailormyresume.feature.tailor.impl.TailorUiState
import com.tailormyresume.feature.tailor.impl.buildTailorUiState
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testEntry
import com.tailormyresume.feature.tailor.impl.testProfile
import org.junit.Test

class ReviewExportConsistencyTest {

    private val assembler = ResumeDocumentAssembler(TestResumeHeadings)

    private val accepted = testBullet(
        "b1",
        original = "Built a tool",
        proposed = "Developed a tool",
        decision = BulletDecision.ACCEPTED,
    )
    private val kept = testBullet("b2", original = "Wrote SQL", proposed = "Wrote SQL reports", decision = BulletDecision.REJECTED)
    private val pending = testBullet("b3", original = "Ran standups", proposed = "Led standups")

    private val education = testEntry("edu-1", EntryCategory.EDUCATION, title = "BSc Computer Science")
    private val tailoredProfile = testProfile(listOf(entryFor("exp-1", accepted, kept, pending), education))
    private val resume = TailoredResume(listOf(accepted, kept, pending), entryIds = listOf("exp-1", "edu-1"))

    private val profileWithLateFact = testProfile(
        tailoredProfile.entries + testEntry(
            id = "U-01",
            title = "Sales Intern",
            organization = "Saffron Retail",
            bullets = listOf(EvidenceBullet("late-1", "Built daily sales dashboards for 12 stores")),
        ).copy(source = FactSource.USER_STATED),
    )

    private fun reviewEntries(profile: CandidateProfile) =
        (
            buildTailorUiState(
                TailorInputs(
                    application = testApplication(resume.bullets).copy(tailoredResume = resume),
                    profile = profile,
                    editedBulletIds = emptySet(),
                ),
            ) as TailorUiState.Success
            ).sections.filterIsInstance<ReviewSection.Entries>().flatMap { it.entries }

    private fun exportEntries(profile: CandidateProfile) =
        assembler.assemble(profile, resume).sections.flatMap { it.entries }

    @Test
    fun factAddedAfterTailoring_isInNeitherTheReviewNorTheExport() {
        val review = reviewEntries(profileWithLateFact)
        val export = exportEntries(profileWithLateFact)

        assertThat(review.map { it.title }).containsExactly("BSc Computer Science", "Title exp-1")
        assertThat(export.map { it.title }).containsExactly("BSc Computer Science", "Title exp-1")
        assertThat(export.flatMap { it.bullets }).doesNotContain("Built daily sales dashboards for 12 stores")
    }

    @Test
    fun reviewAndExport_showTheSameEntriesAndBullets() {
        val review = reviewEntries(profileWithLateFact)
        val export = exportEntries(profileWithLateFact)

        assertThat(review.map { it.title to it.bullets.size }).isEqualTo(export.map { it.title to it.bullets.size })
        assertThat(export.last().bullets).containsExactly("Developed a tool", "Wrote SQL", "Ran standups").inOrder()
    }

    @Test
    fun bulletAddedLaterToAReviewedEntry_isNotExported() {
        val reviewed = tailoredProfile.entries.first()
        val extended = reviewed.copy(bullets = reviewed.bullets + EvidenceBullet("late-2", "Added after review"))

        val export = exportEntries(testProfile(listOf(extended, education)))

        assertThat(export.single { it.title == "Title exp-1" }.bullets).doesNotContain("Added after review")
    }

    @Test
    fun bulletlessEducationPresentAtTailoringTime_isInTheReviewAndTheExportWithItsDates() {
        val review = reviewEntries(tailoredProfile).single { it.entryId == "edu-1" }
        val export = exportEntries(tailoredProfile).single { it.title == "BSc Computer Science" }

        assertThat(review.bullets).isEmpty()
        assertThat(export.bullets).isEmpty()
        assertThat(export.dateRange).isEqualTo("Jan 2024 - Present")
    }

    @Test
    fun bulletlessEntryAddedAfterTailoring_isInNeitherTheReviewNorTheExport() {
        val profile = testProfile(tailoredProfile.entries + testEntry("edu-2", EntryCategory.EDUCATION, title = "Diploma"))

        assertThat(reviewEntries(profile).map { it.title }).doesNotContain("Diploma")
        assertThat(exportEntries(profile).map { it.title }).doesNotContain("Diploma")
    }

    @Test
    fun resultWithoutRecordedIds_keepsEveryConfirmedEntryLikeBeforeTheFieldExisted() {
        val legacy = resume.copy(entryIds = null)

        val export = assembler.assemble(profileWithLateFact, legacy).sections.flatMap { it.entries }

        assertThat(export.map { it.title }).containsExactly("BSc Computer Science", "Title exp-1", "Sales Intern")
    }
}
