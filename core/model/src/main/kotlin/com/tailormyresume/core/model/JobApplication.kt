package com.tailormyresume.core.model

import kotlin.time.Instant

enum class ApplicationStatus { SAVED, APPLIED, INTERVIEW, OFFER, REJECTED }

data class ApplicationKeywordCoverage(val now: Int, val upTo: Int, val final: Int? = null)

data class QuickAnswer(val requirementId: String, val choice: String, val detail: String = "")

data class JobApplication(
    val id: String,
    val job: JobDescription,
    val status: ApplicationStatus,
    val gapAnalysis: GapAnalysis?,
    val tailoredResume: TailoredResume?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val location: String = "",
    val appliedOn: Instant? = null,
    val keywordCoverage: ApplicationKeywordCoverage? = null,
    val exportFileName: String? = null,
    val quickAnswer: QuickAnswer? = null,
    val changesAcceptedAt: Instant? = null,
    val legacyNotes: String = "",
    val legacyStatus: String? = null,
)
