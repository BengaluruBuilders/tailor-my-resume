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
import com.tailormyresume.feature.profile.impl.experience.EditRoleRoute
import com.tailormyresume.feature.profile.impl.experience.ExperienceRoute
import com.tailormyresume.feature.profile.impl.overview.ProfileRoute

fun EntryProviderScope<NavKey>.profileEntry(navigator: Navigator) {
    entry<ProfileNavKey> { ProfileRoute(navigator) }
    entry<ExperienceNavKey> { ExperienceRoute(navigator) }
    entry<EditRoleNavKey> { key -> EditRoleRoute(navigator, key.entryId) }
    entry<EditContactNavKey> { key -> NavKeyPlaceholder(key) }
    entry<SkillsNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ListEditNavKey> { key -> NavKeyPlaceholder(key) }
}
