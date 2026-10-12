package com.tailormyresume.feature.onboarding.impl.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInResponsiveLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun storyContentScrollsWhileSignInAndFooterRemainReachableAcrossSizesAndTextScales() {
        val scenario = mutableStateOf(Scenario(DpSize(320.dp, 480.dp), 1f, SignInStoryState()))
        composeRule.setContent {
            val device = DeviceConfigurationOverride.ForcedSize(scenario.value.size) then
                DeviceConfigurationOverride.FontScale(scenario.value.fontScale)
            DeviceConfigurationOverride(device) {
                TmrTheme {
                    val toast = remember { TmrToastState() }
                    CompositionLocalProvider(LocalTmrToast provides toast) {
                        Box(Modifier.fillMaxSize().background(TmrTheme.colors.background)) {
                            SignInScreen(
                                SignInUiState.Ready,
                                onContinueWithGoogle = {},
                                story = scenario.value.story,
                            )
                            TmrToastHost(toast)
                        }
                    }
                }
            }
        }

        listOf(
            DpSize(320.dp, 480.dp),
            DpSize(320.dp, 640.dp),
            DpSize(374.dp, 640.dp),
            DpSize(374.dp, 834.dp),
            DpSize(427.dp, 834.dp),
        ).forEach { size ->
            listOf(1f, 1.5f, 2f).forEach { fontScale ->
                val story = SignInStoryState()
                composeRule.runOnIdle { scenario.value = Scenario(size, fontScale, story) }
                composeRule.waitForIdle()
                assertStories(story)
            }
        }
    }

    private fun assertStories(story: SignInStoryState) {
        SignInStories.indices.forEach { index ->
            composeRule.runOnIdle { repeat(index - story.index) { story.step(1) } }
            val scrollRange = composeRule.onNodeWithTag(SignInTags.STORY)
                .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertThat(scrollRange.value()).isEqualTo(0f)
            val google = composeRule.onNodeWithTag(SignInTags.GOOGLE_ACTION).unclippedBoundsInRootPx()
            val footer = composeRule.onNodeWithText("Privacy Policy", substring = true).unclippedBoundsInRootPx()
            val root = composeRule.onRoot().unclippedBoundsInRootPx()
            assertThat(google.top).isAtLeast(root.top)
            assertThat(google.bottom).isAtMost(root.bottom)
            assertThat(footer.bottom).isAtMost(root.bottom)

            val headline = composeRule.onNodeWithText(headlines[index], useUnmergedTree = true)
            headline.performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText(sublines[index], useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed()
        }
    }

    private fun SemanticsNodeInteraction.unclippedBoundsInRootPx(): Rect {
        val density = fetchSemanticsNode().layoutInfo.density.density
        val bounds = getUnclippedBoundsInRoot()
        return Rect(
            bounds.left.value * density,
            bounds.top.value * density,
            bounds.right.value * density,
            bounds.bottom.value * density,
        )
    }

    private data class Scenario(
        val size: DpSize,
        val fontScale: Float,
        val story: SignInStoryState,
    )

    private companion object {
        val sublines = listOf(
            "Have your resume or work details and the full job description ready.",
            "AI matches your real skills and achievements to each job description.",
            "Create a tailored resume for each job without rewriting it yourself. Review and approve each version before export.",
        )
        val headlines = listOf(
            "Tired of rewriting your resume?",
            "Built around the job. Backed by your experience.",
            "Apply smarter, not harder.",
        )
    }
}
