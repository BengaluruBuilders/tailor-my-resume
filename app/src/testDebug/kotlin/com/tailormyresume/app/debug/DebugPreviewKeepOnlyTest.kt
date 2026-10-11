package com.tailormyresume.app.debug

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ui.AppRootState
import com.tailormyresume.app.ui.RootViewModelStores
import com.tailormyresume.core.domain.onboarding.StartDestination
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class DebugPreviewKeepOnlyTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val rootState = MutableStateFlow<AppRootState>(AppRootState.Ready(StartDestination.Applications, "account-1"))
    private val hasAccount = MutableStateFlow(true)
    private val stores = RootViewModelStores()

    @Composable
    private fun Preview() {
        observePreviewRoot(opensFirstRunRoot = false, rootState = rootState, hasAccount = hasAccount, rootStores = stores)
    }

    @Test
    fun theMainStoreIsReleasedWhenTheAccountGoesAway() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application.packageManager)
            .addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { it.setContent { Preview() } }
            val mainStore = stores.storeOf(PreviewRoot.Main.accountRoot)
            composeRule.waitForIdle()
            assertThat(stores.storeOf(PreviewRoot.Main.accountRoot)).isSameInstanceAs(mainStore)

            hasAccount.value = false
            composeRule.waitForIdle()

            assertThat(stores.storeOf(PreviewRoot.Main.accountRoot)).isNotSameInstanceAs(mainStore)
        }
    }
}
