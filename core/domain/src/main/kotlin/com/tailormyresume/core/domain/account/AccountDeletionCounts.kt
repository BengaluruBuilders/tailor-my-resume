package com.tailormyresume.core.domain.account

data class AccountDeletionCounts(
    val profileFacts: Int,
    val applications: Int,
    val unusedCredits: Int?,
)
