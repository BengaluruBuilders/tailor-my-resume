package com.tailormyresume.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.api.navigation.navigateToSignIn
import org.junit.Test

class StartOverNavigationTest {

    @Test
    fun navigateToSignIn_dropsUploadFromTheStack() {
        val stack = NavBackStack<NavKey>(DefaultSignInNavKey)
        val navigator = Navigator(NavigationState(stack))
        navigator.navigate(UploadNavKey())

        navigator.navigateToSignIn()

        assertThat(stack.toList()).containsExactly(DefaultSignInNavKey)
    }
}
