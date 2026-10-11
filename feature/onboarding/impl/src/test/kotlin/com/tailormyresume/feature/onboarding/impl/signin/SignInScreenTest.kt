package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val headlines = listOf("Upload your resume", "Paste the job you want", "Get a resume made for it")

    private fun show(story: SignInStoryState, uiState: SignInUiState = SignInUiState.Ready, onContinue: () -> Unit = {}) =
        composeRule.showSignIn { SignInScreen(uiState, onContinue, story = story) }

    private fun stepTo(story: SignInStoryState, index: Int) {
        composeRule.runOnIdle { repeat(index - story.index) { story.step(1) } }
        composeRule.waitForIdle()
    }

    @Test
    fun storyContentPerIndex() {
        val story = SignInStoryState()
        show(story)

        headlines.forEachIndexed { index, headline ->
            stepTo(story, index)
            composeRule.onNodeWithText(headline).assertIsDisplayed()
            composeRule.onNodeWithText("Paige · ${index + 1} of 3", ignoreCase = true).assertIsDisplayed()
            composeRule.onNodeWithText("Tap · hold", ignoreCase = true).assertIsDisplayed()
        }
    }

    @Test
    fun autoAdvanceUsesMainClock() {
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(5_300)

        composeRule.onNodeWithText("Paige · 2 of 3", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun autoAdvanceStopsWhileSigningIn() {
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.SigningIn, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(12_000)

        composeRule.onNodeWithText("Paige · 1 of 3", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun holdShowsPausedHint() {
        val story = SignInStoryState()
        show(story)

        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true).performTouchInput {
            down(percentOffset(0.9f, 0.5f))
            advanceEventTime(400)
            move()
        }
        composeRule.onNodeWithText("Paused", ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true).performTouchInput { up() }

        composeRule.onNodeWithText("Tap · hold", ignoreCase = true).assertIsDisplayed()
        assertThat(story.index).isEqualTo(0)
    }

    @Test
    fun reducedMotionNeverAdvancesButTapsStep() {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(20_000)
        composeRule.onNodeWithText("Paige · 1 of 3", ignoreCase = true).assertIsDisplayed()

        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.9f, 0.5f)) }
        composeRule.onNodeWithText("Paige · 2 of 3", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun tapLeftThirdGoesBackAndWraps() {
        val story = SignInStoryState()
        show(story)

        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.1f, 0.5f)) }

        composeRule.onNodeWithText("Paige · 3 of 3", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun story3ShowsKeywordSticker() {
        val story = SignInStoryState()
        show(story)
        composeRule.onNodeWithText("92% keywords").assertDoesNotExist()

        stepTo(story, 2)

        composeRule.onNodeWithText("92% keywords").assertIsDisplayed()
        composeRule.onNodeWithText("match", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun signingInLabelReplacesContinue() {
        show(SignInStoryState(), SignInUiState.SigningIn)

        composeRule.onNodeWithText("Signing in…").assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Google").assertDoesNotExist()
    }

    @Test
    fun readyLabelTapsContinue() {
        var taps = 0
        show(SignInStoryState()) { taps += 1 }

        composeRule.onNodeWithText("Continue with Google").performTouchInput { click() }

        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun footerHasTermsPrivacyAndFreeLineAndNoApple() {
        show(SignInStoryState())

        composeRule.onNodeWithText("Your first tailored resume is free.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Terms", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Apple").assertDoesNotExist()
    }

    @Test
    fun termsAndPrivacyShowAddressNotSet() {
        show(SignInStoryState())

        tapFooterWord("Terms")
        composeRule.onNodeWithText("Address not set in this build").assertIsDisplayed()
    }

    @Test
    fun noEllipsisAtFont200() {
        composeRule.showSignIn(fontScale = 2f) { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        val node = composeRule.onNodeWithText("Upload your resume").fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        assertThat(layouts).isNotEmpty()
        assertThat(layouts.first().hasVisualOverflow).isFalse()
    }

    @Test
    fun storyAreaAnnouncesAndExposesNextPreviousActions() {
        val story = SignInStoryState()
        show(story)

        val config = composeRule.onNodeWithTag(SignInTags.STORY).fetchSemanticsNode().config
        val description = config[SemanticsProperties.ContentDescription].joinToString(" ")
        assertThat(description).contains("Upload your resume")
        assertThat(description).contains("PDF or DOCX")
        assertThat(description).contains("1 of 3")
        val labels = config[SemanticsActions.CustomActions].map { it.label }
        assertThat(labels).containsExactly("Next story", "Previous story")

        composeRule.runOnUiThread {
            config[SemanticsActions.CustomActions].first { it.label == "Next story" }.action()
        }
        composeRule.onNodeWithText("Paige · 2 of 3", ignoreCase = true).assertIsDisplayed()
    }

    private fun tapFooterWord(word: String) {
        val node = composeRule.onNodeWithText("Privacy Policy", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.first()
        val start = layout.layoutInput.text.text.indexOf(word)
        val box = layout.getBoundingBox(start + 1)
        node.performTouchInput { click(Offset(box.center.x, box.center.y)) }
    }
}
