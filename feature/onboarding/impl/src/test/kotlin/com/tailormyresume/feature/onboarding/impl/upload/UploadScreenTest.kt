package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.signin.NARROW_QUALIFIERS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class UploadScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsHeroUploadCardAndFooter() {
        composeRule.showFlowScreen { UploadScreen(onUploadClick = {}, onPasteClick = {}, onManualClick = {}) }

        listOf(
            "Paige builds your profile",
            "Start with your resume",
            "PDF",
            "DOCX",
            "You can review and edit the imported details.",
            "Upload resume",
            "PDF or DOCX · up to 5\u00A0MB",
            "Paste as text",
            "Fill in myself",
            "Your file stays on your device. We use its text to build your profile.",
        ).forEach { composeRule.onNodeWithText(it, ignoreCase = true).assertIsDisplayed() }
    }

    @Test
    fun eachActionCallsItsOwnCallback() {
        val calls = mutableListOf<String>()
        composeRule.showFlowScreen {
            UploadScreen(
                onUploadClick = { calls += "upload" },
                onPasteClick = { calls += "paste" },
                onManualClick = { calls += "manual" },
            )
        }

        composeRule.onNodeWithText("Upload resume").performClick()
        composeRule.onNodeWithText("Paste as text").performClick()
        composeRule.onNodeWithText("Fill in myself").performClick()

        assertThat(calls).containsExactly("upload", "paste", "manual").inOrder()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun textActionsStackOnANarrowScreenAtFontScale115() {
        composeRule.showFlowScreen(fontScale = 1.15f) {
            UploadScreen(onUploadClick = {}, onPasteClick = {}, onManualClick = {})
        }

        val paste = composeRule.onNodeWithText("Paste as text").getUnclippedBoundsInRoot()
        val manual = composeRule.onNodeWithText("Fill in myself").getUnclippedBoundsInRoot()
        assertThat(manual.top).isGreaterThan(paste.top)
        assertThat(manual.left).isEqualTo(paste.left)
    }
}
