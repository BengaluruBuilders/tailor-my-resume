package com.tailormyresume.feature.onboarding.impl.signin

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SMALL_QUALIFIERS = "w320dp-h568dp-normal-long-notround-any-480dpi-keyshidden-nonav"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignInConstrainedScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = SMALL_QUALIFIERS)
    fun smallPhone() = captureStories(TmrTestDevice("small-320x568", SMALL_QUALIFIERS, 1f))

    @Test
    @Config(qualifiers = SMALL_QUALIFIERS)
    fun smallPhoneLargeText() = captureStories(TmrTestDevice("small-320x568-font-200", SMALL_QUALIFIERS, 2f))

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun prototype() = captureStories(TmrTestDevices.prototype)

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun prototypeLargeText() = captureStories(TmrTestDevices.prototypeLargeFont)

    private fun captureStories(device: TmrTestDevice) {
        reduceMotion()
        val story = SignInStoryState()
        composeRule.showSignIn(fontScale = device.fontScale) {
            SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story)
        }
        SignInStories.indices.forEach { index ->
            composeRule.runOnIdle {
                if (index > 0) story.step(1)
                story.tick(2_000)
            }
            composeRule.captureSignIn("signin_constrained_story${index + 1}", device)
            val context = ApplicationProvider.getApplicationContext<Context>()
            composeRule.onNodeWithText(context.getString(SignInStories[index].subline), useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
            composeRule.onNodeWithText("Privacy Policy", substring = true).assertIsDisplayed()
            composeRule.captureSignIn("signin_constrained_story${index + 1}_explanation", device)
        }
    }
}
