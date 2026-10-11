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
import com.tailormyresume.core.model.confirmedWithinLimits

object KeywordCoverageCalculator {
    private const val YES_REGULARLY = "YES_REGULARLY"
    private const val A_FEW_TIMES = "A_FEW_TIMES"

    fun compute(
        matches: List<RequirementMatch>,
        quickAnswer: QuickAnswer?,
        tailoredResume: TailoredResume?,
        profile: CandidateProfile? = null,
    ): ApplicationKeywordCoverage {
        val requirements = matches.map { it.requirement }
        val keywordSet = requirements.flatMap(::keywordsOf).toSet()
        val evidence = evidenceTextsOf(profile)
        val metKeywords = matches.flatMap { it.presentKeywords(evidence) }.map { it.lowercase() }.toSet()
        val answerKeywords = answerKeywords(requirements, quickAnswer)
        return ApplicationKeywordCoverage(
            now = percent(metKeywords.size, keywordSet.size),
            upTo = percent((metKeywords + answerKeywords).size, keywordSet.size),
            final = tailoredResume?.let { resume ->
                val text = resumeText(resume, profile)
                percent(requirements.flatMap { stated(it, text) }.toSet().size, keywordSet.size)
            },
        )
    }

    private fun resumeText(resume: TailoredResume, profile: CandidateProfile?): String {
        if (profile == null) return resume.bullets.joinToString("\n") { it.acceptedOrOriginalText() }
        val sendable = profile.confirmedWithinLimits()
        val applied = resume.bullets.filter { it.decision == BulletDecision.ACCEPTED && it.violations.isEmpty() }
        val bullets = sendable.entries.flatMap { entry ->
            entry.bullets.map { source ->
                applied.firstOrNull { it.entryId == entry.id && source.id in it.sourceIds && it.originalText.trim() == source.text.trim() }
                    ?.proposedText ?: source.text
            }
        }
        val skills = resume.skills?.takeIf { it.decision == BulletDecision.ACCEPTED }?.skills ?: sendable.skills
        val summary = resume.summary?.takeIf { it.decision == BulletDecision.ACCEPTED }?.text.orEmpty()
        return (sendable.entries.map { it.title } + bullets + skills + summary).joinToString("\n")
    }

    private fun TailoredBullet.acceptedOrOriginalText() =
        if (decision == BulletDecision.ACCEPTED) proposedText else originalText

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
