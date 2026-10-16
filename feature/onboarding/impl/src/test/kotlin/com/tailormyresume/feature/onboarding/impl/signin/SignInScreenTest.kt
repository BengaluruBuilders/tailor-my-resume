package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val headlines = listOf(
        "Tired of rewriting your resume?",
        "Built around the job. Backed by your experience.",
        "Apply smarter, not harder.",
    )

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
            composeRule.onNodeWithText("How it works · ${index + 1} of 3", ignoreCase = true).assertDoesNotExist()
            composeRule.onNodeWithText("Tap to change story · Hold to pause", ignoreCase = true).assertIsDisplayed()
            composeRule.onNodeWithText("Sign in to get started.").assertDoesNotExist()
            if (index == 0) {
                composeRule.onNodeWithText(
                    "Have your resume or work details and the full job description ready.",
                ).assertIsDisplayed()
            } else {
                composeRule.onNodeWithText(
                    "Have your resume or work details and the full job description ready.",
                ).assertDoesNotExist()
            }
            composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        }
    }

    @Test
    fun signInActionStaysVisibleAndStableAcrossStories() {
        val story = SignInStoryState()
        show(story)
        val googlePositions = mutableListOf<Dp>()

        headlines.indices.forEach { index ->
            stepTo(story, index)
            val google = composeRule.onNodeWithTag(SignInTags.GOOGLE_ACTION).getUnclippedBoundsInRoot()
            val screen = composeRule.onRoot().getUnclippedBoundsInRoot()

            assertThat(google.top.value).isAtLeast(screen.top.value)
            assertThat(google.bottom.value).isAtMost(screen.bottom.value)
            googlePositions.add(google.top)
        }

        googlePositions.forEach { position ->
            assertThat(abs((position - googlePositions.first()).value)).isAtMost(1.dp.value)
        }
    }

    @Test
    fun autoAdvanceUsesMainClock() {
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(5_300)

        composeRule.onNodeWithText("Built around the job. Backed by your experience.").assertIsDisplayed()
    }

    @Test
    fun autoAdvanceStopsWhileSigningIn() {
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.SigningIn, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(12_000)

        composeRule.onNodeWithText("Tired of rewriting your resume?").assertIsDisplayed()
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

        composeRule.onNodeWithText("Tap to change story · Hold to pause", ignoreCase = true).assertIsDisplayed()
        assertThat(story.index).isEqualTo(0)
    }

    @Test
    fun verticalSwipeScrollsStoryWithoutChangingIt() {
        val story = SignInStoryState()
        composeRule.showSignIn(fontScale = 2f) {
            SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story)
        }
        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertThat(story.index).isEqualTo(0)
        composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = TmrTestDevices.SMALL_PHONE_QUALIFIERS)
    fun readingScrolledExplanationPausesAutoAdvanceUntilTheNextStory() {
        composeRule.mainClock.autoAdvance = false
        lateinit var story: SignInStoryState
        composeRule.showSignIn(fontScale = 2f) {
            story = rememberSignInStoryState(false)
            SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}, story = story)
        }
        composeRule.onNodeWithTag(SignInTags.STORY).performTouchInput { swipeUp() }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertThat(story.reading).isTrue()

        composeRule.mainClock.advanceTimeBy(12_000)

        assertThat(story.index).isEqualTo(0)
        composeRule.onNodeWithText("Have your resume or work details and the full job description ready.").assertIsDisplayed()
        val config = composeRule.onNodeWithTag(SignInTags.STORY).fetchSemanticsNode().config
        composeRule.runOnUiThread {
            config[SemanticsActions.CustomActions].first { it.label == "Next story" }.action()
        }
        composeRule.mainClock.advanceTimeBy(32)
        assertThat(story.reading).isFalse()
        assertThat(story.index).isEqualTo(1)
        composeRule.mainClock.advanceTimeBy(5_300)
        assertThat(story.index).isEqualTo(2)
        composeRule.onNodeWithText("Apply smarter, not harder.", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun reducedMotionAutoAdvancesAndTapsStep() {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showSignIn { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        composeRule.mainClock.advanceTimeBy(5_300)
        composeRule.onNodeWithText("Built around the job. Backed by your experience.").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.9f, 0.5f)) }
        composeRule.onNodeWithText("Apply smarter, not harder.").assertIsDisplayed()
    }

    @Test
    fun tapLeftThirdGoesBackAndWraps() {
        val story = SignInStoryState()
        show(story)

        composeRule.onNodeWithTag(SignInTags.STORY_TOUCH, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.1f, 0.5f)) }

        composeRule.onNodeWithText("Apply smarter, not harder.").assertIsDisplayed()
    }

    @Test
    fun story3ShowsJobPurposeSticker() {
        val story = SignInStoryState()
        show(story)
        composeRule.onNodeWithText("For this job").assertDoesNotExist()

        stepTo(story, 2)

        composeRule.onNodeWithText("For this job").assertIsDisplayed()
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
    fun signingInDoesNotTapContinue() {
        var taps = 0
        show(SignInStoryState(), SignInUiState.SigningIn) { taps += 1 }

        composeRule.onNodeWithText("Signing in…").performTouchInput { click() }

        assertThat(taps).isEqualTo(0)
    }

    @Test
    fun footerHasTermsPrivacyAndNoApple() {
        show(SignInStoryState())

        composeRule.onNodeWithText("Continue with Google").assertHasClickAction()
        val footer = composeRule.onNodeWithText("Privacy Policy", substring = true).fetchSemanticsNode()
        val text = footer.config[SemanticsProperties.Text].single()
        val links = text.getLinkAnnotations(0, text.length)
        assertThat(links.map { text.subSequence(it.start, it.end).text }).containsExactly("Terms", "Privacy Policy")
        links.forEach { assertThat(it.item).isInstanceOf(LinkAnnotation.Clickable::class.java) }
        composeRule.onNodeWithText("Your first tailored resume is free.", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Terms", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Apple").assertDoesNotExist()
    }

    @Test
    fun termsAndPrivacyShowAddressNotSet() {
        show(SignInStoryState())

        tapFooterWord("Terms")
        composeRule.onNodeWithText("Address not set in this build").assertIsDisplayed()
        tapFooterWord("Privacy Policy")
        composeRule.onNodeWithText("Address not set in this build").assertIsDisplayed()
    }

    @Test
    fun noEllipsisAtFont200() {
        composeRule.showSignIn(fontScale = 2f) { SignInScreen(SignInUiState.Ready, onContinueWithGoogle = {}) }

        val node = composeRule.onNodeWithText("Tired of rewriting your resume?", useUnmergedTree = true).fetchSemanticsNode()
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
        assertThat(description).contains("Tired of rewriting your resume?")
        assertThat(description).contains("Have your resume or work details and the full job description ready")
        assertThat(description).contains("1 of 3")
        val labels = config[SemanticsActions.CustomActions].map { it.label }
        assertThat(labels).containsExactly("Next story", "Previous story")

        composeRule.runOnUiThread {
            config[SemanticsActions.CustomActions].first { it.label == "Next story" }.action()
        }
        composeRule.onNodeWithText("Built around the job. Backed by your experience.").assertIsDisplayed()
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
