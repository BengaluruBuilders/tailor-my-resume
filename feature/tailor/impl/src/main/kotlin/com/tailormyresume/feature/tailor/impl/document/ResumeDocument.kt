package com.tailormyresume.feature.tailor.impl.document

import com.tailormyresume.core.model.EntryCategory

internal data class ResumeDocument(
    val name: String,
    val contactLine: String,
    val headline: String,
    val skills: List<String>,
    val sections: List<ResumeSection>,
    val skillsHeading: String,
    val summary: String = "",
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

internal data class ResumeSection(
    val category: EntryCategory,
    val heading: String,
    val entries: List<ResumeEntry>,
)

internal data class ResumeEntry(
    val title: String,
    val organization: String,
    val dateRange: String,
    val bullets: List<String>,
)
