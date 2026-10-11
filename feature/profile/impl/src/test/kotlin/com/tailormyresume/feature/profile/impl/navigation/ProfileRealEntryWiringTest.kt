package com.tailormyresume.feature.profile.impl.navigation

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.navigation.ChromeActions
import com.tailormyresume.core.navigation.LocalChromeActions
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

@AndroidEntryPoint
class ProfileEntryHostActivity : ComponentActivity()

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ProfileRealEntryWiringTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<ProfileEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, ProfileEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var repository: ProfileRepository

    private val state = NavigationState(NavBackStack<NavKey>(ProfileNavKey()))
    private val navigator = Navigator(state)
    private val chromeActions = ChromeActions()
    private val provider = entryProvider { profileEntry(navigator) }

    @Before
    fun seed() {
        hiltRule.inject()
        runBlocking { repository.saveProfile(PrototypeFixtures.returning().profile) }
    }

    private fun show(key: NavKey) {
        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalChromeActions provides chromeActions) { provider(key).Content() }
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.onAllNodes(hasText(readyText(key))).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun readyText(key: NavKey) = when (key) {
        is ProfileNavKey -> "Experience"
        else -> "+ Add role"
    }

    @Test
    fun profileRowTap_opensExperience() {
        show(ProfileNavKey())

        composeRule.onNodeWithText("Experience").performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(ExperienceNavKey())
    }

    @Test
    fun settingsGearTap_opensSettings() {
        show(ProfileNavKey())

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(SettingsNavKey())
    }

    @Test
    fun saveAction_isDisabledUntilTitleAndCompanyAreFilled() {
        show(EditRoleNavKey(null))
        assertThat(chromeActions.enabled).isFalse()

        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Intern")
        fields[1].performTextInput("Acme")
        composeRule.waitForIdle()

        assertThat(chromeActions.enabled).isTrue()
    }

    @Test
    fun addRole_opensEditorForNewRole() {
        show(ExperienceNavKey())

        composeRule.onNodeWithText("+ Add role").performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(EditRoleNavKey(null))
    }

    @Test
    fun saveNewRole_storesRoleInProfile() {
        show(EditRoleNavKey(null))

        val fields = composeRule.onAllNodes(hasSetTextAction())
        listOf("Intern", "Acme", "Jan 2019", "Mar 2019", "Built a report").forEachIndexed { index, text ->
            fields[index].performTextInput(text)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { checkNotNull(chromeActions.handler).invoke() }
        composeRule.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            savedRoles().any { it.title == "Intern" }
        }

        assertThat(savedRoles().single { it.title == "Intern" }.organization).isEqualTo("Acme")
    }

    private fun savedRoles() = runBlocking { repository.observeProfile().first() }
        ?.entries.orEmpty().filter { it.category == EntryCategory.EXPERIENCE }
}
