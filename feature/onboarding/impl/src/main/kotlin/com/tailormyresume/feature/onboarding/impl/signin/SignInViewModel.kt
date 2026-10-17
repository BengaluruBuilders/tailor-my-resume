package com.tailormyresume.feature.onboarding.impl.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.GuestTrialGateway
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult
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
    private val guests: GuestTrialGateway = UnavailableGuestTrial,
) : ViewModel() {

    private val state = MutableStateFlow<SignInUiState>(SignInUiState.Ready)
    private var guestJob: kotlinx.coroutines.Job? = null

    val uiState: StateFlow<SignInUiState> = state.asStateFlow()

    private val cancelledEvents = Channel<Unit>(Channel.BUFFERED)

    val cancelled: Flow<Unit> = cancelledEvents.receiveAsFlow()

    fun onTryGuest() {
        if (guestJob?.isActive == true || state.value == SignInUiState.SigningIn) return
        state.value = SignInUiState.PreparingGuest
        guestJob = viewModelScope.launch {
            state.value = SignInUiState.GuestOffer(guestResult<GuestTrialOffer>(GuestTrialOffer.Unavailable(canContinue = false)) { guests.prepare() })
        }
    }

    fun onClaimTrial() {
        val current = state.value as? SignInUiState.GuestOffer ?: return
        if (current.busy || current.offer !is GuestTrialOffer.Available || guestJob?.isActive == true) return
        state.value = current.copy(busy = true)
        guestJob = viewModelScope.launch {
            val result = guestResult<GuestTrialResult>(GuestTrialResult.Refresh(GuestTrialOffer.Unavailable())) { guests.claim() }
            state.value = when (result) {
                is GuestTrialResult.Refresh -> SignInUiState.GuestOffer(result.offer)
                else -> SignInUiState.GuestResult(result)
            }
        }
    }

    fun onContinueGuest() {
        if (guestJob?.isActive == true) return
        guestJob = viewModelScope.launch {
            try {
                guests.continueAsGuest()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                state.value = SignInUiState.GuestOffer(GuestTrialOffer.Unavailable(canContinue = false))
            }
        }
    }

    fun onResume() {
        if (state.value is SignInUiState.GuestOffer) onTryGuest()
    }

    private suspend fun <T> guestResult(fallback: T, action: suspend () -> T): T = try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        fallback
    }

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
            if (result == SignInResult.AwaitingChoice) {
                state.value = SignInUiState.Ready
            } else if (result !is SignInResult.SignedIn) {
                state.value = SignInUiState.Cancelled
                cancelledEvents.trySend(Unit)
            }
        }
    }
}

private object UnavailableGuestTrial : GuestTrialGateway {
    override suspend fun prepare(): GuestTrialOffer = GuestTrialOffer.Unavailable()
    override suspend fun claim(): GuestTrialResult = GuestTrialResult.Refresh(GuestTrialOffer.Unavailable())
    override suspend fun continueAsGuest() = Unit
}
