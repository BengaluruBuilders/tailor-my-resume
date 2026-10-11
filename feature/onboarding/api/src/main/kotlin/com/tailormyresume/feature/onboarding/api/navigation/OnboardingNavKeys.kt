package com.tailormyresume.feature.onboarding.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class SignInNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ImportResumeNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

val DefaultSignInNavKey = SignInNavKey()

fun Navigator.navigateToSignIn() {
    navigate(DefaultSignInNavKey)
}

fun Navigator.navigateToImportResume() {
    navigate(ImportResumeNavKey())
}
