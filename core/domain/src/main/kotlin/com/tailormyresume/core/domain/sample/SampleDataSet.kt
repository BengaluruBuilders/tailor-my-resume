package com.tailormyresume.core.domain.sample

import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.SignInAccount

internal enum class ReviewPlan { ALL, PARTIAL, NONE }

internal data class SampleApplicationPlan(
    val id: String,
    val status: ApplicationStatus,
    val createdDaysAgo: Int,
    val updatedDaysAgo: Int,
    val jobText: String,
    val review: ReviewPlan,
)

internal data class SampleExportPlan(
    val applicationId: String,
    val format: ExportFormat,
    val fileName: String,
    val exportedDaysAgo: Int,
)

internal object SampleDataSet {

    val account: SignInAccount = SignInAccount.localAccount

    const val PURCHASED_PACK_ID = ApplicationPack.APPLICATION_PACK_FIVE

    val profile = CandidateProfile(
        fullName = "Priya Deshmukh",
        email = "priya.d@example.com",
        phone = "+91 98220 41873",
        headline = "Data Operations Associate",
        skills = listOf(
            "SQL",
            "Excel",
            "Power BI",
            "Python",
            "Data cleaning",
            "Reporting",
            "Database management systems",
            "Communication",
        ),
        entries = listOf(
            entry(
                id = "E-01",
                category = EntryCategory.EDUCATION,
                title = "B.Tech Computer Science",
                organization = "Deccan Institute of Technology, Pune",
                startDate = "2020",
                endDate = "2024",
                source = FactSource.IMPORTED,
                "Completed a B.Tech in Computer Science with a CGPA of 8.2.",
                "Studied database management systems, including SQL, normalisation and transactions.",
                "Completed coursework in data structures and operating systems.",
            ),
            entry(
                id = "W-01",
                category = EntryCategory.EXPERIENCE,
                title = "Data Operations Associate",
                organization = "Saffron Retail, Pune",
                startDate = "Jul 2024",
                endDate = "Jun 2025",
                source = FactSource.IMPORTED,
                "Cleaned and validated daily sales data from 40 stores using Excel and SQL.",
                "Built weekly stock reports in Power BI for the category managers.",
                "Wrote SQL queries to find missing and duplicate product records and fixed them with the catalogue team.",
                "Automated a weekly sales summary in Excel so the team no longer copied figures by hand.",
            ),
            entry(
                id = "I-01",
                category = EntryCategory.EXPERIENCE,
                title = "Data Intern",
                organization = "Kiran Agro Exports, Pune",
                startDate = "May 2023",
                endDate = "Jul 2023",
                source = FactSource.IMPORTED,
                "Prepared export shipment data in Excel for the finance team.",
                "Checked invoices against shipping records and flagged mismatches.",
                "Built a Power BI view that tracked shipment delays by port.",
            ),
            entry(
                id = "P-01",
                category = EntryCategory.PROJECT,
                title = "Placement Stats Dashboard",
                organization = "College project",
                startDate = "Aug 2023",
                endDate = "Dec 2023",
                source = FactSource.USER_STATED,
                "Built a Power BI dashboard that shows placement statistics by branch and year.",
                "Designed a MySQL database to store student, company and offer records.",
                "Wrote SQL queries to work out the placement rate and the average package for each branch.",
            ),
            entry(
                id = "X-01",
                category = EntryCategory.ACHIEVEMENT,
                title = "Smart City Data Hackathon finalist",
                organization = "Pune",
                startDate = "Feb 2024",
                endDate = "Feb 2024",
                source = FactSource.USER_STATED,
                "Reached the final round of a 24-hour data hackathon in a team of four.",
                "Presented a bus delay analysis, built with Python and pandas, to a panel of judges.",
            ),
        ),
    )

    val applications = listOf(
        SampleApplicationPlan(
            id = "sample-northwind-associate-analyst",
            status = ApplicationStatus.APPLIED,
            createdDaysAgo = 9,
            updatedDaysAgo = 3,
            review = ReviewPlan.ALL,
            jobText = """
                Job title: Associate Analyst
                Company: Northwind GCC
                Location: Pune
                About the role
                You will work with the operations team to turn daily business data into clear reports.
                What we ask for
                Strong SQL for querying and cleaning business data.
                Advanced Excel for reports and summaries.
                Power BI or Tableau for dashboards.
                Basic Python for data work.
                B.Tech or any degree in Computer Science, Statistics or a related field.
                Clear written and spoken communication in English.
                Nice to have
                Experience with data cleaning and validation.
                Knowledge of database management systems.
                Experience with retail or supply chain data.
            """.trimIndent(),
        ),
        SampleApplicationPlan(
            id = "sample-paisa-ledger-data-analyst-intern",
            status = ApplicationStatus.INTERVIEW,
            createdDaysAgo = 6,
            updatedDaysAgo = 1,
            review = ReviewPlan.PARTIAL,
            jobText = """
                Job title: Data Analyst Intern
                Company: Paisa Ledger
                Location: Bengaluru
                About the role
                Join the payments data team for a six month internship.
                What we ask for
                Good SQL skills for joins, filters and aggregation.
                Experience with Excel or Google Sheets.
                Basic Python with pandas.
                Experience building dashboards.
                A degree in Computer Science, Engineering or Statistics.
                Nice to have
                A project or hackathon that used real data.
                Interest in payments or finance.
                Clear communication with people from other teams.
            """.trimIndent(),
        ),
        SampleApplicationPlan(
            id = "sample-sahyadri-motors-graduate-engineer-trainee",
            status = ApplicationStatus.SAVED,
            createdDaysAgo = 0,
            updatedDaysAgo = 0,
            review = ReviewPlan.NONE,
            jobText = """
                Job title: Graduate Engineer Trainee
                Company: Sahyadri Motors
                Location: Pune
                About the role
                A two year programme across plant operations and quality.
                What we ask for
                B.E. or B.Tech in Mechanical, Electrical or Computer Science.
                Good Excel skills for production reports.
                Basic knowledge of quality processes.
                Ability to work in a team on the shop floor.
                Willingness to work in shifts.
                Nice to have
                Experience with SQL or data reporting.
                A project that solved a real business problem.
            """.trimIndent(),
        ),
        SampleApplicationPlan(
            id = "sample-meridian-business-analyst",
            status = ApplicationStatus.APPLIED,
            createdDaysAgo = 24,
            updatedDaysAgo = 14,
            review = ReviewPlan.NONE,
            jobText = """
                Job title: Business Analyst
                Company: Meridian GCC
                Location: Hyderabad
                About the role
                You will work with business teams to define reports and improve processes.
                What we ask for
                Strong SQL and Excel skills.
                Experience in requirement gathering and process documentation.
                Experience with JIRA or a similar tool.
                Clear presentation and stakeholder communication.
                A degree in Engineering, Commerce or a related field.
                Nice to have
                Experience with Power BI.
                Knowledge of Agile delivery.
            """.trimIndent(),
        ),
    )

    val keptJob = KeptJobDescription(
        text = applications.first().jobText,
        company = "Northwind GCC",
        role = "Associate Analyst",
    )

    val exports = listOf(
        SampleExportPlan(
            applicationId = "sample-northwind-associate-analyst",
            format = ExportFormat.PDF,
            fileName = "Priya_Deshmukh_Associate_Analyst.pdf",
            exportedDaysAgo = 4,
        ),
        SampleExportPlan(
            applicationId = "sample-northwind-associate-analyst",
            format = ExportFormat.DOCX,
            fileName = "Priya_Deshmukh_Associate_Analyst.docx",
            exportedDaysAgo = 3,
        ),
    )

    private fun entry(
        id: String,
        category: EntryCategory,
        title: String,
        organization: String,
        startDate: String,
        endDate: String,
        source: FactSource,
        vararg bullets: String,
    ) = ProfileEntry(
        id = id,
        category = category,
        title = title,
        organization = organization,
        startDate = startDate,
        endDate = endDate,
        bullets = bullets.mapIndexed { index, text -> EvidenceBullet(id = "$id-b${index + 1}", text = text) },
        source = source,
        isConfirmed = true,
    )
}
