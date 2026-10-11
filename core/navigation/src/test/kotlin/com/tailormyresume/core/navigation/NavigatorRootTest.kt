package com.tailormyresume.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private object RootFirst : NavKey
private object RootSecond : NavKey
private object RootTabOne : NavKey
private object RootTabTwo : NavKey

class NavigatorRootTest {

    private fun navigatorOf(vararg keys: NavKey): Pair<NavigationState, Navigator> {
        val state = NavigationState(NavBackStack(*keys))
        return state to Navigator(state)
    }

    @Test
    fun rootLeavesOnlyTargetAndBackDoesNotReturn() {
        val (state, navigator) = navigatorOf(RootFirst, RootSecond, RootTabOne)

        navigator.root(RootTabTwo)

        assertThat(state.stack.toList()).containsExactly(RootTabTwo)
        assertThat(navigator.goBack()).isFalse()
        assertThat(state.currentKey).isEqualTo(RootTabTwo)
    }

    @Test
    fun tabSwitchUsesRoot() {
        val (state, navigator) = navigatorOf(RootTabOne, RootFirst)

        navigator.root(RootTabTwo)
        navigator.root(RootTabOne)

        assertThat(state.stack.toList()).containsExactly(RootTabOne)
    }

    @Test
    fun rootOfTheCurrentKeyDropsTheEntriesBelow() {
        val (state, navigator) = navigatorOf(RootFirst, RootTabOne)

        navigator.root(RootTabOne)

        assertThat(state.stack.toList()).containsExactly(RootTabOne)
    }

    @Test
    fun navigatePushesOnTopAndIgnoresTheCurrentKey() {
        val (state, navigator) = navigatorOf(RootTabOne)

        navigator.navigate(RootFirst)
        navigator.navigate(RootFirst)

        assertThat(state.stack.toList()).containsExactly(RootTabOne, RootFirst).inOrder()
    }

    @Test
    fun navigateAllPushesTheKeysInOrder() {
        val (state, navigator) = navigatorOf(RootTabOne)

        navigator.navigateAll(listOf(RootFirst, RootSecond))

        assertThat(state.stack.toList()).containsExactly(RootTabOne, RootFirst, RootSecond).inOrder()
    }

    @Test
    fun replaceSwapsTheTopKeyAndRootsWhenItIsAlone() {
        val (state, navigator) = navigatorOf(RootTabOne, RootFirst)

        navigator.replace(RootSecond)
        assertThat(state.stack.toList()).containsExactly(RootTabOne, RootSecond).inOrder()

        navigator.root(RootTabOne)
        navigator.replace(RootTabTwo)
        assertThat(state.stack.toList()).containsExactly(RootTabTwo)
    }
}
