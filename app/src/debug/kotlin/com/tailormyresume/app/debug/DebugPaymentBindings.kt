package com.tailormyresume.app.debug

import com.tailormyresume.app.billing.PaymentGatewayOverride
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.offline.ForcedPaymentScenario
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebugPaymentBindings {
    @Provides
    @Singleton
    @PaymentGatewayOverride
    fun providePaymentGatewayOverride(
        remote: RemotePaymentGateway,
        offline: OfflinePaymentGateway,
        forced: ForcedPaymentScenario,
    ): PaymentGateway = ScenarioPaymentGateway(remote, offline, forced)
}
