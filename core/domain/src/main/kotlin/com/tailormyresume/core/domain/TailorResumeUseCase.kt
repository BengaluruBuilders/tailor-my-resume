package com.tailormyresume.core.domain

import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.domain.offline.TextTokens
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.model.confirmedWithinLimits
import javax.inject.Inject

class TailorResumeUseCase @Inject constructor(
    private val tailor: ResumeTailor,
    private val guard: FabricationGuard,
) {
    private class Evidence(
        val profileBullets: Map<String, EvidenceBullet>,
        val answerFact: EvidenceBullet?,
        val answerKeywords: List<String>,
        val answerDetail: String,
        val entryOfBullet: Map<String, String>,
    ) {
        val answerBullets: Map<String, EvidenceBullet> =
            answerFact?.let { fact -> AnswerFacts.idsOf(fact).associateWith { fact } }.orEmpty()

        fun resolve(ids: List<String>): List<EvidenceBullet> =
            ids.mapNotNull { profileBullets[it] ?: answerBullets[it] }.distinct()

        fun groupOf(source: EvidenceBullet): String =
            entryOfBullet[source.id] ?: source.id
    }

    suspend operator fun invoke(
        profile: CandidateProfile,
        job: JobDescription,
        gap: GapAnalysis,
        applicationId: String,
        section: EntryCategory? = null,
        quickAnswer: QuickAnswer? = null,
    ): TailoredResume {
        val evidence = Evidence(
            profileBullets = profile.confirmedWithinLimits().entries.flatMap { it.bullets }.associateBy { it.id },
            answerFact = AnswerFacts.factOf(quickAnswer, job),
            answerKeywords = AnswerFacts.keywords(quickAnswer, job),
            answerDetail = quickAnswer?.detail.orEmpty(),
            entryOfBullet = profile.confirmedWithinLimits().entries
                .flatMap { entry -> entry.bullets.map { it.id to entry.id } }.toMap(),
        )
        val proposed = tailor.tailor(profile, job, gap, applicationId, section)
        return TailoredResume(
            bullets = proposed.bullets.mapNotNull { verified(it, evidence, profile) },
            entryIds = profile.entries.filter { it.isConfirmed }.map { it.id },
            summary = proposed.summary?.let { verifiedSummary(it, evidence, profile) },
            skills = proposed.skills?.let { verifiedSkills(it, evidence, profile) },
        )
    }

    private fun verified(bullet: TailoredBullet, evidence: Evidence, profile: CandidateProfile): TailoredBullet? {
        val sources = evidence.resolve(bullet.sourceIds)
        val profileText = bullet.sourceIds.firstNotNullOfOrNull(evidence.profileBullets::get)?.text
        val answerBacked = bullet.sourceIds.any { it !in evidence.profileBullets && AnswerFacts.isAnswerId(it) }
        if (answerBacked && evidence.answerFact != null && !isAttachable(bullet, evidence.answerDetail, profile)) return null
        if (!answerBacked && profileText == null) return null
        val originalText = profileText.orEmpty()
        val violations = guard.check(bullet.proposedText, sources, profile)
        if (violations.isEmpty()) return bullet.copy(originalText = originalText, violations = emptyList())
        return bullet.copy(
            originalText = originalText,
            proposedText = originalText,
            editTypes = emptyList(),
            keywordsUsed = emptyList(),
            violations = violations,
        )
    }

    private fun isAttachable(bullet: TailoredBullet, answerDetail: String, profile: CandidateProfile): Boolean {
        val entry = profile.entries.firstOrNull { it.id == bullet.entryId } ?: return false
        return names(answerDetail, entry)
    }

    private fun names(text: String, entry: ProfileEntry): Boolean =
        listOf(entry.organization, entry.title).filter { it.isNotBlank() }.any { name ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(name.trim())}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
                .containsMatchIn(text)
        }

    private fun verifiedSummary(summary: TailoredText, evidence: Evidence, profile: CandidateProfile): TailoredText {
        val sources = evidence.resolve(summary.sourceIds)
        val violations = summaryViolations(summary.text, sources, evidence, profile)
        if (violations.isEmpty()) {
            return summary.copy(original = profile.summary, violations = emptyList(), decision = BulletDecision.PENDING)
        }
        return summary.copy(
            text = profile.summary,
            original = profile.summary,
            violations = violations,
            decision = BulletDecision.PENDING,
        )
    }

    private fun summaryViolations(
        text: String,
        sources: List<EvidenceBullet>,
        evidence: Evidence,
        profile: CandidateProfile,
    ): List<GuardrailViolation> {
        if (sources.isEmpty()) return listOf(GuardrailViolation.MissingSource)
        val sentences = SENTENCE_BREAK.split(text.trim()).map(::contentClauses).filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return listOf(GuardrailViolation.MissingSource)
        return sentences.flatMap { clauses -> sentenceViolations(clauses, sources, evidence, profile) }.distinct()
    }

    private fun contentClauses(sentence: String): List<String> =
        CLAUSE_BREAK.split(sentence).map { it.trim() }.filter { clause ->
            TextTokens.words(clause).any { it.any(Char::isDigit) || it.lowercase() !in FUNCTION_WORDS } ||
                SkillLexicon.termsIn(clause).isNotEmpty()
        }

    private fun sentenceViolations(
        clauses: List<String>,
        sources: List<EvidenceBullet>,
        evidence: Evidence,
        profile: CandidateProfile,
    ): List<GuardrailViolation> {
        val checks = clauses.map { clause ->
            sources.associateWith { guard.check(clause, listOf(it), profile) }
        }
        val failed = clauses.indices.filter { index -> checks[index].values.none { it.isEmpty() } }
        if (failed.isNotEmpty()) {
            return failed.flatMap { index -> checks[index].values.minBy { it.size } }
        }
        val groupsPerClause = checks.map { byClause ->
            byClause.filterValues { it.isEmpty() }.keys.map(evidence::groupOf).toSet()
        }
        val sharedGroup = groupsPerClause.reduce { common, groups -> common intersect groups }
        if (sharedGroup.isNotEmpty()) return emptyList()
        val best = groupsPerClause.flatten().groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        return clauses.filterIndexed { index, _ -> best !in groupsPerClause[index] }
            .map { GuardrailViolation.UnsupportedTerm(it) }
    }

    private fun verifiedSkills(skills: TailoredSkills, evidence: Evidence, profile: CandidateProfile): TailoredSkills {
        val allowed = (profile.skills + profile.userStatedSkills + evidence.answerKeywords).map { it.lowercase() }.toSet()
        val (kept, removed) = skills.skills.distinctBy { it.lowercase() }.partition { it.lowercase() in allowed }
        return skills.copy(
            skills = kept,
            original = profile.skills,
            violations = removed.map<String, GuardrailViolation> { GuardrailViolation.UnsupportedTerm(it) },
            decision = BulletDecision.PENDING,
        )
    }

    private companion object {
        val SENTENCE_BREAK = Regex("(?<=[.!?])\\s+")
        val CLAUSE_BREAK = Regex("[!?;:]|[.,](?!\\d)|\\b(?:and|or|with)\\b", RegexOption.IGNORE_CASE)
        val FUNCTION_WORDS = setOf("the", "an", "of", "in", "to", "for", "using", "at", "on", "by", "as", "from", "my", "a", "i")
    }
}
