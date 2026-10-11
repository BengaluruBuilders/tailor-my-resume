package com.tailormyresume.feature.profile.impl.contact

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal data class EditContactDraft(
    val fullName: String = "",
    val phone: String = "",
    val city: String = "",
    val linkedin: String = "",
    val portfolio: String = "",
)

internal sealed interface EditContactUiState {
    data object Loading : EditContactUiState

    data class Editing(
        val draft: EditContactDraft,
        val email: String,
        val canSave: Boolean,
    ) : EditContactUiState
}

internal enum class EditContactEvent { Saved }

internal class EditContactViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<EditContactUiState> = MutableStateFlow(EditContactUiState.Loading)

    val events: Flow<EditContactEvent> = emptyFlow()

    fun onFullNameChange(value: String) = Unit

    fun onPhoneChange(value: String) = Unit

    fun onCityChange(value: String) = Unit

    fun onLinkedinChange(value: String) = Unit

    fun onPortfolioChange(value: String) = Unit

    fun save() = Unit
}
