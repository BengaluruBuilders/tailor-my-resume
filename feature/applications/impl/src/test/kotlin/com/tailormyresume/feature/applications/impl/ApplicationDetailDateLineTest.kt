package com.tailormyresume.feature.applications.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailScreen
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ApplicationDetailDateLineTest {

    @get:Rule
    val rule = createComposeRule()

    private var status by mutableStateOf(ApplicationStatus.SAVED)

    private fun ready(appliedOn: String?) = ApplicationDetailUiState.Ready(
        title = "Business Analyst",
        company = "Kestrel Bank",
        place = "Mumbai · On-site",
        status = status,
        appliedOn = appliedOn,
        file = null,
        statusSheet = null,
    )

    private fun show(appliedOn: String?) {
        rule.setContent {
            TmrTheme {
                ApplicationDetailScreen(
                    state = ready(appliedOn),
                    onChangeStatus = {},
                    onPickStatus = {},
                    onDismissStatusSheet = {},
                    onSaveStatus = {},
                    onShare = {},
                    onOpen = {},
                    onRetailor = {},
                    onViewJobAnalysis = {},
                )
            }
        }
    }

    @Test
    fun notAppliedYetOnlyForTheNotAppliedStatus() {
        show(appliedOn = null)
        rule.onNodeWithText("Not applied yet").assertExists()
    }

    @Test
    fun statusPastAppliedWithoutADateShowsNoDateLine() {
        show(appliedOn = null)
        listOf(
            ApplicationStatus.APPLIED,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFER,
            ApplicationStatus.REJECTED,
        ).forEach { past ->
            status = past
            rule.waitForIdle()
            rule.onNodeWithText("Not applied yet").assertDoesNotExist()
        }
    }

    @Test
    fun aKnownDateStillShows() {
        status = ApplicationStatus.INTERVIEW
        show(appliedOn = "6 Oct")
        rule.onNodeWithText("on 6 Oct").assertExists()
    }
}
