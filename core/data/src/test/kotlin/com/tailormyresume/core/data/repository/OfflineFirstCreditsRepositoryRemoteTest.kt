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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    val fetchedOwners = mutableListOf<String?>()

    override fun owner() = owner

    override suspend fun fetch(): CreditSnapshot {
        fetches++
        fetchedOwners += owner
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
    private val ownedScopes = mutableListOf<CoroutineScope>()

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun closeDatabase() {
        runBlocking { ownedScopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() } }
        database.close()
    }

    private fun repository(source: RemoteLedgerSource = remote) =
        OfflineFirstCreditsRepository(database.creditLedgerDao(), UnconfinedTestDispatcher(), CoroutineScope(UnconfinedTestDispatcher()).also(ownedScopes::add), source)

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
    fun unknownRemoteBalanceStaysUnavailableUntilARealSnapshotArrives() = runTest {
        val repository = repository()
        assertThat(repository.observeKnownBalance().first()).isNull()

        remote.failure = java.io.IOException("offline")
        repository.refresh()
        assertThat(repository.observeKnownBalance().first()).isNull()

        remote.failure = null
        remote.snapshot = CreditSnapshot(0, emptyList())
        repository.refresh()
        assertThat(repository.observeKnownBalance().first()).isEqualTo(0)
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
        remote.failure = java.io.IOException("uid-2 offline")
        assertThat(repository.observeKnownBalance().first()).isNull()
        assertThat(repository.observeBalance().first()).isEqualTo(0)
        assertThat(repository.observeLedger().first()).isEmpty()
        assertThat(remote.fetchedOwners.last()).isEqualTo("uid-2")

        remote.failure = null
        remote.snapshot = CreditSnapshot(4, listOf(entry(CreditLedgerKind.PURCHASE, 4, 4)))
        assertThat(repository.observeKnownBalance().first()).isEqualTo(4)

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
        assertThat(repository.observeKnownBalance().first()).isNull()
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
        assertThat(repository.observeKnownBalance().first()).isNull()

        remote.gate?.complete(Unit)
        assertThat(repository.observeKnownBalance().first()).isEqualTo(5)
    }

    @Test
    fun failedRefreshAfterRemoteSpendDoesNotPresentTheOldBalanceAsCurrent() = runTest {
        remote.snapshot = CreditSnapshot(1, emptyList())
        val repository = repository()
        repository.refresh()
        remote.failure = java.io.IOException("offline")

        repository.record(entry(CreditLedgerKind.SPEND, -1, 4))

        assertThat(repository.observeKnownBalance().first()).isNull()
        assertThat(repository.observeLedger().first()).isEmpty()

        remote.failure = null
        remote.snapshot = CreditSnapshot(0, listOf(entry(CreditLedgerKind.SPEND, -1, 4)))
        assertThat(repository.observeKnownBalance().first()).isEqualTo(0)
        assertThat(repository.observeLedger().first()).hasSize(1)
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

    @Test
    fun refreshConfirmedIsFalseWhenTheFetchFails() = runTest {
        remote.snapshot = CreditSnapshot(1, emptyList())
        val repository = repository()
        repository.refresh()
        remote.failure = java.io.IOException("offline")

        assertThat(repository.refreshConfirmed()).isFalse()
    }

    @Test
    fun refreshConfirmedIsFalseWhenANewerRefreshSupersedesItsFetch() = runTest {
        val seedGate = CompletableDeferred(Unit)
        val staleGate = CompletableDeferred<Unit>()
        val newerGate = CompletableDeferred<Unit>()
        val answers = ArrayDeque(
            listOf(seedGate to CreditSnapshot(1, emptyList()), staleGate to CreditSnapshot(6, emptyList()), newerGate to CreditSnapshot(6, emptyList())),
        )
        val gated = repository(object : RemoteLedgerSource {
            override val ownsLedger = true
            override fun owner() = "uid-1"
            override suspend fun fetch(): CreditSnapshot {
                val (gate, snapshot) = answers.removeFirst()
                gate.await()
                return snapshot
            }
        })
        gated.refresh()
        var confirmed: Boolean? = null
        val superseded = launch(UnconfinedTestDispatcher(testScheduler)) { confirmed = gated.refreshConfirmed() }
        val newer = launch(UnconfinedTestDispatcher(testScheduler)) { gated.refresh() }

        staleGate.complete(Unit)
        superseded.join()

        assertThat(confirmed).isFalse()
        assertThat(gated.observeBalance().first()).isEqualTo(1)
        newerGate.complete(Unit)
        newer.join()
        assertThat(gated.observeBalance().first()).isEqualTo(6)
    }

    @Test
    fun refreshConfirmedIsTrueAndStoresTheBalanceWhenTheFetchSucceeds() = runTest {
        remote.snapshot = CreditSnapshot(7, emptyList())
        val repository = repository()

        assertThat(repository.refreshConfirmed()).isTrue()
        assertThat(repository.observeBalance().first()).isEqualTo(7)
    }

    @Test
    fun refreshConfirmedIsTrueWithoutFetchingInDemo() = runTest {
        assertThat(repository(NoRemoteLedger()).refreshConfirmed()).isTrue()
    }

    @Test fun persistedHistoryKeepsStableIdsScopeAndServerBalancesWithoutDeletingEconomicRows() = runTest {
        val ledger = database.creditLedgerDao()
        ledger.insert(com.tailormyresume.core.database.model.CreditLedgerEntity(kind = CreditLedgerKind.FREE_GRANT, amount = 2, applicationId = null, productId = null, createdAt = Instant.fromEpochMilliseconds(0)))
        val reward = entry(CreditLedgerKind.REFERRAL_REWARD, 13, 3).copy(id = "reward-a")
        val second = reward.copy(id = "reward-b", kind = CreditLedgerKind.SPEND, amount = -1, applicationId = "app-a")
        remote.snapshot = CreditSnapshot(7, listOf(reward, second))
        val first = repository()
        first.refresh()
        assertThat(ledger.ownedEntries("uid-1", "").map { it.serverId }).containsExactly("reward-a", "reward-b")
        assertThat(ledger.observeLedger().first().filter { it.owner == null }).hasSize(1)
        val restored = repository()
        assertThat(restored.observeLedger().first().map { it.id }).containsExactly("reward-a", "reward-b")
        assertThat(restored.observeKnownBalance().first()).isNull()
        first.removeForApplication("app-a")
        assertThat(ledger.ownedEntries("uid-1", "")).hasSize(2)
        remote.owner = "uid-2"
        assertThat(repository().observeLedger().first()).isEmpty()
        assertThat(ledger.ownedEntries("uid-1", "")).hasSize(2)
        restored.clear()
        assertThat(ledger.ownedEntries("uid-1", "")).isEmpty()
    }
}
