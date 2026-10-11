package com.tailormyresume.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PendingToast {
    var queued: Int? by mutableStateOf(null)
        private set

    private var targetAccountId: String? = null

    fun set(@StringRes message: Int, forAccountId: String? = null) {
        targetAccountId = forAccountId
        queued = message
    }

    fun consumeFor(accountId: String?): Int? = if (accountId == targetAccountId) consume() else null

    fun consume(): Int? = queued.also { queued = null }
}
