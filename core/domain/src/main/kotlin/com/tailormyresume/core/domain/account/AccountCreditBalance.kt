package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.balanceAfterRefreshIfUnknown
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AccountCreditBalance @Inject constructor(
    private val paymentGateway: PaymentGateway,
    private val creditsRepository: CreditsRepository,
) {

    suspend fun unusedCredits(): Int? = creditsRepository.balanceAfterRefreshIfUnknown()

    suspend fun cachedCredits(): Int? = observeCredits().first()

    fun observeCredits(): Flow<Int?> = creditsRepository.observeKnownBalance()

    suspend fun clearUnusedCredits(): PurchaseEntitlement {
        val cleared = paymentGateway.clearCredits()
        check(cleared.totalCredits == 0) { CREDIT_CLEAR_FAILED }
        return cleared
    }
}

private const val CREDIT_CLEAR_FAILED = "The credit balance did not reach zero."
