package com.tailormyresume.feature.profile.impl.skills

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal sealed interface SkillsUiState {
    data object Loading : SkillsUiState

    data class Editing(
        val skills: List<String>,
        val newSkill: String,
        val canAdd: Boolean,
    ) : SkillsUiState
}

internal enum class SkillsEvent { Saved }

internal class SkillsViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<SkillsUiState> = MutableStateFlow(SkillsUiState.Loading)

    val events: Flow<SkillsEvent> = emptyFlow()

    fun onNewSkillChange(value: String) = Unit

    fun onAdd() = Unit

    fun onRemove(skill: String) = Unit

    fun save() = Unit
}
