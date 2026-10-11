package com.tailormyresume.core.data.model

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
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlin.time.Instant

val everyViolation: List<GuardrailViolation> = listOf(
    GuardrailViolation.MissingSource,
    GuardrailViolation.UnsupportedNumber("40%"),
    GuardrailViolation.UnsupportedTerm("Kubernetes"),
    GuardrailViolation.VerbEscalation(from = "assisted", to = "led"),
    GuardrailViolation.UnsupportedScaleClaim("millions of users"),
)

val testEntries = listOf(
    ProfileEntry(
        id = "exp-1",
        category = EntryCategory.EXPERIENCE,
        title = "Android Developer",
        organization = "Acme",
        startDate = "2022-01",
        endDate = "2024-06",
        bullets = listOf(
            EvidenceBullet("b-1", "Built the checkout screen in Compose"),
            EvidenceBullet("b-2", "Cut crash rate by 12%"),
        ),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    ),
    ProfileEntry(
        id = "edu-1",
        category = EntryCategory.EDUCATION,
        title = "B.Tech Computer Science",
        organization = "State University",
        startDate = "2018",
        endDate = "2022",
        bullets = emptyList(),
        source = FactSource.USER_EDITED,
        isConfirmed = false,
    ),
    ProfileEntry(
        id = "user-stated",
        category = EntryCategory.ACHIEVEMENT,
        title = "Additional experience",
        organization = "",
        startDate = "",
        endDate = "",
        bullets = listOf(EvidenceBullet("b-3", "Mentored two interns")),
        source = FactSource.USER_STATED,
        isConfirmed = true,
    ),
)

val testProfile = CandidateProfile(
    fullName = "Asha Rao",
    email = "asha@example.com",
    phone = "+91 90000 00000",
    headline = "Android developer",
    skills = listOf("Kotlin", "Compose", "Room"),
    entries = testEntries,
)

val testRequirement = JobRequirement(
    id = "r-1",
    text = "3+ years of Kotlin",
    type = RequirementType.SKILL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("kotlin"),
)

val testJob = JobDescription(
    title = "Senior Android Engineer",
    company = "Globex",
    rawText = "We need a senior Android engineer with Kotlin.",
    requirements = listOf(
        testRequirement,
        testRequirement.copy(
            id = "r-2",
            text = "Kubernetes",
            type = RequirementType.TOOL,
            priority = RequirementPriority.NICE_TO_HAVE,
            keywords = listOf("kubernetes", "k8s"),
        ),
    ),
)

val testGapAnalysis = GapAnalysis(
    matches = listOf(
        RequirementMatch(testRequirement, MatchStatus.MET, listOf("b-1")),
        RequirementMatch(testJob.requirements[1], MatchStatus.GAP, emptyList()),
    ),
    keywordCoverage = KeywordCoverage(covered = 1, total = 3),
    generationId = "gen-analysis",
)

val testTailoredResume = TailoredResume(
    bullets = listOf(
        TailoredBullet(
            id = "t-1",
            entryId = "exp-1",
            originalText = "Built the checkout screen in Compose",
            proposedText = "Built the Kotlin checkout screen in Compose",
            sourceIds = listOf("b-1"),
            editTypes = listOf(EditType.REWORD, EditType.EMPHASISE),
            keywordsUsed = listOf("kotlin"),
            violations = emptyList(),
            decision = BulletDecision.ACCEPTED,
            generationId = "gen-tailor",
        ),
        TailoredBullet(
            id = "t-2",
            entryId = "exp-1",
            originalText = "Cut crash rate by 12%",
            proposedText = "Cut crash rate by 40%",
            sourceIds = listOf("b-2"),
            editTypes = emptyList(),
            keywordsUsed = emptyList(),
            violations = everyViolation,
            decision = BulletDecision.REJECTED,
        ),
    ),
    entryIds = listOf("exp-1", "edu-1"),
)

val testApplication = JobApplication(
    id = "app-1",
    job = testJob,
    status = ApplicationStatus.APPLIED,
    gapAnalysis = testGapAnalysis,
    tailoredResume = testTailoredResume,
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
    updatedAt = Instant.fromEpochMilliseconds(1_700_000_500_000),
)

val testBareApplication = testApplication.copy(
    id = "app-2",
    gapAnalysis = null,
    tailoredResume = null,
)
