package com.tailormyresume.feature.applications.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.newApp
import com.tailormyresume.feature.applications.api.navigation.navigateToApplicationDetail
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.settings.api.navigation.navigateToCredits

@Composable
internal fun ApplicationsRoute(viewModel: ApplicationsViewModel, navigator: Navigator) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApplicationsScreen(
        state = state,
        onCredits = navigator::navigateToCredits,
        onAvatar = { navigator.root(DefaultProfileNavKey) },
        onAddJob = navigator::newApp,
        onOpenApplication = navigator::navigateToApplicationDetail,
    )
}
