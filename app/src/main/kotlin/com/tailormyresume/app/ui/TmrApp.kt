package com.tailormyresume.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.tailormyresume.app.R
import com.tailormyresume.app.navigation.TOP_LEVEL_NAV_KEYS
import com.tailormyresume.app.navigation.chromeFor
import com.tailormyresume.app.navigation.isTopLevelDestination
import com.tailormyresume.app.navigation.shellNavigator
import com.tailormyresume.core.designsystem.component.TmrBackground
import com.tailormyresume.core.designsystem.component.TmrVisibility
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.designsystem.component.chrome.TmrStepBar
import com.tailormyresume.core.designsystem.component.chrome.TmrTab
import com.tailormyresume.core.designsystem.component.chrome.TmrTabBar
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBar
import com.tailormyresume.core.designsystem.component.chrome.TmrTopBarLeading
import com.tailormyresume.core.designsystem.component.rememberTmrNavTransitions
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.navigation.ChromeActions
import com.tailormyresume.core.navigation.LocalChromeActions
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.PendingNavigation
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.core.navigation.rememberNavigationState
import com.tailormyresume.core.navigation.toEntries
import com.tailormyresume.feature.analysis.api.navigation.newApp
import com.tailormyresume.feature.analysis.impl.navigation.analysisEntry
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.applications.impl.navigation.applicationDetailEntry
import com.tailormyresume.feature.applications.impl.navigation.applicationsEntry
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.navigation.onboardingEntry
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.profile.impl.navigation.profileEntry
import com.tailormyresume.feature.settings.impl.navigation.settingsEntry
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.impl.navigation.tailorEntry

@Composable
fun TmrApp(
    rootState: AppRootState,
    modifier: Modifier = Modifier,
    hasHome: () -> Boolean = { false },
    entryProvider: ((Navigator) -> (NavKey) -> NavEntry<NavKey>)? = null,
) {
    val stores = rememberRootViewModelStores()
    LaunchedEffect(rootState) {
        if (rootState is AppRootState.Ready) stores.keepOnly(AccountRoot(rootState.accountId))
    }
    TmrBackground(modifier = modifier) {
        when (rootState) {
            AppRootState.Loading -> Unit
            is AppRootState.Ready -> TmrAccountRoot(ready = rootState, hasHome = hasHome, entryProvider = entryProvider)
        }
    }
}

@Composable
internal fun TmrAccountRoot(
    ready: AppRootState.Ready,
    modifier: Modifier = Modifier,
    hasHome: () -> Boolean = { false },
    entryProvider: ((Navigator) -> (NavKey) -> NavEntry<NavKey>)? = null,
) {
    key(ready.accountId) {
        TmrRoot(
            root = AccountRoot(ready.accountId),
            startKey = ready.start.navKey(),
            modifier = modifier,
            hasHome = hasHome,
            entryProvider = entryProvider,
        )
    }
}

private fun StartDestination.navKey(): NavKey = when (this) {
    StartDestination.SignIn -> DefaultSignInNavKey
    StartDestination.Upload -> UploadNavKey()
    StartDestination.Applications -> DefaultApplicationsNavKey
}

@Composable
internal fun TmrRoot(
    root: AccountRoot,
    startKey: NavKey,
    modifier: Modifier = Modifier,
    initialKeys: () -> List<NavKey> = PendingNavigation::consume,
    hasHome: () -> Boolean = { false },
    entryProvider: ((Navigator) -> (NavKey) -> NavEntry<NavKey>)? = null,
) {
    WithRootViewModelStore(root) {
        val navigationState = rememberNavigationState(startKey)
        val navigator = remember(navigationState) {
            shellNavigator(navigationState, hasHome).also { navigator ->
                val pending = initialKeys()
                if (startKey.isTopLevelDestination()) {
                    pending.forEach { key ->
                        if (key.isTopLevelDestination()) navigator.root(key) else navigator.navigate(key)
                    }
                }
            }
        }
        if (entryProvider == null) {
            TmrShell(navigationState = navigationState, navigator = navigator, modifier = modifier, accountId = root.accountId)
        } else {
            TmrShell(
                navigationState = navigationState,
                navigator = navigator,
                modifier = modifier,
                accountId = root.accountId,
                entryProvider = entryProvider(navigator),
            )
        }
    }
}

@Composable
internal fun TmrShell(
    navigationState: NavigationState,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    accountId: String? = null,
    entryProvider: (NavKey) -> NavEntry<NavKey> = remember(navigator) { sharedEntryProvider(navigator) },
) {
    val toastState = remember { TmrToastState() }
    val sheetHost = remember { TmrSheetHostState() }
    val chromeActions = remember { ChromeActions() }
    val key = navigationState.currentKey
    LaunchedEffect(key) { sheetHost.dismiss() }
    val context = LocalContext.current
    val queuedToast = PendingToast.queued
    LaunchedEffect(queuedToast) {
        PendingToast.consumeFor(accountId)?.let { message -> toastState.show(context.getString(message)) }
    }
    val chrome = chromeFor(key, entriesBelow = navigationState.stack.size - 1)
    val tabsVisible = key.isTopLevelDestination()
    val changesSaved = stringResource(R.string.shell_changes_saved)
    BackHandler(enabled = !navigationState.canGoBack && navigator.canHandleBack) { navigator.goBack() }
    CompositionLocalProvider(
        LocalTmrToast provides toastState,
        LocalTmrSheetHost provides sheetHost,
        LocalChromeActions provides chromeActions,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (chrome.topBar) {
                    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                        TmrTopBar(
                            leading = chrome.leading,
                            onLeading = {
                                if (chrome.leading == TmrTopBarLeading.Close && key is ExportedNavKey) {
                                    navigator.root(DefaultApplicationsNavKey)
                                } else {
                                    navigator.goBack()
                                }
                            },
                            title = chrome.title?.let { stringResource(it) },
                            action = chrome.action?.let { stringResource(it.label) },
                            onAction = {
                                val registered = chromeActions.handler
                                if (registered != null) {
                                    registered()
                                } else {
                                    navigator.goBack()
                                    toastState.show(changesSaved)
                                }
                            },
                        )
                        chrome.step?.let { step ->
                            TmrStepBar(
                                current = step,
                                modifier = Modifier.padding(horizontal = TmrTheme.spacing.gutter, vertical = 6.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (chrome.topBar) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)
                        .then(if (tabsVisible) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
                ) {
                    TmrNavDisplay(
                        navigationState = navigationState,
                        navigator = navigator,
                        entryProvider = entryProvider,
                    )
                }
                TmrVisibility(visible = tabsVisible, rise = true) {
                    TmrTabBar(
                        selected = if (key is ProfileNavKey) TmrTab.Profile else TmrTab.Applications,
                        onApplications = { navigator.root(DefaultApplicationsNavKey) },
                        onAdd = navigator::newApp,
                        onProfile = { navigator.root(DefaultProfileNavKey) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    )
                }
            }
            TmrSheetHost(state = sheetHost)
            TmrToastHost(state = toastState)
        }
    }
}

@Composable
private fun TmrNavDisplay(
    navigationState: NavigationState,
    navigator: Navigator,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    val tabbedProvider = remember(entryProvider) { withTabMetadata(entryProvider) }
    val transitions = rememberTmrNavTransitions()
    Box(modifier = modifier) {
        NavDisplay(
            entries = navigationState.toEntries(tabbedProvider),
            onBack = { navigator.goBack() },
            transitionSpec = {
                tabDirection(initialState, targetState)?.let { transitions.tab(this, it, pop = false) }
                    ?: transitions.forward(this, hierarchical = !targetState.isTopLevel())
            },
            popTransitionSpec = {
                tabDirection(initialState, targetState)?.let { transitions.tab(this, it, pop = true) }
                    ?: transitions.back(hierarchical = !initialState.isTopLevel())
            },
            predictivePopTransitionSpec = {
                tabDirection(initialState, targetState)?.let { direction -> transitions.tab(this, direction, pop = true) }
                    ?: transitions.back(hierarchical = !initialState.isTopLevel())
            },
        )
    }
}

private fun Scene<NavKey>.isTopLevel(): Boolean = metadata[TOP_LEVEL_METADATA] == true

private fun tabDirection(from: Scene<NavKey>, to: Scene<NavKey>): Int? {
    val fromIndex = from.metadata[TAB_INDEX_METADATA] as? Int ?: return null
    val toIndex = to.metadata[TAB_INDEX_METADATA] as? Int ?: return null
    return toIndex.compareTo(fromIndex)
}

private const val TOP_LEVEL_METADATA = "tmrTopLevel"
private const val TAB_INDEX_METADATA = "tmrTabIndex"

private fun sharedEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        applicationsEntry(navigator)
        applicationDetailEntry(navigator)
        onboardingEntry(navigator)
        profileEntry(navigator)
        analysisEntry(navigator)
        tailorEntry(navigator)
        settingsEntry(navigator)
    }

private fun withTabMetadata(
    provider: (NavKey) -> NavEntry<NavKey>,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val entry = provider(key)
    val tabIndex = TOP_LEVEL_NAV_KEYS.indexOfFirst { it::class == key::class }
    val metadata = entry.metadata + (TOP_LEVEL_METADATA to key.isTopLevelDestination()) +
        if (tabIndex >= 0) mapOf(TAB_INDEX_METADATA to tabIndex) else emptyMap()
    NavEntry(key = key, contentKey = entry.contentKey, metadata = metadata) { entry.Content() }
}
