package com.tailormyresume.feature.settings.impl.navigation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.IntentCompat
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHost
import com.tailormyresume.core.designsystem.component.chrome.TmrSheetHostState
import com.tailormyresume.core.designsystem.component.chrome.TmrToastHost
import com.tailormyresume.core.designsystem.component.chrome.TmrToastState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataArchive
import com.tailormyresume.core.domain.account.AccountDataArchiveWriter
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.feature.settings.api.navigation.CreditsNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import com.tailormyresume.feature.settings.impl.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class SettingsEntryHostActivity : ComponentActivity()

class RecordingSignInGateway(
    private val delegate: TestSignInGateway = TestSignInGateway(),
) : SignInGateway by delegate {
    var signOutCalls = 0

    override suspend fun signOut() {
        signOutCalls += 1
        delegate.signOut()
    }
}

class CacheDirAccountDataExporter(private val context: Context) : AccountDataExporter {
    override suspend fun export(data: AccountData): AccountDataArchive {
        val file = File(File(context.cacheDir, "data-exports").apply { mkdirs() }, "my-data.zip")
        AccountDataArchiveWriter().write(data, file)
        return AccountDataArchive(fileName = file.name, file = file)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsAppGatewayFakes {
    @Provides
    @Singleton
    fun recordingSignInGateway(): RecordingSignInGateway = RecordingSignInGateway()

    @Provides
    fun signInGateway(recording: RecordingSignInGateway): SignInGateway = recording

    @Provides
    fun serverAccountDeleter(): ServerAccountDeleter = OfflineServerAccountDeleter()

    @Provides
    fun accountWipeFinisher(): AccountWipeFinisher = AccountWipeFinisher.None

    @Provides
    fun exportedFiles(): ExportedFiles = ExportedFiles.None

    @Provides
    @Singleton
    fun paymentGateway(): PaymentGateway = TestPaymentGateway()

    @Provides
    fun contentReportRepository(): ContentReportRepository = TestContentReportRepository()

    @Provides
    fun accountDataExporter(@ApplicationContext context: Context): AccountDataExporter =
        CacheDirAccountDataExporter(context)
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class SettingsRealEntryWiringTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<SettingsEntryHostActivity>()

    private val registerActivity = object : ExternalResource() {
        override fun before() {
            val application = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(application.packageManager)
                .addActivityIfNotPresent(ComponentName(application, SettingsEntryHostActivity::class.java))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(registerActivity).around(composeRule)

    @Inject
    lateinit var settings: ResumeSettingsRepository

    @Inject
    lateinit var signIn: RecordingSignInGateway

    private val state = NavigationState(NavBackStack<NavKey>(SettingsNavKey()))
    private val navigator = Navigator(state)
    private val provider = entryProvider { settingsEntry(navigator) }
    private val sheetHost = TmrSheetHostState()
    private val toast = TmrToastState()

    @Before
    fun setUp() {
        PendingToast.consume()
        hiltRule.inject()
        composeRule.setContent {
            TmrTheme {
                CompositionLocalProvider(LocalTmrSheetHost provides sheetHost, LocalTmrToast provides toast) {
                    provider(SettingsNavKey()).Content()
                    TmrSheetHost(sheetHost)
                    TmrToastHost(toast)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @After
    fun clearQueuedToast() {
        PendingToast.consume()
    }

    private fun tapRow(label: String) {
        waitFor { composeRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(label).performScrollTo().performClick()
    }

    private fun waitFor(condition: () -> Boolean) {
        composeRule.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    @Test
    fun pageSizeRow_flipsTheStoredSetting() {
        tapRow("Page size")

        waitFor { runBlocking { settings.observeSettings().first().pageSize } == PageSize.LETTER }
        composeRule.onNodeWithText("Letter").assertExists()
    }

    @Test
    fun signOutRow_callsTheGatewayAndQueuesTheToast() {
        tapRow("Sign out")

        waitFor { signIn.signOutCalls == 1 }
        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_signed_out)
    }

    @Test
    fun creditsRow_opensCredits() {
        tapRow("Credits & purchases")
        composeRule.waitForIdle()

        assertThat(state.stack.last()).isEqualTo(CreditsNavKey())
    }

    @Test
    fun deleteAccountRow_opensTheSheet() {
        tapRow("Delete account")

        waitFor { composeRule.onAllNodesWithText("Delete your account?").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Keep my account").assertExists()
    }

    @Test
    fun downloadMyDataRow_opensTheShareSheet() {
        tapRow("Download my data")

        val application = ApplicationProvider.getApplicationContext<Application>()
        waitFor { shadowOf(application).peekNextStartedActivity() != null }
        val chooser = checkNotNull(shadowOf(application).nextStartedActivity)
        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        val send = checkNotNull(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java))
        val stream = checkNotNull(IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send.type).isEqualTo("application/zip")
        assertThat(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
        assertThat(stream.authority).isEqualTo("${application.packageName}.settings.fileprovider")
        composeRule.onNodeWithText("Your data is ready to share").assertExists()
    }
}
