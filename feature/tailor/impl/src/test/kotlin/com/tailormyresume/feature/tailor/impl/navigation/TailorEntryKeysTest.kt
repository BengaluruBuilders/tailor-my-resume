package com.tailormyresume.feature.tailor.impl.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailorFailedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import com.tailormyresume.feature.tailor.impl.navigation.tailorEntry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TailorEntryKeysTest {

    private val navigator = Navigator(NavigationState(NavBackStack<NavKey>(TailoringNavKey("app-1", "run-1"))))

    private val provider: (NavKey) -> NavEntry<NavKey> =
        entryProvider {
            tailorEntry(navigator)
        }

    private val routedKeys: List<NavKey> =
        listOf(
            TailoringNavKey("app-1", "run-1"),
            TailorFailedNavKey("app-1", "run-1"),
            TailoredNavKey("app-1"),
            EditResumeNavKey("app-1"),
            ExportedNavKey("app-1"),
        )

    @Test
    fun routedKeysResolveToEntries() {
        routedKeys.forEach { key -> assertNotNull("${key::class.simpleName} entry", provider(key)) }
    }
}
