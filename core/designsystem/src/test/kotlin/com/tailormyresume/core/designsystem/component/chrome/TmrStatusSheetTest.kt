package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrStatusSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val picked = mutableListOf<TmrApplicationStatus>()
    private var saves = 0
    private var dismissals = 0

    private fun show(selected: TmrApplicationStatus = TmrApplicationStatus.Applied) {
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    TmrStatusSheet(
                        selected = selected,
                        onSelect = { picked += it },
                        onSave = { saves++ },
                        onDismiss = { dismissals++ },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun fiveOptionsAndSaveCallback() {
        show()

        listOf("Saved", "Applied", "Interview", "Offer", "Rejected").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
        composeRule.onNodeWithText("Application status").assertExists()
        composeRule.onNodeWithText("Applied").assertIsSelected()

        composeRule.onNodeWithText("Interview").performClick()
        composeRule.onNodeWithText("Save status").performClick()

        assertEquals(listOf(TmrApplicationStatus.Interview), picked)
        assertEquals(1, saves)
        assertEquals(0, dismissals)
    }

    @Test
    fun everyOptionReportsItsStatus() {
        show(TmrApplicationStatus.Saved)

        listOf("Saved", "Applied", "Interview", "Offer", "Rejected").forEach { label ->
            composeRule.onNodeWithText(label).performClick()
        }

        assertEquals(TmrApplicationStatus.entries.toList(), picked)
    }
}
