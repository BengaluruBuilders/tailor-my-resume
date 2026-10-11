package com.tailormyresume.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

private class ScriptedRemote(var owner: String? = "uid-1") : RemoteLedgerSource {
    override val ownsLedger = true
    var snapshot = CreditSnapshot(0, emptyList())
    var failure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null
    var fetches = 0

    override fun owner() = owner

    override suspend fun fetch(): CreditSnapshot {
        fetches++
        gate?.await()
        failure?.let { throw it }
        return snapshot
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OfflineFirstCreditsRepositoryRemoteTest {
    private lateinit var database: TmrDatabase
    private val remote = ScriptedRemote()

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun closeDatabase() = database.close()

    private fun repository(source: RemoteLedgerSource = remote) =
        OfflineFirstCreditsRepository(database.creditLedgerDao(), UnconfinedTestDispatcher(), CoroutineScope(UnconfinedTestDispatcher()), source)

    private fun entry(kind: CreditLedgerKind, amount: Int, day: Int) =
        CreditLedgerEntry(kind, amount, null, null, Instant.parse("2026-10-%02dT09:00:00Z".format(day)))

    @Test
    fun balanceIsWalletCreditsNotRowSum() = runTest {
        remote.snapshot = CreditSnapshot(7, listOf(entry(CreditLedgerKind.PURCHASE, 5, 3), entry(CreditLedgerKind.FREE_GRANT, 1, 1)))
        val repository = repository()

        repository.refresh()

        assertThat(repository.observeBalance().first()).isEqualTo(7)
        assertThat(repository.observeLedger().first().map { it.amount }).containsExactly(5, 1).inOrder()
    }

    @Test
    fun remoteSpendNotDoubleCountedAndDemoStillRecords() = runTest {
        val first = listOf(entry(CreditLedgerKind.PURCHASE, 5, 3), entry(CreditLedgerKind.FREE_GRANT, 1, 1))
        remote.snapshot = CreditSnapshot(6, first)
        val repository = repository()
        repository.refresh()

        repository.record(entry(CreditLedgerKind.SPEND, -1, 4))
        repository.record(entry(CreditLedgerKind.PURCHASE, 40, 4))
        assertThat(repository.observeBalance().first()).isEqualTo(6)
        assertThat(repository.observeLedger().first()).hasSize(2)

        remote.snapshot = CreditSnapshot(5, listOf(entry(CreditLedgerKind.SPEND, -1, 4)) + first)
        repository.refresh()
        assertThat(repository.observeBalance().first()).isEqualTo(5)
        assertThat(repository.observeLedger().first()).hasSize(3)

        val demo = repository(NoRemoteLedger())
        demo.record(entry(CreditLedgerKind.FREE_GRANT, 1, 1))
        demo.record(entry(CreditLedgerKind.SPEND, -1, 2))
        assertThat(demo.observeBalance().first()).isEqualTo(0)
        assertThat(demo.observeLedger().first().map { it.kind })
            .containsExactly(CreditLedgerKind.SPEND, CreditLedgerKind.FREE_GRANT).inOrder()
    }

    @Test
    fun refreshFailureKeepsValuesAndAccountSwitchDropsLateAnswer() = runTest {
        remote.snapshot = CreditSnapshot(7, listOf(entry(CreditLedgerKind.PURCHASE, 7, 3)))
        val repository = repository()
        repository.refresh()

        remote.failure = java.io.IOException("offline")
        repository.refresh()
        assertThat(repository.observeBalance().first()).isEqualTo(7)

        remote.failure = null
        remote.gate = CompletableDeferred()
        val late = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }
        remote.owner = "uid-2"
        remote.gate?.complete(Unit)
        late.join()
        assertThat(repository.observeBalance().first()).isEqualTo(0)
        assertThat(repository.observeLedger().first()).isEmpty()

        remote.owner = "uid-1"
        repository.refresh()
        repository.clear()
        assertThat(repository.observeBalance().first()).isEqualTo(0)
        assertThat(database.creditLedgerDao().observeLedger().first()).isEmpty()
    }

    @Test
    fun clearStillWipesLocallyRecordedRows() = runTest {
        val demo = repository(NoRemoteLedger())
        demo.record(entry(CreditLedgerKind.FREE_GRANT, 1, 1))

        demo.clear()

        assertThat(demo.observeLedger().first()).isEmpty()
    }

    @Test
    fun cancelledRefreshIsRetriedWhenNextObserved() = runTest {
        remote.snapshot = CreditSnapshot(3, emptyList())
        val repository = repository()
        remote.gate = CompletableDeferred()
        val cancelled = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }
        cancelled.cancel()
        cancelled.join()
        remote.gate = null

        assertThat(repository.observeBalance().first()).isEqualTo(3)
        assertThat(remote.fetches).isEqualTo(2)
    }

    @Test
    fun liveCollectorDropsPreviousAccountWhenNextRefreshFails() = runTest {
        remote.snapshot = CreditSnapshot(7, listOf(entry(CreditLedgerKind.PURCHASE, 7, 3)))
        val repository = repository()
        val seen = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeBalance().toList(seen) }
        repository.refresh()
        assertThat(seen.last()).isEqualTo(7)

        remote.owner = "uid-2"
        remote.failure = java.io.IOException("offline")
        repository.refresh()

        assertThat(seen.last()).isEqualTo(0)
    }

    @Test
    fun answerAfterClearIsDroppedEvenForTheSameOwner() = runTest {
        remote.snapshot = CreditSnapshot(7, emptyList())
        val repository = repository()
        remote.gate = CompletableDeferred()
        val late = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }

        repository.clear()
        remote.gate?.complete(Unit)
        late.join()

        assertThat(repository.observeBalance().first()).isEqualTo(0)
    }

    @Test
    fun answerForAnotherOwnerStaysDroppedWhenTheOwnerComesBack() = runTest {
        remote.snapshot = CreditSnapshot(7, emptyList())
        val repository = repository()
        remote.gate = CompletableDeferred()
        val late = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }

        remote.owner = "uid-2"
        remote.gate?.complete(Unit)
        late.join()
        remote.owner = "uid-1"

        assertThat(repository.observeBalance().first()).isEqualTo(0)
    }

    @Test
    fun recordingInProdRefreshesFromTheServer() = runTest {
        remote.snapshot = CreditSnapshot(6, emptyList())
        val repository = repository()
        repository.refresh()

        remote.snapshot = CreditSnapshot(5, listOf(entry(CreditLedgerKind.SPEND, -1, 4)))
        repository.record(entry(CreditLedgerKind.SPEND, -1, 4))

        assertThat(repository.observeBalance().first()).isEqualTo(5)
        assertThat(repository.observeLedger().first()).hasSize(1)
    }

    @Test
    fun recordReturnsBeforeASlowRefreshAndTheBalanceUpdatesWhenItFinishes() = runTest {
        remote.snapshot = CreditSnapshot(6, emptyList())
        val repository = repository()
        repository.refresh()
        remote.snapshot = CreditSnapshot(5, listOf(entry(CreditLedgerKind.SPEND, -1, 4)))
        remote.gate = CompletableDeferred()

        repository.record(entry(CreditLedgerKind.SPEND, -1, 4))
        assertThat(repository.observeBalance().first()).isEqualTo(6)

        remote.gate?.complete(Unit)
        assertThat(repository.observeBalance().first()).isEqualTo(5)
    }

    @Test
    fun anOlderRefreshFinishingLaterIsDropped() = runTest {
        val older = CompletableDeferred<Unit>()
        val newer = CompletableDeferred<Unit>()
        val answers = ArrayDeque(listOf(older to CreditSnapshot(9, emptyList()), newer to CreditSnapshot(4, emptyList())))
        val repository = repository(object : RemoteLedgerSource {
            override val ownsLedger = true
            override fun owner() = "uid-1"
            override suspend fun fetch(): CreditSnapshot {
                val (gate, snapshot) = answers.removeFirst()
                gate.await()
                return snapshot
            }
        })
        val first = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }
        val second = launch(UnconfinedTestDispatcher(testScheduler)) { repository.refresh() }

        newer.complete(Unit)
        second.join()
        older.complete(Unit)
        first.join()

        assertThat(repository.observeBalance().first()).isEqualTo(4)
    }
}
