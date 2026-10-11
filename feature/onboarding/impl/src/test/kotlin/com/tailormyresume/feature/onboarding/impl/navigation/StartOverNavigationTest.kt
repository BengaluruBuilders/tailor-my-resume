package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey
import com.tailormyresume.feature.onboarding.api.navigation.navigateToSignIn
import org.junit.Test

class StartOverNavigationTest {

    @Test
    fun navigateToSignIn_dropsImportResumeFromTheStack() {
        val subStack = NavBackStack<NavKey>(DefaultSignInNavKey)
        val navigator = Navigator(
            NavigationState(
                startKey = DefaultSignInNavKey,
                topLevelStack = NavBackStack(DefaultSignInNavKey),
                subStacks = mapOf(DefaultSignInNavKey to subStack),
            ),
        )
        navigator.navigate(ImportResumeNavKey())

        navigator.navigateToSignIn()

        assertThat(subStack.toList()).containsExactly(DefaultSignInNavKey)
    }
}
