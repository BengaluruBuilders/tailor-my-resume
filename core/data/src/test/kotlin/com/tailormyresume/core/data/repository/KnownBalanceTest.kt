package com.tailormyresume.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.time.Instant

private class ServerLedger : RemoteLedgerSource {
    override val ownsLedger = true
    var owner: String? = "uid-1"
    var snapshot = CreditSnapshot(0, emptyList())
    var failure: Exception? = null

    override fun owner() = owner

    override suspend fun fetch(): CreditSnapshot {
        failure?.let { throw it }
        return snapshot
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class KnownBalanceTest {
    private lateinit var database: TmrDatabase
    private val server = ServerLedger()

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun closeDatabase() = database.close()

    private fun repository(source: RemoteLedgerSource = server) =
        OfflineFirstCreditsRepository(database.creditLedgerDao(), UnconfinedTestDispatcher(), CoroutineScope(UnconfinedTestDispatcher()), source)

    private fun grant() = CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, Instant.parse("2026-10-01T09:00:00Z"))

    @Test
    fun aServerBalanceIsUnknownNotZeroUntilASnapshotArrives() = runTest {
        val repository = repository()

        assertThat(repository.observeKnownBalance().first()).isNull()

        server.failure = IOException("offline")
        repository.refresh()
        assertThat(repository.observeKnownBalance().first()).isNull()
    }

    @Test
    fun aServerBalanceOfZeroIsKnown() = runTest {
        val repository = repository()

        repository.refresh()

        assertThat(repository.observeKnownBalance().first()).isEqualTo(0)
    }

    @Test
    fun aFailedRefreshKeepsTheLastKnownBalance() = runTest {
        server.snapshot = CreditSnapshot(3, listOf(grant()))
        val repository = repository()
        repository.refresh()

        server.failure = IOException("offline")
        repository.refresh()

        assertThat(repository.observeKnownBalance().first()).isEqualTo(3)
    }

    @Test
    fun anotherAccountStartsUnknown() = runTest {
        server.snapshot = CreditSnapshot(3, listOf(grant()))
        val repository = repository()
        repository.refresh()

        server.owner = "uid-2"

        assertThat(repository.observeKnownBalance().first()).isNull()
    }

    @Test
    fun theLocalLedgerBalanceIsAlwaysKnown() = runTest {
        val repository = repository(NoRemoteLedger())
        assertThat(repository.observeKnownBalance().first()).isEqualTo(0)

        repository.record(grant())

        assertThat(repository.observeKnownBalance().first()).isEqualTo(1)
    }
}
