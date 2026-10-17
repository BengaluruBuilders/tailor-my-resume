package com.tailormyresume.core.domain.account

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.PurchaseEntitlement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AccountCreditBalance @Inject constructor(
    private val paymentGateway: AccountWallet,
    private val creditsRepository: CreditsRepository,
) {

    suspend fun unusedCredits(): Int {
        check(creditsRepository.refreshConfirmed()) { "The credit balance is unavailable." }
        return checkNotNull(creditsRepository.observeKnownBalance().first()) { "The credit balance is unavailable." }
    }

    suspend fun cachedCredits(): Int? = observeCredits().first()

    fun observeCredits(): Flow<Int?> = creditsRepository.observeKnownBalance()

    suspend fun creditLine(): AccountCreditLine {
        val entitlement = knownEntitlement()
        return AccountCreditLine(
            freeCredits = entitlement.freeCredits,
            purchasedCredits = entitlement.purchasedCredits,
        )
    }

    private suspend fun knownEntitlement(): PurchaseEntitlement {
        val entitlement = try {
            paymentGateway.entitlement()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            paymentGateway.observeEntitlement().first()
        }
        check(entitlement.hasKnownBalance) { "The credit balance is unavailable." }
        return entitlement
    }

    suspend fun clearUnusedCredits(): PurchaseEntitlement {
        val cleared = paymentGateway.clearCredits()
        check(cleared.totalCredits == 0) { CREDIT_CLEAR_FAILED }
        return cleared
    }
}

private const val CREDIT_CLEAR_FAILED = "The credit balance did not reach zero."
