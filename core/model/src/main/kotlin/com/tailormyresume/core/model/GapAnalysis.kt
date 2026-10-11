package com.tailormyresume.core.model

enum class MatchStatus { MET, PARTIAL, GAP }

data class RequirementMatch(
    val requirement: JobRequirement,
    val status: MatchStatus,
    val evidenceIds: List<String>,
    val reason: String? = null,
)

data class KeywordCoverage(val covered: Int, val total: Int)

data class GapAnalysis(
    val matches: List<RequirementMatch>,
    val keywordCoverage: KeywordCoverage,
    val generationId: String? = null,
    val question: QuickQuestion? = null,
)
