package com.tailormyresume.core.model

enum class AccountMode { ANONYMOUS, GOOGLE, OTHER }

data class SignInAccount(
    val id: String,
    val displayName: String,
    val email: String,
    val mode: AccountMode = AccountMode.GOOGLE,
) {
    companion object {
        val localAccount = SignInAccount(
            id = "local.account.1",
            displayName = "Priya Deshmukh",
            email = "priya.d@example.com",
        )
    }
}
