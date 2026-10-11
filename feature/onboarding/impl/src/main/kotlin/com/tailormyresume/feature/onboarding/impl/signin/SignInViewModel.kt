package com.tailormyresume.feature.onboarding.impl.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
internal class SignInViewModel @Inject constructor(
    private val gateway: SignInGateway,
) : ViewModel() {

    private val state = MutableStateFlow(SignInUiState.Ready)

    val uiState: StateFlow<SignInUiState> = state.asStateFlow()

    private val cancelledEvents = Channel<Unit>(Channel.BUFFERED)

    val cancelled: Flow<Unit> = cancelledEvents.receiveAsFlow()

    fun onContinueWithGoogle() {
        if (state.getAndUpdate { SignInUiState.SigningIn } == SignInUiState.SigningIn) return
        viewModelScope.launch {
            val result = try {
                gateway.signIn()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                null
            }
            if (result !is SignInResult.SignedIn) {
                state.value = SignInUiState.Cancelled
                cancelledEvents.trySend(Unit)
            }
        }
    }
}
