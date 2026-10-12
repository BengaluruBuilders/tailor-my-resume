package com.tailormyresume.feature.onboarding.impl.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.onboarding.MarkProfileReviewedUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
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
            composeRule.nodeCount("Review your profile") > 0
        }
    }

    private fun ComposeContentTestRule.nodeCount(text: String): Int = onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun needsFixStateShowsEveryPrototypeString() {
        showRoute(freshReviewProfile())

        listOf(
            "Review your profile",
            "From Priya_Deshmukh_Resume.pdf",
            "REQUIRED",
            "End date missing for Data Analyst Intern",
            "Add date",
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
            "Education",
            "B.Com, Finance",
            "Symbiosis College, Pune · 2020",
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithText("Looks right, continue").assertIsDisplayed()
    }

    @Test
    fun fixedStateShowsDoneBannerAndNoRequiredBanner() {
        showRoute(fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN))

        composeRule.onNodeWithText("No missing end dates").assertIsDisplayed()
        composeRule.onNodeWithText("Profile complete").assertDoesNotExist()
        composeRule.onNodeWithText("Add date").assertDoesNotExist()
        composeRule.onNodeWithText("Add your LinkedIn").assertDoesNotExist()
    }

    @Test
    fun showsEveryBulletAndSkillBeforeContinue() {
        val splitRole = importedRole("r1").copy(bullets = listOf(EvidenceBullet("r1-b1", "First role fact")))
        val continuation = splitRole.copy(id = "r2", bullets = listOf(EvidenceBullet("r2-b1", "Second role fact")))
        val education = importedRole("e1", title = "B.Com", organization = "College", startDate = "2018", endDate = "2020")
            .copy(category = EntryCategory.EDUCATION, bullets = listOf(EvidenceBullet("e1-b1", "Education fact")))
        showRoute(fixedReviewProfile().copy(entries = listOf(splitRole, continuation, education), skills = listOf("SQL", "Kotlin", "Python", "Room", "Compose", "Coroutines")))

        listOf("• First role fact", "• Second role fact", "• Education fact", "SQL", "Kotlin", "Python", "Room", "Compose", "Coroutines").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Looks right, continue").assertIsDisplayed()
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
