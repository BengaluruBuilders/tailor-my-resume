package com.tailormyresume.app.billing

import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.auth.NoMatcher
import com.tailormyresume.app.auth.SignOutCleaner
import com.tailormyresume.app.auth.api
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import okhttp3.mockwebserver.MockWebServer

internal class FakePlayBilling : PlayBilling {
    var products = mapOf("application_pack_5" to PlayProduct("5 applications", 149_000_000, "INR"))
    var purchaseResult: PlayPurchaseResult = PlayPurchaseResult.Failed
    var owned = emptyList<PlayPurchase>()
    override val unsolicitedPurchases = MutableSharedFlow<PlayPurchase>(extraBufferCapacity = 8)
    var launchedWith: Pair<String, String>? = null

    override suspend fun productDetails(productId: String) = products[productId]

    override suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlayPurchaseResult {
        launchedWith = productId to obfuscatedAccountId
        return purchaseResult
    }

    override suspend fun ownedPurchases() = owned
}

internal fun idleScope() = CoroutineScope(StandardTestDispatcher())

internal class FakeUid(private val value: String?) : FirebaseUidProvider {
    override fun uid() = value
}

internal class SwitchableUid(@Volatile var value: String?) : FirebaseUidProvider {
    override fun uid() = value
}

internal object FixedToken : IdTokenProvider {
    override fun idToken(forceRefresh: Boolean) = "test-token"
}

internal fun walletJson(
    free: Int = 1,
    purchased: Int = 0,
    analyses: Int = 3,
    tailorings: Int = 1,
    unlocked: String = "",
) = """{"freeCredits":$free,"purchasedCredits":$purchased,"analysesLeftToday":$analyses,
"freeTailoringsLeftToday":$tailorings,"day":"2026-10-07","resetsAt":"2026-10-07T18:30:00Z",
"unlockedApplicationIds":[$unlocked]}"""

internal fun purchaseJson(wallet: String) =
    """{"purchase":{"orderId":"GPA.1","productId":"application_pack_5","creditsGranted":5,
"purchasedAt":"2026-10-07T09:12:00Z","state":"COMPLETED"},"wallet":$wallet}"""

internal fun errorJson(code: String) = """{"error":{"code":"$code","message":"m"}}"""

internal fun MockWebServer.signOutCleaner(): SignOutCleaner {
    val store = TestMockStateStore()
    val api = api()
    return SignOutCleaner(
        RemotePaymentGateway(api, WalletSource(api), FakePlayBilling(), FakeUid("uid-1"), idleScope()),
        RemoteJobAnalysisSource(api, NoMatcher),
        PendingReportQueue(store),
        store,
    )
}
