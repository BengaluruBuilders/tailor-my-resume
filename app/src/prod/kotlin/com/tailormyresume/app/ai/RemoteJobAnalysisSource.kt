package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.JobAnalysisSource
import com.tailormyresume.core.domain.upgradedMatch
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.evidenceIds
import com.tailormyresume.core.model.sendableFacts
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.dto.MatchDto
import com.tailormyresume.core.network.mapper.toFactsDto
import com.tailormyresume.core.network.mapper.toJobDescription
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class RemoteJobAnalysisSource @Inject constructor(
    private val api: TailorMyResumeApi,
    private val matcher: GapMatcher,
    private val clock: Clock = Clock.System,
) : JobAnalysisSource {
    private data class Entry(val result: JobAnalysisResult, val baselineProfile: CandidateProfile)

    private val cache = object : LinkedHashMap<String, Entry>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>) = size > CACHE_SIZE
    }

    override suspend fun analyse(profile: CandidateProfile, rawJobText: String): JobAnalysisResult {
        val facts = profile.toFactsDto()
        val key = cacheKey(rawJobText)
        synchronized(cache) { cache[key] }?.let { return rematch(it, profile) }
        val response = remoteAi { api.analyse(AnalysisRequest(rawJobText, facts)) }
        val job = response.job.toJobDescription(rawJobText)
        val factIds = profile.sendableFacts().evidenceIds()
        val gap = GapAnalysis(
            matches = matchesOf(job, response.matches, factIds),
            keywordCoverage = matcher.match(profile, job).keywordCoverage,
            generationId = response.generationId,
        )
        val entry = Entry(JobAnalysisResult(job, gap, clock.now()), profile)
        synchronized(cache) { cache[key] = entry }
        return entry.result
    }

    fun clear() = synchronized(cache) { cache.clear() }

    private fun rematch(entry: Entry, profile: CandidateProfile): JobAnalysisResult {
        val job = entry.result.job
        val factIds = profile.sendableFacts().evidenceIds()
        val baseline = matcher.match(entry.baselineProfile, job).matches.associateBy { it.requirement.id }
        val current = matcher.match(profile, job)
        val currentByRequirement = current.matches.associateBy { it.requirement.id }
        val matches = entry.result.gap.matches.map { server ->
            val evidence = server.evidenceIds.filter { it in factIds }
            val effectiveServerStatus =
                server.status.takeUnless { it != MatchStatus.GAP && evidence.isEmpty() } ?: MatchStatus.GAP
            val effective = RequirementMatch(server.requirement, effectiveServerStatus, evidence)
            upgradedMatch(effective, currentByRequirement[server.requirement.id], baseline[server.requirement.id])
                ?: effective
        }
        return JobAnalysisResult(job, GapAnalysis(matches, current.keywordCoverage, entry.result.gap.generationId), entry.result.analysedAt)
    }

    private fun matchesOf(job: JobDescription, matches: List<MatchDto>, factIds: Set<String>): List<RequirementMatch> {
        val byRequirement = matches.associateBy { it.requirementId }
        return job.requirements.map { requirement ->
            val match = byRequirement[requirement.id]
            val evidence = match?.evidenceIds.orEmpty().filter { it in factIds }
            val status = match?.status?.takeUnless { it != MatchStatus.GAP && evidence.isEmpty() } ?: MatchStatus.GAP
            RequirementMatch(requirement, status, evidence)
        }
    }

    private fun cacheKey(rawJobText: String): String =
        sha256(rawJobText.trim().replace(WHITESPACE, " "))

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CACHE_SIZE = 8
        val WHITESPACE = Regex("\\s+")
    }
}
