package com.tailormyresume.feature.onboarding.impl.upload

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class UnreadableScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsFileCardTipsAndBothActions() {
        val calls = mutableListOf<String>()
        composeRule.showFlowScreen {
            UnreadableScreen(
                failure = ImageOnlyFailure,
                onChooseAnotherClick = { calls += "choose" },
                onPasteClick = { calls += "paste" },
            )
        }

        listOf(
            "We couldn't read that file",
            "Resume_scan.pdf",
            "PDF · image only",
            "It looks like a photo or scan, so there's no text for us to read.",
            "Try one of these",
            "Export your resume as PDF from Word or Google Docs",
            "Upload the DOCX file instead",
            "Paste the text from your resume",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Choose another file").performClick()
        composeRule.onNodeWithText("Paste as text").performClick()

        assertThat(calls).containsExactly("choose", "paste").inOrder()
    }

    @Test
    fun fileProblemAndOversizeShowFileSpecificCopy() {
        composeRule.showFlowScreen {
            UnreadableScreen(
                failure = UploadFailure(UploadFailureKind.TooLarge, "Big.pdf", RESUME_PDF_MIME, 6L * 1024L * 1024L),
                onChooseAnotherClick = {},
                onPasteClick = {},
            )
        }

        composeRule.onNodeWithText("We couldn't read that file").assertIsDisplayed()
        composeRule.onNodeWithText("This file is bigger than 5\u00A0MB.").assertIsDisplayed()
        composeRule.onNodeWithText("PDF · 6.0\u00a0MB").assertIsDisplayed()
    }

    @Test
    fun neutralFailureDoesNotBlameTheFile() {
        composeRule.showFlowScreen {
            UnreadableScreen(failure = NeutralFailure, onChooseAnotherClick = {}, onPasteClick = {})
        }

        composeRule.onNodeWithText("We couldn't read your resume").assertIsDisplayed()
        composeRule.onNodeWithText("Try again, paste the text, or fill it in yourself.").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't read that file").assertDoesNotExist()
    }

    @Test
    fun pastedTextFailureHasNoFileCard() {
        composeRule.showFlowScreen {
            UnreadableScreen(
                failure = UploadFailure(UploadFailureKind.Neutral),
                onChooseAnotherClick = {},
                onPasteClick = {},
            )
        }

        composeRule.onNodeWithText("We couldn't read your resume").assertIsDisplayed()
        composeRule.onNodeWithText("Resume_scan.pdf").assertDoesNotExist()
    }

    @Test
    fun singleLinePastedTextExplainsHowToPasteIt() {
        composeRule.showFlowScreen {
            UnreadableScreen(
                failure = UploadFailure(UploadFailureKind.SingleLinePastedText),
                onChooseAnotherClick = {},
                onPasteClick = {},
            )
        }

        composeRule.onNodeWithText("Paste each resume section on a new line, then try again.").assertIsDisplayed()
    }
}
