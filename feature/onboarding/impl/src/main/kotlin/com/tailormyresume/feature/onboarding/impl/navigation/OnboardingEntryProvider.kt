package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ConfirmFactsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ConsentNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.PasteJobDescriptionNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.WelcomeNavKey

fun EntryProviderScope<NavKey>.onboardingEntry(navigator: Navigator) {
    entry<WelcomeNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<SignInNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<ConsentNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<ImportResumeNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<ConfirmFactsNavKey> { key ->
        NavKeyPlaceholder(key)
    }
    entry<PasteJobDescriptionNavKey> { key ->
        NavKeyPlaceholder(key)
    }
}

@Composable
internal fun NavKeyPlaceholder(key: NavKey) {
    BasicText(text = key::class.simpleName.orEmpty())
}
