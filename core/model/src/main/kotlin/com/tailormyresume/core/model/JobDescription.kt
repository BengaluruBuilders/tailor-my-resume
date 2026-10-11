package com.tailormyresume.core.model

enum class RequirementType { SKILL, TOOL, EXPERIENCE, EDUCATION, SOFT_SKILL }

enum class RequirementPriority { MUST_HAVE, NICE_TO_HAVE }

data class JobRequirement(
    val id: String,
    val text: String,
    val type: RequirementType,
    val priority: RequirementPriority,
    val keywords: List<String>,
)

data class JobDescription(
    val title: String,
    val company: String,
    val rawText: String,
    val requirements: List<JobRequirement>,
    val location: String? = null,
)
