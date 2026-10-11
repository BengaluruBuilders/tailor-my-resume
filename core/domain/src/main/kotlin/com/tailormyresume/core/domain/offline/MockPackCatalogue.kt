package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.domain.ApplicationPack

object MockPackCatalogue {
    val applicationPackFive = pack(ApplicationPack.APPLICATION_PACK_FIVE, credits = 5, priceInPaise = 19_900)
    val applicationPackFifteen = pack(ApplicationPack.APPLICATION_PACK_FIFTEEN, credits = 15, priceInPaise = 44_900)
    val applicationPackForty = pack(ApplicationPack.APPLICATION_PACK_FORTY, credits = 40, priceInPaise = 99_900)

    val all = listOf(applicationPackFive, applicationPackFifteen, applicationPackForty)

    private fun pack(id: String, credits: Int, priceInPaise: Long) = ApplicationPack(
        id = id,
        name = "Application pack",
        credits = credits,
        priceInPaise = priceInPaise,
        currencyCode = ApplicationPack.CURRENCY_INR,
        creditsExpire = false,
    )
}
