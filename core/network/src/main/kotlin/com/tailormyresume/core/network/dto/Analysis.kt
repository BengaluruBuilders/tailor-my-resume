package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisRequest(val jobText: String, val profile: ProfileFactsDto)

@Serializable
data class AllowanceDto(val analysesLeftToday: Int, val day: String, val resetsAt: String)

@Serializable
data class QuestionDto(val requirementId: String, val text: String, val why: String)

@Serializable
data class AnalysisResponse(
    val generationId: String,
    val job: JobDto,
    val matches: List<MatchDto>,
    val allowance: AllowanceDto,
    val question: QuestionDto? = null,
)
