package com.tailormyresume.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

internal data class AccountRoot(val accountId: String?)

internal class RootViewModelStores : ViewModel() {

    private val stores = mutableMapOf<AccountRoot, ViewModelStore>()

    fun storeOf(root: AccountRoot): ViewModelStore = stores.getOrPut(root) { ViewModelStore() }

    fun release(root: AccountRoot) {
        stores.remove(root)?.clear()
    }

    fun releaseAll() = stores.keys.toList().forEach(::release)

    fun keepOnly(root: AccountRoot) = stores.keys.filter { it != root }.forEach(::release)

    override fun onCleared() = releaseAll()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory { initializer { RootViewModelStores() } }
    }
}

internal class RootViewModelStoreOwner(
    parent: ViewModelStoreOwner,
    override val viewModelStore: ViewModelStore,
) : ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory by (parent as HasDefaultViewModelProviderFactory)

@Composable
internal fun rememberRootViewModelStores(): RootViewModelStores =
    viewModel(factory = RootViewModelStores.Factory)

@Composable
internal fun WithRootViewModelStore(
    root: AccountRoot,
    content: @Composable () -> Unit,
) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val stores = rememberRootViewModelStores()
    val owner = remember(parent, stores, root) { RootViewModelStoreOwner(parent, stores.storeOf(root)) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
