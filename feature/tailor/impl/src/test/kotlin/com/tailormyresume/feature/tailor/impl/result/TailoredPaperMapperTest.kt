package com.tailormyresume.feature.tailor.impl.result

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.content.TmrPaperBlock
import com.tailormyresume.core.designsystem.component.content.TmrPaperHighlight
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.document.ResumeSection
import com.tailormyresume.feature.tailor.impl.testBullet
import org.junit.Test

class TailoredPaperMapperTest {

    private val keywordLine = "Rebuilt the reporting pipeline on SQL for three teams"
    private val answerLine = "Worked with senior leaders on roadmap reviews"

    private val keywordBullet = testBullet(
        id = "b-keyword",
        original = "Rebuilt the reporting pipeline for three teams",
        proposed = keywordLine,
        sourceIds = listOf("exp-1-b1"),
        decision = BulletDecision.ACCEPTED,
    ).copy(keywordsUsed = listOf("SQL"))

    private val answerBullet = testBullet(
        id = "b-answer",
        original = "Worked with leads on roadmap reviews",
        proposed = answerLine,
        sourceIds = listOf("ans-q1"),
        decision = BulletDecision.ACCEPTED,
    ).copy(keywordsUsed = listOf("SQL"))

    private fun document(
        bullets: List<String>,
        summary: String = "",
        skills: List<String> = listOf("Kotlin", "SQL"),
        entries: List<ResumeEntry> = listOf(
            ResumeEntry(
                title = "Backend Engineer",
                organization = "Acme",
                dateRange = "2021 - 2024",
                bullets = bullets,
            ),
        ),
    ) = ResumeDocument(
        name = "Priya Sharma",
        contactLine = "priya@example.com",
        headline = "Backend developer",
        skills = skills,
        sections = listOf(
            ResumeSection(
                category = EntryCategory.EXPERIENCE,
                heading = "Experience",
                entries = entries,
            ),
        ),
        skillsHeading = "Skills",
        summary = summary,
    )

    private fun highlights(block: TmrPaperBlock): List<List<TmrPaperHighlight>> =
        block.lines.map { line -> line.map { it.highlight } }

    private fun answerHighlights(blocks: List<TmrPaperBlock>): List<TmrPaperHighlight> =
        blocks.flatMap { it.lines }.flatten().map { it.highlight }.filter { it == TmrPaperHighlight.FromAnswer }

    @Test
    fun highlightsAndCoverageWithAndWithoutAnswer() {
        val document = document(bullets = listOf(keywordLine, answerLine))
        val withAnswer = TailoredResume(bullets = listOf(keywordBullet, answerBullet))

        val blocks = document.toPaperBlocks(withAnswer)
        val entryBlock = blocks.first { it.title != null }

        assertThat(entryBlock.lines).hasSize(2)
        assertThat(highlights(entryBlock)[0])
            .containsExactly(
                TmrPaperHighlight.None,
                TmrPaperHighlight.FromResume,
                TmrPaperHighlight.None,
            ).inOrder()
        assertThat(entryBlock.lines[0].map { it.text }.joinToString("")).isEqualTo(keywordLine)
        assertThat(highlights(entryBlock)[1])
            .containsExactly(TmrPaperHighlight.FromAnswer)
        assertThat(entryBlock.lines[1].map { it.text }.joinToString("")).isEqualTo(answerLine)

        val withoutAnswer = TailoredResume(bullets = listOf(keywordBullet))

        val withoutAnswerBlocks = document.toPaperBlocks(withoutAnswer)

        assertThat(answerHighlights(withoutAnswerBlocks)).isEmpty()
        assertThat(highlights(withoutAnswerBlocks.first { it.title != null })[1])
            .containsExactly(TmrPaperHighlight.None)
        assertThat(withoutAnswerBlocks.flatMap { it.lines }.flatten().map { it.text }.joinToString(""))
            .contains(keywordLine)
    }

    @Test
    fun summaryBlockComesFirstAndIsHighlighted() {
        val summary = "Backend engineer with 4 years of SQL and payments work"
        val document = document(bullets = listOf(keywordLine), summary = summary)
        val resume = TailoredResume(
            bullets = listOf(keywordBullet),
            summary = TailoredText(
                text = summary,
                original = "Backend engineer",
                sourceIds = listOf("src-summary", "ans-q2"),
                decision = BulletDecision.ACCEPTED,
            ),
        )

        val blocks = document.toPaperBlocks(resume)

        assertThat(blocks.first().heading).isNull()
        assertThat(blocks.first().title).isNull()
        assertThat(blocks.first().lines).hasSize(1)
        assertThat(blocks.first().lines[0]).hasSize(1)
        assertThat(blocks.first().lines[0][0].text).isEqualTo(summary)
        assertThat(blocks.first().lines[0][0].highlight).isEqualTo(TmrPaperHighlight.FromAnswer)

        val resumeOnlyFromResume = resume.copy(
            summary = resume.summary?.copy(sourceIds = listOf("src-summary")),
        )

        assertThat(document.toPaperBlocks(resumeOnlyFromResume).first().lines[0][0].highlight)
            .isEqualTo(TmrPaperHighlight.FromResume)

        val withoutSummary = document.copy(summary = "")

        assertThat(withoutSummary.toPaperBlocks(resume).first().title)
            .isEqualTo("Backend Engineer, Acme")
    }

    @Test
    fun skillsBlockIsLastWithDocumentHeading() {
        val document = document(
            bullets = listOf(keywordLine),
            skills = listOf("Kotlin", "SQL", "Kafka"),
        )
        val resume = TailoredResume(bullets = listOf(keywordBullet))

        val blocks = document.toPaperBlocks(resume)

        val last = blocks.last()
        assertThat(last.heading).isEqualTo("Skills")
        assertThat(last.title).isNull()
        assertThat(last.dates).isNull()
        assertThat(last.lines).hasSize(1)
        assertThat(last.lines[0]).hasSize(1)
        assertThat(last.lines[0][0].text).isEqualTo("Kotlin, SQL, Kafka")
        assertThat(last.lines[0][0].highlight).isEqualTo(TmrPaperHighlight.None)

        val noSkills = document.copy(skills = emptyList())

        assertThat(noSkills.toPaperBlocks(resume).last().heading).isEqualTo("Experience")
    }

    @Test
    fun headingOnlyOnFirstEntryOfASection() {
        val first = ResumeEntry(
            title = "Backend Engineer",
            organization = "Acme",
            dateRange = "2021 - 2024",
            bullets = listOf(keywordLine),
        )
        val second = ResumeEntry(
            title = "Consultant",
            organization = "",
            dateRange = "2019 - 2021",
            bullets = listOf("Ran client workshops"),
        )
        val document = document(bullets = emptyList(), entries = listOf(first, second))
        val resume = TailoredResume(bullets = listOf(keywordBullet))

        val blocks = document.toPaperBlocks(resume).filter { it.title != null }

        assertThat(blocks.map { it.heading })
            .containsExactly("Experience", null).inOrder()
        assertThat(blocks.map { it.title })
            .containsExactly("Backend Engineer, Acme", "Consultant").inOrder()
        assertThat(blocks.map { it.dates })
            .containsExactly("2021 - 2024", "2019 - 2021").inOrder()
    }

    @Test
    fun rejectedBulletIsNotHighlighted() {
        val originalLine = "Rebuilt the reporting pipeline for three teams"
        val document = document(bullets = listOf(originalLine))
        val rejected = testBullet(
            id = "b-keyword",
            original = originalLine,
            proposed = keywordLine,
            sourceIds = listOf("exp-1-b1"),
            decision = BulletDecision.REJECTED,
        ).copy(keywordsUsed = listOf("SQL"))

        val blocks = document.toPaperBlocks(TailoredResume(bullets = listOf(rejected)))
        val entryBlock = blocks.first { it.title != null }

        assertThat(entryBlock.lines).hasSize(1)
        assertThat(entryBlock.lines[0]).hasSize(1)
        assertThat(entryBlock.lines[0][0].text).isEqualTo(originalLine)
        assertThat(entryBlock.lines[0][0].highlight).isEqualTo(TmrPaperHighlight.None)
        assertThat(answerHighlights(blocks)).isEmpty()
    }

    @Test
    fun spansAlwaysConcatenateToTheLine() {
        val plainLine = "Owned the on-call rotation for the payments service"
        val document = document(
            bullets = listOf(keywordLine, answerLine, plainLine),
            summary = "Backend engineer with 4 years of SQL work",
            skills = listOf("Kotlin", "SQL"),
        )
        val resume = TailoredResume(
            bullets = listOf(
                keywordBullet,
                answerBullet,
                testBullet(
                    id = "b-plain",
                    original = "Owned the on-call rotation",
                    proposed = plainLine,
                    sourceIds = listOf("exp-1-b2"),
                    decision = BulletDecision.ACCEPTED,
                ).copy(keywordsUsed = listOf("payments", "MISSING-TERM")),
            ),
            summary = TailoredText(
                text = document.summary,
                original = "Backend engineer",
                sourceIds = listOf("ans-q2"),
                decision = BulletDecision.ACCEPTED,
            ),
        )

        val blocks = document.toPaperBlocks(resume)

        val expectedLines = listOf(
            document.summary,
            keywordLine,
            answerLine,
            plainLine,
            "Kotlin, SQL",
        )
        val actualLines = blocks.flatMap { it.lines }.map { line ->
            line.map { it.text }.joinToString("")
        }

        assertThat(actualLines).containsExactlyElementsIn(expectedLines).inOrder()
        assertThat(actualLines).containsNoDuplicates()
        blocks.forEach { block ->
            block.lines.forEach { line ->
                assertThat(line.filter { it.text.isEmpty() }).isEmpty()
            }
        }
    }

    @Test
    fun restoredProfileSummaryIsNotHighlightedButAcceptedTailoredSummaryIs() {
        fun summaryHighlight(decision: BulletDecision) = document(bullets = emptyList(), summary = "Mine")
            .toPaperBlocks(TailoredResume(bullets = emptyList(), summary = TailoredText("New", "Old", decision = decision)))
            .first().lines[0][0].highlight

        assertThat(summaryHighlight(BulletDecision.REJECTED)).isEqualTo(TmrPaperHighlight.None)
        assertThat(summaryHighlight(BulletDecision.PENDING)).isEqualTo(TmrPaperHighlight.None)
        assertThat(summaryHighlight(BulletDecision.ACCEPTED)).isEqualTo(TmrPaperHighlight.FromResume)
    }
}
