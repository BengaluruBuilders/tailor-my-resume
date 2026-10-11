package com.tailormyresume.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private object BackFirst : NavKey
private object BackSecond : NavKey
private object BackThird : NavKey
private object BackHome : NavKey

class NavigatorBackTest {

    private fun navigatorOf(
        vararg keys: NavKey,
        fallback: (NavKey) -> NavKey? = { null },
    ): Pair<NavigationState, Navigator> {
        val state = NavigationState(NavBackStack(*keys))
        return state to Navigator(state, fallback)
    }

    @Test
    fun popsWhenEntriesBelow() {
        val (state, navigator) = navigatorOf(BackFirst, BackSecond, BackThird)

        assertThat(navigator.goBack()).isTrue()

        assertThat(state.stack.toList()).containsExactly(BackFirst, BackSecond).inOrder()
        assertThat(state.currentKey).isEqualTo(BackSecond)
    }

    @Test
    fun emptyStackRootsTheFallbackKey() {
        val (state, navigator) = navigatorOf(BackFirst, fallback = { BackHome })

        assertThat(navigator.goBack()).isTrue()

        assertThat(state.stack.toList()).containsExactly(BackHome)
    }

    @Test
    fun emptyStackWithoutFallbackReturnsFalseAndKeepsTheStack() {
        val (state, navigator) = navigatorOf(BackFirst)

        assertThat(navigator.goBack()).isFalse()

        assertThat(state.stack.toList()).containsExactly(BackFirst)
    }

    @Test
    fun theFallbackIsAskedAboutTheCurrentKeyOnly() {
        val asked = mutableListOf<NavKey>()
        val (_, navigator) = navigatorOf(BackFirst, BackSecond, fallback = {
            asked += it
            null
        })

        navigator.goBack()
        navigator.goBack()

        assertThat(asked).containsExactly(BackFirst)
    }

    @Test
    fun canGoBackIsTrueOnlyWithEntriesBelow() {
        val (state, navigator) = navigatorOf(BackFirst, BackSecond)

        assertThat(state.canGoBack).isTrue()
        navigator.goBack()
        assertThat(state.canGoBack).isFalse()
    }
}
