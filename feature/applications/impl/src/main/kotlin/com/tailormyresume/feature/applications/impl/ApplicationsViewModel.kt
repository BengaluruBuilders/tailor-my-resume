package com.tailormyresume.feature.applications.impl

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class ApplicationsViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val creditsRepository: CreditsRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    val state: StateFlow<ApplicationsUiState> = MutableStateFlow(ApplicationsUiState.Loading)
}
