package com.tailormyresume.feature.profile.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey

fun EntryProviderScope<NavKey>.profileEntry(navigator: Navigator) {
    entry<ProfileNavKey> { key ->
        NavKeyPlaceholder(key)
    }
}

@Composable
internal fun NavKeyPlaceholder(key: NavKey) {
    BasicText(text = key::class.simpleName.orEmpty())
}
