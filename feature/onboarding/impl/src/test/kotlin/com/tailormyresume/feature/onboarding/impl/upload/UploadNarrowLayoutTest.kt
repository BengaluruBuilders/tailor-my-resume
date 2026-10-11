package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_DEVICE
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
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

    @Test
    fun footerSitsInsideTheScreenAtRest() {
        showAt(1f)

        val footer = composeRule.onNodeWithText("Never shared.", substring = true).getUnclippedBoundsInRoot()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertThat(footer.bottom.value).isAtMost(root.bottom.value)
    }
}
