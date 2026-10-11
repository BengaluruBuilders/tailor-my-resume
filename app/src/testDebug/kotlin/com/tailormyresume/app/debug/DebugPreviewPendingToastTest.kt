package com.tailormyresume.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.navigation.registerComposeActivity
import com.tailormyresume.app.ui.AppRootState
import com.tailormyresume.app.ui.RootViewModelStores
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.PendingToast
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.tailormyresume.feature.settings.impl.R as SettingsR

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DebugPreviewPendingToastTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signedOutToastShowsInTheDebugPreviewWithoutAMainActivityRoot() {
        PendingToast.consume()
        val rootState = MutableStateFlow<AppRootState>(AppRootState.Ready(StartDestination.Applications, "account-1"))
        val hasAccount = MutableStateFlow(true)
        rule.setContent {
            TmrTheme {
                DebugScenarioPreview(
                    target = DebugScenarioTarget.Settings,
                    scenario = DebugScenario.defaultValue,
                    rootState = rootState,
                    hasAccount = hasAccount,
                    rootStores = RootViewModelStores(),
                    entryProvider = { _ -> ({ key: NavKey -> NavEntry(key) {} }) },
                )
            }
        }
        rule.waitForIdle()

        rootState.value = AppRootState.Ready(StartDestination.SignIn, null)
        hasAccount.value = false
        PendingToast.set(SettingsR.string.feature_settings_impl_toast_signed_out)
        rule.waitForIdle()

        rule.onNodeWithText("Signed out").assertIsDisplayed()
        assertThat(PendingToast.queued).isNull()
    }
}
