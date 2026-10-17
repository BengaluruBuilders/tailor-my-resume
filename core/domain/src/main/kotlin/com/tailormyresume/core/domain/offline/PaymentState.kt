package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseState
import kotlinx.serialization.Serializable
import kotlin.time.Instant

internal const val PAYMENT_STATE_KEY = "payment.state"

@Serializable
internal data class PaymentState(
    val freeCredits: Int,
    val purchasedCreditAmount: Int? = null,
    val spentPurchasedCredits: Int = 0,
    val confirmedPackIds: List<String> = emptyList(),
    val pendingPackIds: List<String> = emptyList(),
    val purchases: List<PurchaseDto> = emptyList(),
    val closed: Boolean = false,
    val unlockedApplicationIds: List<String> = emptyList(),
) {
    val purchasedCredits: Int
        get() = purchasedCreditAmount ?: (confirmedPackIds.sumOf(::creditsOf) - spentPurchasedCredits).coerceAtLeast(0)

    fun toEntitlement() = PurchaseEntitlement(
        freeCredits = freeCredits,
        purchasedCredits = purchasedCredits,
        pendingPackIds = pendingPackIds,
        unlockedApplicationIds = unlockedApplicationIds.toSet(),
    )

    fun confirm(packId: String, orderId: String, nowMillis: Long): PaymentState = copy(
        confirmedPackIds = confirmedPackIds + packId,
        pendingPackIds = pendingPackIds - packId,
        purchases = purchases.settle(packId, orderId, nowMillis, PurchaseState.COMPLETED),
    )

    fun hold(packId: String, orderId: String, nowMillis: Long): PaymentState = copy(
        pendingPackIds = if (packId in pendingPackIds) pendingPackIds else pendingPackIds + packId,
        purchases = purchases.settle(packId, orderId, nowMillis, PurchaseState.PENDING),
    )

    private fun List<PurchaseDto>.settle(
        packId: String,
        orderId: String,
        nowMillis: Long,
        state: PurchaseState,
    ): List<PurchaseDto> {
        val pending = firstOrNull { it.packId == packId && it.state == PurchaseState.PENDING.name }
        return when {
            pending != null && state == PurchaseState.COMPLETED ->
                map { if (it === pending) it.copy(state = state.name) else it }
            pending != null -> this
            else -> this + PurchaseDto(packId, orderId, nowMillis, state.name)
        }
    }
}

@Serializable
internal data class PurchaseDto(
    val packId: String,
    val orderId: String,
    val purchasedAtMillis: Long,
    val state: String,
) {
    fun toModel() = PurchaseRecord(
        packId = packId,
        orderId = orderId,
        purchasedAt = Instant.fromEpochMilliseconds(purchasedAtMillis),
        state = PurchaseState.entries.find { it.name == state } ?: PurchaseState.COMPLETED,
    )
}

private fun creditsOf(packId: String): Int = when (packId) {
    "application_pack_5" -> 5
    "application_pack_15" -> 15
    "application_pack_40" -> 40
    else -> 0
}
