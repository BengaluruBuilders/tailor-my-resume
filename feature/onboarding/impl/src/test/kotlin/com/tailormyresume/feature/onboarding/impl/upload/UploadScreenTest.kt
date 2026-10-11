package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
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
            "We read it and fill in your profile. You check it and fix anything we got wrong.",
            "Upload resume",
            "PDF or DOCX · up to 5\u00A0MB",
            "Paste as text",
            "Fill in myself",
            "Your resume is only used to build your applications. Never shared.",
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
}
