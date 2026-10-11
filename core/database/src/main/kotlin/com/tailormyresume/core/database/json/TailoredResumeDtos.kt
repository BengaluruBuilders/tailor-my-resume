package com.tailormyresume.core.database.json

import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EditType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GuardrailViolationDto {
    @Serializable
    @SerialName("missing_source")
    data object MissingSource : GuardrailViolationDto

    @Serializable
    @SerialName("unsupported_number")
    data class UnsupportedNumber(val value: String) : GuardrailViolationDto

    @Serializable
    @SerialName("unsupported_term")
    data class UnsupportedTerm(val term: String) : GuardrailViolationDto

    @Serializable
    @SerialName("verb_escalation")
    data class VerbEscalation(val from: String, val to: String) : GuardrailViolationDto

    @Serializable
    @SerialName("unsupported_scale_claim")
    data class UnsupportedScaleClaim(val phrase: String) : GuardrailViolationDto
}

@Serializable
data class TailoredBulletDto(
    val id: String,
    val entryId: String,
    val originalText: String,
    val proposedText: String,
    val sourceIds: List<String>,
    val editTypes: List<EditType>,
    val keywordsUsed: List<String>,
    val violations: List<GuardrailViolationDto>,
    val decision: BulletDecision,
    val generationId: String? = null,
)

@Serializable
data class TailoredTextDto(
    val text: String,
    val original: String,
    val sourceIds: List<String> = emptyList(),
    val violations: List<GuardrailViolationDto> = emptyList(),
    val decision: BulletDecision = BulletDecision.PENDING,
)

@Serializable
data class TailoredSkillsDto(
    val skills: List<String>,
    val original: List<String>,
    val violations: List<GuardrailViolationDto> = emptyList(),
    val decision: BulletDecision = BulletDecision.PENDING,
)

@Serializable
data class TailoredResumeDto(
    val bullets: List<TailoredBulletDto>,
    val entryIds: List<String>? = null,
    val summary: TailoredTextDto? = null,
    val skills: TailoredSkillsDto? = null,
    val runId: String? = null,
)
