package com.tailormyresume.app.debug

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.app.MainActivity
import com.tailormyresume.app.ai.DebugPreviewMode
import com.tailormyresume.app.ui.AppRootState
import com.tailormyresume.app.ui.AppViewModel
import com.tailormyresume.app.ui.NavigationRoot
import com.tailormyresume.app.ui.RootViewModelStores
import com.tailormyresume.app.ui.TmrFirstRunRoot
import com.tailormyresume.app.ui.TmrMainRoot
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.designsystem.component.TmrBackground
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class DebugScenarioActivity : ComponentActivity() {

    private val menuViewModel: DebugMenuViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()
    private val rootStores: RootViewModelStores by viewModels { RootViewModelStores.Factory }

    private var target by mutableStateOf(DebugScenarioTarget.Applications)
    private var scenario by mutableStateOf(DebugScenario.defaultValue)
    private var opened by mutableStateOf(false)

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var previewMode: DebugPreviewMode

    private val previewLifecycle by lazy {
        DebugPreviewLifecycle(
            previewMode = previewMode,
            forcePayment = { menuViewModel.forcePaymentScenario(target, scenario) },
            releasePayment = menuViewModel::releasePaymentScenario,
        )
    }

    private val hasAccount: Flow<Boolean> by lazy { sessionRepository.observeAccount().map { it != null } }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(
            DebugPreviewLifecycleObserver(
                activity = this,
                lifecycle = previewLifecycle,
                opened = { opened },
                closePreview = ::closePreview,
            ),
        )
        setContent {
            TmrTheme {
                BackHandler(enabled = opened) { closePreview() }
                TmrBackground {
                    if (opened) {
                        DebugScenarioPreview(
                            target = target,
                            scenario = scenario,
                            rootState = appViewModel.rootState,
                            hasAccount = hasAccount,
                            rootStores = rootStores,
                        )
                    } else {
                        val uiState by menuViewModel.uiState.collectAsStateWithLifecycle()
                        DebugScenarioMenu(
                            uiState = uiState,
                            selectedTarget = target,
                            selectedScenario = scenario,
                            actions = DebugMenuActions(
                                onLoadSampleData = menuViewModel::loadSampleData,
                                onResetAppData = menuViewModel::resetAppData,
                                onOpenApp = ::openApp,
                                onTargetSelected = { target = it },
                                onScenarioSelected = { scenario = it },
                                onOpenScreen = ::openPreview,
                            ),
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) previewMode.active = false
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyEdgeToEdge()
    }

    private fun applyEdgeToEdge() {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
    }

    private fun openPreview() {
        previewMode.active = true
        rootStores.releaseAll()
        menuViewModel.openPreview(target, scenario) { opened = true }
    }

    private fun closePreview() {
        previewMode.active = false
        opened = false
        rootStores.releaseAll()
        menuViewModel.closePreview(target)
    }

    private fun openApp() {
        if (opensRealApp(target, scenario)) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            openPreview()
        }
    }
}

@Composable
private fun DebugScenarioPreview(
    target: DebugScenarioTarget,
    scenario: DebugScenario,
    rootState: StateFlow<AppRootState>,
    hasAccount: Flow<Boolean>,
    rootStores: RootViewModelStores,
    modifier: Modifier = Modifier,
) {
    val key = target.navKey(scenario)
    val shownRoot = observePreviewRoot(target.opensFirstRunRoot, rootState, hasAccount, rootStores)
    when {
        target.opensFirstRunRoot -> TmrFirstRunRoot(modifier = modifier, startKey = key)
        shownRoot == NavigationRoot.FirstRun -> TmrFirstRunRoot(modifier = modifier, startKey = SignInNavKey())
        else -> TmrMainRoot(modifier = modifier, initialKeys = { listOf(key) })
    }
}

@Composable
internal fun observePreviewRoot(
    opensFirstRunRoot: Boolean,
    rootState: StateFlow<AppRootState>,
    hasAccount: Flow<Boolean>,
    rootStores: RootViewModelStores,
): NavigationRoot {
    val currentRoot by rootState.collectAsStateWithLifecycle()
    val accountPresent by hasAccount.collectAsStateWithLifecycle(initialValue = false)
    var seenMain by remember { mutableStateOf(false) }
    var seenAccount by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoot) {
        if (currentRoot == AppRootState.Main) seenMain = true
    }
    LaunchedEffect(accountPresent) {
        if (accountPresent) seenAccount = true
    }
    val shownRoot = previewNavigationRoot(opensFirstRunRoot, seenMain, currentRoot, seenAccount, accountPresent)
    LaunchedEffect(shownRoot) { rootStores.keepOnly(shownRoot) }
    return shownRoot
}

internal fun previewShowsWelcome(seenMain: Boolean, rootState: AppRootState): Boolean =
    seenMain && rootState == AppRootState.FirstRun

internal fun previewNavigationRoot(
    opensFirstRunRoot: Boolean,
    seenMain: Boolean,
    rootState: AppRootState,
    seenAccount: Boolean,
    hasAccount: Boolean,
): NavigationRoot {
    val welcome = previewShowsWelcome(seenMain, rootState) || (seenAccount && !hasAccount)
    return if (opensFirstRunRoot || welcome) NavigationRoot.FirstRun else NavigationRoot.Main
}

internal fun opensRealApp(target: DebugScenarioTarget, scenario: DebugScenario): Boolean =
    target == DebugScenarioTarget.Applications && scenario == DebugScenario.defaultValue
