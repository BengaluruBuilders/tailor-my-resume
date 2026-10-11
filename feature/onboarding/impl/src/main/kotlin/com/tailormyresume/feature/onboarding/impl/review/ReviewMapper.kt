package com.tailormyresume.feature.onboarding.impl.review

import com.tailormyresume.core.domain.profile.RequiredGaps
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.continues

private const val PRESENT = "Present"
private const val DETAIL_SEPARATOR = " · "
private const val RANGE_SEPARATOR = " – "

internal fun CandidateProfile.toReviewUiState(): ReviewUiState.Ready =
    ReviewUiState.Ready(
        profile = this,
        sourceFileName = sourceFileName?.takeIf(String::isNotBlank),
        gap = RequiredGaps.of(this).firstOrNull()?.let { ReviewGap(it.entryId, it.title) },
        inviteWorkExample = contactUserStated && sourceFileName.isNullOrBlank() && entries.none { it.source == FactSource.IMPORTED } &&
            entries.none { it.category == EntryCategory.EXPERIENCE && it.bullets.any { bullet -> bullet.text.isNotBlank() } },
        contact = ReviewContact(
            name = fullName,
            email = email,
            phoneAndCity = listOf(phone, city).filter(String::isNotBlank).joinToString(DETAIL_SEPARATOR),
        ),
        roles = entries.logicalGroups(EntryCategory.EXPERIENCE).map { group ->
            group.first().let { entry ->
                ReviewRole(entry.title, entry.organization, entry.reviewDates(), group.flatMap { it.bullets }.map { it.text })
            }
        },
        skills = ReviewSkills(
            count = skills.size,
            chips = skills,
        ),
        education = entries.logicalGroups(EntryCategory.EDUCATION).map { group ->
            group.first().let { entry ->
                ReviewEducation(entry.title, entry.educationDetail(), group.flatMap { it.bullets }.map { it.text })
            }
        },
    )

private fun ProfileEntry.reviewDates(): ReviewDates {
    val start = startDate.trim()
    val end = endDate.trim()
    return when {
        start.isEmpty() -> ReviewDates.None
        end.isEmpty() -> ReviewDates.EndMissing(start)
        end.equals(PRESENT, ignoreCase = true) -> ReviewDates.Current(start)
        else -> ReviewDates.Range(start, end)
    }
}

private fun ProfileEntry.educationDetail(): String {
    val years = listOf(startDate, endDate).map(String::trim).filter(String::isNotEmpty).joinToString(RANGE_SEPARATOR)
    return listOf(organization.trim(), years).filter(String::isNotEmpty).joinToString(DETAIL_SEPARATOR)
}

private fun List<ProfileEntry>.logicalGroups(category: EntryCategory): List<List<ProfileEntry>> {
    val groups = mutableListOf<MutableList<ProfileEntry>>()
    forEachIndexed { index, entry ->
        if (entry.category == category) {
            if (index > 0 && entry.continues(this[index - 1])) groups.last().add(entry) else groups += mutableListOf(entry)
        }
    }
    return groups
}
