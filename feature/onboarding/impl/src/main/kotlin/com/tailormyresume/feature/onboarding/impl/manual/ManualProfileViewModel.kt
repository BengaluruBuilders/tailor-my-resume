package com.tailormyresume.feature.onboarding.impl.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.onboarding.ManualContact
import com.tailormyresume.core.domain.onboarding.SaveManualProfileUseCase
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal enum class ManualField { FullName, Email, Phone, City, JobTitle, Company }

internal data class ManualProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val emailEditable: Boolean = false,
    val phone: String = "",
    val city: String = "",
    val jobTitle: String = "",
    val company: String = "",
)

internal enum class ManualProfileEvent { Saved }

private fun isManualRole(entry: ProfileEntry): Boolean =
    entry.category == EntryCategory.EXPERIENCE && entry.source == FactSource.USER_STATED &&
        entry.bullets.isEmpty() && entry.startDate.isBlank() && entry.endDate.isBlank()

@HiltViewModel
internal class ManualProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val saveManualProfile: SaveManualProfileUseCase,
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) : ViewModel() {
    private val workTicket = workFence.ticket()


    private val state = MutableStateFlow(ManualProfileUiState())

    private val eventChannel = Channel<ManualProfileEvent>(Channel.BUFFERED)

    private val saving = AtomicBoolean(false)

    private var savedRoleId: String? = null

    private val dirtyFields = mutableSetOf<ManualField>()

    val uiState: StateFlow<ManualProfileUiState> = state.asStateFlow()

    val events: Flow<ManualProfileEvent> = eventChannel.receiveAsFlow()

    init {
        refreshFromProfile()
    }

    fun onRouteActive() {
        refreshFromProfile()
    }

    private fun refreshFromProfile() {
        viewModelScope.launch {
            val account = sessionRepository.observeAccount().first()
            val profile = profileRepository.observeProfile().first()
            val entries = profile?.entries.orEmpty()
            val manualRole = when (val id = savedRoleId) {
                null -> entries.filter(::isManualRole).singleOrNull()
                else -> entries.firstOrNull { it.id == id && it.category == EntryCategory.EXPERIENCE }
            }
            if (savedRoleId == null) savedRoleId = manualRole?.id
            state.update {
                it.copy(
                    jobTitle = valueUnlessDirty(ManualField.JobTitle, it.jobTitle, manualRole?.title.orEmpty()),
                    company = valueUnlessDirty(ManualField.Company, it.company, manualRole?.organization.orEmpty()),
                    fullName = valueUnlessDirty(
                        ManualField.FullName,
                        it.fullName,
                        profile?.fullName?.takeIf(String::isNotBlank) ?: account?.displayName.orEmpty(),
                    ),
                    email = valueUnlessDirty(
                        ManualField.Email,
                        it.email,
                        if (account?.mode == AccountMode.ANONYMOUS) profile?.email.orEmpty() else account?.email ?: profile?.email.orEmpty(),
                    ),
                    emailEditable = account?.mode == AccountMode.ANONYMOUS,
                    phone = valueUnlessDirty(ManualField.Phone, it.phone, profile?.phone.orEmpty()),
                    city = valueUnlessDirty(ManualField.City, it.city, profile?.city.orEmpty()),
                )
            }
        }
    }

    private fun valueUnlessDirty(field: ManualField, current: String, refreshed: String): String =
        if (field in dirtyFields) current else refreshed

    fun onFieldChange(field: ManualField, value: String) {
        workFence.editing(this, true)
        dirtyFields += field
        state.update {
            when (field) {
                ManualField.FullName -> it.copy(fullName = value)
                ManualField.Email -> if (it.emailEditable) it.copy(email = value) else it
                ManualField.Phone -> it.copy(phone = value)
                ManualField.City -> it.copy(city = value)
                ManualField.JobTitle -> it.copy(jobTitle = value)
                ManualField.Company -> it.copy(company = value)
            }
        }
    }

    fun onContinue() {
        if (!saving.compareAndSet(false, true)) return
        val current = state.value
        viewModelScope.launch {
            try {
                val idsBefore = profileRepository.observeProfile().first()?.entries.orEmpty().map { it.id }.toSet()
                workFence.check(workTicket)
                val saved = saveManualProfile(
                    ManualContact(current.fullName, current.email, current.phone, current.city, current.jobTitle, current.company),
                    savedRoleId,
                )
                savedRoleId = saved.entries.firstOrNull { it.id !in idsBefore }?.id ?: savedRoleId.takeIf { id -> saved.entries.any { it.id == id } }
                dirtyFields.clear()
                workFence.editing(this, false)
                eventChannel.trySend(ManualProfileEvent.Saved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                return@launch
            } finally {
                saving.set(false)
            }
        }
    }
    override fun onCleared() {
        workFence.editing(this, false)
        super.onCleared()
    }

}
