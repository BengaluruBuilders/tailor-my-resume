package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.dto.AnalysisResponse
import com.tailormyresume.core.network.dto.BulletVerification
import com.tailormyresume.core.network.dto.LinkKind
import com.tailormyresume.core.network.dto.ResumeParseResponse
import com.tailormyresume.core.network.dto.TailoringFailureCode
import com.tailormyresume.core.network.dto.TailoringResponse
import org.junit.Test

class ContractV2DecodeTest {
    private val json = tailormyresumeJson()

    @Test
    fun parseResponseDecodesSummaryLocationLinksAndCurrent() {
        val response = json.decodeFromString<ResumeParseResponse>(
            """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,
"summary":"S","location":"Pune","links":[{"kind":"LINKEDIN","url":"https://l"},{"kind":"FUTURE","url":"https://f"}],
"skills":[],"entries":[{"ref":"e1","category":"EXPERIENCE","title":"T","organization":null,"startDate":null,
"endDate":null,"current":true,"bullets":[]}]},"droppedSensitive":[]}""",
        )

        assertThat(response.profile.summary).isEqualTo("S")
        assertThat(response.profile.location).isEqualTo("Pune")
        assertThat(response.profile.links.map { it.kind }).containsExactly(LinkKind.LINKEDIN, LinkKind.UNKNOWN).inOrder()
        assertThat(response.profile.entries.single().current).isTrue()
    }

    @Test
    fun analysisResponseDecodesLocationReasonAndQuestion() {
        val response = json.decodeFromString<AnalysisResponse>(
            """{"generationId":"g","job":{"title":"T","company":"C","location":"Pune","requirements":[]},
"matches":[{"requirementId":"req-1","status":"GAP","evidenceIds":[],"reason":"Not clear."}],
"question":{"requirementId":"req-1","text":"Have you led?","why":"Role asks.","options":["YES_REGULARLY"]},
"allowance":{"analysesLeftToday":1,"day":"2026-10-10","resetsAt":"2026-10-10T18:30:00Z"}}""",
        )

        assertThat(response.job.location).isEqualTo("Pune")
        assertThat(response.matches.single().reason).isEqualTo("Not clear.")
        assertThat(response.question?.text).isEqualTo("Have you led?")
    }

    @Test
    fun unknownFailureCodeAndVerificationDecodeLeniently() {
        val failed = json.decodeFromString<TailoringResponse>(
            """{"tailoring":{"id":"t","status":"FAILED","failureCode":"BRAND_NEW"}}""",
        )
        val summary = json.decodeFromString<TailoringResponse>(
            """{"tailoring":{"id":"t","status":"SUCCEEDED","result":{"generationId":"g","bullets":[],
"summary":{"text":"S","sourceIds":["summary"],"verification":"NEW_KIND"}}}}""",
        )

        assertThat(failed.tailoring.failureCode).isEqualTo(TailoringFailureCode.UNKNOWN)
        val result = checkNotNull(summary.tailoring.result)
        assertThat(result.summary?.verification).isEqualTo(BulletVerification.UNKNOWN)
        assertThat(result.skills).isNull()
    }
}
