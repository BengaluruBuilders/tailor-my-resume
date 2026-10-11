package com.tailormyresume.core.domain.offline

internal class TermPattern private constructor(
    private val canonical: String,
    private val regex: Regex,
) {
    fun findIn(text: String): List<LexiconTerm> =
        regex.findAll(text).map { LexiconTerm(canonical, it.value, it.range) }.toList()

    companion object {
        private const val BLOCKED_NEIGHBOUR = "\\p{L}\\p{N}+#&"
        private val wordSeparators = Regex("[\\s\\-]+")

        fun compileAll(entry: LexiconEntry): List<TermPattern> = listOfNotNull(
            build(entry, entry.insensitiveForms, ignoreCase = true, suffixes = emptyList()),
            build(entry, entry.exactForms, ignoreCase = false, suffixes = entry.blockedSuffixes, prefix = entry.requiredPrefix),
        )

        private fun build(
            entry: LexiconEntry,
            forms: List<String>,
            ignoreCase: Boolean,
            suffixes: List<String>,
            prefix: String? = null,
        ): TermPattern? {
            if (forms.isEmpty()) return null
            val alternatives = forms.sortedByDescending { it.length }.joinToString("|") { formRegex(it) }
            val strict = forms.all { it.length == 1 }
            val before = if (strict) "(?<![$BLOCKED_NEIGHBOUR\\-/.])" else "(?<![$BLOCKED_NEIGHBOUR])"
            val after = if (strict) "(?![$BLOCKED_NEIGHBOUR\\-/]|\\.\\p{L})" else "(?![$BLOCKED_NEIGHBOUR])"
            val blocked = entry.blockedPrefixes.joinToString("") { "(?<!$it)" }
            val required = prefix?.let { "(?<=$it)" }.orEmpty()
            val blockedAfter = suffixes.joinToString("") { "(?!$it)" }
            val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            return TermPattern(entry.canonical, Regex("$before$blocked$required(?:$alternatives)$after$blockedAfter", options))
        }

        private fun formRegex(form: String): String =
            form.trim().split(wordSeparators).joinToString("[\\s\\-]+") { Regex.escape(it) }
    }
}
