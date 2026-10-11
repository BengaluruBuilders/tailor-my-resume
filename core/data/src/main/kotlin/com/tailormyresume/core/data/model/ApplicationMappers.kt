package com.tailormyresume.core.data.model

import com.tailormyresume.core.database.json.JobRequirementDto
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.QuickAnswer

fun JobApplication.asEntity() = JobApplicationEntity(
    id = id,
    jobTitle = job.title,
    company = job.company,
    rawText = job.rawText,
    requirements = job.requirements.map(JobRequirement::asDto),
    status = status,
    gapAnalysis = gapAnalysis?.asDto(),
    tailoredResume = tailoredResume?.asDto(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    location = location,
    appliedOn = appliedOn,
    coverageNow = keywordCoverage?.now,
    coverageUpTo = keywordCoverage?.upTo,
    coverageFinal = keywordCoverage?.final,
    exportFileName = exportFileName,
    quickAnswerRequirementId = quickAnswer?.requirementId,
    quickAnswerChoice = quickAnswer?.choice,
    quickAnswerDetail = quickAnswer?.detail,
    changesAcceptedAt = changesAcceptedAt,
    notes = legacyNotes,
    legacyStatus = legacyStatus,
)

fun JobApplicationEntity.asExternalModel() = JobApplication(
    id = id,
    job = JobDescription(
        title = jobTitle,
        company = company,
        rawText = rawText,
        requirements = requirements.map(JobRequirementDto::asExternalModel),
    ),
    status = status,
    gapAnalysis = gapAnalysis?.asExternalModel(),
    tailoredResume = tailoredResume?.asExternalModel(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    location = location,
    appliedOn = appliedOn,
    keywordCoverage = keywordCoverage(),
    exportFileName = exportFileName,
    quickAnswer = quickAnswer(),
    changesAcceptedAt = changesAcceptedAt,
    legacyNotes = notes,
    legacyStatus = legacyStatus,
)

private fun JobApplicationEntity.keywordCoverage(): ApplicationKeywordCoverage? {
    val now = coverageNow ?: return null
    val upTo = coverageUpTo ?: return null
    return ApplicationKeywordCoverage(now = now, upTo = upTo, final = coverageFinal)
}

private fun JobApplicationEntity.quickAnswer(): QuickAnswer? {
    val requirementId = quickAnswerRequirementId ?: return null
    val choice = quickAnswerChoice ?: return null
    return QuickAnswer(requirementId = requirementId, choice = choice, detail = quickAnswerDetail.orEmpty())
}
