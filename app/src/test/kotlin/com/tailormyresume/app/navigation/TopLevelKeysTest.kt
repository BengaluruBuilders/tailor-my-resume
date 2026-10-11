package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.DefaultSettingsNavKey
import org.junit.Test

class TopLevelKeysTest {

    @Test
    fun theTopLevelKeysAreApplicationsThenProfile() {
        assertThat(TOP_LEVEL_NAV_KEYS).containsExactly(DefaultApplicationsNavKey, DefaultProfileNavKey).inOrder()
        assertThat(START_NAV_KEY).isEqualTo(DefaultApplicationsNavKey)
    }

    @Test
    fun settingsIsNotATopLevelDestination() {
        assertThat(DefaultSettingsNavKey.isTopLevelDestination()).isFalse()
    }

    @Test
    fun onlyApplicationsAndProfileKeysAreTopLevelDestinations() {
        val topLevel = CHROME_TABLE.map { it.key }.filter(NavKey::isTopLevelDestination)

        assertThat(topLevel.map { it::class.simpleName }).containsExactly("ApplicationsNavKey", "ProfileNavKey")
    }
}
