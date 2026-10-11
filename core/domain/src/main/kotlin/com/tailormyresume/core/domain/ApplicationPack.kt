package com.tailormyresume.core.domain

data class ApplicationPack(
    val id: String,
    val name: String,
    val credits: Int,
    val priceInPaise: Long,
    val currencyCode: String,
    val creditsExpire: Boolean,
) {
    companion object {
        const val APPLICATION_PACK_FIVE = "application_pack_5"
        const val APPLICATION_PACK_FIFTEEN = "application_pack_15"
        const val APPLICATION_PACK_FORTY = "application_pack_40"
        const val CURRENCY_INR = "INR"
    }
}
