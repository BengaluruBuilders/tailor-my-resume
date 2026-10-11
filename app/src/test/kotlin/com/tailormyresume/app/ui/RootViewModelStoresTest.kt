package com.tailormyresume.app.ui

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private val SIGNED_OUT = AccountRoot(null)
private val SIGNED_IN = AccountRoot("account-1")

class RootViewModelStoresTest {

    private val stores = RootViewModelStores()

    private class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private fun probeIn(root: AccountRoot): Probe =
        ViewModelProvider.create(
            stores.storeOf(root),
            viewModelFactory { initializer { Probe() } },
        )[Probe::class.java]

    @Test
    fun leavingTheFirstRunRootClearsItsViewModels() {
        val firstRunProbe = probeIn(SIGNED_OUT)

        stores.keepOnly(SIGNED_IN)

        assertThat(firstRunProbe.cleared).isTrue()
    }

    @Test
    fun leavingTheFirstRunRootKeepsTheViewModelsOfTheMainRoot() {
        val mainProbe = probeIn(SIGNED_IN)
        probeIn(SIGNED_OUT)

        stores.keepOnly(SIGNED_IN)

        assertThat(mainProbe.cleared).isFalse()
        assertThat(probeIn(SIGNED_IN)).isSameInstanceAs(mainProbe)
    }

    @Test
    fun returningToTheFirstRunRootGivesNewViewModels() {
        val before = probeIn(SIGNED_OUT)
        stores.keepOnly(SIGNED_IN)

        stores.keepOnly(SIGNED_OUT)

        assertThat(probeIn(SIGNED_OUT)).isNotSameInstanceAs(before)
    }

    @Test
    fun rootOwnerUsesItsOwnStoreAndTheParentFactory() {
        val parent = ParentOwner()
        val store = stores.storeOf(SIGNED_IN)

        val owner = RootViewModelStoreOwner(parent, store)

        assertThat(owner.viewModelStore).isSameInstanceAs(store)
        assertThat(owner.viewModelStore).isNotSameInstanceAs(parent.viewModelStore)
        assertThat(owner.defaultViewModelProviderFactory).isSameInstanceAs(parent.defaultViewModelProviderFactory)
    }

    @Test
    fun clearingTheHolderClearsEveryRoot() {
        val first = probeIn(SIGNED_OUT)
        val main = probeIn(SIGNED_IN)

        stores.releaseAll()

        assertThat(first.cleared).isTrue()
        assertThat(main.cleared).isTrue()
    }

    private class ParentOwner : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = ViewModelStore()
        override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
            viewModelFactory { initializer { Probe() } }
    }
}
