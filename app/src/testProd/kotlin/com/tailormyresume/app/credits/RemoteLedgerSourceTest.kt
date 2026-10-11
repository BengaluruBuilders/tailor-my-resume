package com.tailormyresume.app.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.account.api
import com.tailormyresume.app.account.jsonResponse
import com.tailormyresume.app.billing.FakeUid
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import kotlin.time.Instant

class RemoteLedgerSourceTest {
    private val server = MockWebServer().apply { start() }
    private val source = RemoteCreditsLedgerSource(server.api(), FakeUid("uid-1"))

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun mapsKindsNewestFirstAndSkipsUnknown() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"credits":{"balance":6,"entries":[
{"id":"e6","kind":"REFUND","amount":1,"applicationId":"app-2","productId":null,"createdAt":"2026-10-11T09:00:00.000Z"},
{"id":"e5","kind":"BONUS","amount":9,"applicationId":null,"productId":null,"createdAt":"2026-10-10T09:00:00.000Z"},
{"id":"e4","kind":"TAILORING","amount":-1,"applicationId":"app-2","productId":null,"createdAt":"2026-10-09T09:00:00.000Z"},
{"id":"e3","kind":"PURCHASE","amount":5,"applicationId":null,"productId":"application_pack_5","createdAt":"2026-10-08T09:00:00.000Z"},
{"id":"e2","kind":"MIGRATION","amount":1,"applicationId":null,"productId":null,"createdAt":"2026-10-07T09:00:00.000Z"},
{"id":"e1","kind":"WELCOME","amount":1,"applicationId":null,"productId":null,"createdAt":"2026-10-06T09:00:00.000Z"}]}}""",
            ),
        )

        val snapshot = source.fetch()

        assertThat(server.takeRequest().path).isEqualTo("/v1/tailormyresume/credits")
        assertThat(snapshot.balance).isEqualTo(6)
        assertThat(snapshot.entries.map { it.kind }).containsExactly(
            CreditLedgerKind.REFUND,
            CreditLedgerKind.SPEND,
            CreditLedgerKind.PURCHASE,
            CreditLedgerKind.MIGRATION,
            CreditLedgerKind.FREE_GRANT,
        ).inOrder()
        assertThat(snapshot.entries.map { it.amount }).containsExactly(1, -1, 5, 1, 1).inOrder()
        assertThat(snapshot.entries[1].applicationId).isEqualTo("app-2")
        assertThat(snapshot.entries[2].productId).isEqualTo("application_pack_5")
        assertThat(snapshot.entries.first().createdAt).isEqualTo(Instant.parse("2026-10-11T09:00:00Z"))
        assertThat(source.ownsLedger).isTrue()
        assertThat(source.owner()).isEqualTo("uid-1")
    }
}
