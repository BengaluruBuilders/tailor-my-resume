package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.component.content.TmrPaperBlock
import com.tailormyresume.core.designsystem.component.content.TmrPaperHighlight
import com.tailormyresume.core.designsystem.component.content.TmrPaperSpan
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.tailor.impl.NARROW_DEVICE
import com.tailormyresume.feature.tailor.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.tailor.impl.captureResultScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

internal val CHANGES = listOf(
    ChangeCard(
        id = "summary",
        area = ChangeArea.Summary,
        kind = ChangeKind.Rewritten,
        source = ChangeSource.YourAnswer,
        before = "Finance analyst with 4 years of SQL, Power BI and Excel work across banking and retail.",
        after = "Finance analyst with 4 years of SQL, Power BI and Excel work, presenting insights to senior stakeholders.",
        undone = false,
    ),
    ChangeCard(
        id = "exp-infosys-b1",
        area = ChangeArea.Entry("Business Analyst · Infosys"),
        kind = ChangeKind.Rewritten,
        source = ChangeSource.YourResume,
        before = "Built dashboards in Power BI for monthly reports, cut prep time 40%.",
        after = "Built Power BI dashboards for monthly reporting, cutting preparation time by 40%.",
        undone = false,
    ),
    ChangeCard(
        id = "skills",
        area = ChangeArea.Skills,
        kind = ChangeKind.Added,
        source = ChangeSource.YourResume,
        before = "SQL, Excel, Power BI",
        after = "SQL, Power BI, Excel, Stakeholder updates",
        undone = false,
    ),
)

private fun blocks(withAnswer: Boolean) = listOf(
    TmrPaperBlock(
        lines = listOf(
            listOf(
                TmrPaperSpan(
                    "Finance analyst with 4 years of SQL, Power BI and Excel work, presenting insights to senior stakeholders.",
                    if (withAnswer) TmrPaperHighlight.FromAnswer else TmrPaperHighlight.FromResume,
                ),
            ),
        ),
    ),
    TmrPaperBlock(
        heading = "Experience",
        title = "Business Analyst, Infosys",
        dates = "Jul 2022 - Present",
        lines = listOf(
            listOf(
                TmrPaperSpan("Built "),
                TmrPaperSpan("Power BI", TmrPaperHighlight.FromResume),
                TmrPaperSpan(" dashboards for monthly reporting, cutting preparation time by 40%."),
            ),
            listOf(TmrPaperSpan("Wrote SQL pipelines over 20M+ rows of transaction data.")),
        ),
    ),
    TmrPaperBlock(
        heading = "Skills",
        lines = listOf(listOf(TmrPaperSpan("SQL, Power BI, Excel, Stakeholder updates"))),
    ),
)

internal fun ready(
    withAnswer: Boolean = true,
    changes: List<ChangeCard> = CHANGES,
    accepted: Boolean = false,
    exportEnabled: Boolean = false,
) = TailoredUiState.Ready(
    jobTitle = "Associate Analyst",
    company = "Northwind GCC",
    name = "Priya Deshmukh",
    contact = "priya.deshmukh@gmail.com | +91 98200 41736",
    blocks = blocks(withAnswer),
    coveragePercent = if (withAnswer) 92 else 78,
    changes = changes,
    accepted = accepted,
    exportEnabled = exportEnabled,
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TailoredScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun shoot(
        screenName: String,
        state: TailoredUiState.Ready,
        tab: TailoredTab,
        device: TmrTestDevice,
        onExport: () -> Unit = {},
        extra: @Composable () -> Unit = {},
    ) {
        rule.captureResultScreen(screenName, device) {
            TailoredScreen(
                state = state,
                tab = tab,
                onTabChange = {},
                onUndo = {},
                onAcceptChanges = {},
                onEdit = {},
                onExport = onExport,
            )
            extra()
        }
        rule.onNodeWithText("Your resume is ready").assertExists()
        rule.onNodeWithText("For Associate Analyst · Northwind GCC").assertExists()
        rule.onNodeWithText("Resume").assertExists()
        rule.onNodeWithText("Changes · ${state.changes.size}").assertExists()
        rule.onNodeWithText("Edit").assertExists()
        rule.onNodeWithText("Export PDF").assertExists()
    }

    @Test
    fun resumeTabWithAnswer() {
        shoot("result_resume_with_answer", ready(), TailoredTab.Resume, TmrTestDevices.prototype)
        rule.onNodeWithText("92% keywords").assertExists()
        rule.onNodeWithText("From your answer", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Accept changes").assertExists()
    }

    @Test
    fun resumeTabWithoutAnswer() {
        shoot("result_resume_without_answer", ready(withAnswer = false), TailoredTab.Resume, TmrTestDevices.prototype)
        rule.onNodeWithText("78% keywords").assertExists()
    }

    @Test
    fun changesBeforeAccept() {
        shoot("result_changes_before_accept", ready(), TailoredTab.Changes, TmrTestDevices.prototype)
        rule.onNodeWithText("Business Analyst · Infosys").assertExists()
        rule.onNodeWithText("Accept changes").assertExists()
        assertTrue(rule.onAllUndo().isNotEmpty())
    }

    @Test
    fun changesAfterOneUndo() {
        val undone = CHANGES.map {
            if (it.id == "exp-infosys-b1") it.copy(undone = true, before = null, after = it.before.orEmpty()) else it
        }
        shoot("result_changes_after_undo", ready(changes = undone), TailoredTab.Changes, TmrTestDevices.prototype)
        rule.onNodeWithText("Undone").assertExists()
    }

    private val undoneAdded = listOf(
        ChangeCard(
            id = "b-added",
            area = ChangeArea.Entry("Business Analyst · Infosys"),
            kind = ChangeKind.Added,
            source = ChangeSource.YourAnswer,
            before = null,
            after = "Presented monthly variance analysis to senior stakeholders.",
            undone = true,
        ),
    )

    @Test
    fun changesAfterUndoAdded() {
        shoot("result_changes_after_undo_added", ready(changes = undoneAdded), TailoredTab.Changes, TmrTestDevices.prototype)
        rule.onNodeWithText("Presented monthly variance analysis to senior stakeholders.").assertExists()
        rule.onNodeWithText("Undone").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun changesAfterUndoAdded_font2_337dp() {
        shoot("result_changes_after_undo_added", ready(changes = undoneAdded), TailoredTab.Changes, NARROW_DEVICE)
        rule.onNodeWithText("Undone").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun changesAfterOneUndo_font2_337dp() {
        val undone = CHANGES.map {
            if (it.id == "exp-infosys-b1") it.copy(undone = true, before = null, after = it.before.orEmpty()) else it
        }
        shoot("result_changes_after_undo", ready(changes = undone), TailoredTab.Changes, NARROW_DEVICE)
        assertTrue(rule.onAllUndo().isNotEmpty())
    }

    @Test
    fun acceptedExportEnabled() {
        shoot(
            "result_accepted",
            ready(accepted = true, exportEnabled = true),
            TailoredTab.Changes,
            TmrTestDevices.prototype,
        )
        rule.onNodeWithText("Changes accepted").assertExists()
        rule.onNodeWithText("Export PDF").assertIsEnabled()
    }

    @Test
    fun exportDisabledToast() {
        val toast = TmrToastState()
        var exports = 0
        rule.captureResultScreen("result_export_disabled_toast", TmrTestDevices.prototype, beforeCapture = {
            rule.onNodeWithText("Export PDF").performClick()
        }) {
            TailoredScreen(
                state = ready(),
                tab = TailoredTab.Resume,
                onTabChange = {},
                onUndo = {},
                onAcceptChanges = {},
                onEdit = {},
                onExport = {
                    exports += 1
                    toast.show("Accept the changes first")
                },
            )
            TmrToastHost(state = toast)
        }
        rule.onNodeWithText("Accept the changes first").assertExists()
        assertEquals(1, exports)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun resumeTab_font2_337dp() =
        shoot("result_resume_with_answer", ready(), TailoredTab.Resume, NARROW_DEVICE)

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun changesTab_font2_337dp() {
        shoot("result_changes_before_accept", ready(), TailoredTab.Changes, NARROW_DEVICE)
        assertTrue(rule.onAllUndo().isNotEmpty())
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun accepted_font2_337dp() =
        shoot("result_accepted", ready(accepted = true, exportEnabled = true), TailoredTab.Changes, NARROW_DEVICE)

    private fun ComposeContentTestRule.onAllUndo() =
        onAllNodesWithText("Undo").fetchSemanticsNodes()
}
