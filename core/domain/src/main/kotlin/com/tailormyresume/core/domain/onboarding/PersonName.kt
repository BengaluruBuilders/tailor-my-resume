package com.tailormyresume.core.domain.onboarding

private const val MAX_NAME_CHARS = 50
private const val MAX_NAME_WORDS = 5
private val NOT_IN_A_NAME = Regex("[0-9,;:!?@/()\\[\\]{}<>\"]")
private val STANDALONE_DASH = Regex("(^|\\s)[-\u2013\u2014](\\s|$)")
private val DOTTED_INITIALS = Regex("^(\\p{L}\\.|\\p{L}{1,3}(\\.\\p{L}{1,3})+\\.?)$")
private val TITLES = setOf("dr.", "mr.", "mrs.", "ms.", "jr.", "sr.", "prof.", "smt.")

fun personNameOrNull(candidate: String): String? {
    val name = candidate.trim().split(STANDALONE_DASH, limit = 2).first().trim()
    if (name.isEmpty() || name.length > MAX_NAME_CHARS || NOT_IN_A_NAME.containsMatchIn(name)) return null
    val words = name.split(Regex("\\s+"))
    val plausible = words.size <= MAX_NAME_WORDS &&
        words.all { !it.contains('.') || it.lowercase() in TITLES || DOTTED_INITIALS.matches(it) }
    return name.takeIf { plausible }
}

fun looksLikeName(candidate: String): Boolean = personNameOrNull(candidate) != null
