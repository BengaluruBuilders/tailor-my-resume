package com.tailormyresume.app.billing

import kotlinx.coroutines.flow.Flow

interface PlayBilling {
    val unsolicitedPurchases: Flow<PlayPurchase>

    suspend fun productDetails(productId: String): PlayProduct?

    suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlayPurchaseResult

    suspend fun ownedPurchases(): List<PlayPurchase>
}

data class PlayProduct(val name: String, val priceMicros: Long, val currencyCode: String)

enum class PlayPurchaseState { PURCHASED, PENDING }

data class PlayPurchase(val productId: String, val token: String, val state: PlayPurchaseState)

sealed interface PlayPurchaseResult {
    data class Done(val purchase: PlayPurchase) : PlayPurchaseResult

    data object Cancelled : PlayPurchaseResult

    data object AlreadyOwned : PlayPurchaseResult

    data object Failed : PlayPurchaseResult
}
