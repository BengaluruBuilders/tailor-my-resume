package com.tailormyresume.app.navigation

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import org.junit.Test

class TopLevelNavItemTest {

    @Test
    fun startNavKey_isApplications() {
        assertThat(START_NAV_KEY).isEqualTo(DefaultApplicationsNavKey)
    }

    @Test
    fun topLevelNavKeys_areApplicationsThenProfile() {
        assertThat(TOP_LEVEL_NAV_KEYS)
            .containsExactly(DefaultApplicationsNavKey, DefaultProfileNavKey)
            .inOrder()
    }

    @Test
    fun startNavKey_isATopLevelNavKey() {
        assertThat(TOP_LEVEL_NAV_KEYS).contains(START_NAV_KEY)
    }
}
