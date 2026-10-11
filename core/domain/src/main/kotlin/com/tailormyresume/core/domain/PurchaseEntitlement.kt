package com.tailormyresume.core.domain

data class PurchaseEntitlement(
    val freeCredits: Int,
    val purchasedCredits: Int,
    val pendingPackIds: List<String>,
    val unlockedApplicationIds: Set<String> = emptySet(),
    val hasKnownBalance: Boolean = true,
) {
    val totalCredits: Int
        get() = freeCredits + purchasedCredits
}
