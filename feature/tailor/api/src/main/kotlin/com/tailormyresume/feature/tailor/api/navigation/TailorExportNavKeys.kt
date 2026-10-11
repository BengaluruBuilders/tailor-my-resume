package com.tailormyresume.feature.tailor.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class ExportedNavKey(
    val applicationId: String,
    val format: String = "pdf",
    val scenario: DebugScenario = DebugScenario.defaultValue,
    val spentFreeCredit: Boolean = true,
) : NavKey

@Serializable
data class CreditsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

fun Navigator.navigateToExported(applicationId: String, format: String = "pdf") {
    navigate(ExportedNavKey(applicationId = applicationId, format = format))
}

fun Navigator.navigateToCredits() {
    navigate(CreditsNavKey())
}
