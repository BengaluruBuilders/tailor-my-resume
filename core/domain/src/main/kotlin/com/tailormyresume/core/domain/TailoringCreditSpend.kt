package com.tailormyresume.core.domain

import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlin.time.Clock

object TailoringCreditSpend {
    fun runKey(runId: String): String = "run-$runId"

    suspend fun <T> forSuccess(
        applicationId: String,
        runId: String,
        clock: Clock,
        record: suspend (CreditLedgerEntry) -> Unit,
        tailoring: suspend () -> T,
    ): T {
        val result = tailoring()
        record(CreditLedgerEntry(CreditLedgerKind.SPEND, -1, applicationId, runKey(runId), clock.now()))
        return result
    }
}
