package com.tailormyresume.feature.tailor.impl.edit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.tailor.impl.NARROW_DEVICE
import com.tailormyresume.feature.tailor.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.tailor.impl.captureResultScreen
import com.tailormyresume.feature.tailor.impl.result.ChangeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SUMMARY = "Finance analyst with 4 years of SQL, Power BI and Excel work, presenting insights to senior stakeholders."
private const val SKILLS = "SQL, Power BI, Excel, Variance analysis, Python, Forecasting, Tableau"
private const val BULLET_B1 = "Built Power BI dashboards for monthly financial reporting, cutting prep time by 40%."
private const val BULLET_B2 = "Presented monthly variance analysis to the CFO and regional finance heads."
private const val BULLET_B3 = "Wrote SQL pipelines over 20M+ rows of transaction data."

private fun ready(fitsOnOnePage: Boolean = true) = EditResumeUiState.Ready(
    summary = SUMMARY,
    roles = listOf(
        EditRole(
            label = "Business Analyst · Infosys",
            bullets = listOf(
                EditBullet(id = "b1", text = BULLET_B1, source = ChangeSource.YourResume),
                EditBullet(id = "b2", text = BULLET_B2, source = ChangeSource.YourAnswer),
                EditBullet(id = "b3", text = BULLET_B3, source = ChangeSource.YourResume),
            ),
        ),
    ),
    skills = SKILLS,
    fitsOnOnePage = fitsOnOnePage,
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class EditResumeScreenshots {
    @get:Rule
    val rule = createComposeRule()

    private fun shoot(
        screenName: String,
        state: EditResumeUiState.Ready,
        device: TmrTestDevice,
    ) {
        rule.captureResultScreen(screenName, device) {
            EditResumeScreen(state = state, onBulletChange = { _, _ -> })
        }
    }

    private fun assertReadyTexts() {
        rule.onNodeWithText("Edit resume").assertExists()
        rule.onNodeWithText("Still fits on 1 page").assertExists()
        rule.onNodeWithText("SUMMARY").assertExists()
        rule.onNodeWithText("BUSINESS ANALYST · INFOSYS").assertExists()
        rule.onNodeWithText("SKILLS LINE").assertExists()
        rule.onNodeWithText(SUMMARY).assertExists()
        rule.onNodeWithText(BULLET_B1).assertExists()
        rule.onNodeWithText(BULLET_B2).assertExists()
        rule.onNodeWithText(BULLET_B3).assertExists()
        assertTrue(rule.onAllNodesWithText("From your resume").fetchSemanticsNodes().size == 2)
        rule.onNodeWithText("From your answer").assertExists()
        rule.onNodeWithText(SKILLS).assertExists()
    }

    @Test
    fun edit() {
        shoot("export_edit", ready(), TmrTestDevices.prototype)
        assertReadyTexts()
    }

    @Test
    fun editOverflow() {
        shoot("export_edit_overflow", ready(fitsOnOnePage = false), TmrTestDevices.prototype)
        rule.onNodeWithText("Still fits on 1 page").assertDoesNotExist()
        rule.onNodeWithText("Edit resume").assertExists()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun edit_font2_337dp() {
        shoot("export_edit", ready(), NARROW_DEVICE)
        assertReadyTexts()
    }

    @Test
    fun editingABulletReportsItsId() {
        var changedId: String? = null
        var changedText: String? = null
        rule.setContent {
            TmrTheme {
                EditResumeScreen(
                    state = ready(),
                    onBulletChange = { bulletId, text ->
                        changedId = bulletId
                        changedText = text
                    },
                )
            }
        }
        rule.onNodeWithText(BULLET_B3).performTextReplacement("Wrote SQL pipelines over 25M+ rows.")
        assertEquals("b3", changedId)
        assertEquals("Wrote SQL pipelines over 25M+ rows.", changedText)
    }
}
