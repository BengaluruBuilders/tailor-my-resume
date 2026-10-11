package com.tailormyresume.feature.settings.impl.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import com.tailormyresume.feature.settings.api.navigation.navigateToCredits
import com.tailormyresume.feature.settings.impl.R
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountSheet
import com.tailormyresume.feature.settings.impl.delete.DeleteAccountViewModel
import com.tailormyresume.feature.settings.impl.navigation.createSettingsShareIntent

@Composable
internal fun SettingsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    deleteViewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalTmrToast.current
    val sheetHost = LocalTmrSheetHost.current
    val versionName = remember(context) { context.versionName() }
    val shareSubject = stringResource(R.string.feature_settings_impl_share_subject)
    val shareChooser = stringResource(R.string.feature_settings_impl_share_chooser)
    val dataReady = stringResource(R.string.feature_settings_impl_toast_data_ready)
    val dataFailed = stringResource(R.string.feature_settings_impl_toast_data_failed)
    val supportAddress = stringResource(R.string.feature_settings_impl_support_email)
    val supportSubject = stringResource(R.string.feature_settings_impl_support_subject)
    val supportMissing = stringResource(R.string.feature_settings_impl_toast_support_missing)
    val deleteTitle = stringResource(R.string.feature_settings_impl_delete_title)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShareArchive -> toast.show(
                    if (context.shareArchive(event, shareSubject, shareChooser)) dataReady else dataFailed,
                )
                SettingsEvent.DownloadFailed -> toast.show(dataFailed)
            }
        }
    }
    val actions = remember(viewModel, deleteViewModel, navigator, sheetHost, toast, context) {
        SettingsActions(
            onCredits = navigator::navigateToCredits,
            onPageSize = viewModel::onPageSizeClicked,
            onFileName = viewModel::onFileNameClicked,
            onProductUpdates = viewModel::onProductUpdatesToggled,
            onDownloadData = viewModel::onDownloadMyData,
            onHelp = {
                if (!context.openSupportMail(supportAddress, supportSubject)) toast.show(supportMissing)
            },
            onDeleteAccount = {
                sheetHost.show(deleteTitle) {
                    DeleteAccountSheet(viewModel = deleteViewModel, onKeep = sheetHost::dismiss)
                }
            },
            onSignOut = viewModel::onSignOut,
        )
    }
    SettingsScreen(state = state, versionName = versionName, actions = actions, modifier = modifier)
}

private fun Context.shareArchive(event: SettingsEvent.ShareArchive, subject: String, chooserTitle: String): Boolean =
    try {
        startActivity(createSettingsShareIntent(this, event.file, subject, chooserTitle))
        true
    } catch (missing: ActivityNotFoundException) {
        false
    } catch (outsideSharedPaths: IllegalArgumentException) {
        false
    }

@Suppress("DEPRECATION")
private fun Context.versionName(): String {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    return info.versionName.orEmpty()
}
