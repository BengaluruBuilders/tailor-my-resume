package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

internal object LedgerKindSerializer :
    LenientEnumSerializer<LedgerKind>(LedgerKind.entries.toTypedArray(), LedgerKind.UNKNOWN, "LedgerKind")

@Serializable(with = LedgerKindSerializer::class)
enum class LedgerKind { WELCOME, PURCHASE, TAILORING, REFUND, MIGRATION, UNKNOWN }

@Serializable
data class CreditEntryDto(
    val kind: LedgerKind,
    val amount: Int,
    val createdAt: String,
    val id: String? = null,
    val applicationId: String? = null,
    val productId: String? = null,
)

@Serializable
data class CreditsDto(val balance: Int, val entries: List<CreditEntryDto>)

@Serializable
data class CreditsResponse(val credits: CreditsDto)
