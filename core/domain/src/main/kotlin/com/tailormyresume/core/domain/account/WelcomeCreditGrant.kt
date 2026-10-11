package com.tailormyresume.core.domain.account

fun interface WelcomeCreditGrant {
    suspend fun grantOnce()

    companion object {
        val None = WelcomeCreditGrant { }
    }
}
