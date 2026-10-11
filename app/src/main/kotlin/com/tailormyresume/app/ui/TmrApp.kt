package com.tailormyresume.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.tailormyresume.app.navigation.START_NAV_KEY
import com.tailormyresume.app.navigation.TOP_LEVEL_NAV_ITEMS
import com.tailormyresume.app.navigation.isTopLevelDestination
import com.tailormyresume.core.designsystem.component.LocalTmrBottomInset
import com.tailormyresume.core.designsystem.component.TmrBackground
import com.tailormyresume.core.designsystem.component.TmrDock
import com.tailormyresume.core.designsystem.component.TmrDockDefaults
import com.tailormyresume.core.designsystem.component.TmrDockIcon
import com.tailormyresume.core.designsystem.component.TmrDockItem
import com.tailormyresume.core.designsystem.component.rememberTmrNavTransitions
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.PendingNavigation
import com.tailormyresume.core.navigation.rememberNavigationState
import com.tailormyresume.core.navigation.toEntries
import com.tailormyresume.feature.analysis.impl.navigation.analysisEntry
import com.tailormyresume.feature.applications.impl.navigation.applicationDetailEntry
import com.tailormyresume.feature.applications.impl.navigation.applicationsEntry
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.impl.navigation.onboardingEntry
import com.tailormyresume.feature.profile.impl.navigation.profileEntry
import com.tailormyresume.feature.settings.impl.navigation.settingsEntry
import com.tailormyresume.feature.tailor.impl.navigation.tailorEntry

@Composable
fun TmrApp(
    rootState: AppRootState,
    modifier: Modifier = Modifier,
) {
    val stores = rememberRootViewModelStores()
    LaunchedEffect(rootState) {
        when (rootState) {
            AppRootState.Loading -> Unit
            AppRootState.FirstRun -> stores.keepOnly(NavigationRoot.FirstRun)
            AppRootState.Main -> stores.keepOnly(NavigationRoot.Main)
        }
    }
    TmrBackground(modifier = modifier) {
        when (rootState) {
            AppRootState.Loading -> Unit
            AppRootState.FirstRun -> TmrFirstRunRoot()
            AppRootState.Main -> TmrMainRoot()
        }
    }
}

@Composable
internal fun TmrFirstRunRoot(
    modifier: Modifier = Modifier,
    startKey: NavKey = DefaultSignInNavKey,
    initialKeys: () -> List<NavKey> = PendingNavigation::consume,
) {
    WithRootViewModelStore(NavigationRoot.FirstRun) {
        val navigationState = rememberNavigationState(startKey, setOf(startKey))
        val navigator = remember(navigationState) {
            Navigator(navigationState).also { it.navigateAll(initialKeys()) }
        }
        TmrNavDisplay(
            navigationState = navigationState,
            navigator = navigator,
            dockInset = 0.dp,
            modifier = modifier,
        )
    }
}

@Composable
internal fun TmrMainRoot(
    modifier: Modifier = Modifier,
    initialKeys: () -> List<NavKey> = PendingNavigation::consume,
) {
    WithRootViewModelStore(NavigationRoot.Main) {
        TmrMainRootContent(modifier = modifier, initialKeys = initialKeys)
    }
}

@Composable
private fun TmrMainRootContent(
    modifier: Modifier,
    initialKeys: () -> List<NavKey>,
) {
    val navigationState = rememberNavigationState(START_NAV_KEY, TOP_LEVEL_NAV_ITEMS.keys)
    val navigator = remember(navigationState) {
        Navigator(navigationState).also { navigator -> initialKeys().forEach(navigator::openInOwnTab) }
    }
    Box(modifier = modifier.fillMaxSize()) {
        TmrNavDisplay(
            navigationState = navigationState,
            navigator = navigator,
            dockInset = TmrDockDefaults.inset,
        )
        AnimatedVisibility(
            visible = navigationState.currentKey.isTopLevelDestination(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(TmrTheme.motion.proofSpecs.offset) { it } +
                fadeIn(TmrTheme.motion.proofSpecs.fade),
            exit = slideOutVertically(TmrTheme.motion.proofSpecs.offset) { it } +
                fadeOut(TmrTheme.motion.proofSpecs.fade),
        ) {
            TmrMainDock(navigationState = navigationState, navigator = navigator)
        }
    }
}

@Composable
private fun TmrMainDock(
    navigationState: NavigationState,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    TmrDock(modifier = modifier) {
        TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
            val selected = navKey == navigationState.currentTopLevelKey
            TmrDockItem(
                selected = selected,
                onClick = { navigator.navigate(navKey) },
                label = stringResource(navItem.labelRes),
                icon = { TmrDockIcon(if (selected) navItem.selectedIcon else navItem.unselectedIcon) },
            )
        }
    }
}

@Composable
private fun TmrNavDisplay(
    navigationState: NavigationState,
    navigator: Navigator,
    dockInset: Dp,
    modifier: Modifier = Modifier,
) {
    val entryProvider = remember(navigator, dockInset) {
        withDockInset(sharedEntryProvider(navigator), dockInset)
    }
    val transitions = rememberTmrNavTransitions()
    Box(modifier = modifier) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
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

private fun withDockInset(
    provider: (NavKey) -> NavEntry<NavKey>,
    dockInset: Dp,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val entry = provider(key)
    val topLevel = key.isTopLevelDestination()
    val tabIndex = TOP_LEVEL_NAV_ITEMS.keys.indexOf(key)
    val metadata = entry.metadata + (TOP_LEVEL_METADATA to topLevel) +
        if (tabIndex >= 0) mapOf(TAB_INDEX_METADATA to tabIndex) else emptyMap()
    NavEntry(key = key, contentKey = entry.contentKey, metadata = metadata) {
        CompositionLocalProvider(LocalTmrBottomInset provides if (topLevel) dockInset else 0.dp) { entry.Content() }
    }
}
