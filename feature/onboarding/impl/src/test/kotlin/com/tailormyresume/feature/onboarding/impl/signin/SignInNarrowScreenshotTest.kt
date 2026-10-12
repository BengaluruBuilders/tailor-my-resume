package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun story2() = captureStory(1)

    @Test
    fun everyStoryKeepsSignInAvailableAtNarrowFont200() {
        val story = SignInStoryState()
        composeRule.showSignIn(fontScale = NARROW_DEVICE.fontScale) {
            SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story)
        }
        listOf(
            "Every job asks for something different. You shouldn’t have to start over every time.",
            "AI matches your real skills and achievements to each job description.",
            "Create a tailored resume for each job without rewriting it yourself. Review and approve each version before export.",
        ).forEachIndexed { index, subline ->
            composeRule.runOnIdle { repeat(index - story.index) { story.step(1) } }
            composeRule.onNodeWithText(subline).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun story3() = captureStory(2)

    @Test
    fun cancelledToast() {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        val viewModel = SignInViewModel(ScriptedSignInGateway { SignInResult.Cancelled })
        composeRule.showSignIn(fontScale = NARROW_DEVICE.fontScale) { SignInRoute(viewModel) }
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureSignIn("signin_cancelled_toast", NARROW_DEVICE)
    }
}
