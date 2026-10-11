package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickQuestion
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import javax.inject.Inject

internal class OfflineGapMatcher @Inject constructor() : GapMatcher {
    override fun match(profile: CandidateProfile, job: JobDescription): GapAnalysis {
        val sources = EvidenceSources.of(profile)
        val matches = job.requirements.map { matchRequirement(it, sources) }
        val keywords = job.requirements.flatMap { it.keywords }.distinct()
        val covered = keywords.count { keyword -> sources.any { it.index.supports(keyword) } }
        return GapAnalysis(
            matches,
            KeywordCoverage(covered = covered, total = keywords.size),
            question = matches.firstOrNull(::shouldAskAbout)?.question(),
        )
    }

    private fun matchRequirement(requirement: JobRequirement, sources: List<EvidenceSource>): RequirementMatch {
        val supporting = sources.filter { source -> requirement.keywords.any { source.index.supports(it) } }
        val status = cappedByYears(requirement, statusOf(requirement, sources))
        return RequirementMatch(
            requirement = requirement,
            status = status,
            evidenceIds = supporting.map { it.id }.distinct(),
            reason = reasonFor(status),
        )
    }

    private fun reasonFor(status: MatchStatus): String = when (status) {
        MatchStatus.MET -> "Found in your resume."
        MatchStatus.PARTIAL -> "Partly in your resume."
        MatchStatus.GAP -> "Not in your resume."
    }

    private fun shouldAskAbout(match: RequirementMatch): Boolean =
        match.status != MatchStatus.MET &&
            match.requirement.priority == RequirementPriority.MUST_HAVE &&
            match.requirement.type != RequirementType.EDUCATION

    private fun RequirementMatch.question(): QuickQuestion = QuickQuestion(
        requirementId = requirement.id,
        text = requirement.questionText(),
        why = whyFor(status),
    )

    private fun whyFor(status: MatchStatus): String = when (status) {
        MatchStatus.PARTIAL ->
            "It's a must-have for this role. Your resume has related evidence, " +
                "but it does not fully confirm this requirement."
        MatchStatus.GAP -> "It's a must-have for this role, and your resume doesn't mention it."
        MatchStatus.MET -> error("A met requirement does not need a question.")
    }

    private fun JobRequirement.questionText(): String {
        val subject = text
            .trim()
            .trimEnd('.')
            .removePrefix("Experience ")
            .removePrefix("experience ")
            .removePrefix("with ")
            .removePrefix("With ")
            .removePrefix("in ")
            .removePrefix("In ")
            .ifBlank { keywords.joinToString(" and ", transform = SkillLexicon::displayName) }
        return "Do you have experience with $subject?"
    }

    private fun statusOf(requirement: JobRequirement, sources: List<EvidenceSource>): MatchStatus {
        val units = unitsOf(requirement.keywords)
        val covered = units.count { alternatives ->
            alternatives.any { keyword -> sources.any { it.index.supports(keyword) } }
        }
        return when {
            units.isEmpty() || covered == 0 -> MatchStatus.GAP
            covered == units.size -> MatchStatus.MET
            else -> MatchStatus.PARTIAL
        }
    }

    private fun unitsOf(keywords: List<String>): List<List<String>> {
        val education = keywords.filter { SkillLexicon.typeOf(it) == RequirementType.EDUCATION }
        val (fields, levels) = education.partition(SkillLexicon::isFieldOfStudy)
        val others = keywords.filterNot { it in education }
        return others.map { listOf(it) } + listOfNotNull(levels.takeIf { it.isNotEmpty() }, fields.takeIf { it.isNotEmpty() })
    }

    private fun cappedByYears(requirement: JobRequirement, status: MatchStatus): MatchStatus {
        val years = RequirementCues.minimumYears(requirement.text) ?: return status
        return if (years >= 1 && status == MatchStatus.MET) MatchStatus.PARTIAL else status
    }
}
