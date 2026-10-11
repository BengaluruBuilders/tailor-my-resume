package com.tailormyresume.app.di

import com.tailormyresume.core.data.repository.NoRemoteLedger
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface PaymentBindings {
    @Binds
    fun bindPaymentGateway(impl: OfflinePaymentGateway): PaymentGateway

    @Binds
    fun bindRemoteLedgerSource(impl: NoRemoteLedger): RemoteLedgerSource
}
