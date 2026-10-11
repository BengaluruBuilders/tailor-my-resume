package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.dto.AnalysisResponse
import com.tailormyresume.core.network.dto.ContentReportRequest
import com.tailormyresume.core.network.dto.ContentReportResponse
import com.tailormyresume.core.network.dto.CreditsResponse
import com.tailormyresume.core.network.dto.DeletionResponse
import com.tailormyresume.core.network.dto.JobDto
import com.tailormyresume.core.network.dto.MatchDto
import com.tailormyresume.core.network.dto.MeResponse
import com.tailormyresume.core.network.dto.PacksResponse
import com.tailormyresume.core.network.dto.ProfileFactsDto
import com.tailormyresume.core.network.dto.PurchaseRequest
import com.tailormyresume.core.network.dto.PurchaseResponse
import com.tailormyresume.core.network.dto.PurchasesResponse
import com.tailormyresume.core.network.dto.ResumeParseRequest
import com.tailormyresume.core.network.dto.ResumeParseResponse
import com.tailormyresume.core.network.dto.ServerExportResponse
import com.tailormyresume.core.network.dto.TailoringResponse
import com.tailormyresume.core.network.dto.TailoringStartRequest
import com.tailormyresume.core.network.dto.WalletResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.junit.Test

class DtoRoundTripTest {
    private val json = tailormyresumeJson()
    private val plain = Json

    @Test
    fun everyContractExampleRoundTrips() {
        roundTrip<ProfileFactsDto>("profile-facts")
        roundTrip<JobDto>("job")
        roundTrip<MatchDto>("match")
        roundTrip<MeResponse>("me-response")
        roundTrip<DeletionResponse>("deletion-response")
        roundTrip<ResumeParseRequest>("resume-parse-request")
        roundTrip<ResumeParseResponse>("resume-parse-response")
        roundTrip<AnalysisRequest>("analysis-request")
        roundTrip<AnalysisResponse>("analysis-response")
        roundTrip<TailoringStartRequest>("tailoring-start-request")
        roundTrip<TailoringResponse>("tailoring-start-response")
        roundTrip<TailoringResponse>("tailoring-poll-response")
        roundTrip<WalletResponse>("wallet-response")
        roundTrip<CreditsResponse>("credits-response")
        roundTrip<PacksResponse>("packs-response")
        roundTrip<PurchaseRequest>("purchase-request")
        roundTrip<PurchaseResponse>("purchase-response")
        roundTrip<PurchasesResponse>("purchases-response")
        roundTrip<ContentReportRequest>("content-report-request")
        roundTrip<ContentReportResponse>("content-report-response")
        roundTrip<ServerExportResponse>("me-export-response")
    }

    @Test
    fun tailoringRequestsOmitNullOptionalFieldsAndNeverSendSection() {
        val request = decode<TailoringStartRequest>("tailoring-start-request")
        val wire = json.encodeToString(TailoringStartRequest.serializer(), request)
        assertThat(request.answer).isNull()
        assertThat(wire).doesNotContain("section")
        assertThat(wire).doesNotContain("answer")
        assertThat(wire).doesNotContain("null")
    }

    private inline fun <reified T> roundTrip(name: String) {
        val text = checkNotNull(javaClass.getResource("/contract/$name.json")) { name }.readText()
        val serializer: KSerializer<T> = serializer()
        val decoded = json.decodeFromString(serializer, text)
        val encoded = json.encodeToJsonElement(serializer, decoded)
        assertThat(encoded.withoutNulls()).isEqualTo(plain.parseToJsonElement(text).withoutNulls())
        assertThat(json.decodeFromJsonElement(serializer, encoded)).isEqualTo(decoded)
    }

    private inline fun <reified T> decode(name: String): T =
        json.decodeFromString(checkNotNull(javaClass.getResource("/contract/$name.json")).readText())

    private fun JsonElement.withoutNulls(): JsonElement = when (this) {
        is JsonObject -> JsonObject(filterValues { it !is JsonNull }.mapValues { it.value.withoutNulls() })
        is JsonArray -> JsonArray(map { it.withoutNulls() })
        is JsonPrimitive -> this
    }
}
