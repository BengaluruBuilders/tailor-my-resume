package com.tailormyresume.feature.settings.impl.settings

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.settings.impl.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before
    @After
    fun clearQueuedToast() {
        PendingToast.consume()
    }

    private suspend fun ReceiveTurbine<SettingsUiState>.awaitContent(): SettingsUiState.Content {
        while (true) {
            val item = awaitItem()
            if (item is SettingsUiState.Content) return item
        }
    }

    @Test
    fun pageSizeToggles() = runTest {
        val fixture = SettingsFixture()
        val viewModel = fixture.viewModel()

        viewModel.uiState.test {
            assertThat(awaitContent().pageSize).isEqualTo(PageSize.A4)
            viewModel.onPageSizeClicked()
            assertThat(awaitContent().pageSize).isEqualTo(PageSize.LETTER)
            viewModel.onPageSizeClicked()
            assertThat(awaitContent().pageSize).isEqualTo(PageSize.A4)
        }
    }

    @Test
    fun fileNameCycles() = runTest {
        val fixture = SettingsFixture()
        val viewModel = fixture.viewModel()

        viewModel.uiState.test {
            assertThat(awaitContent().fileNameFormat).isEqualTo(FileNameFormat.NAME_COMPANY_ROLE)
            viewModel.onFileNameClicked()
            assertThat(awaitContent().fileNameFormat).isEqualTo(FileNameFormat.NAME_ROLE)
            viewModel.onFileNameClicked()
            assertThat(awaitContent().fileNameFormat).isEqualTo(FileNameFormat.NAME_RESUME)
            viewModel.onFileNameClicked()
            assertThat(awaitContent().fileNameFormat).isEqualTo(FileNameFormat.NAME_COMPANY_ROLE)
        }
    }

    @Test
    fun productUpdatesFlips() = runTest {
        val fixture = SettingsFixture()
        val viewModel = fixture.viewModel()

        viewModel.uiState.test {
            assertThat(awaitContent().productUpdates).isFalse()
            viewModel.onProductUpdatesToggled()
            assertThat(awaitContent().productUpdates).isTrue()
            viewModel.onProductUpdatesToggled()
            assertThat(awaitContent().productUpdates).isFalse()
        }
    }

    @Test
    fun stateFollowsRepository() = runTest {
        val fixture = SettingsFixture()
        fixture.session.saveAccount(settingsTestAccount)
        fixture.settings.update { it.copy(pageSize = PageSize.LETTER, productUpdates = true) }
        val viewModel = fixture.viewModel()

        viewModel.uiState.test {
            val content = awaitContent()
            assertThat(content.email).isEqualTo(settingsTestAccount.email)
            assertThat(content.pageSize).isEqualTo(PageSize.LETTER)
            assertThat(content.productUpdates).isTrue()
            assertThat(content.credits).isEqualTo(0)

            fixture.credits.record(
                CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 3, null, null, Instant.fromEpochSeconds(1_790_000_000)),
            )

            assertThat(awaitContent().credits).isEqualTo(3)
        }
    }

    @Test
    fun downloadEmitsShareOnce() = runTest {
        val exporter = TestAccountDataExporter()
        val viewModel = SettingsFixture(exporter).viewModel()

        viewModel.events.test {
            viewModel.onDownloadMyData()

            val event = awaitItem() as SettingsEvent.ShareArchive
            assertThat(event.file.exists()).isTrue()
            expectNoEvents()
        }
        assertThat(exporter.exported).hasSize(1)
    }

    @Test
    fun downloadIgnoredWhileRunning() = runTest {
        val exporter = GatedExporter()
        val viewModel = SettingsFixture(exporter).viewModel()

        viewModel.events.test {
            viewModel.onDownloadMyData()
            viewModel.onDownloadMyData()
            exporter.gate.complete(Unit)

            assertThat(awaitItem()).isInstanceOf(SettingsEvent.ShareArchive::class.java)
            expectNoEvents()
            viewModel.onDownloadMyData()
            assertThat(awaitItem()).isInstanceOf(SettingsEvent.ShareArchive::class.java)
        }
        assertThat(exporter.calls).isEqualTo(2)
    }

    @Test
    fun downloadFailureEmitsFailure() = runTest {
        val exporter = FailingExporter()
        val viewModel = SettingsFixture(exporter).viewModel()

        viewModel.events.test {
            viewModel.onDownloadMyData()
            assertThat(awaitItem()).isEqualTo(SettingsEvent.DownloadFailed)

            viewModel.onDownloadMyData()
            assertThat(awaitItem()).isEqualTo(SettingsEvent.DownloadFailed)
        }
        assertThat(exporter.calls).isEqualTo(2)
    }

    @Test
    fun signOutCallsGatewayOnceAndQueuesToast() = runTest {
        val fixture = SettingsFixture()
        val viewModel = fixture.viewModel()

        viewModel.onSignOut()
        viewModel.onSignOut()

        assertThat(fixture.signIn.signOutCalls).isEqualTo(1)
        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_signed_out)
    }

    @Test
    fun signOutSurvivesTheScopeBeingCancelledByTheReRoot() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = SettingsFixture(gate = gate)
        val viewModel = fixture.viewModel()

        viewModel.onSignOut()
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)

        assertThat(fixture.signIn.signOutCompleted).isTrue()
        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_signed_out)
    }
}
