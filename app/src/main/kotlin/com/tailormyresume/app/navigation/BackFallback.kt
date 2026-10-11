package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey

fun backFallback(
    current: NavKey,
    hasHome: Boolean,
): NavKey? {
    if (current is SignInNavKey) return null
    val target: NavKey = if (hasHome) DefaultApplicationsNavKey else UploadNavKey()
    return target.takeIf { it::class != current::class }
}

fun shellNavigator(
    state: NavigationState,
    hasHome: () -> Boolean,
): Navigator = Navigator(state) { current -> backFallback(current, hasHome()) }
