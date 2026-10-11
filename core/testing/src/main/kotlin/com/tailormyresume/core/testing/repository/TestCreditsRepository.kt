package com.tailormyresume.core.testing.repository

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.model.CreditLedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestCreditsRepository : CreditsRepository {

    private val entries = MutableStateFlow<List<CreditLedgerEntry>>(emptyList())
    private val unknown = MutableStateFlow(false)
    private var ledgerOnRefresh: List<CreditLedgerEntry>? = null

    var refreshCalls = 0
        private set

    override fun observeBalance(): Flow<Int> = entries.map { list -> list.sumOf { it.amount } }

    override fun observeKnownBalance(): Flow<Int?> =
        combine(entries, unknown) { list, notLoaded -> if (notLoaded) null else list.sumOf { it.amount } }

    override fun observeLedger(): Flow<List<CreditLedgerEntry>> =
        entries.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun record(entry: CreditLedgerEntry) {
        entries.update { it + entry }
    }

    override suspend fun refresh() {
        refreshCalls++
        ledgerOnRefresh?.let { ledger ->
            entries.value = ledger
            unknown.value = false
        }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }

    override suspend fun removeForApplication(applicationId: String) {
        entries.update { list -> list.filterNot { it.applicationId == applicationId } }
    }

    fun sendLedger(ledger: List<CreditLedgerEntry>) {
        entries.value = ledger
        unknown.value = false
    }

    fun sendUnknown() {
        unknown.value = true
    }

    fun revealOnRefresh(ledger: List<CreditLedgerEntry>) {
        ledgerOnRefresh = ledger
    }
}
