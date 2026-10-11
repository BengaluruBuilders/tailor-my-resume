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
data class UploadNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class UploadErrorNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class PasteResumeNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ManualProfileNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ReadingNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ReviewProfileNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

val DefaultSignInNavKey = SignInNavKey()

fun Navigator.navigateToSignIn() {
    root(DefaultSignInNavKey)
}

fun Navigator.navigateToUpload() {
    navigate(UploadNavKey())
}
