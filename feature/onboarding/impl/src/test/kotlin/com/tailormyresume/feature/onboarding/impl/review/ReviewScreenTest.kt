package com.tailormyresume.feature.onboarding.impl.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.onboarding.MarkProfileReviewedUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReviewScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val repository = TestProfileRepository()
    private var original: CandidateProfile? = null
    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(ReviewProfileNavKey())))

    private fun showRoute(profile: CandidateProfile) {
        original = profile
        repository.sendProfile(profile)
        val viewModel = ReviewViewModel(repository, MarkProfileReviewedUseCase(repository, TestClock()))
        composeRule.showFlowScreen { ReviewRoute(viewModel, navigator) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.nodeCount("Your profile") > 0
        }
    }

    private fun ComposeContentTestRule.nodeCount(text: String): Int = onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun needsFixStateShowsEveryPrototypeString() {
        showRoute(freshReviewProfile())

        listOf(
            "Your profile",
            "From Priya_Deshmukh_Resume.pdf. Tap anything to edit.",
            "90%",
            "Profile complete",
            "Required",
            "End date missing for Data Analyst Intern",
            "Add date",
            "Add your LinkedIn",
            "Optional",
            "Priya Deshmukh",
            "priya.deshmukh@gmail.com",
            "+91 98200 41736 · Pune",
            "Experience · 3",
            "Business Analyst",
            "Jul 2022 – Now",
            "Jun 2021 – end date?",
            "Jun 2020 – May 2021",
            "Skills · 18",
            "SQL",
            "+13",
            "Education",
            "B.Com, Finance",
            "Symbiosis College, Pune · 2020",
            "Looks right, continue",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun fixedStateShowsDoneBannerAndNoRequiredBanner() {
        showRoute(fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN))

        composeRule.onNodeWithText("All required info is done").assertIsDisplayed()
        composeRule.onNodeWithText("100%").assertIsDisplayed()
        composeRule.onNodeWithText("Add date").assertDoesNotExist()
        composeRule.onNodeWithText("Add your LinkedIn").assertDoesNotExist()
    }

    @Test
    fun tappingDisabledContinueShowsToast() {
        showRoute(freshReviewProfile())

        composeRule.onNodeWithText("Looks right, continue").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.nodeCount("Add the missing end date first") > 0
        }

        composeRule.onNodeWithText("Add the missing end date first").assertIsDisplayed()
        assertThat(navigator.state.currentKey).isEqualTo(ReviewProfileNavKey())
        assertThat(runBlocking { repository.observeProfile().first() }).isEqualTo(original)
    }
}
