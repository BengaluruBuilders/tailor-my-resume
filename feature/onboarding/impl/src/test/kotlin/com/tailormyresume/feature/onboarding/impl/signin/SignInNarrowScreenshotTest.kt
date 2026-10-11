package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.domain.SignInResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = NARROW_QUALIFIERS)
class SignInNarrowScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun captureStory(index: Int) {
        val story = SignInStoryState()
        composeRule.showSignIn(fontScale = NARROW_DEVICE.fontScale) {
            SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story)
        }
        composeRule.runOnIdle {
            repeat(index) { story.step(1) }
            story.tick(2_000)
        }
        composeRule.captureSignIn("signin_story${index + 1}", NARROW_DEVICE)
    }

    @Test
    fun story1() = captureStory(0)

    @Test
    fun story3() = captureStory(2)

    @Test
    fun cancelledToast() {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn(fontScale = NARROW_DEVICE.fontScale) {
            SignInRoute(SignInViewModel(ScriptedSignInGateway { SignInResult.Cancelled }))
        }
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureSignIn("signin_cancelled_toast", NARROW_DEVICE)
    }
}
