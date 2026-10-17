package com.tailormyresume.app.di

import com.tailormyresume.app.AppStartTask
import com.tailormyresume.app.billing.GooglePlayBilling
import com.tailormyresume.app.billing.PaymentGatewayOverride
import com.tailormyresume.app.billing.PlayBilling
import com.tailormyresume.app.billing.PurchaseRestorer
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.app.credits.RemoteCreditsLedgerSource
import com.tailormyresume.app.credits.WalletStartTask
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.domain.PaymentGateway
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.Optional

@Module
@InstallIn(SingletonComponent::class)
interface PaymentBindings {
    @Binds
    @IntoSet
    fun bindWalletStartTask(impl: WalletStartTask): AppStartTask

    @BindsOptionalOf
    @PaymentGatewayOverride
    fun optionalPaymentGatewayOverride(): PaymentGateway

    @Binds
    fun bindPlayBilling(impl: GooglePlayBilling): PlayBilling

    @Binds
    fun bindRemoteLedgerSource(impl: RemoteCreditsLedgerSource): RemoteLedgerSource

    @Binds
    @IntoSet
    fun bindPurchaseRestorer(impl: PurchaseRestorer): AppStartTask

    companion object {
        @Provides
        fun providePaymentGateway(
            remote: RemotePaymentGateway,
            @PaymentGatewayOverride override: Optional<PaymentGateway>,
        ): PaymentGateway = override.orElse(remote)
    }
}
