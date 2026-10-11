package com.tailormyresume.core.model

import kotlin.time.Instant

enum class CreditLedgerKind { FREE_GRANT, SPEND, PURCHASE, REFUND, MIGRATION }

data class CreditLedgerEntry(
    val kind: CreditLedgerKind,
    val amount: Int,
    val applicationId: String?,
    val productId: String?,
    val createdAt: Instant,
)
