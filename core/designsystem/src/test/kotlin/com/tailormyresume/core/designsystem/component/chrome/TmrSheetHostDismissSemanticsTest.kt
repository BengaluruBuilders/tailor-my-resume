package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrSheetHostDismissSemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var clicksOnContentUnderTheSheet = 0
    private val state = TmrSheetHostState()

    private fun showSheet(dismissible: Boolean) {
        state.show("Sheet") { Text("Body") }
        state.dismissible = dismissible
        composeRule.setContent {
            TmrPreviewTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().testTag(UNDER_SHEET).clickable { clicksOnContentUnderTheSheet++ })
                    TmrSheetHost(state)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun tapScrimTopEdge() {
        composeRule.onNodeWithTag(TmrChromeTags.SCRIM).performTouchInput { click(topCenter.copy(y = 10f)) }
        composeRule.waitForIdle()
    }

    @Test
    fun theScrimOffersDismissWhileTheSheetCanBeDismissed() {
        showSheet(dismissible = true)

        composeRule.onNodeWithTag(TmrChromeTags.SCRIM).assertHasClickAction()
    }

    @Test
    fun theScrimOffersNoDismissActionWhileTheSheetIsLocked() {
        showSheet(dismissible = false)

        composeRule.onNodeWithTag(TmrChromeTags.SCRIM).assertHasNoClickAction()
    }

    @Test
    fun aTapOnTheScrimOfALockedSheetDoesNotReachTheContentUnderIt() {
        showSheet(dismissible = false)

        tapScrimTopEdge()

        assertEquals(0, clicksOnContentUnderTheSheet)
        assertNotNull(state.current)
    }

    @Test
    fun aTapOnTheScrimDismissesTheSheetWithoutReachingTheContentUnderIt() {
        showSheet(dismissible = true)

        tapScrimTopEdge()

        assertEquals(0, clicksOnContentUnderTheSheet)
        assertNull(state.current)
    }

    private companion object {
        const val UNDER_SHEET = "underSheet"
    }
}
