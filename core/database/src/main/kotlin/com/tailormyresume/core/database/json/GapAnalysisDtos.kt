package com.tailormyresume.core.database.json

import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import kotlinx.serialization.Serializable

@Serializable
data class JobRequirementDto(
    val id: String,
    val text: String,
    val type: RequirementType,
    val priority: RequirementPriority,
    val keywords: List<String>,
)

@Serializable
data class RequirementMatchDto(
    val requirement: JobRequirementDto,
    val status: MatchStatus,
    val evidenceIds: List<String>,
    val reason: String? = null,
)

@Serializable
data class KeywordCoverageDto(
    val covered: Int,
    val total: Int,
)

@Serializable
data class QuickQuestionDto(
    val requirementId: String,
    val text: String,
    val why: String,
)

@Serializable
data class GapAnalysisDto(
    val matches: List<RequirementMatchDto>,
    val keywordCoverage: KeywordCoverageDto,
    val generationId: String? = null,
    val question: QuickQuestionDto? = null,
)
