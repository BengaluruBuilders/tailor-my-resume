package com.tailormyresume.feature.profile.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.ListEditNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey

fun EntryProviderScope<NavKey>.profileEntry(navigator: Navigator) {
    entry<ProfileNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ExperienceNavKey> { key -> NavKeyPlaceholder(key) }
    entry<EditRoleNavKey> { key -> NavKeyPlaceholder(key) }
    entry<EditContactNavKey> { key -> NavKeyPlaceholder(key) }
    entry<SkillsNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ListEditNavKey> { key -> NavKeyPlaceholder(key) }
}
