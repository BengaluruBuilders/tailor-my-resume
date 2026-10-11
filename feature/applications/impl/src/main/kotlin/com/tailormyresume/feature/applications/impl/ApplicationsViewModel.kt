package com.tailormyresume.feature.applications.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class ApplicationsViewModel @Inject constructor(
    applicationRepository: ApplicationRepository,
    creditsRepository: CreditsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val state: StateFlow<ApplicationsUiState> = combine(
        applicationRepository.observeApplications(),
        creditsRepository.observeKnownBalance(),
        profileRepository.observeProfile(),
        ::stateFor,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ApplicationsUiState.Loading,
    )

    private fun stateFor(
        applications: List<JobApplication>,
        credits: Int?,
        profile: CandidateProfile?,
    ): ApplicationsUiState {
        val header = ApplicationsHeader(credits = credits?.coerceAtLeast(0), initials = initialsOf(profile?.fullName.orEmpty()))
        if (applications.isEmpty()) return ApplicationsUiState.Empty(header)
        val rows = applications.sortedByDescending { it.updatedAt }.map(::rowFor)
        return ApplicationsUiState.Populated(
            header = header,
            total = rows.size,
            statusCounts = rows.groupingBy { it.status }.eachCount(),
            rows = rows,
        )
    }

    private fun rowFor(application: JobApplication) = ApplicationRow(
        id = application.id,
        title = application.job.title,
        company = application.job.company,
        keywordPercent = application.keywordCoverage.keywordPercent(),
        initial = (application.job.company.ifBlank { application.job.title }).trim().take(1).uppercase(),
        colorIndex = Math.floorMod(application.id.hashCode(), DISC_COLOR_COUNT),
        status = application.status.toTmrStatus(),
    )

    private fun initialsOf(fullName: String): String {
        val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull())
            .joinToString("") { it.take(1) }
            .uppercase()
    }
}
