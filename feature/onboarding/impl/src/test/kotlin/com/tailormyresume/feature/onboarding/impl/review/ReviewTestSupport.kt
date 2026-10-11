package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.data.PrototypeFixtures

internal const val SAMPLE_SOURCE_FILE = "Priya_Deshmukh_Resume.pdf"

internal const val SAMPLE_LINKEDIN = "linkedin.com/in/priya-deshmukh"

private fun CandidateProfile.withPrototypeEducation(): CandidateProfile = copy(
    entries = entries.map { entry ->
        if (entry.category == EntryCategory.EDUCATION) {
            entry.copy(title = "B.Com, Finance", organization = "Symbiosis College, Pune", endDate = "2020")
        } else {
            entry
        }
    },
)

internal fun freshReviewProfile(sourceFileName: String? = SAMPLE_SOURCE_FILE): CandidateProfile =
    PrototypeFixtures.fresh().profile.withPrototypeEducation().copy(sourceFileName = sourceFileName, reviewedAt = null)

internal fun fixedReviewProfile(sourceFileName: String? = SAMPLE_SOURCE_FILE): CandidateProfile =
    PrototypeFixtures.returning().profile.withPrototypeEducation().copy(sourceFileName = sourceFileName, reviewedAt = null)

internal fun importedRole(
    id: String,
    title: String = "Business Analyst",
    organization: String = "Infosys",
    startDate: String = "Jul 2022",
    endDate: String = "Present",
): ProfileEntry = ProfileEntry(
    id, EntryCategory.EXPERIENCE, title, organization, startDate, endDate, emptyList(), FactSource.IMPORTED, isConfirmed = false,
)

internal fun CandidateProfile.withEntries(vararg replacement: ProfileEntry): CandidateProfile = copy(entries = replacement.toList())

internal fun CandidateProfile.reviewState(): ReviewUiState.Ready = toReviewUiState()

internal fun noopReview(
    state: ReviewUiState.Ready,
    modifier: Modifier = Modifier,
): @Composable () -> Unit = {
    ReviewScreen(
        state = state,
        onAddDateClick = {},
        onContactClick = {},
        onExperienceClick = {},
        onSkillsClick = {},
        onContinueClick = {},
        modifier = modifier,
    )
}
