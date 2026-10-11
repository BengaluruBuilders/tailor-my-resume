package com.tailormyresume.app.navigation

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import org.junit.Test

class BackFallbackTest {

    @Test
    fun emptyStackGoesToApplicationsWhenTheUserHasHome() {
        assertThat(backFallback(DefaultProfileNavKey, hasHome = true)).isEqualTo(DefaultApplicationsNavKey)
    }

    @Test
    fun emptyStackGoesToUploadOtherwise() {
        assertThat(backFallback(DefaultProfileNavKey, hasHome = false)).isEqualTo(UploadNavKey())
    }

    @Test
    fun theTargetItselfHasNoFallbackSoSystemBackLeavesTheApp() {
        assertThat(backFallback(DefaultApplicationsNavKey, hasHome = true)).isNull()
        assertThat(backFallback(UploadNavKey(), hasHome = false)).isNull()
    }

    @Test
    fun signInNeverFallsBackPastItself() {
        assertThat(backFallback(DefaultSignInNavKey, hasHome = true)).isNull()
        assertThat(backFallback(DefaultSignInNavKey, hasHome = false)).isNull()
    }
}
