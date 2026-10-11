package com.tailormyresume.feature.tailor.impl

import com.tailormyresume.core.domain.fact.FactDisplayIds
import com.tailormyresume.core.domain.prep.RequirementPhrase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.continues
import com.tailormyresume.feature.tailor.impl.document.isFreshFor
import com.tailormyresume.feature.tailor.impl.document.reviewedEntries

internal data class TailorInputs(
    val application: JobApplication?,
    val profile: CandidateProfile?,
    val editedBulletIds: Set<String>,
    val reportedIds: Set<String> = emptySet(),
)

internal fun buildTailorUiState(inputs: TailorInputs): TailorUiState {
    val application = inputs.application
    val profile = inputs.profile
    val resume = application?.tailoredResume
    if (application == null || profile == null || resume == null) return TailorUiState.NotFound
    val entries = buildEntries(profile, resume, inputs.editedBulletIds)
    val sections = buildSections(entries, profile.skills)
    return TailorUiState.Success(
        job = JobHeader(title = application.job.title, company = application.job.company),
        sections = sections,
        notAdded = application.gapAnalysis?.matches
            ?.filter { it.status == MatchStatus.GAP }
            ?.map { RequirementPhrase.of(it.requirement.text) }
            ?.filter { it.isNotEmpty() }
            .orEmpty(),
        changes = entries.flatMap { it.bullets }.filter { it.isChange },
        reportedIds = inputs.reportedIds,
    )
}

private fun buildSections(entries: List<TailorEntryUi>, skills: List<String>): List<ReviewSection> {
    val skillList = skills.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
    val byCategory = entries.groupBy { it.category }
    val body = EntryCategory.entries.mapNotNull { category ->
        byCategory[category]?.let { ReviewSection.Entries(category, it) }
    }
    val skillsSection = listOfNotNull(skillList.takeIf { it.isNotEmpty() }?.let { ReviewSection.Skills(it) })
    return body + skillsSection
}

private fun buildEntries(
    profile: CandidateProfile,
    resume: TailoredResume,
    editedBulletIds: Set<String>,
): List<TailorEntryUi> {
    val reviewed = reviewedEntries(profile, resume)
    val sourceById = reviewed
        .flatMap { entry ->
            val displayId = FactDisplayIds.of(entry, profile.entries)
            entry.bullets.map { bullet -> bullet.id to entry.sourceOf(bullet.id, displayId, bullet.text) }
        }
        .toMap()
    val bulletsByEntry = resume.bullets.groupBy { it.entryId }
    val merged = mutableListOf<Pair<ProfileEntry, TailorEntryUi>>()
    val headByCategory = mutableMapOf<EntryCategory, Int>()
    reviewed.forEach { entry ->
        val ui = entry.toUi(bulletsByEntry[entry.id].orEmpty(), sourceById, editedBulletIds)
        val headIndex = headByCategory[entry.category]
        val head = headIndex?.let { merged[it] }
        if (headIndex != null && head != null && entry.continues(head.first)) {
            merged[headIndex] = head.first to head.second.copy(bullets = head.second.bullets + ui.bullets)
        } else {
            headByCategory[entry.category] = merged.size
            merged += entry to ui
        }
    }
    return merged.map { it.second }
}

private fun ProfileEntry.sourceOf(id: String, displayId: String, text: String): TailoredBulletSource =
    TailoredBulletSource(
        id = id,
        displayId = displayId,
        entryId = this.id,
        category = category,
        text = text,
        source = source,
        entryTitle = title.trim(),
        organization = organization.trim(),
        dateRange = listOf(startDate, endDate).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" - "),
    )

private fun ProfileEntry.toUi(
    tailored: List<TailoredBullet>,
    sourceById: Map<String, TailoredBulletSource>,
    editedBulletIds: Set<String>,
): TailorEntryUi = TailorEntryUi(
    entryId = id,
    category = category,
    title = title.trim(),
    organization = organization.trim(),
    dateRange = listOf(startDate, endDate).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" - "),
    bullets = tailored.map { bullet ->
        TailorBulletUi(
            bullet = bullet,
            sources = bullet.sourceIds.mapNotNull { sourceById[it] },
            isStale = !bullet.isFreshFor(this),
            isUserEdited = bullet.id in editedBulletIds,
        )
    },
)
