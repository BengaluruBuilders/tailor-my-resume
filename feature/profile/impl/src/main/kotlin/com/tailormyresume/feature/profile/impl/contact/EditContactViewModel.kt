package com.tailormyresume.feature.profile.impl.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.model.AccountMode
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
    val email: String = "",
)

internal sealed interface EditContactUiState {
    data object Loading : EditContactUiState

    data class Editing(
        val draft: EditContactDraft,
        val email: String,
        val canSave: Boolean,
        val emailFromGoogle: Boolean = true,
        val emailEditable: Boolean = false,
    ) : EditContactUiState
}

internal enum class EditContactEvent { Saved }

@HiltViewModel
internal class EditContactViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) : ViewModel() {
    private val workTicket = workFence.ticket()

    private val draft = MutableStateFlow<EditContactDraft?>(null)

    private val channel = Channel<EditContactEvent>(Channel.BUFFERED)

    val events: Flow<EditContactEvent> = channel.receiveAsFlow()

    private var finished = false

    private var busy = false

    val uiState: StateFlow<EditContactUiState> = combine(
        profileRepository.observeProfile(),
        draft,
        sessionRepository.observeAccount(),
    ) { profile, contactDraft, account ->
        if (profile == null || contactDraft == null) {
            EditContactUiState.Loading
        } else {
            EditContactUiState.Editing(
                draft = contactDraft,
                email = profile.email,
                canSave = contactDraft.isValid() && (account?.mode != AccountMode.ANONYMOUS || contactDraft.email.isBlank() || ContactFieldValidator.isValidEmail(contactDraft.email)),
                emailFromGoogle = account?.mode == AccountMode.GOOGLE && profile.email.equals(account.email, ignoreCase = true),
                emailEditable = account?.mode == AccountMode.ANONYMOUS,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditContactUiState.Loading,
    )

    init {
        viewModelScope.launch(workFence.context(workTicket)) {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            draft.compareAndSet(null, profile.toDraft())
        }
    }

    fun onFullNameChange(value: String): Unit = edit { it.copy(fullName = value) }

    fun onEmailChange(value: String): Unit = edit { it.copy(email = value) }

    fun onPhoneChange(value: String): Unit = edit { it.copy(phone = value) }

    fun onCityChange(value: String): Unit = edit { it.copy(city = value) }

    fun onLinkedinChange(value: String): Unit = edit { it.copy(linkedin = value) }

    fun onPortfolioChange(value: String): Unit = edit { it.copy(portfolio = value) }

    fun save() {
        if (finished || busy) return
        busy = true
        viewModelScope.launch(workFence.context(workTicket)) {
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
        val email = if (sessionRepository.observeAccount().first()?.mode == AccountMode.ANONYMOUS) contactDraft.email.trim() else profile.email
        if (email.isNotBlank() && !ContactFieldValidator.isValidEmail(email)) return
        finished = true
        workFence.check(workTicket)
        profileRepository.saveProfile(
            profile.copy(
                fullName = contactDraft.fullName.trim(),
                email = email,
                phone = contactDraft.phone.trim(),
                city = contactDraft.city.trim(),
                linkedinUrl = contactDraft.linkedin.trim(),
                portfolioUrl = contactDraft.portfolio.trim(),
                contactUserStated = true,
            ),
        )
        workFence.editing(this, false)
        channel.send(EditContactEvent.Saved)
    }

    private fun edit(transform: (EditContactDraft) -> EditContactDraft) {
        workFence.editing(this, true)
        draft.update { contactDraft -> contactDraft?.let(transform) }
    }
    override fun onCleared() {
        workFence.editing(this, false)
        super.onCleared()
    }
}

private fun CandidateProfile.toDraft() = EditContactDraft(
    fullName = fullName,
    email = email,
    phone = phone,
    city = city,
    linkedin = linkedinUrl,
    portfolio = portfolioUrl,
)

private fun EditContactDraft.isValid(): Boolean =
    fullName.isNotBlank() &&
        (phone.isBlank() || ContactFieldValidator.isValidPhone(phone)) &&
        listOf(fullName, phone, city, linkedin, portfolio).all { it.trim().length <= ProfileLimits.MAX_TEXT_LENGTH }
