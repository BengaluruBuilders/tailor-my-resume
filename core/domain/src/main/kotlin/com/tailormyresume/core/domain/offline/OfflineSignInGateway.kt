package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockOperation
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.readValue
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.SignInAccount
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInOutcome
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.domain.account.WelcomeCreditGrant
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSignInGateway @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val latency: MockLatency,
    private val store: MockStateStore,
    private val exportedFiles: ExportedFiles = ExportedFiles.None,
    private val welcomeGrant: WelcomeCreditGrant = WelcomeCreditGrant.None,
) : SignInGateway {

    private var outcome: SignInOutcome = SignInOutcome.SignedIn
    private var account: SignInAccount = SignInAccount.localAccount

    fun withOutcome(outcome: SignInOutcome): OfflineSignInGateway = apply { this.outcome = outcome }

    fun withAccount(account: SignInAccount): OfflineSignInGateway = apply { this.account = account }

    override suspend fun currentAccount(): SignInAccount? = sessionRepository.observeAccount().first()

    override suspend fun signIn(): SignInResult {
        latency.await(MockOperation.SIGN_IN)
        return when (outcome) {
            SignInOutcome.SignedIn -> {
                if (store.readValue(PAYMENT_STATE_KEY, PaymentState.serializer())?.closed == true) {
                    store.remove(PAYMENT_STATE_KEY)
                }
                sessionRepository.saveAccount(account)
                welcomeGrant.grantOnce()
                SignInResult.SignedIn(account)
            }
            SignInOutcome.Cancelled -> SignInResult.Cancelled
            SignInOutcome.Failed -> SignInResult.Failed(SignInFailureReason.ProviderUnavailable)
        }
    }

    override suspend fun signOut() {
        sessionRepository.signOut()
        exportedFiles.deleteAll()
    }
}
