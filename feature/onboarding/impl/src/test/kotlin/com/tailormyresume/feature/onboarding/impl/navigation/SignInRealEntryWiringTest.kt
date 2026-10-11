package com.tailormyresume.feature.onboarding.impl.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.impl.signin.ScriptedSignInGateway
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class SignInEntryHostActivity : ComponentActivity()

@Module
@InstallIn(SingletonComponent::class)
object ScriptedSignInModule {
    @Provides
    @Singleton
    fun scripted(): ScriptedSignInGateway = ScriptedSignInGateway { SignInResult.Cancelled }

    @Provides
    fun gateway(scripted: ScriptedSignInGateway): SignInGateway = scripted
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SignInRealEntryWiringTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<SignInEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, SignInEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var gateway: ScriptedSignInGateway

    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(SignInNavKey())))
    private val provider = entryProvider { onboardingEntry(navigator) }

    @Test
    fun signInTap_reachesTheGatewayThroughTheRealEntry() {
        hiltRule.inject()
        reduceMotion()
        composeRule.setContent {
            TmrTheme {
                val toast = remember { TmrToastState() }
                CompositionLocalProvider(LocalTmrToast provides toast) {
                    provider(SignInNavKey()).Content()
                    TmrToastHost(toast)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()

        assertThat(gateway.calls).isEqualTo(1)
    }
}
