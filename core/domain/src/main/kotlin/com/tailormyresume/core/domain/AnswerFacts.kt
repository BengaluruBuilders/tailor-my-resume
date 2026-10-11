package com.tailormyresume.core.domain

import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.QuickAnswer

internal object AnswerFacts {
    const val YES_REGULARLY = "YES_REGULARLY"
    const val A_FEW_TIMES = "A_FEW_TIMES"
    private const val ID_PREFIX = "ans-"
    private val choicesThatSupportFacts = setOf(YES_REGULARLY, A_FEW_TIMES)

    fun isAnswerId(id: String): Boolean = id.startsWith(ID_PREFIX)

    fun idsOf(fact: EvidenceBullet): List<String> = listOf(fact.id, "${fact.id}-b1")

    fun keywords(answer: QuickAnswer?, job: JobDescription): List<String> {
        if (answer == null || answer.choice !in choicesThatSupportFacts) return emptyList()
        val requirement = job.requirements.firstOrNull { it.id == answer.requirementId } ?: return emptyList()
        val named = requirement.keywords.filter { isNamedSkillKeyword(requirement, it) }.map(SkillLexicon::displayName)
        return (named + keywordsStatedIn(requirement, answer.detail)).distinct()
    }

    fun factOf(answer: QuickAnswer?, job: JobDescription): EvidenceBullet? {
        if (answer == null) return null
        val keywords = keywords(answer, job)
        val text = listOf(answer.detail.trim(), keywords.joinToString(", "))
            .filter { it.isNotEmpty() && answer.choice in choicesThatSupportFacts }
            .joinToString("\n")
        return text.takeIf { it.isNotEmpty() }?.let { EvidenceBullet(ID_PREFIX + answer.requirementId, it) }
    }
}
