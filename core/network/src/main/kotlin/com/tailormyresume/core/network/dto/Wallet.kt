package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WalletDto(
    val freeCredits: Int,
    val purchasedCredits: Int,
    val credits: Int = freeCredits + purchasedCredits,
    val analysesLeftToday: Int,
    val day: String,
    val resetsAt: String,
)

@Serializable
data class WalletResponse(val wallet: WalletDto)

@Serializable
data class PackDto(val productId: String, val credits: Int, val creditsExpire: Boolean)

@Serializable
data class PacksResponse(val packs: List<PackDto>)
