package com.tailormyresume.feature.tailor.impl.result

import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.domain.isNamedSkillKeyword
import com.tailormyresume.core.domain.keywordsStatedIn
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText

internal enum class ChangeKind { New, Rewritten, Added, Reordered }

internal enum class ChangeSource { YourResume, YourAnswer }

internal sealed interface ChangeArea {
    data object Summary : ChangeArea

    data object Skills : ChangeArea

    data class Entry(val label: String) : ChangeArea
}

internal data class ChangeCard(
    val id: String,
    val area: ChangeArea,
    val kind: ChangeKind,
    val source: ChangeSource,
    val before: String?,
    val after: String,
    val undone: Boolean,
)

private const val ANSWER_ID_PREFIX = "ans-"
private const val MISSING_ENTRY_LABEL = "Experience"

internal fun TailoredResume.toChangeCards(
    profile: CandidateProfile,
    quickAnswer: QuickAnswer? = null,
    requirements: List<JobRequirement> = emptyList(),
): List<ChangeCard> {
    val entriesById = profile.entries.associateBy { it.id }
    val cards = mutableListOf<ChangeCard>()
    summary?.takeIf { it.text != it.original }?.let { cards.add(it.toSummaryCard()) }
    bullets.filter { it.proposedText != it.originalText }.forEach { cards.add(it.toBulletCard(entriesById)) }
    skills?.takeIf { it.skills != it.original }?.let { cards.add(it.toSkillsCard(quickAnswer, requirements)) }
    return cards
}

private fun TailoredText.toSummaryCard(): ChangeCard {
    val undone = decision == BulletDecision.REJECTED
    return ChangeCard(
        id = UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID,
        area = ChangeArea.Summary,
        kind = if (original.isBlank()) ChangeKind.New else ChangeKind.Rewritten,
        source = sourceOf(sourceIds),
        before = if (undone) null else original.takeIf { it.isNotBlank() },
        after = if (undone) original else text,
        undone = undone,
    )
}

private fun TailoredBullet.toBulletCard(entriesById: Map<String, ProfileEntry>): ChangeCard {
    val undone = decision == BulletDecision.REJECTED
    return ChangeCard(
        id = id,
        area = ChangeArea.Entry(entriesById[entryId].entryLabel()),
        kind = if (originalText.isBlank()) ChangeKind.Added else ChangeKind.Rewritten,
        source = sourceOf(sourceIds),
        before = if (undone) null else originalText.takeIf { it.isNotBlank() },
        after = if (undone && originalText.isNotBlank()) originalText else proposedText,
        undone = undone,
    )
}

private fun TailoredSkills.toSkillsCard(quickAnswer: QuickAnswer?, requirements: List<JobRequirement>): ChangeCard {
    val undone = decision == BulletDecision.REJECTED
    val originalLine = original.joinToString(", ")
    return ChangeCard(
        id = UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID,
        area = ChangeArea.Skills,
        kind = if (skills.sameItemsAs(original)) ChangeKind.Reordered else ChangeKind.Added,
        source = if (answerAddedSkill(quickAnswer, requirements)) ChangeSource.YourAnswer else ChangeSource.YourResume,
        before = if (undone) null else originalLine.takeIf { it.isNotEmpty() },
        after = if (undone) originalLine else skills.joinToString(", "),
        undone = undone,
    )
}

private fun TailoredSkills.answerAddedSkill(quickAnswer: QuickAnswer?, requirements: List<JobRequirement>): Boolean {
    if (quickAnswer == null || quickAnswer.choice !in setOf("YES_REGULARLY", "A_FEW_TIMES")) return false
    val requirement = requirements.firstOrNull { it.id == quickAnswer.requirementId } ?: return false
    val named = requirement.keywords.filter { isNamedSkillKeyword(requirement, it) }
    val detailed = keywordsStatedIn(requirement, quickAnswer.detail)
    val supported = (named + detailed).map(String::lowercase).toSet()
    return skills.filterNot { skill -> original.any { it.equals(skill, ignoreCase = true) } }
        .any { skill -> keywordsStatedIn(requirement, skill).any { it.lowercase() in supported } }
}

private fun List<String>.sameItemsAs(other: List<String>): Boolean =
    size == other.size && map { it.trim().lowercase() }.sorted() == other.map { it.trim().lowercase() }.sorted()

private fun ProfileEntry?.entryLabel(): String = when {
    this == null -> MISSING_ENTRY_LABEL
    organization.isBlank() -> title
    else -> "$title · $organization"
}

private fun sourceOf(sourceIds: List<String>): ChangeSource =
    if (sourceIds.any { it.startsWith(ANSWER_ID_PREFIX) }) ChangeSource.YourAnswer else ChangeSource.YourResume
