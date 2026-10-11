package com.tailormyresume.feature.profile.api.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.model.DebugScenario
import kotlinx.serialization.Serializable

@Serializable
data class ProfileNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ExperienceNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class EditRoleNavKey(
    val entryId: String? = null,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class EditContactNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class SkillsNavKey(
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
data class ListEditNavKey(
    val section: ProfileListSection,
    val scenario: DebugScenario = DebugScenario.defaultValue,
) : NavKey

@Serializable
enum class ProfileListSection { SUMMARY, EDUCATION, ACHIEVEMENTS }

val DefaultProfileNavKey = ProfileNavKey()
