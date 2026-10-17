package com.tailormyresume.feature.tailor.impl.tailoring

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Test

class TailoringNavigationTest {

    @Test
    fun noCreditReplacesTailoringWithThePaywallThatReturnsToTheApplication() {
        val previous = TailoringNavKey("earlier", "run-0")
        val navigator = Navigator(NavigationState(NavBackStack<NavKey>(previous, TailoringNavKey("app-1", "run-1"))))

        TailoringNavigation(navigator, "app-1").noCredit()

        assertThat(navigator.state.stack.toList()).containsExactly(previous, PaywallNavKey("app-1")).inOrder()
    }
}
