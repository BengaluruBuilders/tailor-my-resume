package com.tailormyresume.feature.settings.impl.delete

private const val CONFIRMATION_WORD = "DELETE"

internal fun matchesDeleteConfirmation(text: String): Boolean =
    text.trim().equals(CONFIRMATION_WORD, ignoreCase = true)
