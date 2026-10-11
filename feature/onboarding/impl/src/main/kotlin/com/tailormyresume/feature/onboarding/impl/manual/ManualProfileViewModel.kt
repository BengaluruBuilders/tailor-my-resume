package com.tailormyresume.feature.onboarding.impl.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.onboarding.ManualContact
import com.tailormyresume.core.domain.onboarding.SaveManualProfileUseCase
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

internal enum class ManualField { FullName, Phone, City, JobTitle, Company }

internal data class ManualProfileUiState(
    val fullName: String = "",
    val email: String = "",
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
    sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val saveManualProfile: SaveManualProfileUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(ManualProfileUiState())

    private val eventChannel = Channel<ManualProfileEvent>(Channel.BUFFERED)

    private val saving = AtomicBoolean(false)

    private var savedRoleId: String? = null

    val uiState: StateFlow<ManualProfileUiState> = state.asStateFlow()

    val events: Flow<ManualProfileEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val account = sessionRepository.observeAccount().first()
            val profile = profileRepository.observeProfile().first()
            val manualRole = profile?.entries.orEmpty().filter(::isManualRole).singleOrNull()
            savedRoleId = manualRole?.id
            state.update {
                it.copy(
                    jobTitle = it.jobTitle.ifBlank { manualRole?.title.orEmpty() },
                    company = it.company.ifBlank { manualRole?.organization.orEmpty() },
                    fullName = it.fullName.ifBlank { profile?.fullName?.takeIf(String::isNotBlank) ?: account?.displayName.orEmpty() },
                    email = account?.email ?: profile?.email.orEmpty(),
                    phone = it.phone.ifBlank { profile?.phone.orEmpty() },
                    city = it.city.ifBlank { profile?.city.orEmpty() },
                )
            }
        }
    }

    fun onFieldChange(field: ManualField, value: String) {
        state.update {
            when (field) {
                ManualField.FullName -> it.copy(fullName = value)
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
                val saved = saveManualProfile(
                    ManualContact(current.fullName, current.email, current.phone, current.city, current.jobTitle, current.company),
                    savedRoleId,
                )
                savedRoleId = saved.entries.firstOrNull { it.id !in idsBefore }?.id ?: savedRoleId.takeIf { id -> saved.entries.any { it.id == id } }
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
}
