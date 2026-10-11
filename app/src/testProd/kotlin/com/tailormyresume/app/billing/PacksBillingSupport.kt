package com.tailormyresume.app.billing

internal val THREE_PACK_IDS = listOf("application_pack_5" to 5, "application_pack_15" to 15, "application_pack_40" to 40)

internal fun threeProducts() = THREE_PACK_IDS.associate { (id, credits) ->
    id to PlayProduct("$credits applications", credits * 10_000_000L, "INR")
}

internal fun packsBody() = THREE_PACK_IDS.joinToString(",", """{"packs":[""", "]}") { (id, credits) ->
    """{"productId":"$id","credits":$credits,"creditsExpire":false}"""
}

internal fun walletV2(free: Int, purchased: Int) =
    """{"credits":${free + purchased},"freeCredits":$free,"purchasedCredits":$purchased,"analysesLeftToday":19,"day":"2026-10-11","resetsAt":"2026-10-11T18:30:00Z"}"""

internal fun purchaseV2(productId: String, credits: Int, free: Int = 1) =
    """{"purchase":{"orderId":"GPA.$productId","productId":"$productId","creditsGranted":$credits,"purchasedAt":"2026-10-11T09:12:00Z","state":"COMPLETED"},"wallet":${walletV2(free, credits)}}"""
