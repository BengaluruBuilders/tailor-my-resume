package com.tailormyresume.core.domain.account

import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountCreditBalance @Inject constructor(
    private val paymentGateway: PaymentGateway,
) {

    suspend fun unusedCredits(): Int = knownEntitlement().totalCredits

    suspend fun cachedCredits(): Int = observeCredits().first()

    fun observeCredits(): Flow<Int> = paymentGateway.observeEntitlement().map { entitlement -> entitlement.totalCredits }

    suspend fun creditLine(): AccountCreditLine {
        val entitlement = knownEntitlement()
        return AccountCreditLine(
            freeCredits = entitlement.freeCredits,
            purchasedCredits = entitlement.purchasedCredits,
        )
    }

    private suspend fun knownEntitlement(): PurchaseEntitlement = try {
        paymentGateway.entitlement()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        paymentGateway.observeEntitlement().first()
    }

    suspend fun clearUnusedCredits(): PurchaseEntitlement {
        val cleared = paymentGateway.clearCredits()
        check(cleared.totalCredits == 0) { CREDIT_CLEAR_FAILED }
        return cleared
    }
}

private const val CREDIT_CLEAR_FAILED = "The credit balance did not reach zero."
