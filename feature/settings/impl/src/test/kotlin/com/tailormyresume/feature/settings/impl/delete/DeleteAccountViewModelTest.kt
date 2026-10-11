package com.tailormyresume.feature.settings.impl.delete

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.navigation.PendingToast
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.settings.impl.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class DeleteAccountViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before
    @After
    fun clearQueuedToast() {
        PendingToast.consume()
    }

    private fun TestScope.keepSubscribed(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private fun DeleteAccountViewModel.content(): DeleteAccountUiState.Content =
        uiState.value as DeleteAccountUiState.Content

    @Test
    fun sheetShowsCounts_pluralised() = runTest {
        val several = DeleteFixture(applications = 3, credits = 2).seed().viewModel()
        val single = DeleteFixture(applications = 1, credits = 1).seed().viewModel()
        val none = DeleteFixture(applications = 0, credits = 0).seed().viewModel()
        listOf(several, single, none).forEach { keepSubscribed(it.uiState) }

        assertThat(several.content().applications).isEqualTo(3)
        assertThat(several.content().unusedCredits).isEqualTo(2)
        assertThat(single.content().applications).isEqualTo(1)
        assertThat(single.content().unusedCredits).isEqualTo(1)
        assertThat(none.content().applications).isEqualTo(0)
        assertThat(none.content().unusedCredits).isEqualTo(0)
    }

    @Test
    fun deleteRefusedUntilDelete() = runTest {
        val fixture = DeleteFixture().seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.onTextChanged("DELET")
        assertThat(viewModel.content().canDelete).isFalse()
        viewModel.onDelete()

        assertThat(fixture.deleter.deleteCalls).isEqualTo(0)
        assertThat(fixture.remainingApplications()).isEqualTo(3)
        assertThat(PendingToast.queued).isNull()

        viewModel.onTextChanged(" delete ")
        assertThat(viewModel.content().canDelete).isTrue()
    }

    @Test
    fun deleteCallsUseCaseOnce() = runTest {
        val fixture = DeleteFixture().seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.onTextChanged("DELETE")
        viewModel.onDelete()
        viewModel.onDelete()

        assertThat(fixture.deleter.deleteCalls).isEqualTo(1)
        assertThat(fixture.exportedFilesCleared).isEqualTo(1)
        assertThat(fixture.remainingApplications()).isEqualTo(0)
    }

    @Test
    fun deletedQueuesToast() = runTest {
        val viewModel = DeleteFixture().seed().viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.onTextChanged("delete")
        viewModel.onDelete()

        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_account_deleted)
    }

    @Test
    fun deleteSingleFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(gate = gate)).seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.onTextChanged("DELETE")
        viewModel.onDelete()
        viewModel.onDelete()

        assertThat(fixture.deleter.deleteCalls).isEqualTo(1)
        assertThat(viewModel.content().deleting).isTrue()
        gate.complete(Unit)
        assertThat(fixture.deleter.deleteCalls).isEqualTo(1)
        assertThat(fixture.remainingApplications()).isEqualTo(0)
    }

    @Test
    fun deleteSurvivesTheScopeBeingCancelledByTheReRoot() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(gate = gate)).seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.onTextChanged("DELETE")
        viewModel.onDelete()
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)

        assertThat(fixture.remainingApplications()).isEqualTo(0)
        assertThat(fixture.exportedFilesCleared).isEqualTo(1)
        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_account_deleted)
    }

    @Test
    fun failedKeepsSheetAndText() = runTest {
        val fixture = DeleteFixture(deleter = RecordingServerAccountDeleter(failure = IOException("offline"))).seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.events.test {
            viewModel.onTextChanged("DELETE")
            viewModel.onDelete()

            assertThat(awaitItem()).isEqualTo(DeleteAccountEvent.DeleteFailed)
            expectNoEvents()
        }

        assertThat(viewModel.content().typed).isEqualTo("DELETE")
        assertThat(viewModel.content().deleting).isFalse()
        assertThat(viewModel.content().canDelete).isTrue()
        assertThat(fixture.remainingApplications()).isEqualTo(3)
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun localWipePendingKeepsSheet() = runTest {
        val deleter = RecordingServerAccountDeleter(failure = IOException("reply lost"), deletesRemoteData = true)
        val fixture = DeleteFixture(deleter = deleter).seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)

        viewModel.events.test {
            viewModel.onTextChanged("DELETE")
            viewModel.onDelete()

            assertThat(awaitItem()).isEqualTo(DeleteAccountEvent.DeleteFailed)
        }

        assertThat(viewModel.content().typed).isEqualTo("DELETE")
        assertThat(viewModel.content().deleting).isFalse()
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun retryAfterPartialDeleteAdoptsTheRequestedMarker() = runTest {
        val deleter = RecordingServerAccountDeleter(failure = IOException("reply lost"), deletesRemoteData = true)
        val fixture = DeleteFixture(deleter = deleter).seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)
        viewModel.onTextChanged("DELETE")
        viewModel.onDelete()

        deleter.failure = null
        viewModel.onDelete()

        assertThat(deleter.deleteCalls).isEqualTo(2)
        assertThat(fixture.pendingWipe.history.count { it == PendingWipeState.REQUESTED })
            .isEqualTo(1)
        assertThat(fixture.remainingApplications()).isEqualTo(0)
        assertThat(PendingToast.consume()).isEqualTo(R.string.feature_settings_impl_toast_account_deleted)
    }

    @Test
    fun closeClearsTextAndDoesNotDelete() = runTest {
        val fixture = DeleteFixture().seed()
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)
        viewModel.onTextChanged("DELETE")

        viewModel.onSheetClosed()

        assertThat(viewModel.content().typed).isEmpty()
        assertThat(viewModel.content().canDelete).isFalse()
        assertThat(fixture.deleter.deleteCalls).isEqualTo(0)
        assertThat(fixture.remainingApplications()).isEqualTo(3)
    }

    @Test
    fun aFailureAfterTheSheetClosedIsNotReplayedWhenTheSheetOpensAgain() = runTest {
        val gate = CompletableDeferred<Unit>()
        val deleter = RecordingServerAccountDeleter(failure = IOException("offline"), gate = gate)
        val viewModel = DeleteFixture(deleter = deleter).seed().viewModel()
        keepSubscribed(viewModel.uiState)
        viewModel.onTextChanged("DELETE")
        viewModel.onDelete()
        viewModel.onSheetClosed()
        gate.complete(Unit)

        viewModel.onSheetOpened()

        viewModel.events.test { expectNoEvents() }
    }

    @Test
    fun retryAfterTheServerClosedTheAccountKeepsTheServerClosedMarker() = runTest {
        val deleter = RecordingServerAccountDeleter(failure = IOException("offline"), deletesRemoteData = true)
        val fixture = DeleteFixture(deleter = deleter).seed().signedInWithServerClosedMarker("uid-a")
        val viewModel = fixture.viewModel()
        keepSubscribed(viewModel.uiState)
        viewModel.onTextChanged("DELETE")

        viewModel.onDelete()

        assertThat(fixture.pendingWipe.history).doesNotContain(PendingWipeState.REQUESTED)
        assertThat(deleter.deleteCalls).isEqualTo(0)
    }
}
