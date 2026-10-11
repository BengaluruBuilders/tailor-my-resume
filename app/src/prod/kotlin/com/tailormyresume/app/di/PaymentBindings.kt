package com.tailormyresume.app.di

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.app.billing.GooglePlayBilling
import com.tailormyresume.app.billing.PlayBilling
import com.tailormyresume.app.billing.PurchaseRestorer
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.core.domain.PaymentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface PaymentBindings {
    @Binds
    fun bindPaymentGateway(impl: RemotePaymentGateway): PaymentGateway

    @Binds
    fun bindPlayBilling(impl: GooglePlayBilling): PlayBilling

    @Binds
    @IntoSet
    fun bindPurchaseRestorer(impl: PurchaseRestorer): AppStartTask
}
