package com.tailormyresume.feature.applications.impl.detail

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

@HiltViewModel(assistedFactory = ApplicationDetailViewModel.Factory::class)
internal class ApplicationDetailViewModel @AssistedInject constructor(
    private val applicationRepository: ApplicationRepository,
    private val creditsRepository: CreditsRepository,
    private val setApplicationStatus: SetApplicationStatusUseCase,
    private val exportedPdfIntents: ExportedPdfIntents,
    @Assisted val applicationId: String,
) : ViewModel() {

    val events: Flow<ApplicationDetailEvent> = emptyFlow()

    val state: StateFlow<ApplicationDetailUiState> = MutableStateFlow(ApplicationDetailUiState.Loading)

    fun onShare() = Unit

    fun onOpen() = Unit

    fun onChangeStatus() = Unit

    fun onPickStatus(status: ApplicationStatus) = Unit

    fun onDismissStatusSheet() = Unit

    fun onSaveStatus() = Unit

    fun onRetailor() = Unit

    fun onSoon() = Unit

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String): ApplicationDetailViewModel
    }
}
