package com.tailormyresume.feature.onboarding.impl.review

import androidx.activity.ComponentActivity
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
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReviewRouteNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val repository = TestProfileRepository()
    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(ReviewProfileNavKey())))

    private fun show(profile: CandidateProfile?) {
        repository.sendProfile(profile)
        val viewModel = ReviewViewModel(repository, MarkProfileReviewedUseCase(repository, TestClock()))
        composeRule.showFlowScreen { ReviewRoute(viewModel, navigator) }
        if (profile != null) {
            composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.onAllNodesWithText("Your profile").fetchSemanticsNodes().isNotEmpty() }
        }
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performClick()

    @Test
    fun addDateOpensEditRoleWithGapEntryId() {
        show(freshReviewProfile())

        tap("Add date")

        assertThat(navigator.state.currentKey).isEqualTo(EditRoleNavKey(entryId = "exp-tata"))
    }

    @Test
    fun contactAndLinkedinOpenEditContact() {
        show(freshReviewProfile())

        tap("Add your LinkedIn")
        assertThat(navigator.state.currentKey).isEqualTo(EditContactNavKey())

        navigator.goBack()
        tap("Priya Deshmukh")
        assertThat(navigator.state.currentKey).isEqualTo(EditContactNavKey())
    }

    @Test
    fun experienceOpensExperience() {
        show(freshReviewProfile())

        tap("Experience · 3")

        assertThat(navigator.state.currentKey).isEqualTo(ExperienceNavKey())
    }

    @Test
    fun skillsOpensSkills() {
        show(freshReviewProfile())

        composeRule.onNodeWithText("Skills · 18").performScrollTo()
        tap("Skills · 18")

        assertThat(navigator.state.currentKey).isEqualTo(SkillsNavKey())
    }

    @Test
    fun educationCardIsNotTappable() {
        show(freshReviewProfile())

        tap("B.Com, Finance")

        assertThat(navigator.state.currentKey).isEqualTo(ReviewProfileNavKey())
    }

    @Test
    fun continuePushesJobNavKey() {
        show(fixedReviewProfile())

        tap("Looks right, continue")
        composeRule.waitUntil(timeoutMillis = 5_000) { navigator.state.currentKey == JobNavKey() }

        assertThat(navigator.state.stack.toList()).containsExactly(ReviewProfileNavKey(), JobNavKey()).inOrder()
    }

    @Test
    fun emptyProfileReplacesWithUpload() {
        show(null)
        composeRule.waitUntil(timeoutMillis = 5_000) { navigator.state.currentKey == UploadNavKey() }

        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey())
    }
}
