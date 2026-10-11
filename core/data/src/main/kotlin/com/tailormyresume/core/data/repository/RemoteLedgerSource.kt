package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.CreditLedgerEntry
import javax.inject.Inject

data class CreditSnapshot(val balance: Int, val entries: List<CreditLedgerEntry>)

interface RemoteLedgerSource {
    val ownsLedger: Boolean

    fun owner(): String?

    suspend fun fetch(): CreditSnapshot
}

class NoRemoteLedger @Inject constructor() : RemoteLedgerSource {
    override val ownsLedger = false

    override fun owner(): String? = null

    override suspend fun fetch() = CreditSnapshot(balance = 0, entries = emptyList())
}
