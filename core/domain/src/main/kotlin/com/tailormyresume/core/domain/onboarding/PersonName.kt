package com.tailormyresume.core.domain.onboarding

private const val MAX_NAME_CHARS = 50
private const val MAX_NAME_WORDS = 5
private const val MAX_ABBREVIATION_CHARS = 3
private val NOT_IN_A_NAME = Regex("[0-9,;:!?@/()\\[\\]{}<>\"]")

fun looksLikeName(candidate: String): Boolean {
    val name = candidate.trim()
    if (name.isEmpty() || name.length > MAX_NAME_CHARS || NOT_IN_A_NAME.containsMatchIn(name)) return false
    val words = name.split(Regex("\\s+"))
    return words.size <= MAX_NAME_WORDS &&
        words.all { !it.contains('.') || it.length <= MAX_ABBREVIATION_CHARS }
}
