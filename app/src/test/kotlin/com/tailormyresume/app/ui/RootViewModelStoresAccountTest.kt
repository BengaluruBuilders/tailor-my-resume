package com.tailormyresume.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RootViewModelStoresAccountTest {

    private val stores = RootViewModelStores()

    private class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private fun probeIn(root: AccountRoot): Probe =
        ViewModelProvider.create(stores.storeOf(root), viewModelFactory { initializer { Probe() } })[Probe::class.java]

    @Test
    fun keepOnly_clearsEveryOtherAccountRoot() {
        val signedOut = probeIn(AccountRoot(null))
        val first = probeIn(AccountRoot("account-1"))
        val second = probeIn(AccountRoot("account-2"))

        stores.keepOnly(AccountRoot("account-2"))

        assertThat(signedOut.cleared).isTrue()
        assertThat(first.cleared).isTrue()
        assertThat(second.cleared).isFalse()
        assertThat(probeIn(AccountRoot("account-2"))).isSameInstanceAs(second)
    }

    @Test
    fun aReleasedAccountRootGivesNewViewModels() {
        val before = probeIn(AccountRoot("account-1"))
        stores.keepOnly(AccountRoot("account-2"))

        assertThat(probeIn(AccountRoot("account-1"))).isNotSameInstanceAs(before)
    }
}
