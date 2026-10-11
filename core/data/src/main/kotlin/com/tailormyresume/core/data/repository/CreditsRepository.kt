package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.flow.Flow

interface CreditsRepository {
    fun observeBalance(): Flow<Int>

    fun observeLedger(): Flow<List<CreditLedgerEntry>>

    suspend fun record(entry: CreditLedgerEntry)

    suspend fun refresh()

    suspend fun clear()

    suspend fun removeForApplication(applicationId: String)
}
