package com.tailormyresume.app.debug

import com.tailormyresume.app.billing.AccountWalletOverride
import com.tailormyresume.app.billing.RemoteAccountWallet
import com.tailormyresume.core.domain.AccountWallet
import com.tailormyresume.core.domain.offline.ForcedWalletScenario
import com.tailormyresume.core.domain.offline.OfflineAccountWallet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebugWalletBindings {
    @Provides
    @Singleton
    @AccountWalletOverride
    fun provideAccountWalletOverride(
        remote: RemoteAccountWallet,
        offline: OfflineAccountWallet,
        forced: ForcedWalletScenario,
    ): AccountWallet = ScenarioAccountWallet(remote, offline, forced)
}
