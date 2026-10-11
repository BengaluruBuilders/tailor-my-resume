package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import com.tailormyresume.feature.onboarding.impl.upload.captureFlowScreen
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReviewScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, state: ReviewUiState.Ready) {
        reduceMotion()
        composeRule.showFlowScreen(content = noopReview(state))
        composeRule.captureFlowScreen(name)
    }

    @Test
    fun needsFix90() = capture("review_needs_fix", freshReviewProfile().reviewState())

    @Test
    fun fixed96() = capture("review_fixed", fixedReviewProfile().reviewState())

    @Test
    fun linkedinAdded100() =
        capture("review_linkedin_added", fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN).reviewState())
}
