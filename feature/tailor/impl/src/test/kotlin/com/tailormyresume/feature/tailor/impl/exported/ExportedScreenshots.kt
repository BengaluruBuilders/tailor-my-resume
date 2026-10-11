package com.tailormyresume.feature.tailor.impl.exported

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tailormyresume.core.designsystem.component.chrome.TmrToastAction
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.tailor.impl.NARROW_DEVICE
import com.tailormyresume.feature.tailor.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.tailor.impl.captureResultScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ExportedScreenshots {
    @get:Rule
    val rule = createComposeRule()

    private fun ready(
        creditsLeft: Int = 2,
        freeResumeUsed: Boolean = false,
        status: ApplicationStatus = ApplicationStatus.SAVED,
        markedOn: String? = null,
        statusSheet: ApplicationStatus? = null,
    ) = ExportedUiState.Ready(
        fileName = "Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf",
        pageCount = 1,
        sizeKb = 48,
        creditsLeft = creditsLeft,
        freeResumeUsed = freeResumeUsed,
        jobTitle = "Associate Analyst",
        company = "Northwind GCC",
        status = status,
        markedOn = markedOn,
        statusSheet = statusSheet,
    )

    private fun shoot(
        screenName: String,
        state: ExportedUiState.Ready,
        device: TmrTestDevice,
        beforeCapture: () -> Unit = {},
        extra: @Composable () -> Unit = {},
    ) {
        rule.captureResultScreen(screenName, device, beforeCapture) {
            ExportedScreen(
                state = state,
                onShare = {},
                onOpen = {},
                onMarkApplied = {},
                onChangeStatus = {},
                onPickStatus = {},
                onDismissStatusSheet = {},
                onSaveStatus = {},
                onSoon = {},
                onGoToApplications = {},
            )
            extra()
        }
    }

    private fun assertPaidTexts() {
        rule.onNodeWithText("Resume exported").assertExists()
        rule.onNodeWithText("Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf").assertExists()
        rule.onNodeWithText("PDF · 1 page · 48 KB").assertExists()
        rule.onNodeWithText("Share").assertExists()
        rule.onNodeWithText("Open").assertExists()
        rule.onNodeWithText("2 left, was 3. Credits never expire.").assertExists()
        rule.onNodeWithText("Saved to Applications: Associate Analyst, Northwind GCC").assertExists()
        rule.onNodeWithText("Did you apply?").assertExists()
        rule.onNodeWithText("✓ Mark as Applied").assertExists()
        rule.onNodeWithText("Get prep questions").assertExists()
        rule.onNodeWithText("Write a cover letter").assertExists()
        rule.onNodeWithText("Go to Applications").assertExists()
    }

    @Test
    fun exportedPaid() {
        shoot("export_exported_paid", ready(), TmrTestDevices.prototype)
        assertPaidTexts()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun exportedPaid_font2_337dp() {
        shoot("export_exported_paid", ready(), NARROW_DEVICE)
        assertPaidTexts()
    }

    @Test
    fun exportedFree() {
        shoot("export_exported_free", ready(creditsLeft = 0, freeResumeUsed = true), TmrTestDevices.prototype)
        rule.onNodeWithText("Free resume used. 0 credits left. Nothing was charged.").assertExists()
        rule.onNodeWithText("✓ Mark as Applied").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun exportedFree_font2_337dp() {
        shoot("export_exported_free", ready(creditsLeft = 0, freeResumeUsed = true), NARROW_DEVICE)
        rule.onNodeWithText("Free resume used. 0 credits left. Nothing was charged.").assertExists()
        rule.onNodeWithText("✓ Mark as Applied").assertExists()
    }

    @Test
    fun exportedAppliedWithUndoToast() {
        val toast = TmrToastState()
        var undos = 0
        shoot(
            screenName = "export_exported_applied_undo",
            state = ready(status = ApplicationStatus.APPLIED, markedOn = "10 Oct"),
            device = TmrTestDevices.prototype,
            beforeCapture = {
                toast.show("Applied, marked on 10 Oct", TmrToastAction("Undo") { undos += 1 })
            },
            extra = { TmrToastHost(state = toast) },
        )
        rule.onNodeWithText("Marked on 10 Oct").assertExists()
        rule.onNodeWithText("Change").assertExists()
        rule.onNodeWithText("Applied").assertIsDisplayed()
        rule.onNodeWithText("Did you apply?").assertDoesNotExist()
        rule.onNodeWithText("Applied, marked on 10 Oct").assertExists()
        rule.onNodeWithText("Undo").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun exportedAppliedWithUndoToast_font2_337dp() {
        val toast = TmrToastState()
        var undos = 0
        shoot(
            screenName = "export_exported_applied_undo",
            state = ready(status = ApplicationStatus.APPLIED, markedOn = "10 Oct"),
            device = NARROW_DEVICE,
            beforeCapture = {
                toast.show("Applied, marked on 10 Oct", TmrToastAction("Undo") { undos += 1 })
            },
            extra = { TmrToastHost(state = toast) },
        )
        rule.onNodeWithText("Marked on 10 Oct").assertExists()
        rule.onNodeWithText("Change").assertExists()
        rule.onNodeWithText("Applied").assertIsDisplayed()
        rule.onNodeWithText("Did you apply?").assertDoesNotExist()
        rule.onNodeWithText("Applied, marked on 10 Oct").assertExists()
        rule.onNodeWithText("Undo").assertExists()
    }

    @Test
    fun exportedStatusSheet() {
        shoot(
            screenName = "export_exported_status_sheet",
            state = ready(status = ApplicationStatus.INTERVIEW, markedOn = "10 Oct", statusSheet = ApplicationStatus.INTERVIEW),
            device = TmrTestDevices.prototype,
        )
        rule.onNodeWithText("Application status").assertExists()
        rule.onNodeWithText("Save status").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun exportedStatusSheet_font2_337dp() {
        shoot(
            screenName = "export_exported_status_sheet",
            state = ready(status = ApplicationStatus.INTERVIEW, markedOn = "10 Oct", statusSheet = ApplicationStatus.INTERVIEW),
            device = NARROW_DEVICE,
        )
        rule.onNodeWithText("Application status").assertExists()
        rule.onNodeWithText("Save status").assertExists()
    }

    @Test
    fun tapsReachTheirCallbacks() {
        var shares = 0
        var opens = 0
        var marked = 0
        var changed = 0
        var saved = 0
        var dismissed = 0
        var prep = 0
        var coverLetter = 0
        var wentToApplications = 0
        val picked = mutableListOf<ApplicationStatus>()
        var sheet by mutableStateOf<ApplicationStatus?>(null)
        var status by mutableStateOf(ApplicationStatus.SAVED)

        rule.setContent {
            TmrTheme {
                ExportedScreen(
                    state = ready(status = status, markedOn = "10 Oct".takeIf { status != ApplicationStatus.SAVED }, statusSheet = sheet),
                    onShare = { shares += 1 },
                    onOpen = { opens += 1 },
                    onMarkApplied = { marked += 1 },
                    onChangeStatus = { changed += 1 },
                    onPickStatus = { picked.add(it) },
                    onDismissStatusSheet = { dismissed += 1 },
                    onSaveStatus = { saved += 1 },
                    onSoon = { prep += 1 },
                    onGoToApplications = { wentToApplications += 1 },
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Share").performClick()
        rule.onNodeWithText("Open").performClick()
        rule.onNodeWithText("✓ Mark as Applied").performClick()
        rule.onNodeWithText("Get prep questions").performClick()
        rule.onNodeWithText("Write a cover letter").performClick()
        rule.onNodeWithText("Go to Applications").performClick()
        status = ApplicationStatus.APPLIED
        rule.waitForIdle()
        rule.onNodeWithText("Change").performClick()

        sheet = ApplicationStatus.INTERVIEW
        rule.waitForIdle()

        rule.onNodeWithText("Offer").performClick()
        rule.onNodeWithText("Save status").performClick()
        rule.waitForIdle()

        assertEquals(1, shares)
        assertEquals(1, opens)
        assertEquals(1, marked)
        assertEquals(1, changed)
        assertEquals(1, saved)
        assertEquals(listOf(ApplicationStatus.OFFER), picked)
        assertEquals(0, dismissed)
        assertEquals(2, prep)
        assertEquals(0, coverLetter)
        assertEquals(1, wentToApplications)
    }
}
