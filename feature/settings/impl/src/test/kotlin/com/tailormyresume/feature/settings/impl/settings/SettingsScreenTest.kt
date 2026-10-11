package com.tailormyresume.feature.settings.impl.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

internal fun settingsContent(
    pageSize: PageSize = PageSize.A4,
    fileNameFormat: FileNameFormat = FileNameFormat.NAME_COMPANY_ROLE,
    productUpdates: Boolean = false,
) = SettingsUiState.Content(
    email = "priya.deshmukh@gmail.com",
    credits = 3,
    pageSize = pageSize,
    fileNameFormat = fileNameFormat,
    productUpdates = productUpdates,
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val taps = mutableListOf<String>()

    private val actions = SettingsActions(
        onCredits = { taps += "credits" },
        onPageSize = { taps += "page" },
        onFileName = { taps += "file" },
        onProductUpdates = { taps += "updates" },
        onDownloadData = { taps += "download" },
        onHelp = { taps += "help" },
        onDeleteAccount = { taps += "delete" },
        onSignOut = { taps += "signOut" },
    )

    private fun show(state: SettingsUiState = settingsContent()) {
        rule.setContent { TmrTheme { SettingsScreen(state, versionName = "1.4.2", actions = actions) } }
    }

    @Test
    fun rowsAndFooterArePresent() {
        show()

        listOf(
            "Settings",
            "Account",
            "priya.deshmukh@gmail.com",
            "Signed in with Google",
            "Credits & purchases",
            "3 left",
            "Resume",
            "Page size",
            "A4",
            "File name",
            "Name_Company_Role",
            "Notifications",
            "Product updates",
            "Privacy & help",
            "Download my data",
            "Help & feedback",
            "Delete account",
            "Sign out",
            "Terms · Privacy · v1.4.2",
        ).forEach { text -> rule.onNodeWithText(text, ignoreCase = true).assertExists() }
    }

    @Test
    fun noFollowUpRemindersToggle() {
        show()

        rule.onNodeWithText("Follow-up reminders", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun valuesFollowTheState() {
        show(settingsContent(pageSize = PageSize.LETTER, fileNameFormat = FileNameFormat.NAME_RESUME))

        rule.onNodeWithText("Letter").assertIsDisplayed()
        rule.onNodeWithText("Name_Resume").assertIsDisplayed()
    }

    @Test
    fun everyRowReportsItsTap() {
        show()

        listOf(
            "Credits & purchases" to "credits",
            "Page size" to "page",
            "File name" to "file",
            "Product updates" to "updates",
            "Download my data" to "download",
            "Help & feedback" to "help",
            "Delete account" to "delete",
            "Sign out" to "signOut",
        ).forEach { (label, expected) ->
            rule.onNodeWithText(label).performScrollTo().performClick()
            assertThat(taps.last()).isEqualTo(expected)
        }
        assertThat(taps).hasSize(8)
    }

    @Test
    fun loadingShowsNothingYet() {
        show(SettingsUiState.Loading)

        rule.onNodeWithText("Sign out").assertDoesNotExist()
    }
}
