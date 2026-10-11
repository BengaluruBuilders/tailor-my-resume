package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey

val START_NAV_KEY: NavKey = DefaultApplicationsNavKey

val TOP_LEVEL_NAV_KEYS: List<NavKey> = listOf(DefaultApplicationsNavKey, DefaultProfileNavKey)

fun NavKey.isTopLevelDestination(): Boolean = this is ApplicationsNavKey || this is ProfileNavKey
