package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadErrorNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeRead
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeTextSource
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import com.tailormyresume.feature.onboarding.impl.reading.ReadingRoute
import com.tailormyresume.feature.onboarding.impl.reading.ReadingViewModel
import com.tailormyresume.feature.onboarding.impl.signin.reduceMotion
import com.tailormyresume.feature.onboarding.impl.upload.NoopResumeFileResolver
import com.tailormyresume.feature.onboarding.impl.upload.UnreadableRoute
import com.tailormyresume.feature.onboarding.impl.upload.UnreadableViewModel
import com.tailormyresume.feature.onboarding.impl.upload.showFlowScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class OnboardingNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val draft = ResumeImportDraft(TestSessionRepository(), scope)
    private val file = ResumeFile("cv.pdf", RESUME_PDF_MIME, 2_048L, "content://cv")

    @After
    fun tearDown() = scope.cancel()

    private fun navigatorOf(vararg keys: NavKey) = Navigator(NavigationState(NavBackStack<NavKey>(*keys)))

    @Test
    fun readingReplacedByReviewAndBackReachesUpload() {
        reduceMotion()
        val navigator = navigatorOf(UploadNavKey(), ReadingNavKey())
        draft.setFile(file)
        val profile = TestProfileRepository()
        val parsed = CandidateProfile("Priya", "", "", "", listOf("SQL"), emptyList())
        val viewModel = ReadingViewModel(
            draft,
            object : ResumeTextSource {
                override suspend fun read(file: ResumeFile): ResumeRead = ResumeRead.Text("Priya. SQL analyst.")
            },
            object : ResumeTextParser {
                override suspend fun parse(rawText: String): CandidateProfile = parsed
            },
            SaveImportedProfileUseCase(profile, FactIdAllocator()),
            profile,
            Dispatchers.Unconfined,
        )

        composeRule.showFlowScreen { ReadingRoute(viewModel, navigator) }
        composeRule.waitForIdle()

        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey(), ReviewProfileNavKey()).inOrder()
        navigator.goBack()
        assertThat(navigator.state.currentKey).isEqualTo(UploadNavKey())
    }

    @Test
    fun readingFailureReplacesWithUnreadable() {
        reduceMotion()
        val navigator = navigatorOf(UploadNavKey(), ReadingNavKey())
        draft.setFile(file)
        val profile = TestProfileRepository()
        val viewModel = ReadingViewModel(
            draft,
            object : ResumeTextSource {
                override suspend fun read(file: ResumeFile): ResumeRead = ResumeRead.NoTextLayer(file.displayName)
            },
            object : ResumeTextParser {
                override suspend fun parse(rawText: String): CandidateProfile = error("not reached")
            },
            SaveImportedProfileUseCase(profile, FactIdAllocator()),
            profile,
            Dispatchers.Unconfined,
        )

        composeRule.showFlowScreen { ReadingRoute(viewModel, navigator) }
        composeRule.waitForIdle()

        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey(), UploadErrorNavKey()).inOrder()
    }

    @Test
    fun unreadableReplacedNotStacked() {
        reduceMotion()
        val navigator = navigatorOf(UploadNavKey(), UploadErrorNavKey())
        draft.fail(UploadFailure(UploadFailureKind.ImageOnly, "scan.pdf", RESUME_PDF_MIME, 2_048L))

        val viewModel = UnreadableViewModel(draft, NoopResumeFileResolver)
        composeRule.showFlowScreen { UnreadableRoute(viewModel, navigator) }
        composeRule.onNodeWithText("Paste as text").performClick()
        composeRule.waitForIdle()

        assertThat(navigator.state.stack.toList()).containsExactly(UploadNavKey(), PasteResumeNavKey()).inOrder()
    }
}
