package com.tailormyresume.app.ai

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit

const val CANDIDATE_NAME = "Priya Deshmukh"
const val CANDIDATE_EMAIL = "priya.deshmukh@example.com"
const val CANDIDATE_PHONE = "+91 98123 45610"
const val FACT_ID = "W-01-b1"
const val FACT_TEXT = "Cleaned weekly sales data for 40 stores in Excel."

fun confirmedEntry(id: String = "W-01", factId: String = FACT_ID, text: String = FACT_TEXT) = ProfileEntry(
    id = id,
    category = EntryCategory.EXPERIENCE,
    title = "Data Operations Associate",
    organization = "Saffron Retail",
    startDate = "Jul 2024",
    endDate = "Present",
    bullets = listOf(EvidenceBullet(factId, text)),
    source = FactSource.IMPORTED,
    isConfirmed = true,
)

val candidate = CandidateProfile(
    fullName = CANDIDATE_NAME,
    email = CANDIDATE_EMAIL,
    phone = CANDIDATE_PHONE,
    headline = "Data analyst",
    skills = listOf("SQL"),
    entries = listOf(confirmedEntry()),
)

val requirement = JobRequirement("req-1", "Strong SQL", RequirementType.SKILL, RequirementPriority.MUST_HAVE, listOf("sql"))

val job = JobDescription("Associate Analyst", "Northwind GCC", "About the role ".repeat(10), listOf(requirement))

fun matchOf(status: MatchStatus = MatchStatus.MET, vararg evidence: String) =
    RequirementMatch(requirement, status, evidence.toList())

val noToken = object : IdTokenProvider {
    override fun idToken(forceRefresh: Boolean): String? = null
}

class FakeBackend {
    val server = MockWebServer().apply { start() }

    val api: TailorMyResumeApi = tailormyresumeApi(
        TailorMyResumeApiConfig(server.url("/").toString().trimEnd('/')),
        tailormyresumeOkHttpClient(noToken).newBuilder().readTimeout(SHORT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build(),
        tailormyresumeJson(),
    )

    fun reply(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    fun fail(code: Int, errorCode: String) = reply(code, """{"error":{"code":"$errorCode","message":"x"}}""")

    fun shutdown() = runCatching { server.shutdown() }.let { }

    private companion object {
        const val SHORT_READ_TIMEOUT_MILLIS = 400L
    }
}

const val RESUME_PARSE_RESPONSE = """{"generationId":"g-parse","profile":{"fullName":"Priya Deshmukh","email":"priya.deshmukh@example.com",
"phone":"+91 98123 45610","headline":"Data analyst","skills":["SQL"],"entries":[{"ref":"e1","category":"EXPERIENCE",
"title":"Data Operations Associate","organization":"Saffron Retail","startDate":"Jul 2024","endDate":"Present",
"bullets":[{"ref":"e1b1","text":"Cleaned weekly sales data for 40 stores in Excel."},{"ref":"e1b2","text":"  "}]}]},
"droppedSensitive":["DATE_OF_BIRTH"]}"""

const val ANALYSIS_RESPONSE = """{"generationId":"g-analysis","job":{"title":"Associate Analyst","company":"Northwind GCC",
"requirements":[{"id":"req-1","text":"Strong SQL","type":"SKILL","priority":"MUST_HAVE","keywords":["sql"]}]},
"matches":[{"requirementId":"req-1","status":"MET","evidenceIds":["W-01-b1"]}],
"allowance":{"analysesLeftToday":2,"day":"2026-10-07","resetsAt":"2026-10-07T18:30:00Z"}}"""

fun tailoringBody(status: String, extra: String = "") =
    """{"tailoring":{"id":"tl_1","status":"$status"$extra}}"""

fun tailoringResult(proposedText: String) =
    ""","result":{"generationId":"g-tailor","bullets":[{"id":"t-1","entryId":"W-01","sourceIds":["W-01-b1"],
"proposedText":"$proposedText","editTypes":["REWORD"],"keywordsUsed":["excel"],"verification":"PASSED"}]}"""
