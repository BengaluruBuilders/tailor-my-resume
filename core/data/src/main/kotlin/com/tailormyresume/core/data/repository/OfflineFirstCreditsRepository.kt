package com.tailormyresume.core.data.repository

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.database.dao.CreditLedgerDao
import com.tailormyresume.core.database.model.CreditLedgerEntity
import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstCreditsRepository @Inject constructor(
    private val creditLedgerDao: CreditLedgerDao,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val remote: RemoteLedgerSource = NoRemoteLedger(),
) : CreditsRepository {
    private class Held(val owner: String?, val snapshot: CreditSnapshot)

    private val held = MutableStateFlow<Held?>(null)
    private val lock = Any()
    private var generation = 0

    @Volatile
    private var refreshCancelled = false

    private fun remoteSnapshots(): Flow<CreditSnapshot> =
        held.map { current -> current?.takeIf { it.owner == remote.owner() }?.snapshot ?: NO_SNAPSHOT }
            .onStart { if (refreshCancelled) refresh() }

    override fun observeBalance(): Flow<Int> =
        if (remote.ownsLedger) remoteSnapshots().map { it.balance } else creditLedgerDao.observeBalance().flowOn(ioDispatcher)

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> =
        if (remote.ownsLedger) {
            remoteSnapshots().map { it.entries }
        } else {
            creditLedgerDao.observeLedger()
                .map { entities -> entities.map(CreditLedgerEntity::asExternalModel) }
                .flowOn(ioDispatcher)
        }

    override suspend fun record(entry: CreditLedgerEntry) {
        if (remote.ownsLedger) scope.launch { refresh() } else creditLedgerDao.insert(entry.asEntity())
    }

    override suspend fun refresh() {
        if (!remote.ownsLedger) return
        val (started, owner) = synchronized(lock) {
            val current = remote.owner()
            held.update { it?.takeIf { held -> held.owner == current } }
            ++generation to current
        }
        val snapshot = try {
            remote.fetch()
        } catch (cancellation: CancellationException) {
            refreshCancelled = true
            throw cancellation
        } catch (failure: Exception) {
            return
        }
        synchronized(lock) {
            if (started == generation && owner == remote.owner()) {
                held.value = Held(owner, snapshot)
                refreshCancelled = false
            }
        }
    }

    override suspend fun clear() {
        synchronized(lock) {
            generation++
            held.value = null
        }
        creditLedgerDao.clear()
    }

    override suspend fun removeForApplication(applicationId: String) = creditLedgerDao.deleteForApplication(applicationId)

    private companion object {
        val NO_SNAPSHOT = CreditSnapshot(balance = 0, entries = emptyList())
    }
}

private fun CreditLedgerEntry.asEntity() = CreditLedgerEntity(
    kind = kind,
    amount = amount,
    applicationId = applicationId,
    productId = productId,
    createdAt = createdAt,
)

private fun CreditLedgerEntity.asExternalModel() = CreditLedgerEntry(
    kind = kind,
    amount = amount,
    applicationId = applicationId,
    productId = productId,
    createdAt = createdAt,
)
