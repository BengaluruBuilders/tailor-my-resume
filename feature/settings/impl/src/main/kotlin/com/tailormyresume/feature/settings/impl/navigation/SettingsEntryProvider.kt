package com.tailormyresume.feature.settings.impl.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.LegacyPaymentNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.settings.impl.credits.CreditsRoute
import com.tailormyresume.feature.settings.impl.settings.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<com.tailormyresume.feature.settings.api.navigation.ReferralsNavKey> { com.tailormyresume.feature.settings.impl.referrals.ReferralsRoute() }
    entry<SettingsNavKey> { key -> SettingsRoute(navigator, showDeleteAccount = key.showDeleteAccount) }
    entry<CreditsNavKey> { CreditsRoute(navigator) }
    entry<LegacyPaymentNavKey> { key ->
        LaunchedEffect(key) { navigator.replace(CreditsNavKey(returnToApplicationId = key.returnToApplicationId)) }
    }
}
