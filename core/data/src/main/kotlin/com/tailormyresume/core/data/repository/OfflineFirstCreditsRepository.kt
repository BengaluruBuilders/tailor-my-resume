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
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstCreditsRepository @Inject constructor(
    private val creditLedgerDao: CreditLedgerDao,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
    private val remote: RemoteLedgerSource = NoRemoteLedger(),
) : CreditsRepository {
    private class Held(val owner: String?, val snapshot: CreditSnapshot, val environment: String)

    private val held = MutableStateFlow<Held?>(null)
    private val persistence = kotlinx.coroutines.sync.Mutex()
    private val cached = MutableStateFlow<Held?>(null)
    private val lock = Any()
    private var generation = 0

    @Volatile
    private var refreshNeeded = false

    private fun remoteSnapshots(): Flow<CreditSnapshot> =
        held.map { current -> current?.takeIf { it.owner == remote.owner() && it.environment == remote.environment() }?.snapshot ?: NO_SNAPSHOT }
            .onStart { if (refreshNeeded) refresh() }

    override fun observeBalance(): Flow<Int> =
        if (remote.ownsLedger) remoteSnapshots().map { it.balance } else creditLedgerDao.observeBalance().flowOn(ioDispatcher)

    override fun observeKnownBalance(): Flow<Int?> =
        if (remote.ownsLedger) {
            held.map { current -> current?.takeIf { it.owner == remote.owner() && it.environment == remote.environment() }?.snapshot?.balance }
                .onStart { if (refreshNeeded) refresh() }
        } else {
            observeBalance().map { it }
        }

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> =
        if (remote.ownsLedger) {
            kotlinx.coroutines.flow.combine(held, cached) { live, disk ->
                (live ?: disk)?.takeIf { it.owner == remote.owner() && it.environment == remote.environment() }?.snapshot?.entries.orEmpty()
            }.onStart {
                val owner = remote.owner()
                val environment = remote.environment()
                if (owner != null) {
                    persistence.withLock {
                        val entries = creditLedgerDao.ownedEntries(owner, environment).map(CreditLedgerEntity::asExternalModel)
                        if (owner == remote.owner() && environment == remote.environment()) cached.value = Held(owner, CreditSnapshot(0, entries), environment)
                    }
                }
                if (refreshNeeded) refresh()
            }
        } else {
            creditLedgerDao.observeLedger()
                .map { entities -> entities.filter { it.owner == null }.map(CreditLedgerEntity::asExternalModel) }
                .flowOn(ioDispatcher)
        }

    override suspend fun record(entry: CreditLedgerEntry) {
        if (remote.ownsLedger) {
            val (scheduled, owner) = synchronized(lock) {
                generation++
                held.value = null
                generation to remote.owner()
            }
            scope.launch {
                if (synchronized(lock) { scheduled == generation && owner == remote.owner() }) refresh()
            }
        } else {
            creditLedgerDao.insert(entry.asEntity())
        }
    }

    override suspend fun refresh() {
        refreshConfirmed()
    }

    override suspend fun refreshConfirmed(): Boolean {
        if (!remote.ownsLedger) return true
        val environment = remote.environment()
        val (started, owner) = synchronized(lock) {
            val current = remote.owner()
            held.update { it?.takeIf { held -> held.owner == current } }
            ++generation to current
        }
        val snapshot = try {
            remote.fetch()
        } catch (cancellation: CancellationException) {
            refreshNeeded = true
            throw cancellation
        } catch (failure: Exception) {
            refreshNeeded = true
            return false
        }
        return persistence.withLock {
            if (synchronized(lock) { started != generation || owner != remote.owner() || environment != remote.environment() }) return@withLock false
            if (owner != null) {
                creditLedgerDao.replaceOwned(owner, environment, snapshot.entries.map { it.asEntity().copy(owner = owner, environment = environment, serverId = it.id) })
            }
            synchronized(lock) {
                val stored = started == generation && owner == remote.owner() && environment == remote.environment()
                if (stored) {
                    held.value = Held(owner, snapshot, environment)
                    refreshNeeded = false
                }
                stored
            }
        }
    }

    override suspend fun clear() {
        synchronized(lock) {
            generation++
            held.value = null
            cached.value = null
        }
        persistence.withLock { creditLedgerDao.clear() }
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
    serverId = id,
)

private fun CreditLedgerEntity.asExternalModel() = CreditLedgerEntry(
    id = serverId,
    kind = kind,
    amount = amount,
    applicationId = applicationId,
    productId = productId,
    createdAt = createdAt,
)
