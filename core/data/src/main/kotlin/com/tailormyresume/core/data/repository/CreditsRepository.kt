package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CreditsRepository {
    fun observeBalance(): Flow<Int>

    fun observeKnownBalance(): Flow<Int?> = observeBalance().map { it }

    fun observeLedger(): Flow<List<CreditLedgerEntry>>

    suspend fun record(entry: CreditLedgerEntry)

    suspend fun refresh()

    suspend fun refreshConfirmed(): Boolean {
        refresh()
        return true
    }

    suspend fun clear()

    suspend fun removeForApplication(applicationId: String)
}
