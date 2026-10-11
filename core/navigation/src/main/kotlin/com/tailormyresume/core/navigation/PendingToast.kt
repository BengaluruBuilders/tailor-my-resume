package com.tailormyresume.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PendingToast {
    var queued: Int? by mutableStateOf(null)
        private set

    fun set(@StringRes message: Int) {
        queued = message
    }

    fun consume(): Int? = queued.also { queued = null }
}
