package com.tailormyresume.feature.tailor.impl.export

import android.text.StaticLayout
import android.text.TextPaint
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.document.ResumeSection

internal class ResumePdfComposer(
    private val writer: PdfPageWriter,
    private val style: PdfResumeStyle,
) {

    fun compose(document: ResumeDocument) {
        header(document)
        document.sections.forEach(::section)
        skills(document)
        writer.finish()
    }

    private fun header(document: ResumeDocument) {
        block(document.name, style.name)
        block(document.contactLine, style.contact)
        block(document.headline, style.headline)
        block(document.summary, style.body)
    }

    private fun section(section: ResumeSection) {
        heading(section.heading)
        section.entries.forEach(::entry)
    }

    private fun skills(document: ResumeDocument) {
        if (document.skills.isEmpty()) return
        heading(document.skillsHeading)
        block(document.skills.joinToString(", "), style.body)
    }

    private fun heading(text: String) {
        writer.space(SECTION_GAP)
        writer.drawBlock(layout(text, style.sectionHeading), keepWithNext = HEADING_KEEP_WITH_NEXT)
        writer.space(RULE_GAP)
        writer.drawRule(style.rule)
        writer.space(RULE_GAP)
    }

    private fun entry(entry: ResumeEntry) {
        val titleLine = listOf(entry.title, entry.organization).filter { it.isNotEmpty() }.joinToString(", ")
        block(titleLine, style.entryTitle, keepWithNext = ENTRY_KEEP_WITH_NEXT)
        block(entry.dateRange, style.entryDetail, keepWithNext = ENTRY_KEEP_WITH_NEXT)
        entry.bullets.forEach(::bullet)
        writer.space(ENTRY_GAP)
    }

    private fun bullet(text: String) {
        val marker = layout(BULLET_MARKER, style.body, width = BULLET_INDENT.toInt())
        val body = layout(text, style.body, width = writer.contentWidth - BULLET_INDENT.toInt())
        writer.drawHanging(marker, body, BULLET_INDENT)
        writer.space(BULLET_GAP)
    }

    private fun block(text: String, paint: TextPaint, keepWithNext: Float = 0f) {
        if (text.isEmpty()) return
        writer.drawBlock(layout(text, paint), keepWithNext)
    }

    private fun layout(text: String, paint: TextPaint, width: Int = writer.contentWidth): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setLineSpacing(0f, LINE_SPACING)
            .build()

    private companion object {
        const val BULLET_MARKER = "•"
        const val BULLET_INDENT = 14f
        const val LINE_SPACING = 1.12f
        const val SECTION_GAP = 12f
        const val RULE_GAP = 3f
        const val ENTRY_GAP = 6f
        const val BULLET_GAP = 2f
        const val HEADING_KEEP_WITH_NEXT = 48f
        const val ENTRY_KEEP_WITH_NEXT = 30f
    }
}
