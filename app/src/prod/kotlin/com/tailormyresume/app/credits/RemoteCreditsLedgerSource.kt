package com.tailormyresume.app.credits

import com.tailormyresume.core.data.repository.CreditSnapshot
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.CreditEntryDto
import com.tailormyresume.core.network.dto.LedgerKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class RemoteCreditsLedgerSource @Inject constructor(
    private val api: TailorMyResumeApi,
    private val uids: FirebaseUidProvider,
) : RemoteLedgerSource {
    override val ownsLedger = true

    override fun owner(): String? = uids.uid()

    override suspend fun fetch(): CreditSnapshot {
        val credits = apiResult { api.credits().credits }.getOrThrow()
        return CreditSnapshot(credits.balance, credits.entries.mapNotNull(::toEntry).sortedByDescending { it.createdAt })
    }

    private fun toEntry(dto: CreditEntryDto): CreditLedgerEntry? {
        val kind = when (dto.kind) {
            LedgerKind.WELCOME -> CreditLedgerKind.FREE_GRANT
            LedgerKind.PURCHASE -> CreditLedgerKind.PURCHASE
            LedgerKind.TAILORING -> CreditLedgerKind.SPEND
            LedgerKind.REFUND -> CreditLedgerKind.REFUND
            LedgerKind.MIGRATION -> CreditLedgerKind.MIGRATION
            LedgerKind.UNKNOWN -> return null
        }
        return CreditLedgerEntry(kind, dto.amount, dto.applicationId, dto.productId, Instant.parse(dto.createdAt))
    }
}
