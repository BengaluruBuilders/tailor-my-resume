package com.tailormyresume.core.model

enum class EditType { REWORD, REORDER, SHORTEN, EMPHASISE, MERGE, UNKNOWN }

enum class BulletDecision { PENDING, ACCEPTED, REJECTED }

sealed interface GuardrailViolation {
    data object MissingSource : GuardrailViolation
    data class UnsupportedNumber(val value: String) : GuardrailViolation
    data class UnsupportedTerm(val term: String) : GuardrailViolation
    data class VerbEscalation(val from: String, val to: String) : GuardrailViolation
    data class UnsupportedScaleClaim(val phrase: String) : GuardrailViolation
}

data class TailoredBullet(
    val id: String,
    val entryId: String,
    val originalText: String,
    val proposedText: String,
    val sourceIds: List<String>,
    val editTypes: List<EditType>,
    val keywordsUsed: List<String>,
    val violations: List<GuardrailViolation>,
    val decision: BulletDecision,
    val generationId: String? = null,
)

data class TailoredText(
    val text: String,
    val original: String,
    val sourceIds: List<String> = emptyList(),
    val violations: List<GuardrailViolation> = emptyList(),
    val decision: BulletDecision = BulletDecision.PENDING,
)

data class TailoredSkills(
    val skills: List<String>,
    val original: List<String>,
    val violations: List<GuardrailViolation> = emptyList(),
    val decision: BulletDecision = BulletDecision.PENDING,
)

data class TailoredResume(
    val bullets: List<TailoredBullet>,
    val entryIds: List<String>? = null,
    val summary: TailoredText? = null,
    val skills: TailoredSkills? = null,
    val runId: String? = null,
) {
    val changeCount: Int
        get() = bullets.count { it.proposedText != it.originalText } +
            listOfNotNull(summary).count { it.text != it.original } +
            listOfNotNull(skills).count { it.skills != it.original }

    val decisions: List<BulletDecision>
        get() = bullets.map { it.decision } + listOfNotNull(summary?.decision, skills?.decision)

    fun withPendingAccepted(): TailoredResume = copy(
        bullets = bullets.map { it.copy(decision = it.decision.acceptingPending()) },
        summary = summary?.let { it.copy(decision = it.decision.acceptingPending()) },
        skills = skills?.let { it.copy(decision = it.decision.acceptingPending()) },
    )
}

private fun BulletDecision.acceptingPending(): BulletDecision =
    if (this == BulletDecision.PENDING) BulletDecision.ACCEPTED else this
