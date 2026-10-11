package com.tailormyresume.feature.profile.impl.experience

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExperienceDatesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(start: String = "", end: String = "") {
        rule.setContent {
            TmrTheme {
                ExperienceScreen(
                    state = ExperienceUiState.Content(listOf(ExperienceRow("role", "Analyst", "Company", start, end, false, false))),
                    onRole = {},
                    onAdd = {},
                )
            }
        }
    }

    @Test
    fun absentDatesHaveNoSeparator() {
        show()
        rule.onNodeWithText("–", substring = true).assertDoesNotExist()
    }

    @Test
    fun aSingleDateHasNoDanglingSeparator() {
        show(end = "Jan 2025")
        rule.onNodeWithText("Jan 2025").assertExists()
        rule.onNodeWithText("–", substring = true).assertDoesNotExist()
    }

    @Test
    fun bothDatesKeepTheRange() {
        show(start = "Jan 2024", end = "Jan 2025")
        rule.onNodeWithText("Jan 2024 – Jan 2025").assertExists()
    }
}
