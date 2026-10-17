package com.tailormyresume.app.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.auth.api
import com.tailormyresume.app.auth.jsonResponse
import com.tailormyresume.app.billing.FakePlayBilling
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.idleScope
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class GuestPurchaseAccessTest {
    @Test fun guestReadsLegitimateBalanceWithoutListingBuyingOrRestoringPlayProducts() = runTest {
        val server = MockWebServer().apply { start() }
        try {
            val identity = GuestIdentity()
            val billing = FakePlayBilling()
            val api = server.api()
            val subject = RemotePaymentGateway(api, WalletSource(api, identity), billing, identity, idleScope(), TestCreditsRepository())
            assertThat(subject.packs()).isEmpty()
            assertThat(subject.purchase("application_pack_5")).isInstanceOf(PurchaseResult.Failed::class.java)
            assertThat(billing.launchedWith).isNull()
            assertThat(server.requestCount).isEqualTo(0)
            server.enqueue(jsonResponse(200, """{"wallet":{"freeCredits":3,"purchasedCredits":4,"analysesLeftToday":20,"day":"2026-10-17","resetsAt":"2026-10-17T18:30:00Z"}}"""))
            assertThat(subject.restorePurchases().totalCredits).isEqualTo(7)
            assertThat(server.takeRequest().path).isEqualTo("/v1/tailormyresume/wallet")
        } finally {
            server.shutdown()
        }
    }
}
