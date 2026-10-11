package com.tailormyresume.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.navigation.registerComposeActivity
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.onboarding.StartDestination
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.DefaultSignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import com.tailormyresume.feature.settings.impl.R as SettingsR

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrAppRootTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var ready by mutableStateOf<AppRootState.Ready>(AppRootState.Ready(StartDestination.SignIn, null))
    private var navigator: Navigator? = null

    private fun show() {
        rule.setContent {
            TmrTheme {
                TmrAccountRoot(
                    ready = ready,
                    entryProvider = { shellNavigator ->
                        navigator = shellNavigator
                        ({ key: NavKey -> NavEntry(key) {} })
                    },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun startKeys(): List<NavKey> = rule.runOnIdle { navigator?.state?.stack?.toList().orEmpty() }

    @Test
    fun startsAtTheKeyOfTheStartDestination() {
        ready = AppRootState.Ready(StartDestination.Upload, "account-1")
        show()

        assertThat(startKeys()).containsExactly(UploadNavKey())
    }

    @Test
    fun accountChange_resetsBackStackToNewStart() {
        ready = AppRootState.Ready(StartDestination.Applications, "account-1")
        show()
        rule.runOnIdle { navigator?.navigate(DefaultProfileNavKey) }
        rule.waitForIdle()
        assertThat(startKeys()).containsExactly(DefaultApplicationsNavKey, DefaultProfileNavKey).inOrder()

        ready = AppRootState.Ready(StartDestination.SignIn, null)
        rule.waitForIdle()

        assertThat(startKeys()).containsExactly(DefaultSignInNavKey)
    }

    private class ClearedProbe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private fun showRoot(startKey: NavKey, pending: List<NavKey>, account: String?) {
        rule.setContent {
            TmrTheme {
                TmrRoot(
                    root = AccountRoot(account),
                    startKey = startKey,
                    initialKeys = { pending },
                    entryProvider = { shellNavigator ->
                        navigator = shellNavigator
                        ({ key: NavKey -> NavEntry(key) {} })
                    },
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun pendingKeyAtTabStart_replacesTheStack() {
        showRoot(DefaultApplicationsNavKey, listOf(DefaultProfileNavKey), "account-1")

        assertThat(startKeys()).containsExactly(DefaultProfileNavKey)
    }

    @Test
    fun pendingKeyAtSignInStart_isDropped() {
        showRoot(DefaultSignInNavKey, listOf(DefaultProfileNavKey), null)

        assertThat(startKeys()).containsExactly(DefaultSignInNavKey)
    }

    @Test
    fun pendingKeyAtUploadStart_isDropped() {
        showRoot(UploadNavKey(), listOf(DefaultProfileNavKey), "account-1")

        assertThat(startKeys()).containsExactly(UploadNavKey())
    }

    @Test
    fun accountChange_clearsThePreviousAccountsViewModels() {
        var probe: ClearedProbe? = null
        var current by mutableStateOf<AppRootState>(AppRootState.Ready(StartDestination.Applications, "account-1"))
        rule.setContent {
            TmrTheme {
                TmrApp(
                    rootState = current,
                    entryProvider = { _ ->
                        (
                            { key: NavKey ->
                                NavEntry(key) {
                                    val created = viewModel<ClearedProbe>(key = "probe-$key") { ClearedProbe() }
                                    if (probe == null) probe = created
                                }
                            }
                            )
                    },
                )
            }
        }
        rule.waitForIdle()
        val accountOneProbe = checkNotNull(probe)
        assertThat(accountOneProbe.cleared).isFalse()

        current = AppRootState.Ready(StartDestination.SignIn, null)
        rule.waitForIdle()

        assertThat(accountOneProbe.cleared).isTrue()
    }

    private fun reRootAfterAccountLeaves(queue: () -> Unit) {
        ready = AppRootState.Ready(StartDestination.Applications, "account-1")
        show()
        queue()
        ready = AppRootState.Ready(StartDestination.SignIn, null)
        rule.waitForIdle()
    }

    @Test
    fun signedOutToastShowsInNewRoot() {
        PendingToast.consume()

        reRootAfterAccountLeaves {}
        PendingToast.set(SettingsR.string.feature_settings_impl_toast_signed_out)
        rule.waitForIdle()

        rule.onNodeWithText("Signed out").assertIsDisplayed()
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun accountDeletedToastShowsInNewRoot() {
        PendingToast.consume()

        reRootAfterAccountLeaves { PendingToast.set(SettingsR.string.feature_settings_impl_toast_account_deleted) }

        rule.onNodeWithText("Account deleted").assertIsDisplayed()
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun toastQueuedWhileTheOutgoingRootIsLiveSurvivesToTheNewRoot() {
        PendingToast.consume()

        reRootAfterAccountLeaves {
            PendingToast.set(SettingsR.string.feature_settings_impl_toast_signed_out)
            rule.waitForIdle()
            assertThat(PendingToast.queued).isNotNull()
        }

        rule.onNodeWithText("Signed out").assertIsDisplayed()
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun noQueuedToastShowsNothing() {
        PendingToast.consume()

        reRootAfterAccountLeaves {}

        rule.onNodeWithText("Signed out").assertDoesNotExist()
        rule.onNodeWithText("Account deleted").assertDoesNotExist()
    }
}
