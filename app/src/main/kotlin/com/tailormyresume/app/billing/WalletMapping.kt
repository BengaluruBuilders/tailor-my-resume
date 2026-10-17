package com.tailormyresume.app.billing

import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.network.dto.WalletDto

internal fun WalletDto.toEntitlement(pendingPackIds: List<String>) = PurchaseEntitlement(
    freeCredits = freeCredits,
    purchasedCredits = purchasedCredits,
    pendingPackIds = pendingPackIds,
    unlockedApplicationIds = unlockedApplicationIds.toSet(),
)

internal val NO_CREDITS = PurchaseEntitlement(freeCredits = 0, purchasedCredits = 0, pendingPackIds = emptyList())
