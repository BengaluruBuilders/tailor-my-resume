package com.tailormyresume.feature.tailor.impl.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailorFailedNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import com.tailormyresume.feature.tailor.impl.TailorViewModel
import com.tailormyresume.feature.tailor.impl.result.TailorFailedNavigation
import com.tailormyresume.feature.tailor.impl.result.TailorFailedScreen
import com.tailormyresume.feature.tailor.impl.result.TailoredRoute
import com.tailormyresume.feature.tailor.impl.result.TailoredViewModel
import com.tailormyresume.feature.tailor.impl.tailoring.TailoringRoute
import com.tailormyresume.feature.tailor.impl.tailoring.TailoringViewModel

internal object TailorViewModelKeys {
    fun tailor(applicationId: String) = "tailor-$applicationId"

    fun tailored(applicationId: String) = "tailored-$applicationId"
}

fun EntryProviderScope<NavKey>.tailorEntry(navigator: Navigator) {
    entry<TailoringNavKey> { key ->
        TailoringRoute(
            viewModel = hiltViewModel<TailoringViewModel, TailoringViewModel.Factory>(key = key.runId) { factory ->
                factory.create(key.applicationId, key.runId)
            },
            onDone = { navigator.replace(TailoredNavKey(key.applicationId, key.scenario)) },
            onFailed = { navigator.replace(TailorFailedNavKey(key.applicationId, key.runId, key.scenario)) },
        )
    }
    entry<TailorFailedNavKey> { key ->
        val navigation = remember(key) { TailorFailedNavigation(navigator, key.applicationId, key.runId) }
        TailorFailedScreen(onTryAgain = navigation::retry, onGoBack = navigation::goBack)
    }
    entry<TailoredNavKey> { key ->
        TailoredRoute(
            tailorViewModel = hiltViewModel<TailorViewModel, TailorViewModel.Factory>(key = TailorViewModelKeys.tailor(key.applicationId)) { factory ->
                factory.create(key.applicationId, key.scenario)
            },
            tailoredViewModel = hiltViewModel<TailoredViewModel, TailoredViewModel.Factory>(key = TailorViewModelKeys.tailored(key.applicationId)) { factory ->
                factory.create(key.applicationId)
            },
            onNavigate = navigator::navigate,
        )
    }
    entry<EditResumeNavKey> { key -> NavKeyPlaceholder(key) }
    entry<ExportedNavKey> { key -> NavKeyPlaceholder(key) }
}
