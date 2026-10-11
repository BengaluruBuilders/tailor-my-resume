package com.tailormyresume.feature.profile.impl.experience

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.profile.RequiredGaps
import com.tailormyresume.core.model.EntryCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal data class ExperienceRow(
    val entryId: String,
    val title: String,
    val company: String,
    val start: String,
    val end: String,
    val isCurrent: Boolean,
    val needsEndDate: Boolean,
)

internal sealed interface ExperienceUiState {
    data object Loading : ExperienceUiState

    data class Content(val rows: List<ExperienceRow>) : ExperienceUiState
}

@HiltViewModel
internal class ExperienceViewModel @Inject constructor(
    profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<ExperienceUiState> = profileRepository.observeProfile().map { profile ->
        val requiredGaps = profile?.let { RequiredGaps.of(it).map { gap -> gap.entryId }.toSet() }.orEmpty()
        val rows = profile?.entries.orEmpty()
            .filter { it.category == EntryCategory.EXPERIENCE }
            .map { entry ->
                ExperienceRow(
                    entryId = entry.id,
                    title = entry.title,
                    company = entry.organization,
                    start = entry.startDate,
                    end = entry.endDate,
                    isCurrent = entry.endDate.trim().equals("Present", ignoreCase = true),
                    needsEndDate = entry.id in requiredGaps,
                )
            }
        ExperienceUiState.Content(rows)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExperienceUiState.Loading,
    )
}
