package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrSheetHostDismissSemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showSheet(dismissible: Boolean) {
        val state = TmrSheetHostState()
        state.show("Sheet") { Text("Body") }
        state.dismissible = dismissible
        composeRule.setContent { TmrPreviewTheme { TmrSheetHost(state) } }
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
}
