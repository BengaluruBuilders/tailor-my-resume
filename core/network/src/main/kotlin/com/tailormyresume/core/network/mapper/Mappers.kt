package com.tailormyresume.core.network.mapper

import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.isSkillUserStated
import com.tailormyresume.core.model.sendableFacts
import com.tailormyresume.core.network.dto.AnswerChoice
import com.tailormyresume.core.network.dto.AnswerDto
import com.tailormyresume.core.network.dto.BulletDto
import com.tailormyresume.core.network.dto.ContentReportRequest
import com.tailormyresume.core.network.dto.FactEntryDto
import com.tailormyresume.core.network.dto.JobDto
import com.tailormyresume.core.network.dto.MatchDto
import com.tailormyresume.core.network.dto.ProfileFactsDto
import com.tailormyresume.core.network.dto.RequirementDto
import com.tailormyresume.core.network.dto.SummaryFactDto
import com.tailormyresume.core.network.dto.TailoredBulletDto

fun CandidateProfile.toFactsDto(): ProfileFactsDto {
    val sent = sendableFacts()
    return ProfileFactsDto(
        skills = sent.skills,
        userStatedSkills = sent.skills.filter { sent.isSkillUserStated(it) },
        summary = summaryFactOf(sent),
        entries = sent.entries.map { entry ->
            FactEntryDto(
                id = entry.id,
                category = entry.category,
                title = entry.title,
                organization = entry.organization,
                startDate = entry.startDate,
                endDate = entry.endDate,
                source = entry.source,
                bullets = entry.bullets.map { BulletDto(it.id, it.text) },
            )
        },
    )
}

private fun summaryFactOf(sent: CandidateProfile): SummaryFactDto? {
    val text = sent.summary.trim()
    if (text.isEmpty() || text.length > MAX_SUMMARY_LENGTH) return null
    val taken = sent.entries.flatMap { entry -> listOf(entry.id) + entry.bullets.map { it.id } }.toSet()
    val id = generateSequence(0) { it + 1 }
        .map { if (it == 0) SUMMARY_ID else "$SUMMARY_ID-$it" }
        .first { it !in taken }
    return SummaryFactDto(id, text)
}

fun QuickAnswer.toAnswerDto(job: JobDescription): AnswerDto? {
    val known = AnswerChoice.entries.firstOrNull { it.name == choice } ?: return null
    if (job.requirements.none { it.id == requirementId }) return null
    return AnswerDto(requirementId, known, detail.trim().takeIf { it.isNotEmpty() }?.let(::cappedDetail))
}

private fun cappedDetail(text: String): String {
    if (text.length <= MAX_ANSWER_DETAIL) return text
    val end = if (text[MAX_ANSWER_DETAIL - 1].isHighSurrogate()) MAX_ANSWER_DETAIL - 1 else MAX_ANSWER_DETAIL
    return text.substring(0, end).trimEnd()
}

fun JobDescription.toDto(): JobDto = JobDto(
    title = title,
    company = company,
    requirements = requirements.map { RequirementDto(it.id, it.text, it.type, it.priority, it.keywords) },
)

fun JobDto.toJobDescription(rawText: String): JobDescription = JobDescription(
    title = title,
    company = company,
    rawText = rawText,
    requirements = requirements.map { JobRequirement(it.id, it.text, it.type, it.priority, it.keywords) },
    location = location?.trim()?.takeIf { it.isNotEmpty() },
)

fun RequirementMatch.toDto(): MatchDto = MatchDto(requirement.id, status, evidenceIds)

fun List<MatchDto>.toRequirementMatches(job: JobDescription): List<RequirementMatch> {
    val requirementsById = job.requirements.associateBy { it.id }
    return mapNotNull { match ->
        requirementsById[match.requirementId]?.let { RequirementMatch(it, match.status, match.evidenceIds, match.reason) }
    }
}

fun TailoredBulletDto.toTailoredBullet(originalText: String): TailoredBullet = TailoredBullet(
    id = id,
    entryId = entryId,
    originalText = originalText,
    proposedText = proposedText,
    sourceIds = sourceIds,
    editTypes = editTypes.filter { it != EditType.UNKNOWN },
    keywordsUsed = keywordsUsed,
    violations = emptyList(),
    decision = BulletDecision.PENDING,
)

fun ContentReport.toRequest(): ContentReportRequest =
    ContentReportRequest(applicationId, itemKind, itemId, generationId, itemText.take(MAX_REPORT_TEXT))

private const val MAX_REPORT_TEXT = 2_000
private const val MAX_SUMMARY_LENGTH = 1_000
private const val MAX_ANSWER_DETAIL = 400
private const val SUMMARY_ID = "summary"
