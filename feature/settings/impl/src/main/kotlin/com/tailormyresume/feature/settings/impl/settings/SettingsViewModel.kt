package com.tailormyresume.feature.settings.impl.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.ExportAccountDataUseCase
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.feature.settings.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    creditsRepository: CreditsRepository,
    private val resumeSettingsRepository: ResumeSettingsRepository,
    private val exportAccountData: ExportAccountDataUseCase,
    private val signInGateway: SignInGateway,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        sessionRepository.observeAccount(),
        creditsRepository.observeBalance(),
        resumeSettingsRepository.observeSettings(),
    ) { account, credits, settings ->
        SettingsUiState.Content(
            email = account?.email.orEmpty(),
            credits = credits,
            pageSize = settings.pageSize,
            fileNameFormat = settings.fileNameFormat,
            productUpdates = settings.productUpdates,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)

    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()

    private val preparingData = MutableStateFlow(false)

    private val signingOut = MutableStateFlow(false)

    fun onPageSizeClicked() {
        viewModelScope.launch {
            resumeSettingsRepository.update { settings ->
                settings.copy(pageSize = if (settings.pageSize == PageSize.A4) PageSize.LETTER else PageSize.A4)
            }
        }
    }

    fun onFileNameClicked() {
        viewModelScope.launch {
            resumeSettingsRepository.update { settings ->
                settings.copy(fileNameFormat = settings.fileNameFormat.next())
            }
        }
    }

    fun onProductUpdatesToggled() {
        viewModelScope.launch {
            resumeSettingsRepository.update { settings ->
                settings.copy(productUpdates = !settings.productUpdates)
            }
        }
    }

    fun onDownloadMyData() {
        if (!preparingData.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                eventChannel.send(SettingsEvent.ShareArchive(exportAccountData().file))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                eventChannel.send(SettingsEvent.DownloadFailed)
            } finally {
                preparingData.value = false
            }
        }
    }

    fun onSignOut() {
        if (!signingOut.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val signedOut = withContext(NonCancellable) {
                try {
                    signInGateway.signOut()
                    PendingToast.set(R.string.feature_settings_impl_toast_signed_out)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    false
                }
            }
            if (!signedOut) signingOut.value = false
        }
    }
}

private fun FileNameFormat.next(): FileNameFormat = FileNameFormat.entries[(ordinal + 1) % FileNameFormat.entries.size]
