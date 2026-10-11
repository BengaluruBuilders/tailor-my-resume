package com.tailormyresume.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BackTargetViewModel @Inject constructor(
    applicationRepository: ApplicationRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val hasHome: StateFlow<Boolean> = combine(
        applicationRepository.observeApplications(),
        profileRepository.observeProfile(),
    ) { applications, profile -> applications.isNotEmpty() || profile?.reviewedAt != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
