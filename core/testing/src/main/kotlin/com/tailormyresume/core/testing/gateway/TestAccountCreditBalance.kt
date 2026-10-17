package com.tailormyresume.core.testing.gateway

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.account.AccountCreditBalance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

fun testAccountCreditBalance(paymentGateway: AccountWallet): AccountCreditBalance = AccountCreditBalance(
    paymentGateway,
    object : CreditsRepository by com.tailormyresume.core.testing.repository.TestCreditsRepository() {
        override fun observeKnownBalance() = paymentGateway.observeEntitlement().map { entitlement ->
            entitlement.totalCredits.takeIf { entitlement.hasKnownBalance }
        }

        override suspend fun refreshConfirmed(): Boolean = try {
            paymentGateway.entitlement().hasKnownBalance
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            observeKnownBalance().first() != null
        }
    },
)
