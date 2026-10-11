package com.tailormyresume.feature.tailor.impl

import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import kotlin.time.Instant

internal val EXPORT_ACCEPTED_AT: Instant = Instant.fromEpochSeconds(1_789_000_000)

internal fun acceptedApplication(
    bullets: List<TailoredBullet>,
    summary: TailoredText? = null,
    skills: TailoredSkills? = null,
    status: ApplicationStatus = ApplicationStatus.SAVED,
    appliedOn: Instant? = null,
    accepted: Boolean = true,
    title: String = "Associate Analyst",
    company: String = "Northwind GCC",
): JobApplication = testApplication(bullets, entryIds = listOf("exp-1")).copy(
    job = JobDescription(title = title, company = company, rawText = "", requirements = emptyList()),
    status = status,
    appliedOn = appliedOn,
    tailoredResume = TailoredResume(bullets = bullets, entryIds = listOf("exp-1"), summary = summary, skills = skills),
    changesAcceptedAt = if (accepted) EXPORT_ACCEPTED_AT else null,
)

internal fun acceptedBullet(
    id: String,
    original: String,
    proposed: String,
    sourceId: String = "src-$id",
    decision: BulletDecision = BulletDecision.ACCEPTED,
): TailoredBullet = testBullet(id, original = original, proposed = proposed, sourceIds = listOf(sourceId), decision = decision)
