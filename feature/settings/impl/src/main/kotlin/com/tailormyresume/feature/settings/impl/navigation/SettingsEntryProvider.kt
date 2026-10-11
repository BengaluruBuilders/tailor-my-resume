package com.tailormyresume.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.settings.impl.credits.CreditsRoute
import com.tailormyresume.feature.settings.impl.paywall.PaywallRoute
import com.tailormyresume.feature.settings.impl.settings.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> { key -> SettingsRoute(navigator, showDeleteAccount = key.showDeleteAccount) }
    entry<CreditsNavKey> { CreditsRoute(navigator) }
    entry<PaywallNavKey> { key -> PaywallRoute(navigator, key.returnToApplicationId) }
}
