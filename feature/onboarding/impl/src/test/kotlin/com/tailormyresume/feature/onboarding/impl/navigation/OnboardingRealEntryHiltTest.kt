package com.tailormyresume.feature.onboarding.impl.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
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
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
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

@AndroidEntryPoint
class OnboardingEntryHostActivity : ComponentActivity()

@Module
@InstallIn(SingletonComponent::class)
object FixedResumeParserModule {
    @Provides
    fun parser(): ResumeTextParser = object : ResumeTextParser {
        override suspend fun parse(rawText: String): CandidateProfile =
            CandidateProfile("Priya Deshmukh", "", "", "", emptyList(), emptyList())
    }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class OnboardingRealEntryHiltTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<OnboardingEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, OnboardingEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    private fun show(navigator: Navigator, key: NavKey) {
        hiltRule.inject()
        reduceMotion()
        val provider = entryProvider { onboardingEntry(navigator) }
        composeRule.setContent { TmrTheme { provider(key).Content() } }
        composeRule.waitForIdle()
    }

    private fun navigatorOf(vararg keys: NavKey) = Navigator(NavigationState(NavBackStack<NavKey>(*keys)))

    @Test
    fun uploadPasteAsTextReachesPasteResume() {
        val navigator = navigatorOf(UploadNavKey())
        show(navigator, UploadNavKey())

        composeRule.onNodeWithText("Paste as text").performClick()
        composeRule.waitForIdle()

        assertThat(navigator.state.currentKey).isEqualTo(PasteResumeNavKey())
    }

    @Test
    fun pasteReadTextReachesReading() {
        val navigator = navigatorOf(UploadNavKey(), PasteResumeNavKey())
        show(navigator, PasteResumeNavKey())

        composeRule.onNode(hasSetTextAction()).performTextInput("Priya Deshmukh. Business analyst at Northwind Traders since 2021.")
        composeRule.onNodeWithText("Read text").performClick()
        composeRule.waitForIdle()

        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey(), ReadingNavKey()).inOrder()
    }

    @Test
    fun readingWithEmptyDraftReplacesWithUpload() {
        val navigator = navigatorOf(SignInNavKey(), ReadingNavKey())
        show(navigator, ReadingNavKey())
        composeRule.waitForIdle()

        assertThat(navigator.state.stack.toList()).containsExactly(SignInNavKey(), UploadNavKey()).inOrder()
    }
}
