package com.tailormyresume.app.referrals

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReferralInstallationStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = PrivateReferralStore(context, UnconfinedTestDispatcher())
    private val code = "tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567"
    private val environment = "https://api.example.invalid"

    @After fun clean() {
        File(context.filesDir, "referral-installation.json").delete()
        File(context.filesDir, "manual-referrals").deleteRecursively()
    }

    @Test fun captureAndTerminalConsumptionSurviveProcessBeforeBinding() = runTest {
        assertThat(store.beginCollection(store.epoch())).isTrue()
        assertThat(store.capture(code, environment, store.epoch(), terminalReferrer = true)).isTrue()
        val recovered = PrivateReferralStore(context, UnconfinedTestDispatcher())
        assertThat(recovered.inbox(environment)).isEqualTo(code)
        assertThat(recovered.beginCollection(recovered.epoch())).isFalse()
        val bound = recovered.bindInbox(ReferralJournal("guest", environment, "ANONYMOUS", 0), recovered.epoch()) {}
        assertThat(bound.code).isEqualTo(code)
        assertThat(bound.source).isEqualTo("LINK")
        assertThat(recovered.inbox(environment)).isNull()
        assertThat(recovered.read("guest", environment)?.code).isEqualTo(code)
    }

    @Test fun manualFirstWinsAndCleanupDoesNotRearmOldInstall() = runTest {
        store.capture(code, environment, store.epoch(), terminalReferrer = true)
        val manual = ReferralJournal("owner", environment, "GOOGLE", 0, code = code.replace("staging", "production"))
        val bound = store.bindInbox(manual, store.epoch()) {}
        assertThat(bound).isEqualTo(manual)
        val oldEpoch = store.epoch()
        store.clearAll()
        assertThat(store.beginCollection(store.epoch())).isFalse()
        assertThat(runCatching { store.capture(code, environment, oldEpoch, terminalReferrer = true) }.isFailure).isTrue()
        assertThat(store.capture(code, environment, store.epoch())).isTrue()
        assertThat(store.inbox(environment)).isEqualTo(code)
    }

    @Test fun serviceRetryIsBoundedAndTerminalNoDataIsConsumed() = runTest {
        repeat(3) { assertThat(store.beginCollection(store.epoch())).isTrue() }
        assertThat(store.beginCollection(store.epoch())).isFalse()
        store.capture(null, environment, store.epoch(), terminalReferrer = true)
        assertThat(store.beginCollection(store.epoch())).isFalse()
    }
}
