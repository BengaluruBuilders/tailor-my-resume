package com.tailormyresume.feature.onboarding.impl.upload

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = NARROW_QUALIFIERS)
class UploadNarrowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showAt(fontScale: Float) =
        composeRule.showFlowScreen(fontScale) {
            UploadScreen(onUploadClick = {}, onPasteClick = {}, onManualClick = {})
        }

    @Test
    fun pasteAndManualStackAsFullWidthButtonsAtLargestFont() {
        showAt(NARROW_DEVICE.fontScale)

        val paste = composeRule.onNodeWithText("Paste as text").getUnclippedBoundsInRoot()
        val manual = composeRule.onNodeWithText("Fill in myself").getUnclippedBoundsInRoot()

        assertThat(manual.top).isAtLeast(paste.bottom)
        assertThat((paste.right - paste.left).value).isGreaterThan(200f)
        assertThat((manual.right - manual.left).value).isGreaterThan(200f)
    }

    private fun labelLineCount(label: String): Int {
        val node = composeRule.onNodeWithText(label).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        checkNotNull(node.config[SemanticsActions.GetTextLayoutResult].action).invoke(results)
        return results.single().lineCount
    }

    private fun assertActionsNeverWrap() {
        val paste = composeRule.onNodeWithText("Paste as text").getUnclippedBoundsInRoot()
        val manual = composeRule.onNodeWithText("Fill in myself").getUnclippedBoundsInRoot()
        assertThat(labelLineCount("Paste as text")).isEqualTo(1)
        assertThat(labelLineCount("Fill in myself")).isEqualTo(1)
        assertThat(paste.top == manual.top || manual.top >= paste.bottom).isTrue()
    }

    @Test
    @Config(qualifiers = "w337dp-h740dp-normal-long-notround-any-480dpi-keyshidden-nonav")
    fun actionLabelsStayOnOneLineAt337dpAtRegularFont() {
        showAt(1f)

        assertActionsNeverWrap()
        val paste = composeRule.onNodeWithText("Paste as text").getUnclippedBoundsInRoot()
        val manual = composeRule.onNodeWithText("Fill in myself").getUnclippedBoundsInRoot()
        assertThat(manual.top).isAtLeast(paste.bottom)
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-normal-long-notround-any-480dpi-keyshidden-nonav")
    fun actionLabelsStayOnOneLineAt360dpAtRegularFont() {
        showAt(1f)

        assertActionsNeverWrap()
    }

    @Test
    fun stackedContentEndsAboveTheGestureBar() {
        val inset = 48
        var deliveredInset = 0
        composeRule.showFlowScreen(1.15f) {
            val view = LocalView.current
            SideEffect {
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, inset))
                    .build()
                ViewCompat.dispatchApplyWindowInsets(view, insets)
            }
            deliveredInset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            UploadScreen(onUploadClick = {}, onPasteClick = {}, onManualClick = {})
        }
        composeRule.waitForIdle()

        assertThat(deliveredInset).isEqualTo(inset)
        val scrollArea = composeRule.onNode(hasScrollAction()).getUnclippedBoundsInRoot()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val insetDp = (inset / ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.density).dp

        assertThat(scrollArea.bottom.value).isAtMost((root.bottom - insetDp).value)
    }

    @Test
    fun footerSitsInsideTheScreenAtRest() {
        showAt(1f)

        val footer = composeRule.onNodeWithText("Your file stays on your device. We use its text to build your profile.").getUnclippedBoundsInRoot()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertThat(footer.bottom.value).isAtMost(root.bottom.value)
    }
}
