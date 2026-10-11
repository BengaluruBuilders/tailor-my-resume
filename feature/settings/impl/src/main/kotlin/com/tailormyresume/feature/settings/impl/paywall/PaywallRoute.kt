package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.settings.impl.R

@Composable
internal fun PaywallRoute(
    navigator: Navigator,
    returnToApplicationId: String?,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val sheetHost = LocalTmrSheetHost.current
    val resources = LocalContext.current.resources
    val failedTitle = stringResource(R.string.feature_settings_impl_pay_failed_title)
    val pendingMessage = stringResource(R.string.feature_settings_impl_toast_payment_pending)
    val nothingToRestore = stringResource(R.string.feature_settings_impl_toast_no_purchases)
    val restoreFailed = stringResource(R.string.feature_settings_impl_toast_restore_failed)
    val pricesFailed = stringResource(R.string.feature_settings_impl_toast_prices_failed)
    val showFailure: (Boolean) -> Unit = { unconfirmed ->
        sheetHost.show(failedTitle) {
            PaymentFailedSheetContent(
                onTryAgain = {
                    sheetHost.dismiss()
                    viewModel.onBuy()
                },
                onCancel = sheetHost::dismiss,
                unconfirmed = unconfirmed,
            )
        }
    }
    LaunchedEffect(viewModel, navigator, returnToApplicationId) {
        viewModel.events.collect { event ->
            when (event) {
                is PaywallEvent.Added -> {
                    toast.show(resources.getQuantityString(R.plurals.feature_settings_impl_toast_credits_added, event.credits, event.credits))
                    if (returnToApplicationId != null) {
                        navigator.replace(QuickQuestionNavKey(returnToApplicationId))
                    } else {
                        navigator.goBack()
                    }
                }
                PaywallEvent.Pending -> toast.show(pendingMessage)
                PaywallEvent.NothingToRestore -> toast.show(nothingToRestore)
                PaywallEvent.RestoreFailed -> toast.show(restoreFailed)
                PaywallEvent.PricesUnavailable -> toast.show(pricesFailed)
                PaywallEvent.PaymentFailed -> showFailure(false)
                PaywallEvent.PaymentUnconfirmed -> showFailure(true)
            }
        }
    }
    val actions = remember(viewModel) {
        PaywallActions(onSelectPack = viewModel::onSelectPack, onBuy = viewModel::onBuy, onRestore = viewModel::onRestore)
    }
    PaywallScreen(state = state, actions = actions, modifier = modifier)
}
