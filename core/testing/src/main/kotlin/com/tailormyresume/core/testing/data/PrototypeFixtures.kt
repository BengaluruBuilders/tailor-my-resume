package com.tailormyresume.core.testing.data

import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import kotlin.time.Instant

data class PrototypeScenario(
    val profile: CandidateProfile,
    val applications: List<JobApplication>,
    val ledger: List<CreditLedgerEntry>,
)

object PrototypeFixtures {

    private const val NORTHWIND_JOB_TEXT = "Associate Analyst, Northwind GCC\nBengaluru · Hybrid · Full-time\n\nAbout the role\nYou'll join the Finance Analytics team supporting business partners across EMEA. You'll build and maintain Power BI dashboards, write SQL against large datasets, and turn findings into clear recommendations for senior stakeholders.\n\nWhat you'll need\n• 2+ years in analytics or finance\n• Strong SQL and Excel\n• Power BI or Tableau\n• Financial reporting and variance analysis\n• Experience presenting to senior stakeholders\n\nNice to have\n• Python\n• Forecasting models"

    private val skills = listOf(
        "SQL", "Excel", "Power BI", "Python", "Forecasting", "Tableau", "Financial modelling",
        "Data cleaning", "Dashboards", "Pandas", "Reconciliation", "Month-end close", "Looker",
        "A/B testing", "Budgeting", "Reporting", "Presenting", "Stakeholder updates",
    )

    private val achievements = listOf(
        "Cut monthly reporting prep time by 40%",
        "Built 12 dashboards used by 3 regional teams",
        "Automated reports for 5 business teams",
        "Top performer award, Infosys 2023",
        "Led intern onboarding for 6 hires",
        "Closed books 2 days faster each month",
    )

    fun fresh(): PrototypeScenario = PrototypeScenario(
        profile = profile(internEndDate = "", reviewedAt = null),
        applications = emptyList(),
        ledger = emptyList(),
    )

    fun returning(): PrototypeScenario = PrototypeScenario(
        profile = profile(internEndDate = "May 2022", reviewedAt = at(2, 9)),
        applications = listOf(
            application(
                id = "nw",
                title = "Associate Analyst",
                company = "Northwind GCC",
                location = "Bengaluru · Hybrid",
                status = ApplicationStatus.APPLIED,
                appliedDay = 8,
                match = 92,
                nowMatch = 61,
                file = "Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf",
                rawText = NORTHWIND_JOB_TEXT,
                quickAnswer = QuickAnswer(requirementId = "presenting-to-senior-leaders", choice = "YES_REGULARLY"),
            ),
            application(
                id = "kb",
                title = "Business Analyst",
                company = "Kestrel Bank",
                location = "Mumbai · On-site",
                status = ApplicationStatus.INTERVIEW,
                appliedDay = 6,
                match = 88,
                file = "Priya-Deshmukh_Kestrel-Bank_Business-Analyst.pdf",
            ),
            application(
                id = "hr",
                title = "Data Analyst",
                company = "Halden Retail",
                location = "Pune · Remote",
                status = ApplicationStatus.SAVED,
                appliedDay = null,
                match = 85,
                file = "Priya-Deshmukh_Halden-Retail_Data-Analyst.pdf",
            ),
        ),
        ledger = listOf(
            CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, at(2, 9)),
            CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "hr", null, at(4, 9)),
            CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, "application_pack_5", at(5, 9)),
            CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "kb", null, at(6, 9)),
            CreditLedgerEntry(CreditLedgerKind.SPEND, -1, "nw", null, at(8, 9)),
        ),
    )

    private fun at(day: Int, hour: Int): Instant = Instant.parse("2026-10-%02dT%02d:00:00Z".format(day, hour))

    private fun profile(internEndDate: String, reviewedAt: Instant?) = CandidateProfile(
        fullName = "Priya Deshmukh",
        email = "priya.deshmukh@gmail.com",
        phone = "+91 98200 41736",
        headline = "Finance analyst",
        skills = skills,
        entries = roles(internEndDate) + education() + achievementEntries(),
        city = "Pune",
        summary = "Finance analyst with 4 years of SQL, Power BI and Excel work across banking and retail.",
        reviewedAt = reviewedAt,
    )

    private fun roles(internEndDate: String) = listOf(
        role(
            "exp-infosys",
            "Business Analyst",
            "Infosys",
            "Jul 2022",
            "Present",
            "Built dashboards in Power BI for monthly reports, cut prep time 40%.",
            "Wrote SQL pipelines over 20M+ rows of transaction data.",
        ),
        role(
            "exp-tata",
            "Data Analyst Intern",
            "Tata Digital",
            "Jun 2021",
            internEndDate,
            "Built forecasting models in Python for category demand.",
            "Automated weekly Excel reports for 5 business teams.",
        ),
        role(
            "exp-bajaj",
            "Finance Associate",
            "Bajaj Finserv",
            "Jun 2020",
            "May 2021",
            "Reconciled ledgers and prepared month-end close packs.",
        ),
    )

    private fun role(
        id: String,
        title: String,
        organization: String,
        startDate: String,
        endDate: String,
        vararg bullets: String,
    ) = ProfileEntry(
        id = id,
        category = EntryCategory.EXPERIENCE,
        title = title,
        organization = organization,
        startDate = startDate,
        endDate = endDate,
        bullets = bullets.mapIndexed { index, text -> EvidenceBullet("$id-b${index + 1}", text) },
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )

    private fun education() = listOf(
        listItem("edu-1", EntryCategory.EDUCATION, "B.Com, Finance · Symbiosis College, Pune · 2020"),
    )

    private fun achievementEntries() = achievements.mapIndexed { index, text ->
        listItem("ach-${index + 1}", EntryCategory.ACHIEVEMENT, text)
    }

    private fun listItem(id: String, category: EntryCategory, title: String) = ProfileEntry(
        id = id,
        category = category,
        title = title,
        organization = "",
        startDate = "",
        endDate = "",
        bullets = emptyList(),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )

    private fun application(
        id: String,
        title: String,
        company: String,
        location: String,
        status: ApplicationStatus,
        appliedDay: Int?,
        match: Int,
        file: String,
        nowMatch: Int = match,
        rawText: String = "",
        quickAnswer: QuickAnswer? = null,
    ) = JobApplication(
        id = id,
        job = JobDescription(title = title, company = company, rawText = rawText, requirements = emptyList()),
        status = status,
        gapAnalysis = null,
        tailoredResume = null,
        createdAt = at(appliedDay ?: 4, 9),
        updatedAt = at(appliedDay ?: 4, 9),
        location = location,
        appliedOn = appliedDay?.let { at(it, 9) },
        keywordCoverage = ApplicationKeywordCoverage(now = nowMatch, upTo = match, final = match),
        exportFileName = file,
        quickAnswer = quickAnswer,
    )
}
