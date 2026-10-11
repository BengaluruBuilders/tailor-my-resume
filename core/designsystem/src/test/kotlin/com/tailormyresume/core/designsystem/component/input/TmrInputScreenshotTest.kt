package com.tailormyresume.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrInputScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        screenName: String,
        device: TmrTestDevice = TmrTestDevices.prototype,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            TmrPreviewTheme {
                Column(
                    modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background).padding(24.dp),
                ) { content() }
            }
        }
        runBlocking {
            composeRule.captureForDevice(outputDirectory = SCREENSHOT_DIRECTORY, screenName = screenName, device = device)
        }
    }

    @Test
    fun buttonPrimary() = capture("input_button_primary") {
        TmrPrimaryButton(label = "Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
        TmrPrimaryButton(label = "Continue", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }

    @Test
    fun buttonSecondary() = capture("input_button_secondary") {
        TmrSecondaryButton(label = "Paste as text", onClick = {}, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun buttonText() = capture("input_button_text") {
        TmrTextButton(label = "Skip for now", onClick = {})
    }

    @Test
    fun uploadCard() = capture("input_upload_card") {
        TmrUploadCard(title = "Upload your resume", hint = "PDF or Word, up to 5 MB", onClick = {})
    }

    @Test
    fun choiceSelected() = capture("input_choice_selected") {
        TmrChoiceRow(label = "Yes, a few times", selected = true, onClick = {}, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun choiceUnselected() = capture("input_choice_unselected") {
        TmrChoiceRow(label = "Not really", selected = false, onClick = {}, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun choiceSelectedSheet() = capture("input_choice_selected_sheet") {
        TmrChoiceRow(label = "Applied", selected = true, onClick = {}, onSheet = true, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun choiceUnselectedSheet() = capture("input_choice_unselected_sheet") {
        TmrChoiceRow(label = "Interview", selected = false, onClick = {}, onSheet = true, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun toggleOn() = capture("input_toggle_on") { TmrToggle(label = "Notify me", checked = true, onCheckedChange = {}) }

    @Test
    fun toggleOff() = capture("input_toggle_off") { TmrToggle(label = "Notify me", checked = false, onCheckedChange = {}) }

    @Test
    fun fieldDefault() = capture("input_field_default") {
        TmrTextField(value = "", onValueChange = {}, label = "Email", placeholder = "you@example.com")
    }

    @Test
    fun fieldFilled() = capture("input_field_filled") {
        TmrTextField(value = "asha@example.com", onValueChange = {}, label = "Email")
    }

    @Test
    fun fieldReadonly() = capture("input_field_readonly") {
        TmrTextField(value = "asha@example.com", onValueChange = {}, label = "Email", readOnly = true)
    }

    @Test
    fun fieldAmber() = capture("input_field_amber") {
        TmrTextField(value = "asha@", onValueChange = {}, label = "Email", amber = true)
    }

    @Test
    fun fieldArea() = capture("input_field_area") {
        TmrTextArea(value = "", onValueChange = {}, label = "Job description", placeholder = "Paste the full job description here")
    }

    private companion object {
        const val SCREENSHOT_DIRECTORY = "src/test/screenshots"
    }
}
