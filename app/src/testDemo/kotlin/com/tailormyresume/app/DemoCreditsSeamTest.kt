package com.tailormyresume.app

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.di.PaymentBindings
import com.tailormyresume.core.data.repository.NoRemoteLedger
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import org.junit.Test

class DemoCreditsSeamTest {
    private val bound = PaymentBindings::class.java.methods.associate { it.returnType to it.parameterTypes.single() }

    @Test
    fun demoKeepsLocalLedgerAndOfflineGateway() {
        assertThat(bound[RemoteLedgerSource::class.java]).isEqualTo(NoRemoteLedger::class.java)
        assertThat(bound[PaymentGateway::class.java]).isEqualTo(OfflinePaymentGateway::class.java)
        assertThat(NoRemoteLedger().ownsLedger).isFalse()
        assertThat(NoRemoteLedger().owner()).isNull()
    }

    @Test
    fun demoClasspathHasNoPlayBillingOrNetwork() {
        listOf("com.android.billingclient.api.BillingClient", "com.tailormyresume.core.network.TailorMyResumeApi").forEach { name ->
            val found = runCatching { Class.forName(name) }.isSuccess
            assertThat(found).isFalse()
        }
    }
}
