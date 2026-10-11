package com.tailormyresume.feature.settings.impl.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.account.AccountDeletionResult
import com.tailormyresume.core.domain.account.DeleteAccountUseCase
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal sealed interface DeleteAccountUiState {
    data object Loading : DeleteAccountUiState

    data class Content(
        val applications: Int,
        val unusedCredits: Int,
        val typed: String,
        val canDelete: Boolean,
        val deleting: Boolean,
    ) : DeleteAccountUiState
}

internal sealed interface DeleteAccountEvent {
    data object DeleteFailed : DeleteAccountEvent
}

@HiltViewModel
internal class DeleteAccountViewModel @Inject constructor(
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    private val typed = MutableStateFlow("")

    private val deleting = MutableStateFlow(false)

    private val eventChannel = Channel<DeleteAccountEvent>(Channel.BUFFERED)

    val uiState: StateFlow<DeleteAccountUiState> = combine(
        flow { emit(deleteAccount.preview()) },
        typed,
        deleting,
    ) { counts, text, inFlight ->
        DeleteAccountUiState.Content(
            applications = counts.applications,
            unusedCredits = counts.unusedCredits,
            typed = text,
            canDelete = matchesDeleteConfirmation(text),
            deleting = inFlight,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeleteAccountUiState.Loading,
    )

    val events: Flow<DeleteAccountEvent> = eventChannel.receiveAsFlow()

    fun onTextChanged(text: String) {
        typed.value = text
    }

    fun onDelete() {
        if (!matchesDeleteConfirmation(typed.value)) return
        if (!deleting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val deleted = withContext(NonCancellable) {
                val result = try {
                    deleteAccount()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    null
                }
                (result is AccountDeletionResult.Deleted).also { removed ->
                    if (removed) PendingToast.set(R.string.feature_settings_impl_toast_account_deleted)
                }
            }
            if (!deleted) {
                deleting.value = false
                eventChannel.send(DeleteAccountEvent.DeleteFailed)
            }
        }
    }

    fun onSheetOpened() {
        while (eventChannel.tryReceive().isSuccess) continue
    }

    fun onSheetClosed() {
        typed.value = ""
    }
}
