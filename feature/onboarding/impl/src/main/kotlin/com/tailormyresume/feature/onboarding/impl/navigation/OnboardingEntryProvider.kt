package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ManualProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.PasteResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadErrorNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey

fun EntryProviderScope<NavKey>.onboardingEntry(navigator: Navigator) {
    entry<SignInNavKey> { key -> NavKeyPlaceholder(key) }
    entry<UploadNavKey> { key -> NavKeyPlaceholder(key) }
    entry<UploadErrorNavKey> { key -> NavKeyPlaceholder(key) }
    entry<PasteResumeNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ManualProfileNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ReadingNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ReviewProfileNavKey> { key -> NavKeyPlaceholder(key) }
}
