package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.confirmedWithinLimits
import javax.inject.Inject

class OfflineResumeTailor @Inject constructor() : ResumeTailor {
    override suspend fun tailor(
        profile: CandidateProfile,
        job: JobDescription,
        gap: GapAnalysis,
        applicationId: String,
        answer: QuickAnswer?,
    ): TailoredResume {
        val weights = keywordWeights(gap)
        val rewriter = BulletRewriter(weights.keys)
        val jobKeywords = job.requirements.flatMap { it.keywords }.distinct()
        val bullets = profile.confirmedWithinLimits().entries
            .flatMap { tailorEntry(it, weights, rewriter, jobKeywords) }
        return TailoredResume(bullets)
    }

    private fun keywordWeights(gap: GapAnalysis): Map<String, Int> {
        val weights = LinkedHashMap<String, Int>()
        gap.matches.filter { it.status != MatchStatus.GAP }.forEach { match ->
            val weight = if (match.requirement.priority == RequirementPriority.MUST_HAVE) MUST_WEIGHT else NICE_WEIGHT
            match.requirement.keywords.forEach { weights.merge(it, weight) { a, b -> maxOf(a, b) } }
        }
        return weights
    }

    private fun tailorEntry(
        entry: ProfileEntry,
        weights: Map<String, Int>,
        rewriter: BulletRewriter,
        jobKeywords: List<String>,
    ): List<TailoredBullet> {
        val scored = entry.bullets.map { it to relevance(it, weights) }
        val ordered = scored.sortedByDescending { it.second }.map { it.first }
        return ordered.mapIndexed { newIndex, bullet ->
            val moved = entry.bullets.indexOf(bullet) != newIndex
            tailorBullet(entry, bullet, moved, rewriter, jobKeywords)
        }
    }

    private fun relevance(bullet: EvidenceBullet, weights: Map<String, Int>): Int {
        val index = EvidenceIndex.ofText(bullet.text)
        return weights.entries.sumOf { (keyword, weight) -> if (index.supports(keyword)) weight else 0 }
    }

    private fun tailorBullet(
        entry: ProfileEntry,
        bullet: EvidenceBullet,
        moved: Boolean,
        rewriter: BulletRewriter,
        jobKeywords: List<String>,
    ): TailoredBullet {
        val rewrite = rewriter.rewrite(bullet.text)
        val proposedIndex = EvidenceIndex.ofText(rewrite.text)
        val editTypes = if (moved) listOf(EditType.REORDER) + rewrite.editTypes else rewrite.editTypes
        return TailoredBullet(
            id = "$TAILORED_ID_PREFIX${bullet.id}",
            entryId = entry.id,
            originalText = bullet.text,
            proposedText = rewrite.text,
            sourceIds = listOf(bullet.id),
            editTypes = editTypes,
            keywordsUsed = jobKeywords.filter(proposedIndex::supports),
            violations = emptyList(),
            decision = BulletDecision.PENDING,
        )
    }

    private companion object {
        const val MUST_WEIGHT = 2
        const val NICE_WEIGHT = 1
        const val TAILORED_ID_PREFIX = "tailored-"
    }
}
