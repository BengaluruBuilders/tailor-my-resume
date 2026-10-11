package com.tailormyresume.feature.tailor.impl

import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlin.time.Instant

internal fun testBullet(
    id: String,
    entryId: String = "exp-1",
    original: String = "Built an internal tool",
    proposed: String = "Developed an internal tool",
    sourceIds: List<String> = listOf("src-$id"),
    violations: List<GuardrailViolation> = emptyList(),
    decision: BulletDecision = BulletDecision.PENDING,
    editTypes: List<EditType> = if (original == proposed) emptyList() else listOf(EditType.REWORD),
): TailoredBullet = TailoredBullet(
    id = id,
    entryId = entryId,
    originalText = original,
    proposedText = proposed,
    sourceIds = sourceIds,
    editTypes = editTypes,
    keywordsUsed = emptyList(),
    violations = violations,
    decision = decision,
)

internal fun testEntry(
    id: String,
    category: EntryCategory = EntryCategory.EXPERIENCE,
    isConfirmed: Boolean = true,
    bullets: List<EvidenceBullet> = emptyList(),
    title: String = "Title $id",
    organization: String = "Org $id",
    startDate: String = "Jan 2024",
    endDate: String = "Present",
): ProfileEntry = ProfileEntry(
    id = id,
    category = category,
    title = title,
    organization = organization,
    startDate = startDate,
    endDate = endDate,
    bullets = bullets,
    source = FactSource.IMPORTED,
    isConfirmed = isConfirmed,
)

internal fun testProfile(
    entries: List<ProfileEntry>,
    skills: List<String> = listOf("Kotlin", "SQL"),
): CandidateProfile = CandidateProfile(
    fullName = "Priya Sharma",
    email = "priya@example.com",
    phone = "+91 98765 43210",
    headline = "Backend developer",
    skills = skills,
    entries = entries,
)

internal fun testApplication(
    bullets: List<TailoredBullet>,
    gapAnalysis: GapAnalysis? = null,
    entryIds: List<String>? = null,
): JobApplication = JobApplication(
    id = "app-1",
    job = JobDescription(title = "Backend Engineer", company = "Acme", rawText = "", requirements = emptyList()),
    status = ApplicationStatus.SAVED,
    gapAnalysis = gapAnalysis,
    tailoredResume = TailoredResume(bullets, entryIds),
    createdAt = Instant.fromEpochSeconds(0),
    updatedAt = Instant.fromEpochSeconds(0),
)

internal fun sourceBullets(vararg ids: String): List<EvidenceBullet> =
    ids.map { EvidenceBullet(id = "src-$it", text = "Source text of $it") }

internal fun evidenceOf(bullet: TailoredBullet): EvidenceBullet =
    EvidenceBullet(id = bullet.sourceIds.first(), text = bullet.originalText)

internal fun entryFor(
    id: String,
    vararg bullets: TailoredBullet,
    category: EntryCategory = EntryCategory.EXPERIENCE,
): ProfileEntry = testEntry(id, category, bullets = bullets.map(::evidenceOf))
