package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.isNamedSkillKeyword
import com.tailormyresume.core.domain.keywordsStatedIn
import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.sendableFacts

object KeywordCoverageCalculator {
    private const val YES_REGULARLY = "YES_REGULARLY"
    private const val A_FEW_TIMES = "A_FEW_TIMES"

    fun compute(
        matches: List<RequirementMatch>,
        quickAnswer: QuickAnswer?,
        tailoredResume: TailoredResume?,
        profile: CandidateProfile,
    ): ApplicationKeywordCoverage {
        val requirements = matches.map { it.requirement }
        val keywordSet = requirements.flatMap(::keywordsOf).toSet()
        val metKeywords = evidencedKeywords(matches, profile)
        val answerKeywords = answerKeywords(requirements, quickAnswer)
        return ApplicationKeywordCoverage(
            now = percent(metKeywords.size, keywordSet.size),
            upTo = percent((metKeywords + answerKeywords).size, keywordSet.size),
            final = tailoredResume?.let { resume ->
                val text = finalText(resume, profile)
                percent(requirements.flatMap { stated(it, text) }.toSet().size, keywordSet.size)
            },
        )
    }

    private fun finalText(resume: TailoredResume, profile: CandidateProfile): String {
        val sendable = profile.sendableFacts()
        val accepted = resume.bullets.filter { it.decision == BulletDecision.ACCEPTED && it.violations.isEmpty() }
        val entries = resumeEntries(resume, sendable)
        val bullets = entries.flatMap { entry ->
            entry.bullets.map { source -> accepted.appliedText(entry.id, source.id, source.text) }
        }
        val summary = resume.summary
            ?.takeIf { it.decision == BulletDecision.ACCEPTED && it.violations.isEmpty() }
            ?.text
            ?: sendable.summary
        val skills = resume.skills
            ?.takeIf { it.decision == BulletDecision.ACCEPTED && it.violations.isEmpty() }
            ?.skills
            ?: sendable.skills
        return (entries.map { it.title } + bullets + summary + skills).joinToString("\n")
    }

    private fun resumeEntries(resume: TailoredResume, profile: CandidateProfile) = buildList {
        val tailoredByEntry = resume.bullets.groupBy { it.entryId }
        val recordedIds = resume.entryIds?.toSet()
        profile.entries.forEach { entry ->
            val tailored = tailoredByEntry[entry.id]
            val recorded = recordedIds == null || entry.id in recordedIds
            if (tailored != null || recorded) {
                val sourceIds = tailored?.flatMap { it.sourceIds }?.toSet().orEmpty()
                val bullets = if (tailored == null) {
                    entry.bullets
                } else {
                    entry.bullets.filter { it.id in sourceIds }
                }
                add(entry.copy(bullets = bullets))
            }
        }
    }

    private fun List<TailoredBullet>.appliedText(entryId: String, sourceId: String, sourceText: String): String =
        firstOrNull {
            it.entryId == entryId && sourceId in it.sourceIds && it.originalText.trim() == sourceText.trim()
        }?.proposedText ?: sourceText

    private fun keywordOf(keyword: String): String = SkillLexicon.displayName(keyword).lowercase()

    private fun keywordsOf(requirement: JobRequirement): List<String> =
        requirement.keywords.map(::keywordOf)

    private fun stated(
        requirement: JobRequirement,
        statement: String,
    ): List<String> = keywordsStatedIn(requirement, statement).map(String::lowercase)

    private fun answerKeywords(
        requirements: List<JobRequirement>,
        quickAnswer: QuickAnswer?,
    ): List<String> {
        if (quickAnswer == null || quickAnswer.choice !in setOf(YES_REGULARLY, A_FEW_TIMES)) return emptyList()
        val requirement = requirements.firstOrNull { it.id == quickAnswer.requirementId } ?: return emptyList()
        val named = requirement.keywords.filter { isNamedSkillKeyword(requirement, it) }
        val statedInDetail = keywordsStatedIn(requirement, quickAnswer.detail)
        return (named.map(::keywordOf) + statedInDetail.map(::keywordOf)).distinct()
    }

    private fun percent(covered: Int, total: Int): Int = if (total == 0) 0 else (200 * covered + total) / (2 * total)
}
