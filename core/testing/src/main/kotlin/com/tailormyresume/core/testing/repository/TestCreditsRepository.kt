package com.tailormyresume.core.testing.repository

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestCreditsRepository : CreditsRepository {

    private val entries = MutableStateFlow<List<CreditLedgerEntry>>(emptyList())

    override fun observeBalance(): Flow<Int> = entries.map { list -> list.sumOf { it.amount } }

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> =
        entries.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun record(entry: CreditLedgerEntry) {
        entries.update { it + entry }
    }

    override suspend fun refresh() = Unit

    override suspend fun clear() {
        entries.value = emptyList()
    }

    override suspend fun removeForApplication(applicationId: String) {
        entries.update { list -> list.filterNot { it.applicationId == applicationId } }
    }

    fun sendLedger(ledger: List<CreditLedgerEntry>) {
        entries.value = ledger
    }
}
