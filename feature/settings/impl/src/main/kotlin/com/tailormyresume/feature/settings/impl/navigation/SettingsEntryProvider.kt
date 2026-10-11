package com.tailormyresume.feature.settings.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.DeleteAccountNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> { key ->
        BasicText(text = key::class.simpleName.orEmpty())
    }
    entry<DeleteAccountNavKey> { key ->
        BasicText(text = key::class.simpleName.orEmpty())
    }
}
