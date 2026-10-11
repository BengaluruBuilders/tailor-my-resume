package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.SignInFailureReason
import com.tailormyresume.core.domain.SignInResult
import com.tailormyresume.core.screenshot.TmrTestDevices
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SignInRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showRoute(gateway: ScriptedSignInGateway) {
        reduceMotion()
        composeRule.showSignIn { SignInRoute(SignInViewModel(gateway)) }
    }

    @Test
    fun realRouteWithViewModelContinueTapSignsInOnce() {
        val gate = CompletableDeferred<SignInResult>()
        val gateway = ScriptedSignInGateway { gate.await() }
        showRoute(gateway)

        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Signing in…").performClick()
        composeRule.waitForIdle()

        assertThat(gateway.calls).isEqualTo(1)
        composeRule.onNodeWithText("Signing in…").assertIsDisplayed()
    }

    @Test
    fun cancelledShowsToastWithRetry() {
        showRoute(ScriptedSignInGateway { SignInResult.Cancelled })

        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test
    fun failureShowsTheSameToast() {
        showRoute(ScriptedSignInGateway { SignInResult.Failed(SignInFailureReason.ProviderUnavailable) })

        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertIsDisplayed()
    }

    @Test
    fun retryTapCallsSignInTwiceTotal() {
        val gateway = ScriptedSignInGateway { SignInResult.Cancelled }
        showRoute(gateway)
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()

        assertThat(gateway.calls).isEqualTo(2)
        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertIsDisplayed()
    }

    @Test
    fun rotationDoesNotShowTheToastAgain() {
        reduceMotion()
        val viewModel = SignInViewModel(ScriptedSignInGateway { SignInResult.Cancelled })
        val generation = mutableIntStateOf(0)
        composeRule.showSignIn { key(generation.intValue) { SignInRoute(viewModel) } }
        composeRule.onNodeWithText("Continue with Google").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(TOAST_TIMEOUT_MS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertDoesNotExist()

        composeRule.runOnIdle { generation.intValue += 1 }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Sign-in cancelled. Nothing was saved.").assertDoesNotExist()
    }
}

private const val TOAST_TIMEOUT_MS = 30_000L
