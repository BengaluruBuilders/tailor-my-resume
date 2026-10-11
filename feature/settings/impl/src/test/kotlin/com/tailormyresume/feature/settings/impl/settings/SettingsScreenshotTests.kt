package com.tailormyresume.feature.settings.impl.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.chrome.TmrBottomSheet
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.settings.impl.NARROW_DEVICE
import com.tailormyresume.feature.settings.impl.NARROW_QUALIFIERS
import com.tailormyresume.feature.settings.impl.assertNoTruncatedText
import com.tailormyresume.feature.settings.impl.assertNoWordBrokenAcrossLines
import com.tailormyresume.feature.settings.impl.assertTexts
import com.tailormyresume.feature.settings.impl.capture
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountSheetContent
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountUiState
import com.tailormyresume.feature.settings.impl.setTmrContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val noActions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {})

internal fun deleteSheetState(typed: String, deleting: Boolean = false) = DeleteAccountUiState.Content(
    applications = 3,
    unusedCredits = 2,
    typed = typed,
    canDelete = typed.trim().equals("DELETE", ignoreCase = true),
    deleting = deleting,
)

internal fun settingsWithDeleteSheet(typed: String, deleting: Boolean = false): @Composable () -> Unit = {
    SettingsScreen(settingsContent(), versionName = "1.0.0", actions = noActions)
    TmrBottomSheet(onDismiss = {}, title = "Delete your account?") {
        DeleteAccountSheetContent(deleteSheetState(typed, deleting), onTextChanged = {}, onDelete = {}, onKeep = {})
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun settingsDefault() {
        rule.setTmrContent(TmrTestDevices.prototype) {
            SettingsScreen(settingsContent(), versionName = "1.0.0", actions = noActions)
        }
        rule.assertTexts("Account", "Credits & purchases", "Page size", "Product updates", "Sign out", "Terms · Privacy · v1.0.0")
        rule.assertNoTruncatedText()
        rule.capture("settings_default", TmrTestDevices.prototype)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteAccountSheetScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun settingsDeleteSheetEmpty() {
        rule.setTmrContent(TmrTestDevices.prototype, settingsWithDeleteSheet(typed = ""))
        rule.assertTexts("Delete your account?", "Type DELETE to confirm", "Keep my account")
        rule.assertNoTruncatedText()
        rule.capture("settings_delete_sheet_empty", TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun settingsDeleteSheetTyped() {
        rule.setTmrContent(TmrTestDevices.prototype, settingsWithDeleteSheet(typed = "DELETE"))
        rule.assertTexts("Delete your account?", "DELETE", "Keep my account")
        rule.assertNoTruncatedText()
        rule.capture("settings_delete_sheet_typed", TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun settingsDeleteSheetDeleting() {
        rule.setTmrContent(TmrTestDevices.prototype, settingsWithDeleteSheet(typed = "DELETE", deleting = true))
        rule.assertTexts("Delete your account?", "DELETE", "Keep my account")
        rule.assertNoTruncatedText()
        rule.capture("settings_delete_sheet_deleting", TmrTestDevices.prototype)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsLargeFontScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun settingsDefaultFont200At337dp() {
        rule.setTmrContent(NARROW_DEVICE) {
            SettingsScreen(settingsContent(), versionName = "1.0.0", actions = noActions)
        }
        rule.assertNoTruncatedText()
        rule.assertNoWordBrokenAcrossLines()
        rule.capture("settings_default", NARROW_DEVICE)
        rule.onNodeWithText("Terms", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sign out").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun settingsDeleteSheetEmptyFont200At337dp() {
        rule.setTmrContent(NARROW_DEVICE, settingsWithDeleteSheet(typed = ""))
        rule.assertNoTruncatedText()
        rule.assertNoWordBrokenAcrossLines()
        rule.capture("settings_delete_sheet_empty", NARROW_DEVICE)
        rule.onNodeWithText("Keep my account").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun settingsDeleteSheetTypedFont200At337dp() {
        rule.setTmrContent(NARROW_DEVICE, settingsWithDeleteSheet(typed = "DELETE"))
        rule.onNodeWithText("Keep my account").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Type DELETE to confirm", substring = true).assertIsDisplayed()
        rule.assertNoTruncatedText()
        rule.assertNoWordBrokenAcrossLines()
        rule.capture("settings_delete_sheet_typed", NARROW_DEVICE)
    }
}
