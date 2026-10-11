package com.tailormyresume.feature.tailor.impl.document

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.evidenceOf
import com.tailormyresume.feature.tailor.impl.sourceBullets
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testEntry
import com.tailormyresume.feature.tailor.impl.testProfile
import org.junit.Test

class ResumeDocumentAssemblerTest {

    private val assembler = ResumeDocumentAssembler(TestResumeHeadings)

    private fun bulletsOf(document: ResumeDocument): List<String> =
        document.sections.flatMap { it.entries }.flatMap { it.bullets }

    private fun assembleBullets(
        bullets: List<TailoredBullet>,
        entry: ProfileEntry,
        entryIds: List<String>? = null,
    ): List<String> =
        bulletsOf(assembler.assemble(testProfile(listOf(entry)), TailoredResume(bullets, entryIds)))

    @Test
    fun acceptedBullet_usesProposedText() {
        val bullet = testBullet("b1", original = "Built a tool", proposed = "Developed a tool", decision = BulletDecision.ACCEPTED)

        assertThat(assembleBullets(listOf(bullet), entryFor("exp-1", bullet))).containsExactly("Developed a tool")
    }

    @Test
    fun rejectedBullet_usesOriginalText() {
        val bullet = testBullet("b1", original = "Built a tool", proposed = "Developed a tool", decision = BulletDecision.REJECTED)

        assertThat(assembleBullets(listOf(bullet), entryFor("exp-1", bullet))).containsExactly("Built a tool")
    }

    @Test
    fun pendingBullet_usesOriginalText() {
        val bullet = testBullet("b1", original = "Built a tool", proposed = "Developed a tool")

        assertThat(assembleBullets(listOf(bullet), entryFor("exp-1", bullet))).containsExactly("Built a tool")
    }

    @Test
    fun acceptedBulletWithViolation_stillUsesOriginalText() {
        val bullet = testBullet(
            id = "b1",
            original = "Helped a team",
            proposed = "Led a team",
            violations = listOf(GuardrailViolation.VerbEscalation("Helped", "Led")),
            decision = BulletDecision.ACCEPTED,
        )

        assertThat(assembleBullets(listOf(bullet), entryFor("exp-1", bullet))).containsExactly("Helped a team")
    }

    @Test
    fun unchangedBullet_keepsItsText() {
        val bullet = testBullet("b1", original = "Wrote SQL", proposed = "Wrote SQL")

        assertThat(assembleBullets(listOf(bullet), entryFor("exp-1", bullet))).containsExactly("Wrote SQL")
    }

    @Test
    fun editedSource_exportsCurrentTextEvenWhenAccepted() {
        val bullet = testBullet("b1", original = "Led team of 5", proposed = "Led a team of 5", decision = BulletDecision.ACCEPTED)
        val editedEntry = testEntry("exp-1", bullets = listOf(EvidenceBullet("src-b1", "Worked with a team")))

        assertThat(assembleBullets(listOf(bullet), editedEntry)).containsExactly("Worked with a team")
    }

    @Test
    fun deletedSource_dropsTheTailoredBullet() {
        val kept = testBullet("b1", original = "Kept", proposed = "Kept and improved", decision = BulletDecision.ACCEPTED)
        val deleted = testBullet("b2", original = "Led team of 5", proposed = "Led a team of 5", decision = BulletDecision.ACCEPTED)
        val entry = testEntry("exp-1", bullets = listOf(evidenceOf(kept)))

        assertThat(assembleBullets(listOf(kept, deleted), entry)).containsExactly("Kept and improved")
    }

    @Test
    fun addedBullet_isLeftOutBecauseTheReviewNeverShowedIt() {
        val tailored = testBullet("b1", original = "Built a tool", proposed = "Developed a tool", decision = BulletDecision.ACCEPTED)
        val entry = testEntry("exp-1", bullets = listOf(evidenceOf(tailored), EvidenceBullet("new", "Added later")))

        assertThat(assembleBullets(listOf(tailored), entry)).containsExactly("Developed a tool")
    }

    @Test
    fun entryWithoutTailoredBullets_isLeftOut() {
        val other = testBullet("b1", entryId = "exp-2", original = "Built a tool", proposed = "Developed a tool", decision = BulletDecision.ACCEPTED)
        val entry = testEntry("exp-1", bullets = listOf(evidenceOf(other)))

        assertThat(assembleBullets(listOf(other), entry, entryIds = emptyList())).isEmpty()
    }

    @Test
    fun acceptedReorder_movesTheBulletToTheTailoredPosition() {
        val first = testBullet("b1", original = "First", proposed = "First")
        val second = testBullet("b2", original = "Second", proposed = "Second")
        val moved = testBullet("b3", original = "Third", proposed = "Third", decision = BulletDecision.ACCEPTED, editTypes = listOf(EditType.REORDER))
        val entry = entryFor("exp-1", first, second, moved)

        assertThat(assembleBullets(listOf(moved, first, second), entry))
            .containsExactly("Third", "First", "Second")
            .inOrder()
    }

    @Test
    fun rejectedReorder_keepsTheProfilePosition() {
        val first = testBullet("b1", original = "First", proposed = "First")
        val second = testBullet("b2", original = "Second", proposed = "Second")
        val moved = testBullet("b3", original = "Third", proposed = "Third", decision = BulletDecision.REJECTED, editTypes = listOf(EditType.REORDER))
        val entry = entryFor("exp-1", first, second, moved)

        assertThat(assembleBullets(listOf(moved, first, second), entry))
            .containsExactly("First", "Second", "Third")
            .inOrder()
    }

    @Test
    fun pendingReorder_keepsTheProfilePosition() {
        val first = testBullet("b1", original = "First", proposed = "First")
        val moved = testBullet("b2", original = "Second", proposed = "Second", editTypes = listOf(EditType.REORDER))
        val entry = entryFor("exp-1", first, moved)

        assertThat(assembleBullets(listOf(moved, first), entry)).containsExactly("First", "Second").inOrder()
    }

    @Test
    fun acceptedReorderWithRewording_usesProposedTextAtTheNewPosition() {
        val first = testBullet("b1", original = "First", proposed = "First")
        val moved = testBullet(
            id = "b2",
            original = "Second",
            proposed = "Second, improved",
            decision = BulletDecision.ACCEPTED,
            editTypes = listOf(EditType.REORDER, EditType.REWORD),
        )
        val entry = entryFor("exp-1", first, moved)

        assertThat(assembleBullets(listOf(moved, first), entry))
            .containsExactly("Second, improved", "First")
            .inOrder()
    }

    @Test
    fun acceptedReorderWithViolation_keepsTheProfilePosition() {
        val first = testBullet("b1", original = "First", proposed = "First")
        val moved = testBullet(
            id = "b2",
            original = "Second",
            proposed = "Second",
            violations = listOf(GuardrailViolation.MissingSource),
            decision = BulletDecision.ACCEPTED,
            editTypes = listOf(EditType.REORDER),
        )
        val entry = entryFor("exp-1", first, moved)

        assertThat(assembleBullets(listOf(moved, first), entry)).containsExactly("First", "Second").inOrder()
    }

    @Test
    fun unconfirmedEntry_isLeftOut() {
        val kept = testBullet("b1", entryId = "exp-1", original = "Kept", proposed = "Kept")
        val hidden = testBullet("b2", entryId = "exp-2", original = "Hidden", proposed = "Hidden")
        val profile = testProfile(
            listOf(
                entryFor("exp-1", kept),
                testEntry("exp-2", isConfirmed = false, bullets = listOf(evidenceOf(hidden))),
            ),
        )

        val document = assembler.assemble(profile, TailoredResume(listOf(kept, hidden)))

        assertThat(document.sections.flatMap { it.entries }.map { it.title }).containsExactly("Title exp-1")
        assertThat(bulletsOf(document)).containsExactly("Kept")
    }

    private fun reviewedEntry(id: String, category: EntryCategory, startDate: String = "Jan 2024", endDate: String = "Present") =
        testBullet("b-$id", entryId = id, original = "Text $id", proposed = "Text $id").let { bullet ->
            testEntry(id, category, bullets = listOf(evidenceOf(bullet)), startDate = startDate, endDate = endDate) to bullet
        }

    @Test
    fun sections_followStandardOrderAndSkipEmptyOnes() {
        val pairs = listOf(
            reviewedEntry("ach", EntryCategory.ACHIEVEMENT),
            reviewedEntry("edu", EntryCategory.EDUCATION),
            reviewedEntry("proj", EntryCategory.PROJECT),
            reviewedEntry("exp", EntryCategory.EXPERIENCE),
            reviewedEntry("cert", EntryCategory.CERTIFICATION),
        )

        val document = assembler.assemble(testProfile(pairs.map { it.first }), TailoredResume(pairs.map { it.second }))

        assertThat(document.sections.map { it.heading })
            .containsExactly(
                "Heading education",
                "Heading experience",
                "Heading project",
                "Heading certification",
                "Heading achievement",
            )
            .inOrder()
    }

    @Test
    fun skillsHeading_comesFromTheHeadings() {
        val document = assembler.assemble(testProfile(emptyList()), TailoredResume(emptyList()))

        assertThat(document.skillsHeading).isEqualTo("Heading skills")
    }

    @Test
    fun categoryWithoutReviewedEntries_hasNoSection() {
        val (edu, eduBullet) = reviewedEntry("edu", EntryCategory.EDUCATION)

        val document = assembler.assemble(testProfile(listOf(edu, testEntry("exp"))), TailoredResume(listOf(eduBullet), entryIds = listOf("edu")))

        assertThat(document.sections.map { it.category }).containsExactly(EntryCategory.EDUCATION)
    }

    @Test
    fun entryWithoutTailoredBullets_isLeftOutEvenWhenTheProfileHasBullets() {
        val profile = testProfile(listOf(testEntry("edu", EntryCategory.EDUCATION, bullets = sourceBullets("a", "b"))))

        val document = assembler.assemble(profile, TailoredResume(emptyList(), entryIds = emptyList()))

        assertThat(document.sections).isEmpty()
    }

    @Test
    fun header_carriesNameContactHeadlineAndSkills() {
        val profile = testProfile(emptyList(), skills = listOf("Kotlin", " kotlin ", "", "SQL", "SQL"))

        val document = assembler.assemble(profile, TailoredResume(emptyList()))

        assertThat(document.name).isEqualTo("Priya Sharma")
        assertThat(document.contactLine).isEqualTo("priya@example.com | +91 98765 43210")
        assertThat(document.headline).isEqualTo("Backend developer")
        assertThat(document.skills).containsExactly("Kotlin", "SQL").inOrder()
    }

    @Test
    fun dateRange_skipsBlankParts() {
        val pairs = listOf(
            reviewedEntry("edu", EntryCategory.EDUCATION, startDate = "2021", endDate = "2025"),
            reviewedEntry("proj", EntryCategory.PROJECT, startDate = "", endDate = "Apr 2025"),
            reviewedEntry("cert", EntryCategory.CERTIFICATION, startDate = "", endDate = ""),
        )

        val document = assembler.assemble(testProfile(pairs.map { it.first }), TailoredResume(pairs.map { it.second }))

        assertThat(document.sections.map { it.entries.single().dateRange })
            .containsExactly("2021 - 2025", "Apr 2025", "")
            .inOrder()
    }

    @Test
    fun blankBulletText_isDropped() {
        val entry = testEntry("exp-1", bullets = listOf(EvidenceBullet("src-b1", "  ")))

        assertThat(assembleBullets(emptyList(), entry)).isEmpty()
    }

    @Test
    fun onlyAcceptedSummaryAndSkillsReachTheDocument() {
        val profile = testProfile(emptyList()).copy(skills = listOf("SQL", "Excel"))
        fun assembled(decision: BulletDecision) = assembler.assemble(
            profile,
            TailoredResume(
                emptyList(),
                summary = TailoredText("New summary", "Old", decision = decision),
                skills = TailoredSkills(listOf("Excel"), listOf("SQL", "Excel"), decision = decision),
            ),
        )

        assertThat(assembled(BulletDecision.ACCEPTED).summary).isEqualTo("New summary")
        assertThat(assembled(BulletDecision.ACCEPTED).skills).containsExactly("Excel")
        listOf(BulletDecision.PENDING, BulletDecision.REJECTED).forEach { decision ->
            assertThat(assembled(decision).summary).isEmpty()
            assertThat(assembled(decision).skills).containsExactly("SQL", "Excel").inOrder()
        }
    }
}
