package com.tailormyresume.feature.tailor.impl.result

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.TailorFailedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TailorFailedNavigationTest {

    private val previous = TailoringNavKey("earlier", "run-0")

    private fun navigatorOnFailed(): Navigator =
        Navigator(NavigationState(NavBackStack<NavKey>(previous, TailorFailedNavKey("app-1", "run-1"))))

    @Test
    fun retryReplacesWithTailoringAndBackPops() {
        val retrying = navigatorOnFailed()
        TailorFailedNavigation(retrying, "app-1", "run-1").retry()
        assertThat(retrying.state.stack.toList()).containsExactly(previous, TailoringNavKey("app-1", "run-1")).inOrder()

        val leaving = navigatorOnFailed()
        TailorFailedNavigation(leaving, "app-1", "run-1").goBack()
        assertThat(leaving.state.stack.toList()).containsExactly(previous)
    }
}
