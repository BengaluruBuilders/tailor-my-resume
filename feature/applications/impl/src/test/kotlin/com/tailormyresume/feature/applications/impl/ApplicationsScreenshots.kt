package com.tailormyresume.feature.applications.impl

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailScreen
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailUiState
import com.tailormyresume.feature.applications.impl.detail.ApplicationFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NARROW_QUALIFIERS = "w337dp-h1600dp-normal-long-notround-any-480dpi-keyshidden-nonav"

private val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private const val SCREENSHOT_DIRECTORY = "src/test/screenshots"

private fun ComposeContentTestRule.captureApplicationsScreen(
    screenName: String,
    device: TmrTestDevice = TmrTestDevices.prototype,
    beforeCapture: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
            TmrTheme {
                Box(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background)) { content() }
            }
        }
    }
    waitForIdle()
    beforeCapture()
    waitForIdle()
    runBlocking { captureForDevice(SCREENSHOT_DIRECTORY, screenName, device) }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class ApplicationsScreenshots {
    @get:Rule
    val rule = createComposeRule()

    private fun empty() = ApplicationsUiState.Empty(ApplicationsHeader(credits = 1, initials = "PD"))

    private fun populated() =
        ApplicationsUiState.Populated(
            header = ApplicationsHeader(credits = 3, initials = "PD"),
            total = 3,
            statusCounts =
                mapOf(
                    TmrApplicationStatus.Applied to 1,
                    TmrApplicationStatus.Interview to 1,
                    TmrApplicationStatus.Saved to 1,
                ),
            rows =
                listOf(
                    ApplicationRow(
                        id = "nw",
                        title = "Associate Analyst",
                        company = "Northwind GCC",
                        keywordPercent = 92,
                        initial = "N",
                        colorIndex = 0,
                        status = TmrApplicationStatus.Applied,
                    ),
                    ApplicationRow(
                        id = "kb",
                        title = "Business Analyst",
                        company = "Kestrel Bank",
                        keywordPercent = 88,
                        initial = "K",
                        colorIndex = 1,
                        status = TmrApplicationStatus.Interview,
                    ),
                    ApplicationRow(
                        id = "hr",
                        title = "Data Analyst",
                        company = "Halden Retail",
                        keywordPercent = 85,
                        initial = "H",
                        colorIndex = 2,
                        status = TmrApplicationStatus.Saved,
                    ),
                ),
        )

    private fun detail(
        status: ApplicationStatus = ApplicationStatus.INTERVIEW,
        appliedOn: String? = "6 Oct",
        file: ApplicationFile? = ApplicationFile("Priya-Deshmukh_Kestrel-Bank_Business-Analyst.pdf", 88),
        statusSheet: ApplicationStatus? = null,
    ) = ApplicationDetailUiState.Ready(
        title = "Business Analyst",
        company = "Kestrel Bank",
        place = "Mumbai · On-site",
        status = status,
        appliedOn = appliedOn,
        file = file,
        statusSheet = statusSheet,
    )

    private fun shootApplications(
        screenName: String,
        state: ApplicationsUiState,
        device: TmrTestDevice,
        onCredits: () -> Unit = {},
        onAvatar: () -> Unit = {},
        onAddJob: () -> Unit = {},
        onOpenApplication: (String) -> Unit = {},
    ) {
        rule.captureApplicationsScreen(screenName, device) {
            ApplicationsScreen(
                state = state,
                onCredits = onCredits,
                onAvatar = onAvatar,
                onAddJob = onAddJob,
                onOpenApplication = onOpenApplication,
            )
        }
    }

    private fun shootDetail(
        screenName: String,
        state: ApplicationDetailUiState.Ready,
        device: TmrTestDevice,
        onChangeStatus: () -> Unit = {},
        onPickStatus: (ApplicationStatus) -> Unit = {},
        onDismissStatusSheet: () -> Unit = {},
        onSaveStatus: () -> Unit = {},
        onShare: () -> Unit = {},
        onOpen: () -> Unit = {},
        onRetailor: () -> Unit = {},
        onViewJobAnalysis: () -> Unit = {},
        onSoon: () -> Unit = {},
    ) {
        rule.captureApplicationsScreen(screenName, device) {
            ApplicationDetailScreen(
                state = state,
                onChangeStatus = onChangeStatus,
                onPickStatus = onPickStatus,
                onDismissStatusSheet = onDismissStatusSheet,
                onSaveStatus = onSaveStatus,
                onShare = onShare,
                onOpen = onOpen,
                onRetailor = onRetailor,
                onViewJobAnalysis = onViewJobAnalysis,
                onSoon = onSoon,
            )
        }
    }

    private fun assertEmptyTexts() {
        rule.onNodeWithText("Applications").assertExists()
        rule.onNodeWithText("1 credit").assertExists()
        rule.onNodeWithText("No applications yet").assertExists()
        rule.onNodeWithText("Tailor your resume for a job and it shows up here.").assertExists()
        rule.onNodeWithText("Add a job").assertExists()
    }

    private fun assertListTexts() {
        rule.onNodeWithText("Tailored resumes").assertExists()
        rule.onNodeWithText("3").assertExists()
        rule.onNodeWithText("Recent").assertExists()
        rule.onNodeWithText("Associate Analyst").assertExists()
        rule.onNodeWithText("Northwind GCC · 92% keywords").assertExists()
        rule.onNodeWithText("Kestrel Bank · 88% keywords").assertExists()
        rule.onNodeWithText("Applied 1").assertExists()
    }

    private fun assertDetailTexts() {
        rule.onNodeWithText("Business Analyst").assertExists()
        rule.onNodeWithText("Kestrel Bank · Mumbai · On-site").assertExists()
        rule.onNodeWithText("Status").assertExists()
        rule.onNodeWithText("Interview").assertExists()
        rule.onNodeWithText("on 6 Oct").assertExists()
        rule.onNodeWithText("Change").assertExists()
        rule.onNodeWithText("PDF · 88% keywords").assertExists()
        rule.onNodeWithText("Share").assertExists()
        rule.onNodeWithText("Open").assertExists()
        rule.onNodeWithText("Next steps").assertExists()
        rule.onNodeWithText("Re-tailor with profile changes").assertExists()
        rule.onNodeWithText("1 credit").assertExists()
        rule.onNodeWithText("View job analysis").assertExists()
        rule.onNodeWithText("Get prep questions").assertExists()
        rule.onNodeWithText("Write a cover letter").assertExists()
    }

    @Test
    fun applicationsEmpty() {
        shootApplications("applications_empty", empty(), TmrTestDevices.prototype)
        assertEmptyTexts()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun applicationsEmpty_font2_337dp() {
        shootApplications("applications_empty", empty(), NARROW_DEVICE)
        assertEmptyTexts()
    }

    @Test
    fun applicationsList() {
        shootApplications("applications_list", populated(), TmrTestDevices.prototype)
        assertListTexts()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun applicationsList_font2_337dp() {
        shootApplications("applications_list", populated(), NARROW_DEVICE)
        assertListTexts()
    }

    @Test
    fun applicationDetail() {
        shootDetail("application_detail", detail(), TmrTestDevices.prototype)
        assertDetailTexts()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun applicationDetail_font2_337dp() {
        shootDetail("application_detail", detail(), NARROW_DEVICE)
        assertDetailTexts()
    }

    @Test
    fun applicationDetailNoFile() {
        shootDetail(
            "application_detail_no_file",
            detail(status = ApplicationStatus.SAVED, appliedOn = null, file = null),
            TmrTestDevices.prototype,
        )
        rule.onNodeWithText("Business Analyst").assertExists()
        rule.onNodeWithText("Not applied yet").assertExists()
        rule.onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun applicationDetailNoFile_font2_337dp() {
        shootDetail(
            "application_detail_no_file",
            detail(status = ApplicationStatus.SAVED, appliedOn = null, file = null),
            NARROW_DEVICE,
        )
        rule.onNodeWithText("Business Analyst").assertExists()
        rule.onNodeWithText("Not applied yet").assertExists()
        rule.onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    fun applicationStatusSheetInterviewKestrel() {
        shootDetail(
            "application_status_sheet_interview_kestrel",
            detail(statusSheet = ApplicationStatus.INTERVIEW),
            TmrTestDevices.prototype,
        )
        rule.onNodeWithText("Application status").assertExists()
        rule.onNodeWithText("Save status").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun applicationStatusSheetInterviewKestrel_font2_337dp() {
        shootDetail(
            "application_status_sheet_interview_kestrel",
            detail(statusSheet = ApplicationStatus.INTERVIEW),
            NARROW_DEVICE,
        )
        rule.onNodeWithText("Application status").assertExists()
        rule.onNodeWithText("Save status").assertExists()
    }

    @Test
    fun applicationsTapsReachTheirCallbacks() {
        var credits = 0
        var avatar = 0
        var addJob = 0
        val opened = mutableListOf<String>()

        rule.setContent {
            TmrTheme {
                ApplicationsScreen(
                    state = populated(),
                    onCredits = { credits += 1 },
                    onAvatar = { avatar += 1 },
                    onAddJob = { addJob += 1 },
                    onOpenApplication = { opened.add(it) },
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Add a job").performClick()
        rule.onNodeWithText("3 credits").performClick()
        rule.onNodeWithText("Business Analyst").performClick()
        rule.waitForIdle()

        assertEquals(1, addJob)
        assertEquals(1, credits)
        assertEquals(listOf("kb"), opened)
        assertEquals(0, avatar)
    }

    @Test
    fun applicationDetailTapsReachTheirCallbacks() {
        var changed = 0
        var saved = 0
        var dismissed = 0
        var shares = 0
        var opens = 0
        var retailored = 0
        var analyses = 0
        var soon = 0
        val picked = mutableListOf<ApplicationStatus>()

        rule.setContent {
            TmrTheme {
                ApplicationDetailScreen(
                    state = detail(statusSheet = ApplicationStatus.INTERVIEW),
                    onChangeStatus = { changed += 1 },
                    onPickStatus = { picked.add(it) },
                    onDismissStatusSheet = { dismissed += 1 },
                    onSaveStatus = { saved += 1 },
                    onShare = { shares += 1 },
                    onOpen = { opens += 1 },
                    onRetailor = { retailored += 1 },
                    onViewJobAnalysis = { analyses += 1 },
                    onSoon = { soon += 1 },
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Share").performClick()
        rule.onNodeWithText("Open").performClick()
        rule.onNodeWithText("Change").performClick()
        rule.onNodeWithText("Re-tailor with profile changes").performClick()
        rule.onNodeWithText("View job analysis").performClick()
        rule.onNodeWithText("Get prep questions").performClick()
        rule.onNodeWithText("Write a cover letter").performClick()
        rule.onNodeWithText("Offer").performClick()
        rule.onNodeWithText("Save status").performClick()
        rule.waitForIdle()

        assertEquals(1, shares)
        assertEquals(1, opens)
        assertEquals(1, changed)
        assertEquals(1, saved)
        assertEquals(1, retailored)
        assertEquals(1, analyses)
        assertEquals(2, soon)
        assertEquals(0, dismissed)
        assertEquals(listOf(ApplicationStatus.OFFER), picked)
    }
}
