package com.tailormyresume.feature.tailor.impl.exported

import android.content.Intent
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.testAccountCreditBalance
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.impl.acceptedApplication
import com.tailormyresume.feature.tailor.impl.acceptedBullet
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.export.ExportDirectory
import com.tailormyresume.feature.tailor.impl.export.ExportResumeUseCase
import com.tailormyresume.feature.tailor.impl.export.FakeResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ExportedRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val clock = TestClock()
    private val toast = TmrToastState()
    private val state = NavigationState(NavBackStack<NavKey>(ExportedNavKey("app-1")))
    private val navigator = Navigator(state)
    private val bullet = acceptedBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")

    private fun viewModel(accepted: Boolean): ExportedViewModel {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet), accepted = accepted)))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))).copy(fullName = "Priya Deshmukh"))
        val directory = folder.newFolder()
        return ExportedViewModel(
            applicationRepository = applicationRepository,
            creditsRepository = TestCreditsRepository(),
            creditBalance = testAccountCreditBalance(TestPaymentGateway().withFreeCredits(0)),
            exportResume = ExportResumeUseCase(
                applicationRepository,
                profileRepository,
                TestResumeSettingsRepository(),
                TestExportHistoryRepository(),
                ResumeDocumentAssembler(TestResumeHeadings),
                FakeResumePdfRenderer(directory),
                clock,
            ),
            setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, clock),
            fileStore = object : ExportedFileStore {
                override fun fileFor(applicationId: String, fileName: String): File? =
                    ExportDirectory(directory).existing(applicationId, fileName)
            },
            exportedPdfIntents = object : ExportedPdfIntents {
                override suspend fun shareIntent(applicationId: String): Intent? = null

                override suspend fun viewIntent(applicationId: String): Intent? = null
            },
            savedStateHandle = SavedStateHandle(),
            ioDispatcher = UnconfinedTestDispatcher(),
            applicationId = "app-1",
        )
    }

    private fun show(viewModel: ExportedViewModel) {
        composeRule.setContent {
            TmrTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalTmrToast provides toast) {
                    ExportedRoute(
                        viewModel = viewModel,
                        onGoToApplications = { navigator.root(DefaultApplicationsNavKey) },
                        onBack = { navigator.goBack() },
                    )
                    TmrToastHost(state = toast)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun goToApplicationsRootsApplications() {
        show(viewModel(accepted = true))

        composeRule.onNodeWithText("Go to Applications").performClick()
        composeRule.waitForIdle()

        assertThat(state.stack.toList()).containsExactly(DefaultApplicationsNavKey)
    }

    @Test
    fun comingSoonNoteHasNoAction() {
        show(viewModel(accepted = true))

        composeRule.onNodeWithText("Coming soon: prep questions and cover letters.").assertHasNoClickAction()
        assertThat(state.stack.toList()).containsExactly(ExportedNavKey("app-1"))
    }

    @Test
    fun markAppliedShowsTheToastWithUndo() {
        show(viewModel(accepted = true))

        composeRule.onNodeWithText("✓ Mark as Applied").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Undo").assertExists()
        assertThat(toast.current?.message).startsWith("Applied, marked on ")
    }

    @Test
    fun withoutAcceptTheRouteWarnsAndGoesBack() {
        show(viewModel(accepted = false))

        assertThat(toast.current?.message).isEqualTo("Accept the changes first")
        composeRule.onNodeWithText("Resume exported").assertDoesNotExist()
    }
}
