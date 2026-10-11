package com.tailormyresume.feature.settings.impl.navigation

import com.tailormyresume.core.data.repository.NoRemoteLedger
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SettingsLedgerFakes {
    @Provides
    fun remoteLedgerSource(): RemoteLedgerSource = NoRemoteLedger()
}
