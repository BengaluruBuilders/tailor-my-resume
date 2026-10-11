package com.tailormyresume.feature.settings.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class SettingsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class CreditsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class PaywallNavKey(
    val returnToApplicationId: String? = null,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

val DefaultSettingsNavKey = SettingsNavKey()

fun Navigator.navigateToSettings() {
    navigate(DefaultSettingsNavKey)
}

fun Navigator.navigateToCredits() {
    navigate(CreditsNavKey())
}

fun Navigator.navigateToPaywall(returnToApplicationId: String? = null) {
    navigate(PaywallNavKey(returnToApplicationId))
}
