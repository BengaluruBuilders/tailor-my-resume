package com.tailormyresume.core.testing.gateway

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.offline.ForcedWalletScenario
import com.tailormyresume.core.domain.offline.OfflineAccountWallet
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator

class TestAccountWallet private constructor(
    private val delegate: OfflineAccountWallet,
) : AccountWallet by delegate {

    constructor(
        store: MockStateStore = TestMockStateStore(),
        clock: TestClock = TestClock(),
        forced: ForcedWalletScenario = ForcedWalletScenario(),
    ) : this(OfflineAccountWallet(store, NoMockLatency, clock, TestIdGenerator("order"), forced))

    suspend fun withHistoricalPurchase(packId: String, credits: Int = 5) = delegate.withHistoricalPurchase(packId, credits)
    suspend fun spendFixtureCredit(applicationId: String) = delegate.spendFixtureCredit(applicationId)
    fun withFreeCredits(credits: Int): TestAccountWallet = apply { delegate.withFreeCredits(credits) }
}
