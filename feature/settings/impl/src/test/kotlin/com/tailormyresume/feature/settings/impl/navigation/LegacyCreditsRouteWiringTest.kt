package com.tailormyresume.feature.settings.impl.navigation

import android.app.Application
import android.content.ComponentName
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.LegacyPaymentNavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class LegacyCreditsRouteWiringTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<SettingsEntryHostActivity>()
    private val register = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, SettingsEntryHostActivity::class.java))
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(hilt).around(register).around(compose)
    @Before fun setUp() { hilt.inject() }

    @Test fun restoredLegacyKeyRedirectsWithoutStartingTailoringAndBackKeepsTheApplication() {
        val legacy = Json.decodeFromString<LegacyPaymentNavKey>("""{"returnToApplicationId":"saved-application"}""")
        assertThat(LegacyPaymentNavKey.serializer().descriptor.serialName).isEqualTo("com.tailormyresume.feature.settings.api.navigation.PaywallNavKey")
        val opener = JobResultNavKey("saved-application")
        val harness = EntryHarness(compose, listOf(opener, legacy))
        harness.show()
        harness.waitFor { harness.state.currentKey == CreditsNavKey(returnToApplicationId = "saved-application") }
        compose.onNodeWithText("Buy more").assertDoesNotExist()
        compose.onNodeWithText("Restore purchases").assertDoesNotExist()
        harness.navigator.goBack()
        assertThat(harness.state.currentKey).isEqualTo(opener)
    }
}
