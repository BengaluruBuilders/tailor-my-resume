package com.tailormyresume.core.domain

object RequirementPhrase {
    private val leadingMarker = Regex(
        "^(?:must[- ]have|nice[- ]to[- ]have|good[- ]to[- ]have)\\b[\\s:\\-–]*",
        RegexOption.IGNORE_CASE,
    )
    private val trailingPunctuation = Regex("[\\s.,;:!?]+$")

    fun of(text: String): String {
        val trimmed = text.trim().replace(trailingPunctuation, "")
        val withoutMarker = trimmed.replace(leadingMarker, "").trim()
        return withoutMarker.ifEmpty { trimmed }
    }
}
