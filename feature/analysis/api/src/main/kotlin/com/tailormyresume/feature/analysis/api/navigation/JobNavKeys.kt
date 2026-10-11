package com.tailormyresume.feature.analysis.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class JobNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class JobLinkNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class JobResultNavKey(
    val applicationId: String,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class QuickQuestionNavKey(
    val applicationId: String,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

fun Navigator.newApp() {
    navigate(JobNavKey())
}

fun Navigator.navigateToJobResult(applicationId: String) {
    navigate(JobResultNavKey(applicationId))
}

fun Navigator.navigateToQuickQuestion(applicationId: String) {
    navigate(QuickQuestionNavKey(applicationId))
}
