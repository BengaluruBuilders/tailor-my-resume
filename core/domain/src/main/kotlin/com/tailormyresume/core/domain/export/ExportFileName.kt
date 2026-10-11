package com.tailormyresume.core.domain.export

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobDescription

enum class ExportFileNameFormat { NAME_COMPANY_ROLE, NAME_ROLE, NAME_RESUME }

object ExportFileName {
    private const val MAX_BASE_LENGTH = 100
    private const val FALLBACK_BASE = "Resume"
    private const val EXTENSION = ".pdf"
    private val nonWord = Regex("[^\\p{L}\\p{M}\\p{N}]+")

    fun build(profile: CandidateProfile, job: JobDescription, format: ExportFileNameFormat): String {
        val parts = when (format) {
            ExportFileNameFormat.NAME_COMPANY_ROLE -> listOf(profile.fullName, job.company, job.title)
            ExportFileNameFormat.NAME_ROLE -> listOf(profile.fullName, job.title)
            ExportFileNameFormat.NAME_RESUME -> listOf(profile.fullName, FALLBACK_BASE)
        }
        val base = parts.map(::hyphenated).filter { it.isNotEmpty() }
            .joinToString("_")
            .take(MAX_BASE_LENGTH)
            .trim('-', '_')
        return base.ifEmpty { FALLBACK_BASE } + EXTENSION
    }

    private fun hyphenated(part: String): String = part.trim().replace(nonWord, "-").trim('-')
}
