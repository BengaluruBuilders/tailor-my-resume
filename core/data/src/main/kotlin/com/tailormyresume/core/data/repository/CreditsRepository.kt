package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface CreditsRepository {
    fun observeBalance(): Flow<Int>

    fun observeKnownBalance(): Flow<Int?> = observeBalance()

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

suspend fun CreditsRepository.balanceAfterRefreshIfUnknown(): Int? {
    observeKnownBalance().first()?.let { return it }
    refreshConfirmed()
    return observeKnownBalance().first()
}
