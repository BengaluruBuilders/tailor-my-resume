package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.model.RequirementType

internal object SkillLexicon {
    private val separators = Regex("[\\s\\-]+")

    val entries: List<LexiconEntry> = languageEntries + frameworkEntries + engineeringPracticeEntries +
        databaseEntries + analyticsEntries + financeEntries + marketingEntries + operationsEntries +
        softSkillEntries + degreeEntries + fieldOfStudyEntries

    private val entriesByCanonical: Map<String, LexiconEntry> = entries.associateBy { it.canonical }
    private val fieldOfStudyCanonicals: Set<String> = fieldOfStudyEntries.map { it.canonical }.toSet()
    private val entriesByKey: Map<String, LexiconEntry> = buildKeyIndex(LexiconEntry::allForms)
    private val entriesByStrictKey: Map<String, LexiconEntry> =
        buildKeyIndex { listOf(it.display) + it.aliases + it.exactForms }
    private val patterns: List<TermPattern> = entries.flatMap(TermPattern::compileAll)

    fun termsIn(text: String): List<LexiconTerm> {
        val normalisedText = text.replace('’', '\'')
        val candidates = patterns.flatMap { it.findIn(normalisedText) }
            .sortedWith(compareBy<LexiconTerm> { it.range.first }.thenByDescending { it.range.last })
        return withoutOverlaps(candidates)
    }

    fun canonicalsIn(text: String): List<String> = termsIn(text).map { it.canonical }.distinct()

    fun strictCanonicalsIn(text: String): Set<String> =
        termsIn(text).filterNot(::isLooseAlias).map { it.canonical }.toSet()

    fun surfaceKeysIn(text: String): Set<String> = termsIn(text).map { keyOf(it.surface) }.toSet()

    fun keyOfForm(form: String): String = keyOf(form)

    fun normalise(term: String): String? = entriesByKey[keyOf(term)]?.canonical

    fun normaliseStrict(term: String): String? = entriesByStrictKey[keyOf(term)]?.canonical

    fun isFieldOfStudy(canonical: String): Boolean = canonical in fieldOfStudyCanonicals

    fun isKnown(canonical: String): Boolean = canonical in entriesByCanonical

    fun displayName(term: String): String =
        (entriesByCanonical[term] ?: entriesByStrictKey[keyOf(term)])?.display ?: term

    fun typeOf(canonical: String): RequirementType? = entriesByCanonical[canonical]?.type

    fun isLooseSurface(term: LexiconTerm): Boolean {
        val entry = entriesByCanonical[term.canonical] ?: return true
        val surface = keyOf(term.surface)
        val ambiguousExactForm = entry.exactForms.any { keyOf(it) == surface && keyOf(it) != keyOf(entry.display) }
        return ambiguousExactForm || entry.looseAliases.any { keyOf(it) == surface }
    }

    private fun isLooseAlias(term: LexiconTerm): Boolean {
        val entry = entriesByCanonical[term.canonical] ?: return false
        val surface = keyOf(term.surface)
        return entry.looseAliases.any { keyOf(it) == surface } && !" $surface ".contains(" ${keyOf(entry.display)} ")
    }

    fun surfaceMatchesDisplay(term: LexiconTerm): Boolean {
        val entry = entriesByCanonical[term.canonical] ?: return true
        return keyOf(term.surface).trimEnd('s') == keyOf(entry.display).trimEnd('s')
    }

    fun withImplied(canonicals: Collection<String>): Set<String> {
        val result = canonicals.toMutableSet()
        val pending = ArrayDeque(canonicals)
        while (pending.isNotEmpty()) {
            val implied = entriesByCanonical[pending.removeFirst()]?.implies.orEmpty()
            implied.filter(result::add).forEach(pending::addLast)
        }
        return result
    }

    private fun keyOf(form: String): String = form.trim().lowercase().replace(separators, " ")

    private fun buildKeyIndex(formsOf: (LexiconEntry) -> List<String>): Map<String, LexiconEntry> {
        val index = LinkedHashMap<String, LexiconEntry>()
        entries.forEach { entry -> formsOf(entry).forEach { index.putIfAbsent(keyOf(it), entry) } }
        return index
    }

    private fun withoutOverlaps(sorted: List<LexiconTerm>): List<LexiconTerm> {
        val kept = mutableListOf<LexiconTerm>()
        var lastEnd = -1
        for (term in sorted) {
            if (term.range.first > lastEnd) {
                kept += term
                lastEnd = term.range.last
            }
        }
        return kept
    }
}
