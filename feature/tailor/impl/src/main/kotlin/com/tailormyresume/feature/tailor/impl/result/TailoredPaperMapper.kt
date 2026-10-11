package com.tailormyresume.feature.tailor.impl.result

import com.tailormyresume.core.designsystem.component.content.TmrPaperBlock
import com.tailormyresume.core.designsystem.component.content.TmrPaperHighlight
import com.tailormyresume.core.designsystem.component.content.TmrPaperSpan
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument

private const val ANSWER_ID_PREFIX = "ans-"

internal fun ResumeDocument.toPaperBlocks(resume: TailoredResume): List<TmrPaperBlock> =
    buildList<TmrPaperBlock> {
        val acceptedSummary = resume.summary?.takeIf { it.decision == BulletDecision.ACCEPTED }
        val summaryHighlight = when {
            acceptedSummary == null -> TmrPaperHighlight.None
            acceptedSummary.sourceIds.any { it.startsWith(ANSWER_ID_PREFIX) } -> TmrPaperHighlight.FromAnswer
            else -> TmrPaperHighlight.FromResume
        }
        if (summary.isNotBlank()) {
            add(TmrPaperBlock(lines = listOf(listOf(TmrPaperSpan(summary, summaryHighlight)))))
        }
        val applied = resume.bullets.filter {
            it.decision == BulletDecision.ACCEPTED &&
                it.violations.isEmpty() &&
                it.proposedText != it.originalText
        }
        sections.forEach { section ->
            section.entries.forEachIndexed { index, entry ->
                add(
                    TmrPaperBlock(
                        heading = section.heading.takeIf { index == 0 },
                        title = if (entry.organization.isBlank()) {
                            entry.title
                        } else {
                            entry.title + ", " + entry.organization
                        },
                        dates = entry.dateRange.takeIf { it.isNotBlank() },
                        lines = entry.bullets.map { line -> line.toSpans(applied) },
                    ),
                )
            }
        }
        if (skills.isNotEmpty()) {
            add(
                TmrPaperBlock(
                    heading = skillsHeading,
                    lines = listOf(listOf(TmrPaperSpan(skills.joinToString(", ")))),
                ),
            )
        }
    }

private fun String.toSpans(applied: List<TailoredBullet>): List<TmrPaperSpan> {
    if (isEmpty()) return emptyList()
    val bullet = applied.firstOrNull { it.proposedText.trim() == this } ?: return plain()
    if (bullet.sourceIds.any { it.startsWith(ANSWER_ID_PREFIX) }) {
        return listOf(TmrPaperSpan(this, TmrPaperHighlight.FromAnswer))
    }
    return keywordSpans(bullet.keywordsUsed)
}

private fun String.plain(): List<TmrPaperSpan> = listOf(TmrPaperSpan(this))

private fun String.keywordSpans(keywords: List<String>): List<TmrPaperSpan> {
    val ranges = keywords.flatMap { keyword ->
        if (keyword.isEmpty()) {
            emptyList()
        } else {
            occurrencesOf(keyword)
        }
    }.filter { it.first <= it.last }.sortedBy { it.first }
    if (ranges.isEmpty()) return plain()
    val spans = mutableListOf<TmrPaperSpan>()
    var cursor = 0
    ranges.forEach { range ->
        val start = maxOf(range.first, cursor)
        val end = maxOf(range.last, start - 1)
        if (start > cursor) spans += TmrPaperSpan(substring(cursor, start))
        spans += TmrPaperSpan(substring(start, end + 1), TmrPaperHighlight.FromResume)
        cursor = end + 1
    }
    if (cursor < length) spans += TmrPaperSpan(substring(cursor))
    return spans.filter { it.text.isNotEmpty() }
}

private fun String.occurrencesOf(keyword: String): List<IntRange> {
    val haystack = lowercase()
    val needle = keyword.lowercase()
    val found = mutableListOf<IntRange>()
    var index = haystack.indexOf(needle)
    while (index >= 0) {
        found += index until index + needle.length
        index = haystack.indexOf(needle, index + needle.length)
    }
    return found
}
