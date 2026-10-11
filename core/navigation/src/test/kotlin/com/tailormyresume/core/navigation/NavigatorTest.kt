package com.tailormyresume.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

private object TestStartKey : NavKey
private object TestKeyFirst : NavKey
private object TestKeySecond : NavKey

class NavigatorTest {

    private lateinit var navigationState: NavigationState
    private lateinit var navigator: Navigator

    @Before
    fun setup() {
        navigationState = NavigationState(NavBackStack(TestStartKey))
        navigator = Navigator(navigationState)
    }

    @Test
    fun testStartKey() {
        assertThat(navigationState.currentKey).isEqualTo(TestStartKey)
    }

    @Test
    fun testNavigate() {
        navigator.navigate(TestKeyFirst)

        assertThat(navigationState.stack).containsExactly(TestStartKey, TestKeyFirst).inOrder()
    }

    @Test
    fun testNavigateSingleTop() {
        navigator.navigate(TestKeyFirst)
        navigator.navigate(TestKeyFirst)

        assertThat(navigationState.stack).containsExactly(TestStartKey, TestKeyFirst).inOrder()
    }

    @Test
    fun testPopOne() {
        navigator.navigate(TestKeyFirst)
        navigator.navigate(TestKeySecond)

        navigator.goBack()

        assertThat(navigationState.stack).containsExactly(TestStartKey, TestKeyFirst).inOrder()
        assertThat(navigationState.currentKey).isEqualTo(TestKeyFirst)
    }

    @Test
    fun popMultiple() {
        navigator.navigate(TestKeyFirst)
        navigator.navigate(TestKeySecond)

        navigator.goBack()
        navigator.goBack()

        assertThat(navigationState.stack).containsExactly(TestStartKey)
        assertThat(navigationState.currentKey).isEqualTo(TestStartKey)
    }

    @Test
    fun goBackOnTheStartKeyReturnsFalseAndKeepsTheStack() {
        val handled = navigator.goBack()

        assertThat(handled).isFalse()
        assertThat(navigationState.stack).containsExactly(TestStartKey)
    }

    @Test
    fun goBackReturnsTrueWhenItPopsAScreen() {
        navigator.navigate(TestKeyFirst)

        assertThat(navigator.goBack()).isTrue()
        assertThat(navigator.goBack()).isFalse()
    }

    @Test
    fun canGoBackIsFalseOnlyOnTheStartKey() {
        assertThat(navigationState.canGoBack).isFalse()

        navigator.navigate(TestKeyFirst)
        assertThat(navigationState.canGoBack).isTrue()

        navigator.goBack()
        assertThat(navigationState.canGoBack).isFalse()
    }

    @Test
    fun navigateAllPushesTheKeysInOrder() {
        navigator.navigateAll(listOf(TestKeyFirst, TestKeySecond))

        assertThat(navigationState.stack).containsExactly(TestStartKey, TestKeyFirst, TestKeySecond).inOrder()
    }

    @Test
    fun navigateAllWithNoKeysChangesNothing() {
        navigator.navigateAll(emptyList())

        assertThat(navigationState.stack).containsExactly(TestStartKey)
    }

    @Test
    fun replaceSwapsTheTopKeyOfTheStack() {
        navigator.navigate(TestKeyFirst)

        navigator.replace(TestKeySecond)

        assertThat(navigationState.stack.toList()).containsExactly(TestStartKey, TestKeySecond).inOrder()
    }

    @Test
    fun replaceOnTheOnlyKeyRootsTheNewKey() {
        navigator.replace(TestKeyFirst)

        assertThat(navigationState.stack.toList()).containsExactly(TestKeyFirst)
    }

    @Test
    fun goBackAfterReplaceSkipsTheReplacedKey() {
        navigator.navigate(TestKeyFirst)
        navigator.replace(TestKeySecond)

        navigator.goBack()

        assertThat(navigationState.currentKey).isEqualTo(TestStartKey)
    }

    @Test
    fun canHandleBackFollowsTheStackAndTheFallback() {
        assertThat(navigator.canHandleBack).isFalse()

        navigator.navigate(TestKeyFirst)
        assertThat(navigator.canHandleBack).isTrue()

        val withFallback = Navigator(NavigationState(NavBackStack(TestStartKey))) { TestKeyFirst }
        assertThat(withFallback.canHandleBack).isTrue()
    }
}
