package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.model.RequirementType

internal data class LexiconEntry(
    val display: String,
    val type: RequirementType,
    val aliases: List<String> = emptyList(),
    val looseAliases: List<String> = emptyList(),
    val exactForms: List<String> = emptyList(),
    val matchesCanonical: Boolean = true,
    val implies: List<String> = emptyList(),
    val blockedPrefixes: List<String> = emptyList(),
    val blockedSuffixes: List<String> = emptyList(),
    val requiredPrefix: String? = null,
) {
    val canonical: String = display.lowercase()

    val insensitiveForms: List<String> =
        (if (matchesCanonical) listOf(display) else emptyList()) + aliases + looseAliases

    val allForms: List<String> = listOf(display) + aliases + looseAliases + exactForms
}

internal data class LexiconTerm(val canonical: String, val surface: String, val range: IntRange)
