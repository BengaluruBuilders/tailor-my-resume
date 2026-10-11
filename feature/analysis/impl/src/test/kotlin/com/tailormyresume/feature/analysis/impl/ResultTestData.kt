package com.tailormyresume.feature.analysis.impl

import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.QuickQuestion
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredResume
import kotlin.time.Instant

internal object ResultTestData {
    const val APP_ID = "app-1"
    const val STAKEHOLDER_ID = "r-stake"
    const val STAKEHOLDER_DETAIL = "Ran stakeholder management for the quarterly finance review"

    private fun requirement(
        id: String,
        text: String,
        type: RequirementType,
        priority: RequirementPriority,
        vararg keywords: String,
    ) = JobRequirement(id, text, type, priority, keywords.toList())

    val sql = requirement("r-sql", "Strong SQL and Excel", RequirementType.SKILL, RequirementPriority.MUST_HAVE, "sql", "excel")
    val powerBi = requirement("r-bi", "Power BI or Tableau", RequirementType.TOOL, RequirementPriority.MUST_HAVE, "power bi", "tableau")
    val stakeholders = requirement(
        STAKEHOLDER_ID,
        "Experience presenting to senior stakeholders",
        RequirementType.SOFT_SKILL,
        RequirementPriority.MUST_HAVE,
        "stakeholder management",
    )
    val python = requirement("r-py", "Python", RequirementType.SKILL, RequirementPriority.NICE_TO_HAVE, "python")

    val matches = listOf(
        RequirementMatch(sql, MatchStatus.MET, listOf("e1"), "Used SQL daily at your internship"),
        RequirementMatch(powerBi, MatchStatus.PARTIAL, listOf("e2"), "Power BI at your internship, no Tableau"),
        RequirementMatch(stakeholders, MatchStatus.GAP, emptyList(), null),
        RequirementMatch(python, MatchStatus.GAP, emptyList(), "Not in your resume"),
    )

    val profile = CandidateProfile(
        fullName = "Candidate",
        email = "",
        phone = "",
        headline = "",
        skills = emptyList(),
        entries = listOf(
            ProfileEntry(
                "e1", EntryCategory.EXPERIENCE, "Analyst", "Company", "2020", "2021",
                listOf(EvidenceBullet("e1-b1", "Used SQL and Excel")), FactSource.IMPORTED, true,
            ),
            ProfileEntry(
                "e2", EntryCategory.EXPERIENCE, "Analyst", "Company", "2021", "2022",
                listOf(EvidenceBullet("e2-b1", "Built Power BI dashboards")), FactSource.IMPORTED, true,
            ),
        ),
    )

    val question = QuickQuestion(
        requirementId = STAKEHOLDER_ID,
        text = "Have you presented to senior leaders?",
        why = "It's a must-have for this role, and your resume doesn't mention it.",
    )

    fun application(
        id: String = APP_ID,
        matches: List<RequirementMatch> = ResultTestData.matches,
        question: QuickQuestion? = ResultTestData.question,
        quickAnswer: QuickAnswer? = null,
        location: String = "Bengaluru · Hybrid",
        tailoredResume: TailoredResume? = null,
    ) = JobApplication(
        id = id,
        job = JobDescription(
            title = "Associate Analyst",
            company = "Northwind GCC",
            rawText = "Associate Analyst at Northwind GCC",
            requirements = matches.map { it.requirement },
            location = "Pune",
        ),
        status = ApplicationStatus.SAVED,
        gapAnalysis = GapAnalysis(matches, KeywordCoverage(0, 0), question = question),
        tailoredResume = tailoredResume,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        location = location,
        quickAnswer = quickAnswer,
    )
}
