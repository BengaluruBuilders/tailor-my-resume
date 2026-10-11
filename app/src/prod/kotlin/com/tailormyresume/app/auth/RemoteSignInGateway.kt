package com.tailormyresume.app.auth

import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSignInGateway @Inject constructor(
    private val config: FirebaseConfig,
    private val credentials: GoogleCredentialSource,
    private val firebase: FirebaseSessionClient,
    private val api: TailorMyResumeApi,
    private val sessionRepository: SessionRepository,
    private val cleaner: SignOutCleaner,
    private val wiper: LocalDataWiper = LocalDataWiper.None,
    private val pendingWipe: PendingAccountWipe = PendingAccountWipe.None,
) : SignInGateway {

    override suspend fun currentAccount(): SignInAccount? = sessionRepository.observeAccount().first()

    override suspend fun signIn(): SignInResult {
        if (!config.isComplete) return AuthFailure.NotConfigured.toSignInResult()
        val googleToken = when (val outcome = credentials.idToken()) {
            is AuthOutcome.Success -> outcome.value
            is AuthOutcome.Failure -> return outcome.failure.toSignInResult()
        }
        val user = when (val outcome = firebase.signIn(googleToken)) {
            is AuthOutcome.Success -> outcome.value
            is AuthOutcome.Failure -> return outcome.failure.toSignInResult()
        }
        apiResult { api.me() }.onFailure { failure ->
            withContext(NonCancellable) { firebase.signOut() }
            return failure.toAuthFailure().toSignInResult()
        }
        val account = SignInAccount(id = user.uid, displayName = user.displayName.ifBlank { user.email }, email = user.email)
        val previousAccountId = sessionRepository.lastAccountId()
        val markerOwner = if (pendingWipe.state() == PendingWipeState.NONE) null else pendingWipe.uid()
        val markerIsForAnotherAccount = markerOwner != null && markerOwner != user.uid
        withContext(NonCancellable) {
            if ((previousAccountId != null && previousAccountId != user.uid) || markerIsForAnotherAccount) {
                cleaner.clear()
                wipeKeepingOnboardingInput()
            }
            if (markerIsForAnotherAccount) pendingWipe.clear()
            sessionRepository.saveLastAccountId(user.uid)
            sessionRepository.saveAccount(account)
        }
        return SignInResult.SignedIn(account)
    }

    override suspend fun signOut() {
        withContext(NonCancellable) {
            firebase.signOut()
            credentials.clearState()
            sessionRepository.observeAccount().first()?.let { sessionRepository.saveLastAccountId(it.id) }
            sessionRepository.signOut()
            cleaner.clear()
        }
    }

    private suspend fun wipeKeepingOnboardingInput() {
        val keptJob = sessionRepository.observeKeptJobDescription().first()
        wiper.wipeAll()
        keptJob?.let { sessionRepository.keepJobDescription(it) }
    }

    private fun Throwable.toAuthFailure(): AuthFailure = when ((this as? ApiException)?.error) {
        ApiError.Offline, ApiError.Timeout -> AuthFailure.Offline
        else -> AuthFailure.Failed
    }
}
