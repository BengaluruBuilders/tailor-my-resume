package com.tailormyresume.core.testing.data

import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EditType
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
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlin.time.Instant

const val CANONICAL_TAILORING_EFFECT =
    "Every bullet keeps your facts. Only the wording moves to the language of the job."

val canonicalContactFact = ProfileEntry(
    id = "P-01",
    category = EntryCategory.ACHIEVEMENT,
    title = "Contact",
    organization = "Priya Deshmukh",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(id = "P-01-b1", text = "Email priya.d@example.com."),
        EvidenceBullet(id = "P-01-b2", text = "Phone +91 90000 00000."),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalPersonFact = ProfileEntry(
    id = "P-02",
    category = EntryCategory.ACHIEVEMENT,
    title = "Priya Deshmukh",
    organization = "Pune, Maharashtra",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(id = "P-02-b1", text = "Final-year B.Tech student in Pune, Maharashtra."),
        EvidenceBullet(id = "P-02-b2", text = "Looking for an Android engineering role in a product team."),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalSkillsFact = ProfileEntry(
    id = "P-03",
    category = EntryCategory.ACHIEVEMENT,
    title = "Skills",
    organization = "Priya Deshmukh",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(
            id = "P-03-b1",
            text = "Kotlin, Java, Android SDK, Jetpack Compose, Room, Coroutines, Flow, Retrofit, SQL, Git.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalLanguagesFact = ProfileEntry(
    id = "P-04",
    category = EntryCategory.ACHIEVEMENT,
    title = "Languages",
    organization = "Priya Deshmukh",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(id = "P-04-b1", text = "English at a professional working level."),
        EvidenceBullet(id = "P-04-b2", text = "Hindi and Marathi as mother tongues."),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalAvailabilityFact = ProfileEntry(
    id = "P-05",
    category = EntryCategory.ACHIEVEMENT,
    title = "Availability",
    organization = "Priya Deshmukh",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(id = "P-05-b1", text = "Open to relocating to Bengaluru for the right role."),
        EvidenceBullet(id = "P-05-b2", text = "Available to join after the current semester ends."),
    ),
    source = FactSource.USER_STATED,
    isConfirmed = false,
)

val canonicalInternshipOne = ProfileEntry(
    id = "I-01",
    category = EntryCategory.EXPERIENCE,
    title = "Android developer intern",
    organization = "Kitebox Software, Pune",
    startDate = "2025-06",
    endDate = "2025-11",
    bullets = listOf(
        EvidenceBullet(
            id = "I-01-b1",
            text = "Wrote Android screens in Kotlin with Jetpack Compose for an internal logistics app.",
        ),
        EvidenceBullet(
            id = "I-01-b2",
            text = "Replaced a hand-written database helper with Room and added unit tests with JUnit.",
        ),
        EvidenceBullet(
            id = "I-01-b3",
            text = "Reviewed pull requests from two other interns in Git.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalInternshipTwo = ProfileEntry(
    id = "I-02",
    category = EntryCategory.EXPERIENCE,
    title = "Backend developer intern",
    organization = "Sahyadri Data Works, Pune",
    startDate = "2024-05",
    endDate = "2024-08",
    bullets = listOf(
        EvidenceBullet(
            id = "I-02-b1",
            text = "Wrote SQL queries and stored procedures for a reporting service.",
        ),
        EvidenceBullet(
            id = "I-02-b2",
            text = "Used Coroutines and Flow to stream records from a Retrofit client into a dashboard.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalProjectOne = ProfileEntry(
    id = "C-01",
    category = EntryCategory.PROJECT,
    title = "Placement Stats Dashboard",
    organization = "College training and placement cell",
    startDate = "2025",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(
            id = "C-01-b1",
            text = "Built a Power BI dashboard of three batches of placement data for the college training and placement cell.",
        ),
        EvidenceBullet(
            id = "C-01-b2",
            text = "Cleaned the placement register in Excel and wrote the SQL used to load it.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalProjectTwo = ProfileEntry(
    id = "C-02",
    category = EntryCategory.PROJECT,
    title = "Campus Events App",
    organization = "Personal project",
    startDate = "2024",
    endDate = "2024",
    bullets = listOf(
        EvidenceBullet(
            id = "C-02-b1",
            text = "Built an Android app in Kotlin that lists campus events offline with Room.",
        ),
        EvidenceBullet(
            id = "C-02-b2",
            text = "Added event reminders with WorkManager and shared a beta with classmates.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalProjectThree = ProfileEntry(
    id = "C-03",
    category = EntryCategory.PROJECT,
    title = "DBMS mini project",
    organization = "Course project",
    startDate = "2025",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(
            id = "C-03-b1",
            text = "Used a BigQuery sandbox on a public dataset for my DBMS mini project.",
        ),
    ),
    source = FactSource.USER_STATED,
    isConfirmed = false,
)

val canonicalCertification = ProfileEntry(
    id = "X-01",
    category = EntryCategory.CERTIFICATION,
    title = "Android Kotlin bootcamp",
    organization = "Pune Institute of Technology, continuing education",
    startDate = "2024",
    endDate = "2024",
    bullets = listOf(
        EvidenceBullet(
            id = "X-01-b1",
            text = "Finished a twelve-week Android Kotlin bootcamp with a final project review.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalRoleOne = ProfileEntry(
    id = "R-01",
    category = EntryCategory.EXPERIENCE,
    title = "Target role",
    organization = "Northwind GCC",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(
            id = "R-01-b1",
            text = "Applying for Associate Android Engineer at Northwind GCC in Bengaluru.",
        ),
    ),
    source = FactSource.USER_EDITED,
    isConfirmed = true,
)

val canonicalRoleTwo = ProfileEntry(
    id = "R-02",
    category = EntryCategory.EXPERIENCE,
    title = "Open source contributor",
    organization = "Compose date picker library",
    startDate = "2025",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(
            id = "R-02-b1",
            text = "Merged two pull requests that fixed Compose state handling in an open source date picker.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalRoleThree = ProfileEntry(
    id = "R-03",
    category = EntryCategory.EXPERIENCE,
    title = "Coding club lead",
    organization = "Department coding club",
    startDate = "2024",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(
            id = "R-03-b1",
            text = "Ran weekly Android study sessions for the department coding club.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalRoleFour = ProfileEntry(
    id = "R-04",
    category = EntryCategory.EXPERIENCE,
    title = "Role preference",
    organization = "Priya Deshmukh",
    startDate = "",
    endDate = "",
    bullets = listOf(
        EvidenceBullet(
            id = "R-04-b1",
            text = "Prefers product engineering work over platform or infrastructure work.",
        ),
    ),
    source = FactSource.USER_STATED,
    isConfirmed = false,
)

val canonicalEducationOne = ProfileEntry(
    id = "U-01",
    category = EntryCategory.EDUCATION,
    title = "B.Tech in Information Technology",
    organization = "University of Pune",
    startDate = "2022",
    endDate = "2026",
    bullets = listOf(
        EvidenceBullet(
            id = "U-01-b1",
            text = "Studying Information Technology at the University of Pune from 2022 to 2026.",
        ),
        EvidenceBullet(id = "U-01-b2", text = "CGPA 8.4 out of 10 at the end of the fifth semester."),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalEducationTwo = ProfileEntry(
    id = "U-02",
    category = EntryCategory.EDUCATION,
    title = "Coursework",
    organization = "University of Pune",
    startDate = "2022",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(
            id = "U-02-b1",
            text = "Coursework in databases, data structures, operating systems, and computer networks.",
        ),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalEducationThree = ProfileEntry(
    id = "U-03",
    category = EntryCategory.ACHIEVEMENT,
    title = "Batch result",
    organization = "University of Pune",
    startDate = "2025",
    endDate = "2025",
    bullets = listOf(
        EvidenceBullet(id = "U-03-b1", text = "Ranked in the top ten percent of the batch by CGPA."),
    ),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val canonicalCandidateProfile = CandidateProfile(
    fullName = "Priya Deshmukh",
    email = "priya.d@example.com",
    phone = "+91 90000 00000",
    headline = "Final-year Information Technology student in Pune, looking for an Android engineering role",
    skills = listOf("Kotlin", "Android", "Java", "Jetpack Compose", "Room", "Coroutines", "Flow", "SQL", "Git"),
    entries = listOf(
        canonicalPersonFact,
        canonicalContactFact,
        canonicalSkillsFact,
        canonicalLanguagesFact,
        canonicalAvailabilityFact,
        canonicalInternshipOne,
        canonicalInternshipTwo,
        canonicalProjectOne,
        canonicalProjectTwo,
        canonicalProjectThree,
        canonicalCertification,
        canonicalRoleOne,
        canonicalRoleTwo,
        canonicalRoleThree,
        canonicalRoleFour,
        canonicalEducationOne,
        canonicalEducationTwo,
        canonicalEducationThree,
    ),
)

val canonicalProfileWithoutEntries = canonicalCandidateProfile.copy(entries = emptyList())

val canonicalKotlinRequirement = JobRequirement(
    id = "req-kotlin",
    text = "Strong Kotlin for Android app development",
    type = RequirementType.SKILL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("kotlin", "android"),
)

val canonicalComposeRequirement = JobRequirement(
    id = "req-compose",
    text = "Jetpack Compose for modern Android user interfaces",
    type = RequirementType.SKILL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("jetpack compose"),
)

val canonicalRoomRequirement = JobRequirement(
    id = "req-room",
    text = "Local persistence with Room",
    type = RequirementType.TOOL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("room"),
)

val canonicalRetrofitRequirement = JobRequirement(
    id = "req-retrofit",
    text = "Retrofit or Ktor for network calls",
    type = RequirementType.TOOL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("retrofit", "ktor"),
)

val canonicalCoroutineRequirement = JobRequirement(
    id = "req-coroutines",
    text = "Coroutines and Flow for asynchronous work",
    type = RequirementType.SKILL,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("coroutines", "flow"),
)

val canonicalDegreeRequirement = JobRequirement(
    id = "req-degree",
    text = "B.Tech in Computer Science or Information Technology",
    type = RequirementType.EDUCATION,
    priority = RequirementPriority.MUST_HAVE,
    keywords = listOf("b.tech"),
)

val canonicalTestingRequirement = JobRequirement(
    id = "req-testing",
    text = "Unit tests written with JUnit",
    type = RequirementType.SKILL,
    priority = RequirementPriority.NICE_TO_HAVE,
    keywords = listOf("junit", "unit test"),
)

val canonicalGitRequirement = JobRequirement(
    id = "req-git",
    text = "Git and code review practice",
    type = RequirementType.TOOL,
    priority = RequirementPriority.NICE_TO_HAVE,
    keywords = listOf("git"),
)

val canonicalAgileRequirement = JobRequirement(
    id = "req-agile",
    text = "Agile delivery with JIRA",
    type = RequirementType.TOOL,
    priority = RequirementPriority.NICE_TO_HAVE,
    keywords = listOf("agile", "jira"),
)

val canonicalCommunicationRequirement = JobRequirement(
    id = "req-communication",
    text = "Clear written communication in English",
    type = RequirementType.SOFT_SKILL,
    priority = RequirementPriority.NICE_TO_HAVE,
    keywords = listOf("communication"),
)

const val CANONICAL_JOB_RAW_TEXT = """Northwind GCC is hiring an Associate Android Engineer in Bengaluru.
About the role
You will build and maintain Android features for our candidate products in Kotlin.
What we ask for
Strong Kotlin for Android app development.
Jetpack Compose for modern Android user interfaces.
Local persistence with Room.
Retrofit or Ktor for network calls.
Coroutines and Flow for asynchronous work.
B.Tech in Computer Science or Information Technology.
Nice to have
Unit tests written with JUnit.
Git and code review practice.
Agile delivery with JIRA.
Clear written communication in English.
Nothing renews and there is no subscription in this role."""

val canonicalJobDescription = JobDescription(
    title = "Associate Android Engineer",
    company = "Northwind GCC",
    rawText = CANONICAL_JOB_RAW_TEXT,
    requirements = listOf(
        canonicalKotlinRequirement,
        canonicalComposeRequirement,
        canonicalRoomRequirement,
        canonicalRetrofitRequirement,
        canonicalCoroutineRequirement,
        canonicalDegreeRequirement,
        canonicalTestingRequirement,
        canonicalGitRequirement,
        canonicalAgileRequirement,
        canonicalCommunicationRequirement,
    ),
)

val canonicalGapAnalysis = GapAnalysis(
    matches = listOf(
        RequirementMatch(
            requirement = canonicalKotlinRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("P-03-b1", "I-01-b1"),
        ),
        RequirementMatch(
            requirement = canonicalComposeRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("P-03-b1", "I-01-b1"),
        ),
        RequirementMatch(
            requirement = canonicalRoomRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("P-03-b1", "C-02-b1"),
        ),
        RequirementMatch(
            requirement = canonicalRetrofitRequirement,
            status = MatchStatus.PARTIAL,
            evidenceIds = listOf("I-02-b2"),
        ),
        RequirementMatch(
            requirement = canonicalCoroutineRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("P-03-b1", "I-02-b2"),
        ),
        RequirementMatch(
            requirement = canonicalDegreeRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("U-01-b1"),
        ),
        RequirementMatch(
            requirement = canonicalTestingRequirement,
            status = MatchStatus.PARTIAL,
            evidenceIds = listOf("I-01-b2"),
        ),
        RequirementMatch(
            requirement = canonicalGitRequirement,
            status = MatchStatus.MET,
            evidenceIds = listOf("P-03-b1", "I-01-b3"),
        ),
        RequirementMatch(
            requirement = canonicalAgileRequirement,
            status = MatchStatus.GAP,
            evidenceIds = emptyList(),
        ),
        RequirementMatch(
            requirement = canonicalCommunicationRequirement,
            status = MatchStatus.GAP,
            evidenceIds = emptyList(),
        ),
    ),
    keywordCoverage = KeywordCoverage(covered = 10, total = 15),
)

val canonicalTailoredResume = TailoredResume(
    bullets = listOf(
        TailoredBullet(
            id = "tailored-1",
            entryId = "I-01",
            originalText = "Wrote Android screens in Kotlin with Jetpack Compose for an internal logistics app.",
            proposedText = "Wrote Android screens in Kotlin with Jetpack Compose for an internal logistics Android app.",
            sourceIds = listOf("I-01-b1"),
            editTypes = listOf(EditType.EMPHASISE),
            keywordsUsed = listOf("Android", "Kotlin", "Jetpack Compose"),
            violations = emptyList(),
            decision = BulletDecision.ACCEPTED,
        ),
        TailoredBullet(
            id = "tailored-2",
            entryId = "C-02",
            originalText = "Built an Android app in Kotlin that lists campus events offline with Room.",
            proposedText = "Built an Android app in Kotlin that lists campus events offline, with Room as the local store.",
            sourceIds = listOf("C-02-b1"),
            editTypes = listOf(EditType.REWORD),
            keywordsUsed = listOf("Android", "Kotlin", "Room"),
            violations = emptyList(),
            decision = BulletDecision.PENDING,
        ),
        TailoredBullet(
            id = "tailored-3",
            entryId = "I-02",
            originalText = "Used Coroutines and Flow to stream records from a Retrofit client into a dashboard.",
            proposedText = "Used Coroutines and Flow with a Retrofit client to stream records into a dashboard.",
            sourceIds = listOf("I-02-b2"),
            editTypes = listOf(EditType.REWORD),
            keywordsUsed = listOf("Coroutines", "Flow", "Retrofit"),
            violations = emptyList(),
            decision = BulletDecision.PENDING,
        ),
        TailoredBullet(
            id = "tailored-4",
            entryId = "C-01",
            originalText = "Built a Power BI dashboard of three batches of placement data for the college training and placement cell.",
            proposedText = "Built a Power BI dashboard of three batches of placement data for the college training and placement cell.",
            sourceIds = listOf("C-01-b1"),
            editTypes = emptyList(),
            keywordsUsed = emptyList(),
            violations = emptyList(),
            decision = BulletDecision.REJECTED,
        ),
    ),
)

val canonicalApplication = JobApplication(
    id = "application-northwind-1",
    job = canonicalJobDescription,
    status = ApplicationStatus.SAVED,
    gapAnalysis = canonicalGapAnalysis,
    tailoredResume = canonicalTailoredResume,
    createdAt = Instant.fromEpochSeconds(1_773_158_400),
    updatedAt = Instant.fromEpochSeconds(1_773_158_400),
)
