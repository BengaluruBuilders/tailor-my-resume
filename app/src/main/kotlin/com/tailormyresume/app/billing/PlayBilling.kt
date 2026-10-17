package com.tailormyresume.app.billing

import kotlinx.coroutines.flow.Flow

interface PlayBilling {
    val unsolicitedPurchases: Flow<PlayPurchase>
    suspend fun ownedPurchases(): List<PlayPurchase>
}

enum class PlayPurchaseState { PURCHASED, PENDING }
data class PlayPurchase(val productId: String, val token: String, val state: PlayPurchaseState)
