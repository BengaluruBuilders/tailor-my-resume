package com.tailormyresume.core.testing.gateway

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AccountWallet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

abstract class AccountWalletContractTest {
    protected abstract fun createAccountWallet(): AccountWallet

    @Test fun observedBalanceMatchesRead() = runTest {
        val wallet = createAccountWallet()
        assertThat(wallet.observeEntitlement().first()).isEqualTo(wallet.entitlement())
    }

    @Test fun historyReadDoesNotChangeTheWallet() = runTest {
        val wallet = createAccountWallet()
        val before = wallet.entitlement()
        assertThat(wallet.observePurchaseHistory().first()).isEqualTo(wallet.purchaseHistory())
        assertThat(wallet.entitlement()).isEqualTo(before)
    }

    @Test fun clearDropsBalanceAndHistory() = runTest {
        val wallet = createAccountWallet()
        wallet.clearCredits()
        assertThat(wallet.entitlement().totalCredits).isEqualTo(0)
        assertThat(wallet.purchaseHistory()).isEmpty()
    }
}
