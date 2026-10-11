package com.tailormyresume.core.data.model

import com.tailormyresume.core.database.json.GapAnalysisDto
import com.tailormyresume.core.database.json.JobRequirementDto
import com.tailormyresume.core.database.json.KeywordCoverageDto
import com.tailormyresume.core.database.json.QuickQuestionDto
import com.tailormyresume.core.database.json.RequirementMatchDto
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.QuickQuestion
import com.tailormyresume.core.model.RequirementMatch

fun JobRequirement.asDto() = JobRequirementDto(
    id = id,
    text = text,
    type = type,
    priority = priority,
    keywords = keywords,
)

fun JobRequirementDto.asExternalModel() = JobRequirement(
    id = id,
    text = text,
    type = type,
    priority = priority,
    keywords = keywords,
)

fun RequirementMatch.asDto() = RequirementMatchDto(
    requirement = requirement.asDto(),
    status = status,
    evidenceIds = evidenceIds,
    reason = reason,
)

fun RequirementMatchDto.asExternalModel() = RequirementMatch(
    requirement = requirement.asExternalModel(),
    status = status,
    evidenceIds = evidenceIds,
    reason = reason,
)

fun KeywordCoverage.asDto() = KeywordCoverageDto(covered = covered, total = total)

fun KeywordCoverageDto.asExternalModel() = KeywordCoverage(covered = covered, total = total)

fun GapAnalysis.asDto() = GapAnalysisDto(
    matches = matches.map(RequirementMatch::asDto),
    keywordCoverage = keywordCoverage.asDto(),
    generationId = generationId,
    question = question?.asDto(),
)

fun GapAnalysisDto.asExternalModel() = GapAnalysis(
    matches = matches.map(RequirementMatchDto::asExternalModel),
    keywordCoverage = keywordCoverage.asExternalModel(),
    generationId = generationId,
    question = question?.asExternalModel(),
)

fun QuickQuestion.asDto() = QuickQuestionDto(requirementId = requirementId, text = text, why = why)

fun QuickQuestionDto.asExternalModel() = QuickQuestion(requirementId = requirementId, text = text, why = why)
