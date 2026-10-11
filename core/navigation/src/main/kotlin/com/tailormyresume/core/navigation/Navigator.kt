package com.tailormyresume.core.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(
    val state: NavigationState,
    private val backFallback: (NavKey) -> NavKey? = { null },
) {

    val canHandleBack: Boolean
        get() = state.canGoBack || backFallback(state.currentKey) != null

    fun navigate(key: NavKey) {
        if (state.currentKey != key) state.stack.add(key)
    }

    fun navigateAll(keys: List<NavKey>) {
        keys.forEach(::navigate)
    }

    fun replace(key: NavKey) {
        if (state.canGoBack) {
            state.stack.removeLastOrNull()
            navigate(key)
        } else {
            root(key)
        }
    }

    fun root(key: NavKey) {
        state.stack.clear()
        state.stack.add(key)
    }

    fun goBack(): Boolean {
        if (state.canGoBack) {
            state.stack.removeLastOrNull()
            return true
        }
        val fallback = backFallback(state.currentKey) ?: return false
        root(fallback)
        return true
    }
}
