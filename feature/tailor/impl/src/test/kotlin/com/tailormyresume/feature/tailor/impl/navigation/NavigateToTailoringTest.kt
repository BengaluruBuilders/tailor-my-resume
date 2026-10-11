package com.tailormyresume.feature.tailor.impl.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import com.tailormyresume.feature.tailor.api.navigation.navigateToTailoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigateToTailoringTest {

    private fun navigatorOver(vararg keys: NavKey) =
        Navigator(NavigationState(NavBackStack(*keys)))

    private fun Navigator.tailoringKeys() = state.stack.filterIsInstance<TailoringNavKey>()

    @Test
    fun twoRapidCallsLeaveOneTailoringKey() {
        val navigator = navigatorOver(TailoredNavKey("app-1"))
        navigator.navigateToTailoring("app-1")
        navigator.navigateToTailoring("app-1")
        assertEquals(1, navigator.tailoringKeys().size)
    }

    @Test
    fun tapAfterTheRunFinishedMintsANewRunId() {
        val navigator = navigatorOver(TailoredNavKey("app-1"))
        navigator.navigateToTailoring("app-1")
        val first = navigator.tailoringKeys().single()
        navigator.replace(TailoredNavKey("app-1"))
        navigator.navigateToTailoring("app-1")
        assertNotEquals(first.runId, navigator.tailoringKeys().single().runId)
    }
}
