package com.tailormyresume.core.data.model

import com.tailormyresume.core.database.json.GuardrailViolationDto
import com.tailormyresume.core.database.json.TailoredBulletDto
import com.tailormyresume.core.database.json.TailoredResumeDto
import com.tailormyresume.core.database.json.TailoredSkillsDto
import com.tailormyresume.core.database.json.TailoredTextDto
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText

fun GuardrailViolation.asDto(): GuardrailViolationDto = when (this) {
    GuardrailViolation.MissingSource -> GuardrailViolationDto.MissingSource
    is GuardrailViolation.UnsupportedNumber -> GuardrailViolationDto.UnsupportedNumber(value)
    is GuardrailViolation.UnsupportedTerm -> GuardrailViolationDto.UnsupportedTerm(term)
    is GuardrailViolation.VerbEscalation -> GuardrailViolationDto.VerbEscalation(from, to)
    is GuardrailViolation.UnsupportedScaleClaim -> GuardrailViolationDto.UnsupportedScaleClaim(phrase)
}

fun GuardrailViolationDto.asExternalModel(): GuardrailViolation = when (this) {
    GuardrailViolationDto.MissingSource -> GuardrailViolation.MissingSource
    is GuardrailViolationDto.UnsupportedNumber -> GuardrailViolation.UnsupportedNumber(value)
    is GuardrailViolationDto.UnsupportedTerm -> GuardrailViolation.UnsupportedTerm(term)
    is GuardrailViolationDto.VerbEscalation -> GuardrailViolation.VerbEscalation(from, to)
    is GuardrailViolationDto.UnsupportedScaleClaim -> GuardrailViolation.UnsupportedScaleClaim(phrase)
}

fun TailoredBullet.asDto() = TailoredBulletDto(
    id = id,
    entryId = entryId,
    originalText = originalText,
    proposedText = proposedText,
    sourceIds = sourceIds,
    editTypes = editTypes,
    keywordsUsed = keywordsUsed,
    violations = violations.map(GuardrailViolation::asDto),
    decision = decision,
    generationId = generationId,
)

fun TailoredBulletDto.asExternalModel() = TailoredBullet(
    id = id,
    entryId = entryId,
    originalText = originalText,
    proposedText = proposedText,
    sourceIds = sourceIds,
    editTypes = editTypes,
    keywordsUsed = keywordsUsed,
    violations = violations.map(GuardrailViolationDto::asExternalModel),
    decision = decision,
    generationId = generationId,
)

fun TailoredText.asDto() = TailoredTextDto(
    text = text,
    original = original,
    sourceIds = sourceIds,
    violations = violations.map(GuardrailViolation::asDto),
    decision = decision,
)

fun TailoredTextDto.asExternalModel() = TailoredText(
    text = text,
    original = original,
    sourceIds = sourceIds,
    violations = violations.map(GuardrailViolationDto::asExternalModel),
    decision = decision,
)

fun TailoredSkills.asDto() = TailoredSkillsDto(
    skills = skills,
    original = original,
    violations = violations.map(GuardrailViolation::asDto),
    decision = decision,
)

fun TailoredSkillsDto.asExternalModel() = TailoredSkills(
    skills = skills,
    original = original,
    violations = violations.map(GuardrailViolationDto::asExternalModel),
    decision = decision,
)

fun TailoredResume.asDto() = TailoredResumeDto(
    bullets = bullets.map(TailoredBullet::asDto),
    entryIds = entryIds,
    summary = summary?.asDto(),
    skills = skills?.asDto(),
)

fun TailoredResumeDto.asExternalModel() = TailoredResume(
    bullets = bullets.map(TailoredBulletDto::asExternalModel),
    entryIds = entryIds,
    summary = summary?.asExternalModel(),
    skills = skills?.asExternalModel(),
)
