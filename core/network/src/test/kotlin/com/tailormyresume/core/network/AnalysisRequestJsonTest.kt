package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.dto.AnswerChoice
import com.tailormyresume.core.network.dto.TailoringStartRequest
import com.tailormyresume.core.network.mapper.toAnswerDto
import com.tailormyresume.core.network.mapper.toDto
import com.tailormyresume.core.network.mapper.toFactsDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class AnalysisRequestJsonTest {
    private val json = tailormyresumeJson()
    private val requirement = JobRequirement("req-1", "Strong SQL", RequirementType.SKILL, RequirementPriority.MUST_HAVE, listOf("sql"))
    private val job = JobDescription("Analyst", "Northwind", "raw", listOf(requirement), location = "Bengaluru · Hybrid")
    private val match = RequirementMatch(requirement, MatchStatus.MET, listOf("E1-1"), reason = "SQL in your role.")
    private val profile = CandidateProfile("P", "p@example.com", "+91", "H", listOf("SQL"), emptyList(), summary = "A summary.")

    private fun tailoringBody(answer: QuickAnswer?): String = json.encodeToString(
        TailoringStartRequest.serializer(),
        TailoringStartRequest(
            requestId = "r-1",
            applicationId = "app-1",
            job = job.toDto(),
            matches = listOf(match.toDto()),
            profile = profile.toFactsDto(),
            answer = answer?.toAnswerDto(job),
        ),
    )

    @Test
    fun requestOmitsReasonLocationSectionAndNullAnswer() {
        val tailoring = tailoringBody(null)
        val analysis = json.encodeToString(AnalysisRequest.serializer(), AnalysisRequest("jd text", profile.toFactsDto()))

        listOf("reason", "location", "section", "answer", "null").forEach {
            assertThat(tailoring).doesNotContain(it)
            assertThat(analysis).doesNotContain(it)
        }
        assertThat(tailoring).contains(""""requirementId":"req-1"""")
    }

    @Test
    fun blankDetailIsSentAsNullNotOmitted() {
        listOf("NOT_YET", "SKIPPED").forEach { choice ->
            val body = tailoringBody(QuickAnswer("req-1", choice, "  "))

            assertThat(body).contains(""""answer":{"requirementId":"req-1","choice":"$choice","detail":null}""")
        }
    }

    @Test
    fun everyAnswerChoiceCarriesTheThreeKeysTheServerRequires() {
        AnswerChoice.entries.forEach { choice ->
            listOf("", "Led SQL reporting.").forEach { detail ->
                val request = Json.parseToJsonElement(tailoringBody(QuickAnswer("req-1", choice.name, detail))).jsonObject
                val answer = request.getValue("answer").jsonObject

                assertThat(answer.keys).containsExactly("requirementId", "choice", "detail")
                assertThat(answer.getValue("choice").jsonPrimitive.content).isEqualTo(choice.name)
                assertThat(request.keys).containsExactly("requestId", "applicationId", "job", "matches", "profile", "answer")
            }
        }
    }

    @Test
    fun requiredKeysOfEveryOtherObjectAreSent() {
        val request = Json.parseToJsonElement(tailoringBody(null)).jsonObject

        assertThat(request.keys).containsExactly("requestId", "applicationId", "job", "matches", "profile")
        assertThat(request.getValue("profile").jsonObject.keys).containsExactly("skills", "entries", "summary")
        assertThat(request.getValue("job").jsonObject.keys).containsExactly("title", "company", "requirements")
        assertThat(request.getValue("matches").jsonArray.single().jsonObject.keys)
            .containsExactly("requirementId", "status", "evidenceIds")
        assertThat(request.getValue("job").jsonObject.getValue("requirements").jsonArray.single().jsonObject.keys)
            .containsExactly("id", "text", "type", "priority", "keywords")
    }
}
