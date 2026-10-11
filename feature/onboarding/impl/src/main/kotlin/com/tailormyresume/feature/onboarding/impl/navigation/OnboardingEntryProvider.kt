package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.tailormyresume.feature.onboarding.impl.manual.ManualProfileRoute
import com.tailormyresume.feature.onboarding.impl.paste.PasteResumeRoute
import com.tailormyresume.feature.onboarding.impl.reading.ReadingRoute
import com.tailormyresume.feature.onboarding.impl.signin.SignInRoute
import com.tailormyresume.feature.onboarding.impl.upload.UnreadableRoute
import com.tailormyresume.feature.onboarding.impl.upload.UploadRoute

fun EntryProviderScope<NavKey>.onboardingEntry(navigator: Navigator) {
    entry<SignInNavKey> { SignInRoute(hiltViewModel()) }
    entry<UploadNavKey> { UploadRoute(hiltViewModel(), navigator) }
    entry<UploadErrorNavKey> { UnreadableRoute(hiltViewModel(), navigator) }
    entry<PasteResumeNavKey> { PasteResumeRoute(hiltViewModel(), navigator) }
    entry<ManualProfileNavKey> { ManualProfileRoute(hiltViewModel(), navigator) }
    entry<ReadingNavKey> { ReadingRoute(hiltViewModel(), navigator) }
    entry<ReviewProfileNavKey> { key -> NavKeyPlaceholder(key) }
}
