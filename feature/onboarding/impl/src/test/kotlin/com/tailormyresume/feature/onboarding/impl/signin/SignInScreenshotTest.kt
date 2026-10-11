package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.screenshot.TmrTestDevices
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SignInScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun captureStory(index: Int, name: String) {
        val story = SignInStoryState()
        composeRule.showSignIn { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story) }
        composeRule.runOnIdle {
            repeat(index) { story.step(1) }
            story.tick(2_000)
        }
        composeRule.captureSignIn(name)
    }

    @Test
    fun story1() = captureStory(0, "signin_story1")

    @Test
    fun story2() = captureStory(1, "signin_story2")

    @Test
    fun story3() = captureStory(2, "signin_story3")

    @Test
    fun signingIn() {
        reduceMotion()
        val gate = CompletableDeferred<SignInResult>()
        composeRule.showSignIn { SignInRoute(SignInViewModel(ScriptedSignInGateway { gate.await() })) }
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.captureSignIn("signin_signing_in")
        assertThat(gate.isCompleted).isFalse()
    }

    @Test
    fun cancelledToast() {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInRoute(SignInViewModel(ScriptedSignInGateway { SignInResult.Cancelled })) }
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureSignIn("signin_cancelled_toast")
    }
}
