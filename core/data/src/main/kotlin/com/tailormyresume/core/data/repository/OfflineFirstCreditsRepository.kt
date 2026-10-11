package com.tailormyresume.core.data.repository

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.database.dao.CreditLedgerDao
import com.tailormyresume.core.database.model.CreditLedgerEntity
import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineFirstCreditsRepository @Inject constructor(
    private val creditLedgerDao: CreditLedgerDao,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CreditsRepository {

    override fun observeBalance(): Flow<Int> = creditLedgerDao.observeBalance().flowOn(ioDispatcher)

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> =
        creditLedgerDao.observeLedger()
            .map { entities -> entities.map(CreditLedgerEntity::asExternalModel) }
            .flowOn(ioDispatcher)

    override suspend fun record(entry: CreditLedgerEntry) = creditLedgerDao.insert(entry.asEntity())

    override suspend fun refresh() = Unit

    override suspend fun clear() = creditLedgerDao.clear()

    override suspend fun removeForApplication(applicationId: String) = creditLedgerDao.deleteForApplication(applicationId)
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
