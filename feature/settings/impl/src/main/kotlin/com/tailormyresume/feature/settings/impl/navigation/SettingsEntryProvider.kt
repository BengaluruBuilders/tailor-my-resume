package com.tailormyresume.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.settings.impl.settings.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> { SettingsRoute(navigator) }
    entry<CreditsNavKey> { key -> NavKeyPlaceholder(key) }
    entry<PaywallNavKey> { key -> NavKeyPlaceholder(key) }
}
