package com.tailormyresume.feature.onboarding.impl.reading

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.domain.onboarding.SaveImportedProfileUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeRead
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeTextSource
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import com.tailormyresume.feature.onboarding.impl.upload.readingStateAt
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ReadingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsFileLineTitlePercentRowsAndTime() {
        composeRule.showFlowScreen { ReadingScreen(readingStateAt(100)) }

        listOf(
            "Reading your resume",
            "Priya_Deshmukh_Resume.pdf",
            "PDF · 112 KB",
            "100%",
            "Takes about 20 seconds",
            "Contact details",
            "Work experience",
            "Education",
            "Skills",
            "Achievements",
            "Found",
            "3 roles",
            "1 degree",
            "12 found",
            "6 found",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun reducedMotionKeepsRowStateVisible() {
        reduceMotion()
        composeRule.showFlowScreen { ReadingScreen(readingStateAt(40)) }

        composeRule.onAllNodesWithText("Reading…").assertCountEquals(5)
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "In progress"))
            .assertCountEquals(5)
    }

    @Test
    fun hasNoBackControlAndSystemBackIsConsumed() {
        val navigator = Navigator(NavigationState(NavBackStack<NavKey>(UploadNavKey(), ReadingNavKey())))
        val pending = CompletableDeferred<ResumeRead>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val draft = ResumeImportDraft(TestSessionRepository(), scope)
        draft.setFile(ResumeFile("cv.pdf", RESUME_PDF_MIME, 2_048L, "content://cv"))
        val profile = TestProfileRepository()
        val viewModel = ReadingViewModel(
            draft,
            object : ResumeTextSource {
                override suspend fun read(file: ResumeFile): ResumeRead = pending.await()
            },
            object : ResumeTextParser {
                override suspend fun parse(rawText: String): CandidateProfile = error("not reached")
            },
            SaveImportedProfileUseCase(profile, FactIdAllocator()),
            profile,
            Dispatchers.Unconfined,
        )
        composeRule.showFlowScreen { ReadingRoute(viewModel, navigator) }

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertThat(composeRule.activity.isFinishing).isFalse()
        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey(), ReadingNavKey()).inOrder()
        scope.cancel()
    }
}
