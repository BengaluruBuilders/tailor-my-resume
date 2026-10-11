package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val TALL_QUALIFIERS = "w374dp-h6000dp-normal-long-notround-any-480dpi-keyshidden-nonav"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TALL_QUALIFIERS)
class TailoredUndoTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun summaryBulletAndSkillsEachHaveTheirOwnUndo() {
        val undone = mutableListOf<String>()
        rule.setContent {
            TmrTheme {
                TailoredScreen(
                    state = ready(),
                    tab = TailoredTab.Changes,
                    onTabChange = {},
                    onUndo = { undone += it },
                    onAcceptChanges = {},
                    onEdit = {},
                    onExport = {},
                )
            }
        }

        val buttons = rule.onAllNodesWithText("Undo")
        assertThat(buttons.fetchSemanticsNodes()).hasSize(3)
        (0..2).forEach { buttons[it].performClick() }

        assertThat(undone).containsExactly("summary", "exp-infosys-b1", "skills").inOrder()
    }
}
