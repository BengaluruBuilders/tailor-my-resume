package com.tailormyresume.feature.applications.impl.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationsNavKey
import com.tailormyresume.feature.applications.impl.ApplicationsRoute
import com.tailormyresume.feature.applications.impl.ApplicationsViewModel
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailRoute
import com.tailormyresume.feature.applications.impl.detail.ApplicationDetailViewModel

fun EntryProviderScope<NavKey>.applicationsEntry(navigator: Navigator) {
    entry<ApplicationsNavKey> {
        ApplicationsRoute(viewModel = hiltViewModel<ApplicationsViewModel>(), navigator = navigator)
    }
}

fun EntryProviderScope<NavKey>.applicationDetailEntry(navigator: Navigator) {
    entry<ApplicationDetailNavKey> { key ->
        ApplicationDetailRoute(
            viewModel = hiltViewModel<ApplicationDetailViewModel, ApplicationDetailViewModel.Factory>(
                key = "application-${key.applicationId}",
            ) { factory -> factory.create(key.applicationId) },
            navigator = navigator,
        )
    }
}
