package com.tailormyresume.feature.settings.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.Navigator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
    val showDeleteAccount: Boolean = false,
) : NavKey

@Serializable
data class CreditsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
    val returnToApplicationId: String? = null,
) : NavKey

@Serializable
@SerialName("com.tailormyresume.feature.settings.api.navigation.PaywallNavKey")
data class LegacyPaymentNavKey(
    val returnToApplicationId: String? = null,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data object ReferralsNavKey : NavKey

fun Navigator.navigateToReferrals() {
    navigate(ReferralsNavKey)
}

val DefaultSettingsNavKey = SettingsNavKey()

fun Navigator.navigateToSettings() {
    navigate(DefaultSettingsNavKey)
}

fun Navigator.navigateToCredits() {
    navigate(CreditsNavKey())
}

fun Navigator.navigateToEarnCredits(applicationId: String) {
    navigate(CreditsNavKey(returnToApplicationId = applicationId))
}
