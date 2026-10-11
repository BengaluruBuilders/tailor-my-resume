package com.tailormyresume.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.onboarding.ObserveStartDestinationUseCase
import com.tailormyresume.core.domain.onboarding.StartDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    observeStartDestination: ObserveStartDestinationUseCase,
) : ViewModel() {

    val rootState: StateFlow<AppRootState> = observeStartDestination()
        .map { destination ->
            when (destination) {
                StartDestination.SignIn -> AppRootState.FirstRun
                StartDestination.Applications -> AppRootState.Main
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppRootState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
