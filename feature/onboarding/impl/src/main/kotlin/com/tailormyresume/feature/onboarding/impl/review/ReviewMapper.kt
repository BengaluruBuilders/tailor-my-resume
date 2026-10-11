package com.tailormyresume.feature.onboarding.impl.review

import com.tailormyresume.core.domain.profile.ProfileCompleteness
import com.tailormyresume.core.domain.profile.RequiredGaps
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.logicalEntries

private const val PRESENT = "Present"
private const val SKILL_CHIPS_SHOWN = 5
private const val DETAIL_SEPARATOR = " · "
private const val RANGE_SEPARATOR = " – "

internal fun CandidateProfile.toReviewUiState(): ReviewUiState.Ready =
    ReviewUiState.Ready(
        sourceFileName = sourceFileName?.takeIf(String::isNotBlank),
        percent = ProfileCompleteness.percent(this),
        gap = RequiredGaps.of(this).firstOrNull()?.let { ReviewGap(it.entryId, it.title) },
        showLinkedinRow = linkedinUrl.isBlank(),
        contact = ReviewContact(
            name = fullName,
            email = email,
            phoneAndCity = listOf(phone, city).filter(String::isNotBlank).joinToString(DETAIL_SEPARATOR),
        ),
        roles = entries.logicalEntries(EntryCategory.EXPERIENCE).map { ReviewRole(it.title, it.organization, it.reviewDates()) },
        skills = ReviewSkills(
            count = skills.size,
            chips = skills.take(SKILL_CHIPS_SHOWN),
            more = (skills.size - SKILL_CHIPS_SHOWN).coerceAtLeast(0),
        ),
        education = entries.logicalEntries(EntryCategory.EDUCATION).map { ReviewEducation(it.title, it.educationDetail()) },
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
