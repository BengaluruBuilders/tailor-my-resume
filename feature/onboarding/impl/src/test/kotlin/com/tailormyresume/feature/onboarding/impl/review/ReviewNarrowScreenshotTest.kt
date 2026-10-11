package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
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
@Config(qualifiers = NARROW_QUALIFIERS)
class ReviewNarrowScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, state: ReviewUiState.Ready) {
        reduceMotion()
        composeRule.mainClock.autoAdvance = false
        composeRule.showFlowScreen(NARROW_DEVICE.fontScale, noopReview(state))
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.captureFlowScreen(name, NARROW_DEVICE)
    }

    @Test
    fun needsFix() = capture("review_needs_fix", freshReviewProfile().reviewState())

    @Test
    fun fixed() = capture("review_fixed", fixedReviewProfile().reviewState())
}
