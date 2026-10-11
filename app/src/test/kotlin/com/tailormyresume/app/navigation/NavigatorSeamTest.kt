package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavBackStack
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.navigateToSignIn
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.DefaultSettingsNavKey
import org.junit.Test

class NavigatorSeamTest {

    @Test
    fun signOutSeamRootsSignIn() {
        val state = NavigationState(
            NavBackStack(DefaultApplicationsNavKey, DefaultProfileNavKey, DefaultSettingsNavKey),
        )
        val navigator = Navigator(state)

        navigator.navigateToSignIn()

        assertThat(state.stack.toList()).containsExactly(DefaultSignInNavKey)
        assertThat(navigator.goBack()).isFalse()
    }
}
