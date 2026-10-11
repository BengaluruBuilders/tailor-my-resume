package com.tailormyresume.feature.tailor.impl.export

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.document.ResumeSection
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResumePdfFitTest {

    private val profile = PrototypeFixtures.returning().profile

    private val document: ResumeDocument =
        ResumeDocumentAssembler(TestResumeHeadings).assemble(profile, tailoredResume())

    private fun fit(document: ResumeDocument, size: PageSize) =
        ResumePdfFit.compose(document, size, ::BitmapPdfPages)

    private fun tailoredResume(): TailoredResume = TailoredResume(
        bullets = profile.entries
            .filter { it.id in TAILORED_ENTRY_IDS }
            .flatMap { entry ->
                entry.bullets.map { bullet -> tailoredBullet(entry.id, bullet.id, bullet.text) }
            },
        entryIds = null,
        summary = TailoredText(
            text = "Finance analyst with 4 years of SQL, Power BI and Excel work, presenting insights to senior stakeholders.",
            original = profile.summary,
            decision = BulletDecision.ACCEPTED,
        ),
        skills = TailoredSkills(
            skills = profile.skills,
            original = profile.skills,
            decision = BulletDecision.ACCEPTED,
        ),
    )

    private fun tailoredBullet(entryId: String, bulletId: String, original: String): TailoredBullet =
        TailoredBullet(
            id = bulletId,
            entryId = entryId,
            originalText = original,
            proposedText = when (bulletId) {
                INFOSYS_B1 -> INFOSYS_B1_PROPOSED
                TATA_B1 -> TATA_B1_PROPOSED
                else -> original
            },
            sourceIds = listOf(bulletId),
            editTypes = emptyList(),
            keywordsUsed = emptyList(),
            violations = emptyList(),
            decision = if (bulletId == TATA_B1) BulletDecision.REJECTED else BulletDecision.ACCEPTED,
        )

    private fun expectedLines(document: ResumeDocument): List<String> = buildList {
        listOf(document.name, document.contactLine, document.headline, document.summary)
            .filter { it.isNotEmpty() }
            .forEach(::add)
        document.sections.forEach { section ->
            add(section.heading)
            section.entries.forEach { entry ->
                add(listOf(entry.title, entry.organization).filter { it.isNotEmpty() }.joinToString(", "))
                if (entry.dateRange.isNotEmpty()) add(entry.dateRange)
                entry.bullets.forEach(::add)
            }
        }
        if (document.skills.isNotEmpty()) {
            add(document.skillsHeading)
            add(document.skills.joinToString(", "))
        }
    }

    @Test
    fun prototypeDataIsOnePageInA4AndLetter() {
        listOf(PageSize.A4, PageSize.LETTER).forEach { size ->
            val fitted = fit(document, size)

            assertThat(fitted.pageCount).isEqualTo(1)
            assertThat(fitted.pages.pageSizes).hasSize(1)
        }
    }

    @Test
    fun pagesAreCreatedAtTheSettingsPageSize() {
        assertThat(fit(document, PageSize.A4).pages.pageSizes).containsExactly(595 to 842)
        assertThat(fit(document, PageSize.LETTER).pages.pageSizes).containsExactly(612 to 792)
    }

    @Test
    fun renderedLinesEqualAcceptedDocument() {
        val lines = fit(document, PageSize.A4).lines

        assertThat(lines).containsExactlyElementsIn(expectedLines(document)).inOrder()
        assertThat(lines).contains(INFOSYS_B1_PROPOSED)
        assertThat(lines).contains(TATA_B1_ORIGINAL)
        assertThat(lines).doesNotContain(TATA_B1_PROPOSED)
    }

    @Test
    fun oversizedResumeSpillsToMoreThanOnePage() {
        val oversized = ResumeDocument(
            name = "Priya Deshmukh",
            contactLine = "priya.deshmukh@gmail.com | +91 98200 41736",
            headline = "Finance analyst",
            skills = listOf("SQL", "Power BI"),
            sections = listOf(
                ResumeSection(
                    category = EntryCategory.EXPERIENCE,
                    heading = "Experience",
                    entries = (1..60).map { index ->
                        ResumeEntry(
                            title = "Role $index",
                            organization = "Company $index",
                            dateRange = "Jan 2020 - Dec 2021",
                            bullets = List(3) { "$LONG_BULLET entry $index point ${it + 1}" },
                        )
                    },
                ),
            ),
            skillsHeading = "Skills",
        )

        assertThat(fit(oversized, PageSize.A4).pageCount).isGreaterThan(1)
    }

    private fun documentWithBullets(count: Int) = ResumeDocument(
        name = "Priya Deshmukh",
        contactLine = "priya.deshmukh@gmail.com | +91 98200 41736",
        headline = "Finance analyst",
        skills = listOf("SQL", "Power BI"),
        sections = listOf(
            ResumeSection(
                category = EntryCategory.EXPERIENCE,
                heading = "Experience",
                entries = listOf(
                    ResumeEntry(
                        title = "Business Analyst",
                        organization = "Infosys",
                        dateRange = "Jul 2022 - Present",
                        bullets = List(count) { "$LONG_BULLET point ${it + 1}" },
                    ),
                ),
            ),
        ),
        skillsHeading = "Skills",
    )

    private fun pagesFor(document: ResumeDocument, pass: FitPass): Int {
        val writer = PdfPageWriter(BitmapPdfPages(), PageSize.A4)
        ResumePdfComposer(writer, PdfResumeStyle(pass)).compose(document)
        return writer.pageCount
    }

    @Test
    fun secondPassFitsWhatTheFirstPassCannot() {
        val count = (5..40).first { n ->
            val candidate = documentWithBullets(n)
            pagesFor(candidate, FitPass.Regular) > 1 && pagesFor(candidate, FitPass.Compact) == 1
        }

        assertThat(fit(documentWithBullets(count), PageSize.A4).pageCount).isEqualTo(1)
    }

    private companion object {
        val TAILORED_ENTRY_IDS = setOf("exp-infosys", "exp-tata")
        const val INFOSYS_B1 = "exp-infosys-b1"
        const val TATA_B1 = "exp-tata-b1"
        const val INFOSYS_B1_PROPOSED = "Built Power BI dashboards for monthly financial reporting, cutting prep time by 40%."
        const val TATA_B1_ORIGINAL = "Built forecasting models in Python for category demand."
        const val TATA_B1_PROPOSED = "Trained forecasting models in Python for category demand across 5 business teams."
        const val LONG_BULLET = "Owned the end to end delivery of a cross functional reporting programme, aligned finance, " +
            "sales and operations leaders on one set of numbers, and cut the monthly close by two working days."
    }
}
