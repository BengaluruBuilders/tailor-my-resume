package com.tailormyresume.feature.settings.impl.delete

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class DeleteAccountSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private val host = TmrSheetHostState()
    private val toast = TmrToastState()

    private fun showHosted(fixture: DeleteFixture) {
        val viewModel = fixture.viewModel()
        rule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalTmrSheetHost provides host, LocalTmrToast provides toast) {
                    TmrSheetHost(host)
                    TmrToastHost(toast)
                }
            }
        }
        rule.runOnIdle {
            host.show("Delete your account?") { DeleteAccountSheet(viewModel, onKeep = host::dismiss) }
        }
        rule.waitForIdle()
    }

    @Test
    fun bodyPluralisesCounts() {
        var state by mutableStateOf(contentState(applications = 1, unusedCredits = 1))
        rule.setContent {
            TmrTheme { DeleteAccountSheetContent(state, onTextChanged = {}, onDelete = {}, onKeep = {}) }
        }

        rule.onNodeWithText(
            "This removes your profile, 1 tailored resume and your application history. " +
                "Your 1 unused credit will be lost. This can't be undone.",
        ).assertIsDisplayed()

        state = contentState(applications = 3, unusedCredits = 2)
        rule.waitForIdle()

        rule.onNodeWithText(
            "This removes your profile, 3 tailored resumes and your application history. " +
                "Your 2 unused credits will be lost. This can't be undone.",
        ).assertIsDisplayed()
    }

    @Test
    fun deleteButtonDoesNothingUntilConfirmed() {
        var deletes = 0
        var state by mutableStateOf(contentState(typed = "DELET", canDelete = false))
        rule.setContent {
            TmrTheme { DeleteAccountSheetContent(state, onTextChanged = {}, onDelete = { deletes += 1 }, onKeep = {}) }
        }

        rule.onNodeWithText("Delete account").performScrollTo().performClick()
        assertThat(deletes).isEqualTo(0)

        state = contentState(typed = "delete", canDelete = true)
        rule.waitForIdle()
        rule.onNodeWithText("Delete account").performScrollTo().performClick()

        assertThat(deletes).isEqualTo(1)
    }

    @Test
    fun keepMyAccountDismissesWithoutDelete() {
        val fixture = DeleteFixture()
        showHosted(fixture)
        rule.onNodeWithText("Delete your account?").assertIsDisplayed()

        rule.onNodeWithText("Keep my account").performClick()
        rule.waitForIdle()

        assertThat(host.current).isNull()
        assertThat(fixture.deleter.deleteCalls).isEqualTo(0)
    }

    @Test
    fun scrimTapDismissesWithoutDelete() {
        val fixture = DeleteFixture()
        showHosted(fixture)

        rule.onNode(hasTestTag("tmr_chrome_scrim")).performTouchInput { click(Offset(5f, 5f)) }
        rule.waitForIdle()

        assertThat(host.current).isNull()
        assertThat(fixture.deleter.deleteCalls).isEqualTo(0)
    }

    @Test
    fun typingDeleteEnablesTheButtonAndDeletes() {
        val fixture = DeleteFixture()
        runBlocking { fixture.seed() }
        showHosted(fixture)

        rule.onNode(hasSetTextAction()).performTextInput("Delete")
        rule.waitForIdle()
        rule.onNodeWithText("Delete account").performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(fixture.deleter.deleteCalls).isEqualTo(1)
        assertThat(runBlocking { fixture.remainingApplications() }).isEqualTo(0)
    }

    @Test
    fun failedDeleteKeepsTheSheetOpenWithOneToast() {
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(failure = IOException("offline")))
        runBlocking { fixture.seed() }
        showHosted(fixture)

        rule.onNode(hasSetTextAction()).performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithText("Delete account").performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't delete your account. Try again.").assertIsDisplayed()
        rule.onNodeWithText("Delete your account?").assertIsDisplayed()
        assertThat(host.current).isNotNull()
        assertThat(runBlocking { fixture.remainingApplications() }).isEqualTo(3)
    }

    @Test
    fun keepAndDismissDoNothingWhileTheDeleteIsInFlight() {
        val gate = CompletableDeferred<Unit>()
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(gate = gate))
        runBlocking { fixture.seed() }
        showHosted(fixture)

        rule.onNode(hasSetTextAction()).performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithText("Delete account").performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Keep my account").performScrollTo().assertIsNotEnabled()
        rule.onNode(hasTestTag("tmr_chrome_scrim")).performTouchInput { click(Offset(5f, 5f)) }
        rule.waitForIdle()
        assertThat(host.current).isNotNull()

        gate.complete(Unit)
        rule.waitForIdle()
        assertThat(runBlocking { fixture.remainingApplications() }).isEqualTo(0)
    }

    @Test
    fun keepIsAvailableAgainAfterAFailedDelete() {
        val gate = CompletableDeferred<Unit>()
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(failure = IOException("offline"), gate = gate))
        runBlocking { fixture.seed() }
        showHosted(fixture)
        rule.onNode(hasSetTextAction()).performTextInput("DELETE")
        rule.waitForIdle()
        rule.onNodeWithText("Delete account").performScrollTo().performClick()
        rule.waitForIdle()

        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithText("Keep my account").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Keep my account").performClick()
        rule.waitForIdle()

        assertThat(host.current).isNull()
    }

    private fun contentState(
        applications: Int = 3,
        unusedCredits: Int = 2,
        typed: String = "",
        canDelete: Boolean = false,
    ) = DeleteAccountUiState.Content(
        applications = applications,
        unusedCredits = unusedCredits,
        typed = typed,
        canDelete = canDelete,
        deleting = false,
    )
}
