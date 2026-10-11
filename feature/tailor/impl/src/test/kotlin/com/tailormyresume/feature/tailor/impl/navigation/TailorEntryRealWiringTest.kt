package com.tailormyresume.feature.tailor.impl.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.impl.TailorViewModel
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.result.TailoredViewModel
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class TailorEntryRealWiringTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val bullet = testBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")
    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(TailoredNavKey("app-1"))))

    @Test
    fun realTailoredEntryResolvesBothViewModelsFromTheirOwnKeysAndAcceptsChanges() {
        applicationRepository.sendApplications(listOf(testApplication(listOf(bullet), entryIds = listOf("exp-1"))))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))))
        val store = ViewModelStore()
        val provider = ViewModelProvider.create(
            store,
            tailorEntryViewModelFactory(applicationRepository, profileRepository, TestClock()),
        )
        provider[TailorViewModelKeys.tailor("app-1"), TailorViewModel::class]
        provider[TailorViewModelKeys.tailored("app-1"), TailoredViewModel::class]
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = store
        }
        val entry = entryProvider<NavKey> { tailorEntry(navigator) }(TailoredNavKey("app-1"))

        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { entry.Content() }
            }
        }
        composeRule.onNodeWithText("Accept changes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { applicationRepository.observeApplication("app-1").first()?.changesAcceptedAt != null }
        }

        assertThat(runBlocking { applicationRepository.observeApplication("app-1").first()?.changesAcceptedAt }).isNotNull()
    }
}
