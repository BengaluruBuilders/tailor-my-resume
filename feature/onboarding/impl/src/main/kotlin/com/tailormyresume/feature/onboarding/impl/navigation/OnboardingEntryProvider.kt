package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey

fun EntryProviderScope<NavKey>.onboardingEntry(navigator: Navigator) {
    entry<SignInNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<ImportResumeNavKey> { key ->
        NavKeyPlaceholder(key)
    }
}

@Composable
internal fun NavKeyPlaceholder(key: NavKey) {
    BasicText(text = key::class.simpleName.orEmpty())
}
