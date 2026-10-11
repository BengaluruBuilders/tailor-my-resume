package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.DefaultSettingsNavKey
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun theTwoTabKeysAreTopLevel() {
        listOf<NavKey>(DefaultApplicationsNavKey, DefaultProfileNavKey)
            .forEach { key -> assertThat(key.isTopLevelDestination()).isTrue() }
    }

    @Test
    fun aTabKeyWithAScenarioIsStillTopLevel() {
        assertThat(ApplicationsNavKey(scenario = DebugScenario.EMPTY).isTopLevelDestination()).isTrue()
    }

    @Test
    fun pushedAndFirstRunKeysAreNotTopLevel() {
        listOf<NavKey>(
            DefaultSignInNavKey,
            DefaultSettingsNavKey,
            JobResultNavKey(applicationId = "a-1"),
            ApplicationDetailNavKey(applicationId = "a-1"),
        ).forEach { key -> assertThat(key.isTopLevelDestination()).isFalse() }
    }
}
