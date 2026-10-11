package com.tailormyresume.feature.profile.impl.listedit

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal sealed interface ListEditUiState {
    data object Loading : ListEditUiState

    data class Editing(
        val section: ProfileListSection,
        val texts: List<String>,
        val canSave: Boolean,
        val canAdd: Boolean,
    ) : ListEditUiState
}

internal enum class ListEditEvent { Saved }

internal class ListEditViewModel(
    private val profileRepository: ProfileRepository,
    private val section: ProfileListSection,
) : ViewModel() {

    val uiState: StateFlow<ListEditUiState> = MutableStateFlow(ListEditUiState.Loading)

    val events: Flow<ListEditEvent> = emptyFlow()

    fun onTextChange(index: Int, value: String) = Unit

    fun onAdd() = Unit

    fun save() = Unit
}
