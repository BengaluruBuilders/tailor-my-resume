package com.tailormyresume.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> { key -> NavKeyPlaceholder(key) }
    entry<CreditsNavKey> { key -> NavKeyPlaceholder(key) }
    entry<PaywallNavKey> { key -> NavKeyPlaceholder(key) }
}
