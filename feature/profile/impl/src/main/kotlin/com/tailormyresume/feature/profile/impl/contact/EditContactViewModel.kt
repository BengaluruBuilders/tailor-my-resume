package com.tailormyresume.feature.profile.impl.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.feature.profile.impl.guidedform.ContactFieldValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
internal class EditContactViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val draft = MutableStateFlow<EditContactDraft?>(null)

    private val channel = Channel<EditContactEvent>(Channel.BUFFERED)

    val events: Flow<EditContactEvent> = channel.receiveAsFlow()

    private var finished = false

    private var busy = false

    val uiState: StateFlow<EditContactUiState> = combine(
        profileRepository.observeProfile(),
        draft,
    ) { profile, contactDraft ->
        if (profile == null || contactDraft == null) {
            EditContactUiState.Loading
        } else {
            EditContactUiState.Editing(contactDraft, profile.email, contactDraft.isValid())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditContactUiState.Loading,
    )

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            draft.compareAndSet(null, profile.toDraft())
        }
    }

    fun onFullNameChange(value: String): Unit = edit { it.copy(fullName = value) }

    fun onPhoneChange(value: String): Unit = edit { it.copy(phone = value) }

    fun onCityChange(value: String): Unit = edit { it.copy(city = value) }

    fun onLinkedinChange(value: String): Unit = edit { it.copy(linkedin = value) }

    fun onPortfolioChange(value: String): Unit = edit { it.copy(portfolio = value) }

    fun save() {
        if (finished || busy) return
        busy = true
        viewModelScope.launch {
            try {
                write()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun write() {
        val contactDraft = draft.value ?: return
        if (!contactDraft.isValid()) return
        val profile = profileRepository.observeProfile().first() ?: return
        finished = true
        profileRepository.saveProfile(
            profile.copy(
                fullName = contactDraft.fullName.trim(),
                phone = contactDraft.phone.trim(),
                city = contactDraft.city.trim(),
                linkedinUrl = contactDraft.linkedin.trim(),
                portfolioUrl = contactDraft.portfolio.trim(),
                contactUserStated = true,
            ),
        )
        channel.send(EditContactEvent.Saved)
    }

    private fun edit(transform: (EditContactDraft) -> EditContactDraft) {
        draft.update { contactDraft -> contactDraft?.let(transform) }
    }
}

private fun CandidateProfile.toDraft() = EditContactDraft(
    fullName = fullName,
    phone = phone,
    city = city,
    linkedin = linkedinUrl,
    portfolio = portfolioUrl,
)

private fun EditContactDraft.isValid(): Boolean =
    fullName.isNotBlank() &&
        (phone.isBlank() || ContactFieldValidator.isValidPhone(phone)) &&
        listOf(fullName, phone, city, linkedin, portfolio).all { it.trim().length <= ProfileLimits.MAX_TEXT_LENGTH }
